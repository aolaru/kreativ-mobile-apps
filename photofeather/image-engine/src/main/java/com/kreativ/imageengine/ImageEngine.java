package com.kreativ.imageengine;

import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CancellationException;

/** No Activities, providers, export policy or network. One image at a time, at most 35 encodes. */
public final class ImageEngine {
  public interface Cancellation {
    void check() throws CancellationException;
  }

  public interface DiskSpace {
    long available(File directory);
  }

  private final DiskSpace space;

  public ImageEngine() {
    this(File::getUsableSpace);
  }

  public ImageEngine(DiskSpace space) {
    this.space = space;
  }

  public static final class Info {
    public final int width, height, orientation;
    public final String mime;
    public final long bytes;
    public final boolean mayHaveAlpha;

    Info(int w, int h, int orientation, String mime, long bytes, boolean alpha) {
      width = w;
      height = h;
      this.orientation = orientation;
      this.mime = mime;
      this.bytes = bytes;
      mayHaveAlpha = alpha;
    }
  }

  public static final class Result {
    public final File file;
    public final Info original;
    public final int width, height, quality, attempts;
    public final long bytes;
    public final boolean targetReached, smaller;

    Result(File f, Info i, int w, int h, int q, int attempts, boolean reached) {
      file = f;
      original = i;
      width = w;
      height = h;
      quality = q;
      this.attempts = attempts;
      bytes = f.length();
      targetReached = reached;
      smaller = bytes < i.bytes;
    }
  }

  public Info inspect(File input) throws IOException {
    BitmapFactory.Options b = new BitmapFactory.Options();
    b.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(input.getPath(), b);
    if (b.outWidth <= 0 || b.outHeight <= 0)
      throw new IOException("Unreadable or corrupt image. Select another photo.");
    if (!"image/jpeg".equals(b.outMimeType)
        && !"image/png".equals(b.outMimeType)
        && !"image/webp".equals(b.outMimeType))
      throw new IOException("Unsupported format. Choose JPEG, PNG or static WebP.");
    if ("image/jpeg".equals(b.outMimeType)) JpegInput.verify(input);
    if (isAnimated(input, b.outMimeType))
      throw new IOException("Animated PNG and WebP are not supported. Choose a still image.");
    int o = 1;
    // Orientation is necessary for correctness; unreadable JPEG EXIF fails explicitly.
    try {
      o = new ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
    } catch (IOException e) {
      if ("image/jpeg".equals(b.outMimeType))
        throw new IOException("Image orientation could not be read.", e);
    }
    boolean swap = o >= 5 && o <= 8;
    return new Info(
        swap ? b.outHeight : b.outWidth,
        swap ? b.outWidth : b.outHeight,
        o,
        b.outMimeType,
        input.length(),
        !"image/jpeg".equals(b.outMimeType));
  }

  private boolean isAnimated(File f, String mime) throws IOException {
    if ("image/jpeg".equals(mime)) return false;
    try (RandomAccessFile in = new RandomAccessFile(f, "r")) {
      if ("image/png".equals(mime)) {
        in.seek(8);
        for (int n = 0; n < 10000 && in.getFilePointer() + 12 <= in.length(); n++) {
          long len = Integer.toUnsignedLong(in.readInt());
          int type = in.readInt();
          if (type == 0x6163544C) return true; // acTL before IDAT
          if (type == 0x49444154) return false;
          long next = in.getFilePointer() + len + 4;
          if (next > in.length()) throw new IOException("Corrupt PNG");
          in.seek(next);
        }
      } else {
        in.seek(12);
        for (int n = 0; n < 10000 && in.getFilePointer() + 8 <= in.length(); n++) {
          int type = in.readInt();
          long len = Integer.toUnsignedLong(Integer.reverseBytes(in.readInt()));
          if (type == 0x414E494D || type == 0x414E4D46) return true;
          if (type == 0x56503858 && len >= 1) {
            int flags = in.readUnsignedByte();
            if ((flags & 2) != 0) return true;
            in.seek(in.getFilePointer() - 1);
          }
          long next = in.getFilePointer() + len + (len & 1);
          if (next > in.length()) throw new IOException("Corrupt WebP");
          in.seek(next);
        }
      }
    }
    return false;
  }

