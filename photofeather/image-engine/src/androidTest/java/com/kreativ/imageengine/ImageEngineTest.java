package com.kreativ.imageengine;

import static org.junit.Assert.*;

import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import org.junit.*;

public class ImageEngineTest {
  File directory;
  ImageEngine engine = new ImageEngine();

  @Before
  public void setup() {
    directory =
        new File(
            InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),
            "engine-" + UUID.randomUUID());
    directory.mkdirs();
  }

  @After
  public void clean() {
    File[] files = directory.listFiles();
    if (files != null) for (File f : files) f.delete();
    directory.delete();
  }

  File image(int w, int h, boolean png, boolean noise) throws Exception {
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bitmap);
    c.drawColor(png ? Color.TRANSPARENT : Color.rgb(40, 120, 90));
    if (noise) {
      Random random = new Random(42);
      int[] row = new int[w];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) row[x] = 0xff000000 | random.nextInt(0x1000000);
        bitmap.setPixels(row, 0, w, 0, y, w, 1);
      }
    } else {
      Paint paint = new Paint();
      paint.setColor(Color.RED);
      c.drawRect(0, 0, w / 2f, h / 2f, paint);
      paint.setColor(Color.BLUE);
      c.drawRect(w / 2f, 0, w, h / 2f, paint);
    }
    File file = new File(directory, UUID.randomUUID() + (png ? ".png" : ".jpg"));
    try (FileOutputStream out = new FileOutputStream(file)) {
      bitmap.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 95, out);
    }
    bitmap.recycle();
    return file;
  }

  File out() {
    return new File(directory, UUID.randomUUID() + ".jpg");
  }

  CompressionOptions options(long target, int edge) {
    return new CompressionOptions(target, edge, 85, Color.WHITE, true);
  }

  byte[] hash(File f) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
  }

  @Test
  public void progressiveJpegIsAcceptedAndItsTruncatedVersionIsRejected() throws Exception {
    File source = new File(directory, "progressive.jpg");
    try (InputStream in =
            InstrumentationRegistry.getInstrumentation()
                .getContext()
                .getAssets()
                .open("progressive.jpg");
        OutputStream out = new FileOutputStream(source)) {
      byte[] buffer = new byte[8192];
      int count;
      while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
    }
    ImageEngine.Result result = engine.process(source, out(), options(200000, 600), () -> {});
    assertEquals(600, result.width);
    assertEquals(400, result.height);
    assertTrue(result.targetReached);
    byte[] complete = Files.readAllBytes(source.toPath());
    Files.write(source.toPath(), Arrays.copyOf(complete, complete.length / 2));
    try {
      engine.process(source, out(), options(200000, 600), () -> {});
      fail("Truncated progressive JPEG accepted");
    } catch (IOException expected) {
    }
  }

  @Test
  public void targetBytesAreMeasuredAndOriginalUnchanged() throws Exception {
    File f = image(1800, 1200, false, true);
    byte[] original = hash(f);
    ImageEngine.Result r = engine.process(f, out(), options(200_000, 1800), () -> {});
    assertTrue(r.targetReached);
    assertTrue(r.bytes <= 200_000);
    assertEquals(r.file.length(), r.bytes);
    assertTrue(r.attempts <= 35);
    assertArrayEquals(original, hash(f));
    ImageEngine.verifyOutput(r.file, r.width, r.height);
  }

  @Test
  public void allExifOrientationsBecomeUprightPixels() throws Exception {
    for (int orientation = 1; orientation <= 8; orientation++) {
      File f = image(800, 400, false, false);
      ExifInterface exif = new ExifInterface(f);
      exif.setAttribute(ExifInterface.TAG_ORIENTATION, "" + orientation);
      exif.saveAttributes();
      byte[] original = hash(f);
      ImageEngine.Result r = engine.process(f, out(), options(0, 1000), () -> {});
      assertEquals(orientation >= 5 ? 400 : 800, r.width);
      assertEquals(orientation >= 5 ? 800 : 400, r.height);
      ExifInterface clean = new ExifInterface(r.file);
      assertTrue(clean.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0) <= 1);
      // Independent EXIF corner mapping, rather than using the production matrix as the oracle.
      int[] redCorner = {0, 1, 3, 2, 0, 1, 3, 2}, blueCorner = {1, 0, 2, 3, 2, 3, 1, 0};
      Bitmap actual = BitmapFactory.decodeFile(r.file.getPath());
      int red = redCorner[orientation - 1], blue = blueCorner[orientation - 1];
      int redPixel =
          actual.getPixel(
              (int) (actual.getWidth() * (red % 2 == 0 ? .25f : .75f)),
              (int) (actual.getHeight() * (red < 2 ? .25f : .75f)));
      int bluePixel =
          actual.getPixel(
              (int) (actual.getWidth() * (blue % 2 == 0 ? .25f : .75f)),
              (int) (actual.getHeight() * (blue < 2 ? .25f : .75f)));
      assertTrue(
          "Red corner orientation " + orientation,
          Color.red(redPixel) > 220 && Color.blue(redPixel) < 20);
      assertTrue(
          "Blue corner orientation " + orientation,
          Color.blue(bluePixel) > 220 && Color.red(bluePixel) < 20);
      actual.recycle();
      assertArrayEquals(original, hash(f));
    }
  }

  @Test
  public void largeInputIsMemoryBounded() throws Exception {
    File f = image(6000, 4000, false, false);
    ImageEngine.Result r = engine.process(f, out(), options(0, 8192), () -> {});
    assertTrue((long) r.width * r.height <= ResizePolicy.MAX_PIXELS);
    assertEquals(3000, r.width);
    assertEquals(2000, r.height);
  }

  @Test
  public void transparencyRequiresConsentAndUsesChosenBackground() throws Exception {
    File f = image(400, 400, true, false);
    try {
      engine.process(f, out(), new CompressionOptions(0, 400, 85, Color.BLACK, false), () -> {});
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("Confirm"));
    }
    ImageEngine.Result r =
        engine.process(f, out(), new CompressionOptions(0, 400, 85, Color.BLACK, true), () -> {});
    Bitmap b = BitmapFactory.decodeFile(r.file.getPath());
    assertTrue(Color.red(b.getPixel(300, 300)) < 5);
    assertEquals(255, Color.alpha(b.getPixel(300, 300)));
    b.recycle();
  }

  @Test
  public void alreadyTinyImageReportsNoBenefit() throws Exception {
    File f = image(16, 16, true, false);
    ImageEngine.Result r = engine.process(f, out(), options(500_000, 2560), () -> {});
    assertFalse(r.smaller);
    assertEquals(16, r.width);
    assertEquals(16, r.height);
  }

  @Test
  public void impossibleTargetIsHonestAndBounded() throws Exception {
    File f = image(700, 700, false, true);
    ImageEngine.Result r = engine.process(f, out(), options(1000, 700), () -> {});
    assertFalse(r.targetReached);
    assertTrue(r.bytes > 1000);
    assertEquals(320, r.width);
    assertTrue(r.attempts <= 35);
  }

  @Test
  public void corruptImageFailsWithoutOutput() throws Exception {
    File f = new File(directory, "bad.jpg");
    Files.write(f.toPath(), new byte[] {1, 2, 3, 4});
    File out = out();
    try {
      engine.process(f, out, options(200_000, 2560), () -> {});
      fail();
    } catch (IOException expected) {
      assertFalse(out.exists());
    }
  }

  @Test
  public void lowStorageIsExplicitAndLeavesOriginal() throws Exception {
    File f = image(400, 300, false, false);
    byte[] original = hash(f);
    File out = out();
    try {
      new ImageEngine(dir -> 0).process(f, out, options(200_000, 400), () -> {});
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("Low storage"));
      assertFalse(out.exists());
      assertArrayEquals(original, hash(f));
    }
  }

  @Test
  public void cancellationCleansPartialFiles() throws Exception {
    File f = image(1400, 900, false, true);
    File out = out();
    final int[] calls = {0};
    try {
      engine.process(
          f,
          out,
          options(1000, 1400),
          () -> {
            if (++calls[0] >= 6) throw new CancellationException();
          });
      fail();
    } catch (CancellationException expected) {
      assertFalse(out.exists());
      assertFalse(new File(out + ".raw").exists());
      assertFalse(new File(out + ".candidate").exists());
    }
  }

  @Test
  public void sourceMetadataIsRemovedAndOutputReopens() throws Exception {
    File f = image(800, 600, false, false);
    ExifInterface exif = new ExifInterface(f);
    exif.setLatLong(44.4, 26.1);
    exif.setAttribute(ExifInterface.TAG_MAKE, "Test camera");
    exif.setAttribute(ExifInterface.TAG_MODEL, "Private model");
    exif.setAttribute(ExifInterface.TAG_DATETIME, "2026:10:22 10:00:00");
    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Private comment");
    exif.saveAttributes();
    byte[] original = hash(f);
    ImageEngine.Result r = engine.process(f, out(), options(500_000, 800), () -> {});
    JpegMetadata.verify(r.file);
    ExifInterface check = new ExifInterface(r.file);
    for (String tag :
        new String[] {
          ExifInterface.TAG_GPS_LATITUDE,
          ExifInterface.TAG_GPS_LONGITUDE,
          ExifInterface.TAG_MAKE,
          ExifInterface.TAG_MODEL,
          ExifInterface.TAG_DATETIME,
          ExifInterface.TAG_USER_COMMENT
        }) assertNull(tag, check.getAttribute(tag));
    assertArrayEquals(original, hash(f));
    ImageEngine.verifyOutput(r.file, r.width, r.height);
  }

  @Test
  public void animatedPngIsExplicitlyRejected() throws Exception {
    File source = image(400, 300, true, false);
    byte[] png = Files.readAllBytes(source.toPath());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    bytes.write(png, 0, 33);
    DataOutputStream d = new DataOutputStream(bytes);
    d.writeInt(8);
    byte[] chunk = {97, 99, 84, 76, 0, 0, 0, 2, 0, 0, 0, 0};
    d.write(chunk);
    java.util.zip.CRC32 crc = new java.util.zip.CRC32();
    crc.update(chunk);
    d.writeInt((int) crc.getValue());
    bytes.write(png, 33, png.length - 33);
    Files.write(source.toPath(), bytes.toByteArray());
    try {
      engine.inspect(source);
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("Animated"));
    }
  }

  @Test
  public void staticWebPIsSupported() throws Exception {
    File source = image(400, 300, false, false);
    Bitmap bitmap = BitmapFactory.decodeFile(source.getPath());
    File webp = new File(directory, "still.webp");
    try (FileOutputStream output = new FileOutputStream(webp)) {
      bitmap.compress(Bitmap.CompressFormat.WEBP, 100, output);
    }
    bitmap.recycle();
    ImageEngine.Result result = engine.process(webp, out(), options(0, 200), () -> {});
    assertEquals(200, result.width);
    assertEquals(150, result.height);
  }

  @Test
  public void resizeOnlyHonoursMaximumQualityAndShape() throws Exception {
    File source = image(1200, 600, false, true);
    ImageEngine.Result result =
        engine.process(
            source, out(), new CompressionOptions(0, 600, 72, Color.WHITE, false), () -> {});
    assertEquals(600, result.width);
    assertEquals(300, result.height);
    assertEquals(72, result.quality);
    assertEquals(1, result.attempts);
  }

  @Test
  public void resizePresetsMatchMeasuredJpegDimensions() throws Exception {
    File f = image(2400, 1800, false, false);
    byte[] original = hash(f);
    for (int edge : new int[] {1280, 1920, 2560}) {
      int[] preview = ResizePolicy.previewSize(2400, 1800, edge);
      ImageEngine.Result r = engine.process(f, out(), options(0, edge), () -> {});
      assertEquals(preview[0], r.width);
      assertEquals(preview[1], r.height);
      assertEquals(r.bytes, r.file.length());
      ImageEngine.verifyOutput(r.file, r.width, r.height);
    }
    assertArrayEquals(original, hash(f));
  }

  @Test
  public void customResizeMatchesOrientedPortraitPreview() throws Exception {
    File f = image(2400, 1800, false, false);
    ExifInterface exif = new ExifInterface(f);
    exif.setAttribute(ExifInterface.TAG_ORIENTATION, "6");
    exif.saveAttributes();
    InfoCheck(f, 1500, 1125, 1500);
  }

  void InfoCheck(File f, int edge, int width, int height) throws Exception {
    ImageEngine.Info info = engine.inspect(f);
    int[] preview = ResizePolicy.previewSize(info.width, info.height, edge);
    ImageEngine.Result r = engine.process(f, out(), options(0, edge), () -> {});
    assertEquals(width, r.width);
    assertEquals(height, r.height);
    assertEquals(preview[0], r.width);
    assertEquals(preview[1], r.height);
  }

  @Test
  public void memoryLimitedLargeImageMatchesTheResizePreview() throws Exception {
    File f = image(4000, 3000, false, false);
    InfoCheck(f, 2560, 2000, 1500);
  }

  @Test
  public void oddPngAndWebpOutputsStayWithinPlannedBounds() throws Exception {
    File png = image(4001, 3000, true, false);
    for (File f : new File[] {png, webpCopy(png)}) {
      ImageEngine.Info info = engine.inspect(f);
      int[] bound = ResizePolicy.previewSize(info.width, info.height, 1280);
      ImageEngine.Result r = engine.process(f, out(), options(0, 1280), () -> {});
      assertTrue("width", r.width <= bound[0]);
      assertTrue("height", r.height <= bound[1]);
      assertTrue(Math.abs(r.width / (double) r.height - info.width / (double) info.height) < .005);
      ImageEngine.verifyOutput(r.file, r.width, r.height);
    }
  }

  File webpCopy(File png) throws Exception {
    Bitmap b = BitmapFactory.decodeFile(png.getPath());
    File f = new File(directory, "odd.webp");
    try (FileOutputStream out = new FileOutputStream(f)) {
      assertTrue(b.compress(Bitmap.CompressFormat.WEBP, 90, out));
    }
    b.recycle();
    return f;
  }

  @Test
  public void byteTargetMayReduceDimensionsBelowResizePreview() throws Exception {
    File f = image(1600, 1200, false, true);
    int[] preview = ResizePolicy.previewSize(1600, 1200, 1280);
    ImageEngine.Result r = engine.process(f, out(), options(1000, 1280), () -> {});
    assertTrue(r.width < preview[0]);
    assertTrue(r.height < preview[1]);
    assertEquals(r.file.length(), r.bytes);
  }
}
