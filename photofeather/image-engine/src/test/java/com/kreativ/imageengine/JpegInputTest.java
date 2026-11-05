package com.kreativ.imageengine;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Test;

public class JpegInputTest {
  byte[] stream(boolean progressive) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream d = new DataOutputStream(bytes);
    d.writeShort(0xFFD8);
    d.writeShort(progressive ? 0xFFC2 : 0xFFC0);
    d.writeShort(11);
    d.write(new byte[] {8, 0, 16, 0, 16, 1, 1, 0x11, 0});
    for (int i = 0; i < (progressive ? 2 : 1); i++) {
      d.writeShort(0xFFDA);
      d.writeShort(8);
      d.write(new byte[] {1, 1, 0, 0, 63, 0});
      d.write(new byte[] {1, 2, (byte) 255, 0, 3, (byte) 255, (byte) 0xD0, 4});
    }
    d.writeShort(0xFFD9);
    return bytes.toByteArray();
  }

  void verify(byte[] bytes) throws IOException {
    File f = File.createTempFile("source-jpeg", ".jpg");
    try {
      Files.write(f.toPath(), bytes);
      JpegInput.verify(f);
    } finally {
      f.delete();
    }
  }

  @Test
  public void baselineMarkersAndStuffedScanAreAccepted() throws Exception {
    verify(stream(false));
  }

  @Test
  public void progressiveMultipleScansAreAccepted() throws Exception {
    verify(stream(true));
  }

  @Test
  public void missingEndOfImageIsRejected() throws Exception {
    byte[] bytes = stream(false);
    try {
      verify(Arrays.copyOf(bytes, bytes.length - 2));
      fail();
    } catch (IOException e) {
      assertTrue(e.getMessage().contains("damaged JPEG"));
    }
  }

  @Test
  public void scanTruncationIsRejected() throws Exception {
    byte[] bytes = stream(true);
    try {
      verify(Arrays.copyOf(bytes, bytes.length / 2));
      fail();
    } catch (IOException expected) {
    }
  }

  @Test
  public void segmentTruncationIsRejected() throws Exception {
    try {
      verify(new byte[] {(byte) 255, (byte) 216, (byte) 255, (byte) 224, 0, 25, 1});
      fail();
    } catch (IOException expected) {
    }
  }

  @Test
  public void trailingDataDoesNotRejectACompleteFirstImage() throws Exception {
    byte[] bytes = stream(false);
    verify(Arrays.copyOf(bytes, bytes.length + 16));
  }
}