  public Result process(File source, File output, CompressionOptions options, Cancellation cancel)
      throws IOException {
    cancel.check();
    Info info = inspect(source);
    if (info.mayHaveAlpha && !options.allowFlatten)
      throw new IOException("Confirm a background colour before converting PNG or WebP to JPEG.");
    requireSpace(output.getParentFile(), 32_000_000);
    Bitmap bitmap = null;
    File raw = new File(output.getPath() + ".raw"),
        candidate = new File(output.getPath() + ".candidate");
    boolean complete = false;
    try {
      BitmapFactory.Options decode = new BitmapFactory.Options();
      decode.inSampleSize = ResizePolicy.sample(info.width, info.height);
      decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
      bitmap = BitmapFactory.decodeFile(source.getPath(), decode);
      if (bitmap == null) throw new IOException("Could not decode image pixels.");
      cancel.check();
      Matrix matrix = orientationMatrix(info.orientation);
      if (!matrix.isIdentity()) {
        Bitmap oriented =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (oriented != bitmap) bitmap.recycle();
        bitmap = oriented;
      }
      int[] fit =
          ResizePolicy.fit(
              bitmap.getWidth(), bitmap.getHeight(), options.longestEdge, ResizePolicy.MAX_PIXELS);
      if (fit[0] != bitmap.getWidth() || fit[1] != bitmap.getHeight()) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, fit[0], fit[1], true);
        if (scaled != bitmap) bitmap.recycle();
        bitmap = scaled;
      }
      if (bitmap.hasAlpha()) {
        Bitmap flat =
            Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(flat);
        canvas.drawColor(options.background);
        canvas.drawBitmap(bitmap, 0, 0, null);
        bitmap.recycle();
        bitmap = flat;
      }
      int initial = Math.max(bitmap.getWidth(), bitmap.getHeight()),
          attempts = 0,
          finalQ = options.maxQuality;
      boolean reached = false;
      for (int level = 0; level < 5; level++) {
        cancel.check();
        int edge = ResizePolicy.attemptEdge(initial, level);
        int[] dims =
            ResizePolicy.fit(bitmap.getWidth(), bitmap.getHeight(), edge, ResizePolicy.MAX_PIXELS);
        if (dims[0] != bitmap.getWidth() || dims[1] != bitmap.getHeight()) {
          Bitmap small = Bitmap.createScaledBitmap(bitmap, dims[0], dims[1], true);
          if (small != bitmap) bitmap.recycle();
          bitmap = small;
        }
        long bytes = encode(bitmap, options.maxQuality, raw, candidate, cancel);
        attempts++;
        finalQ = options.maxQuality;
        Files.move(candidate.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (options.targetBytes == 0 || bytes <= options.targetBytes) {
          reached = true;
          break;
        }
        int low = Math.min(40, options.maxQuality), high = options.maxQuality - 1;
        bytes = encode(bitmap, low, raw, candidate, cancel);
        attempts++;
        finalQ = low;
        Files.move(candidate.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (bytes <= options.targetBytes) {
          reached = true;
          low++;
          for (int search = 0; search < 5 && low <= high; search++) {
            int mid = (low + high) / 2;
            bytes = encode(bitmap, mid, raw, candidate, cancel);
            attempts++;
            if (bytes <= options.targetBytes) {
              finalQ = mid;
              Files.move(candidate.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
              low = mid + 1;
            } else high = mid - 1;
          }
          break;
        }
        if (edge <= Math.min(initial, 320)) break;
      }
      cancel.check();
      verifyOutput(output, bitmap.getWidth(), bitmap.getHeight());
      reached = options.targetBytes == 0 || output.length() <= options.targetBytes;
      complete = true;
      return new Result(
          output, info, bitmap.getWidth(), bitmap.getHeight(), finalQ, attempts, reached);
    } catch (OutOfMemoryError e) {
      throw new IOException("Not enough memory for this photo. Try a smaller image.", e);
    } finally {
      if (bitmap != null) bitmap.recycle();
      raw.delete();
      candidate.delete();
      if (!complete) output.delete();
    }
  }

  private long encode(Bitmap bitmap, int q, File raw, File candidate, Cancellation cancel)
      throws IOException {
    cancel.check();
    requireSpace(
        raw.getParentFile(),
        Math.max(8_000_000, (long) bitmap.getWidth() * bitmap.getHeight() * 4));
    try (FileOutputStream out = new FileOutputStream(raw)) {
      if (!bitmap.compress(Bitmap.CompressFormat.JPEG, q, out))
        throw new IOException("JPEG encoding failed.");
      out.getFD().sync();
    }
    cancel.check();
    JpegMetadata.strip(raw, candidate);
    return candidate.length();
  }

  public void requireSpace(File dir, long bytes) throws IOException {
    if (space.available(dir) < bytes + 2_000_000)
      throw new IOException("Low storage. Free some device space and try again.");
  }

  public static void verifyOutput(File file, int width, int height) throws IOException {
    JpegMetadata.verify(file);
    BitmapFactory.Options b = new BitmapFactory.Options();
    b.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getPath(), b);
    if (b.outWidth != width || b.outHeight != height || !"image/jpeg".equals(b.outMimeType))
      throw new IOException("Output verification failed; copy withheld.");
    BitmapFactory.Options small = new BitmapFactory.Options();
    small.inSampleSize = ResizePolicy.sample(width, height) * 4;
    Bitmap check = BitmapFactory.decodeFile(file.getPath(), small);
    if (check == null) throw new IOException("Output cannot be reopened; copy withheld.");
    check.recycle();
  }

  public static Matrix orientationMatrix(int o) {
    Matrix m = new Matrix();
    switch (o) {
      case 2:
        m.setScale(-1, 1);
        break;
      case 3:
        m.setRotate(180);
        break;
      case 4:
        m.setScale(1, -1);
        break;
      case 5:
        m.setRotate(90);
        m.postScale(-1, 1);
        break;
      case 6:
        m.setRotate(90);
        break;
      case 7:
        m.setRotate(-90);
        m.postScale(-1, 1);
        break;
      case 8:
        m.setRotate(-90);
        break;
      default:
        break;
    }
    return m;
  }

  public static Bitmap preview(File file, int orientation, int maxEdge) throws IOException {
    BitmapFactory.Options b = new BitmapFactory.Options();
    b.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getPath(), b);
    int sample = 1;
    while (Math.max(b.outWidth, b.outHeight) / sample > maxEdge) sample *= 2;
    b = new BitmapFactory.Options();
    b.inSampleSize = sample;
    b.inPreferredConfig = Bitmap.Config.ARGB_8888;
    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), b);
    if (bitmap == null) throw new IOException("Preview unavailable.");
    Matrix m = orientationMatrix(orientation);
    if (!m.isIdentity()) {
      Bitmap upright =
          Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
      if (upright != bitmap) bitmap.recycle();
      bitmap = upright;
    }
    return bitmap;
  }
}
