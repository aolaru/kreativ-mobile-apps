package com.kreativ.imageengine;

import java.io.*;

/**
 * Structural validation of the first JPEG image, including progressive/multiple scans. Android's
 * pixel decoder may silently conceal missing scan data; require a complete codestream. APP/COM
 * contents and trailing bytes are permitted here and never copied into exports.
 */
public final class JpegInput {
  private JpegInput() {}

  public static void verify(File file) throws IOException {
    try (DataInputStream in =
        new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      if (in.readUnsignedShort() != 0xFFD8) throw damaged();
      boolean frame = false, scan = false;
      int pending = -1;
      for (int segments = 0; segments < 100_000; segments++) {
        int marker;
        if (pending >= 0) {
          marker = pending;
          pending = -1;
        } else {
          if (in.readUnsignedByte() != 255) throw damaged();
          do {
            marker = in.readUnsignedByte();
          } while (marker == 255);
        }
        if (marker == 0xD9) {
          if (!frame || !scan) throw damaged();
          return;
        }
        if (marker == 0 || marker == 0xD8 || (marker >= 0xD0 && marker <= 0xD7)) throw damaged();
        if (marker == 1) continue; // TEM is a standalone marker.
        int length = in.readUnsignedShort();
        if (length < 2) throw damaged();
        int payload = length - 2;
        if (marker >= 0xC0
            && marker <= 0xCF
            && marker != 0xC4
            && marker != 0xC8
            && marker != 0xCC) {
          if (payload < 6) throw damaged();
          in.readUnsignedByte();
          int height = in.readUnsignedShort(), width = in.readUnsignedShort();
          int components = in.readUnsignedByte();
          if (width == 0 || height == 0 || components == 0 || payload != 6 + 3 * components)
            throw damaged();
          payload -= 6;
          frame = true;
        }
        if (marker == 0xDA) {
          if (!frame || payload < 4) throw damaged();
          int components = in.readUnsignedByte();
          if (components == 0 || payload != 4 + 2 * components) throw damaged();
          payload--;
        }
        skipFully(in, payload);
        if (marker != 0xDA) continue;
        scan = true;
        // A scan ends at the next unstuffed, non-restart marker. It may be another SOS.
        while (true) {
          if (in.readUnsignedByte() != 255) continue;
          int next;
          do {
            next = in.readUnsignedByte();
          } while (next == 255);
          if (next == 0 || (next >= 0xD0 && next <= 0xD7)) continue;
          pending = next;
          break;
        }
      }
      throw damaged();
    } catch (EOFException e) {
      throw new IOException("Incomplete or damaged JPEG. Select an intact original photo.", e);
    }
  }

  private static void skipFully(DataInputStream in, int bytes) throws IOException {
    while (bytes > 0) {
      int skipped = in.skipBytes(bytes);
      if (skipped == 0) {
        in.readUnsignedByte();
        skipped = 1;
      }
      bytes -= skipped;
    }
  }

  private static IOException damaged() {
    return new IOException("Incomplete or damaged JPEG. Select an intact original photo.");
  }
}
