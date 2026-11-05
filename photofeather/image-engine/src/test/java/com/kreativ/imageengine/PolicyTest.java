package com.kreativ.imageengine;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import org.junit.Test;

public class PolicyTest {
  @Test
  public void portraitAspectRatio() {
    assertArrayEquals(new int[] {1000, 2000}, ResizePolicy.fit(3000, 6000, 2000, 8_000_000));
  }

  @Test
  public void landscapeAspectRatio() {
    assertArrayEquals(new int[] {2000, 1000}, ResizePolicy.fit(6000, 3000, 2000, 8_000_000));
  }

  @Test
  public void neverUpscales() {
    assertArrayEquals(new int[] {31, 15}, ResizePolicy.fit(31, 15, 2560, 8_000_000));
  }

  @Test
  public void memoryBoundHandlesHugeDimensions() {
    int[] size = ResizePolicy.fit(100000, 100000, 8192, 8_000_000);
    assertTrue((long) size[0] * size[1] <= 8_000_000);
    assertTrue(ResizePolicy.sample(100000, 100000) > 1);
  }

  @Test
  public void narrowImagesRetainAtLeastOnePixel() {
    assertArrayEquals(new int[] {64, 1}, ResizePolicy.fit(100000, 1, 64, 8_000_000));
  }

  @Test
  public void exactDecimalUnits() {
    assertEquals(200_000, CompressionOptions.parseKilobytes("200"));
    assertEquals(1_000_000, CompressionOptions.parseKilobytes("1000"));
    assertEquals(50_000_000, CompressionOptions.parseKilobytes("50000"));
  }

  @Test
  public void rejectsInvalidCustomSizes() {
    for (String v :
        new String[] {"", "0", "-1", "1.5", " 200", "50001", "9999999999999999", "hello"}) {
      try {
        CompressionOptions.parseKilobytes(v);
        fail(v);
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void rejectsInvalidOptions() {
    int[][] values = {{0, 63, 85}, {0, 8193, 85}, {0, 2560, 9}, {0, 2560, 101}, {999, 2560, 85}};
    for (int[] v : values) {
      try {
        new CompressionOptions(v[0], v[1], v[2], 0xFFFFFFFF, true);
        fail();
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void boundedEdgesFinishAtMinimum() {
    assertEquals(2560, ResizePolicy.attemptEdge(2560, 0));
    assertEquals(320, ResizePolicy.attemptEdge(2560, 4));
    assertEquals(100, ResizePolicy.attemptEdge(100, 4));
    int last = 2560;
    for (int n = 0; n < 5; n++) {
      int edge = ResizePolicy.attemptEdge(2560, n);
      assertTrue(edge <= last);
      assertTrue(edge >= 320);
      last = edge;
    }
  }

  @Test
  public void metadataSegmentsStrippedAndVerified() throws Exception {
    File in = File.createTempFile("metadata", ".jpg"), out = File.createTempFile("clean", ".jpg");
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream d = new DataOutputStream(bytes);
      d.writeShort(0xffd8);
      for (int m = 0xe0; m <= 0xef; m++) {
        d.writeShort(0xff00 | m);
        d.writeShort(5);
        d.write(new byte[] {1, 2, 3});
      }
      d.writeShort(0xfffe);
      d.writeShort(3);
      d.writeByte(1);
      d.writeShort(0xffc0);
      d.writeShort(2);
      d.writeShort(0xffda);
      d.writeShort(2);
      d.write(new byte[] {4, 5, (byte) 255, 0, 6, (byte) 255, (byte) 217});
      Files.write(in.toPath(), bytes.toByteArray());
      try {
        JpegMetadata.verify(in);
        fail();
      } catch (IOException expected) {
      }
      JpegMetadata.strip(in, out);
      JpegMetadata.verify(out);
      assertEquals(17, out.length());
    } finally {
      in.delete();
      out.delete();
    }
  }

  @Test
  public void trailingMetadataIsRejected() throws Exception {
    File f = File.createTempFile("trailing", ".jpg");
    try {
      Files.write(
          f.toPath(),
          new byte[] {
            (byte) 255,
            (byte) 216,
            (byte) 255,
            (byte) 192,
            0,
            2,
            (byte) 255,
            (byte) 218,
            0,
            2,
            1,
            (byte) 255,
            (byte) 217,
            0
          });
      try {
        JpegMetadata.verify(f);
        fail();
      } catch (IOException expected) {
      }
    } finally {
      f.delete();
    }
  }

  @Test
  public void corruptJpegIsRejected() throws Exception {
    File f = File.createTempFile("broken", ".jpg");
    try {
      Files.write(f.toPath(), new byte[] {1, 2, 3});
      try {
        JpegMetadata.verify(f);
        fail();
      } catch (IOException expected) {
      }
    } finally {
      f.delete();
    }
  }
}
