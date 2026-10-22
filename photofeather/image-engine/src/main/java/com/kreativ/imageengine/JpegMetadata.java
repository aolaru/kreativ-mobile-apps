package com.kreativ.imageengine;

import java.io.*;

/**
 * Keeps JPEG coding data only; removes ALL APP0–APP15 and COM segments, including EXIF, XMP, ICC
 * and comments. Accepts the single-scan baseline JPEG produced by Android's Bitmap encoder. Fails
 * closed otherwise.
 */
public final class JpegMetadata {
  private JpegMetadata() {}

  public static void strip(File input, File output) throws IOException {
    try (DataInputStream in =
            new DataInputStream(new BufferedInputStream(new FileInputStream(input)));
        DataOutputStream out =
            new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
      parse(in, out, false);
    }
  }

  public static void verify(File file) throws IOException {
    try (DataInputStream in =
        new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      parse(in, null, true);
    }
  }

  private static void parse(DataInputStream in, DataOutputStream out, boolean verify)
      throws IOException {
    if (in.readUnsignedShort() != 0xFFD8) throw new IOException("Invalid JPEG header");
    if (out != null) out.writeShort(0xFFD8);
    boolean frame = false;
    for (int segments = 0; segments < 100; segments++) {
      if (in.readUnsignedByte() != 255) throw new IOException("Invalid JPEG segment");
      int marker;
      do {
        marker = in.readUnsignedByte();
      } while (marker == 255);
      if (marker == 0 || marker == 0xD8 || marker == 0xD9) throw new IOException("Incomplete JPEG");
      int length = in.readUnsignedShort();
      if (length < 2) throw new IOException("Invalid JPEG segment length");
      byte[] data = new byte[length - 2];
      in.readFully(data);
      boolean metadata = (marker >= 0xE0 && marker <= 0xEF) || marker == 0xFE;
      if (verify && metadata) throw new IOException("Unexpected metadata; copy withheld");
      if (!metadata && out != null) {
        out.writeShort(0xFF00 | marker);
        out.writeShort(length);
        out.write(data);
      }
      if (marker == 0xC0) frame = true;
      if (marker == 0xDA) {
        if (!frame) throw new IOException("Unsupported JPEG scan");
        // Inspect entropy data too: no unverified trailing APP, comment or secondary image.
        int value;
        while ((value = in.read()) != -1) {
          if (out != null) out.write(value);
          if (value != 255) continue;
          int next = in.readUnsignedByte();
          if (out != null) out.write(next);
          if (next == 0 || (next >= 0xD0 && next <= 0xD7)) continue;
          if (next == 0xD9 && in.read() == -1) return;
          throw new IOException("Unexpected JPEG scan data");
        }
        throw new IOException("Truncated JPEG");
      }
    }
    throw new IOException("Too many JPEG segments");
  }
}
