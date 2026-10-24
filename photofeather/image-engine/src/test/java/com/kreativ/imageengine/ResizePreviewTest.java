package com.kreativ.imageengine;

import static org.junit.Assert.*;

import org.junit.Test;

public class ResizePreviewTest {
  @Test
  public void presetsGiveExpectedLandscapeDimensions() {
    int[] edges = {1280, 1920, 2560};
    int[][] expected = {{1280, 960}, {1920, 1440}, {2560, 1920}};
    for (int i = 0; i < edges.length; i++)
      assertArrayEquals(expected[i], ResizePolicy.previewSize(3200, 2400, edges[i]));
  }

  @Test
  public void presetsGiveExpectedPortraitDimensions() {
    int[] edges = {1280, 1920, 2560};
    int[][] expected = {{960, 1280}, {1440, 1920}, {1920, 2560}};
    for (int i = 0; i < edges.length; i++)
      assertArrayEquals(expected[i], ResizePolicy.previewSize(2400, 3200, edges[i]));
  }

  @Test
  public void smallerPhotosAreNeverEnlarged() {
    for (int edge : new int[] {1280, 1920, 2560, 8192})
      assertArrayEquals(new int[] {640, 480}, ResizePolicy.previewSize(640, 480, edge));
    assertArrayEquals(new int[] {16, 12}, ResizePolicy.previewSize(16, 12, 64));
  }

  @Test
  public void previewIncludesSafeDecodeLimitInsteadOfPromisingUnreachableDimensions() {
    assertArrayEquals(new int[] {2000, 1500}, ResizePolicy.previewSize(4000, 3000, 2560));
    assertArrayEquals(new int[] {1500, 2000}, ResizePolicy.previewSize(3000, 4000, 2560));
    assertArrayEquals(new int[] {2560, 1706}, ResizePolicy.previewSize(6000, 4000, 2560));
  }

  @Test
  public void oddSampledDimensionsBoundBothDecoderRoundingDirections() {
    int w = 4001, h = 3000, sample = ResizePolicy.sample(w, h);
    int[] preview = ResizePolicy.previewSize(w, h, 1280);
    for (int dw : new int[] {w / sample, (int) Math.ceil(w / (double) sample)})
      for (int dh : new int[] {h / sample, (int) Math.ceil(h / (double) sample)}) {
        int[] actual = ResizePolicy.fit(dw, dh, 1280, ResizePolicy.MAX_PIXELS);
        assertTrue(actual[0] <= preview[0]);
        assertTrue(actual[1] <= preview[1]);
      }
    assertArrayEquals(new int[] {1280, 960}, preview);
  }

  @Test
  public void customBoundsAcceptWholePixels() {
    assertEquals(64, ResizePolicy.parseLongestEdge("64"));
    assertEquals(1500, ResizePolicy.parseLongestEdge("1500"));
    assertEquals(8192, ResizePolicy.parseLongestEdge("8192"));
    assertArrayEquals(new int[] {1500, 1000}, ResizePolicy.previewSize(3000, 2000, 1500));
  }

  @Test
  public void invalidCustomValuesNeverBecomeDimensions() {
    for (String value :
        new String[] {
          null, "", "0", "63", "8193", "-100", "1.5", "1e3", " 1280", "999999999999", "hello"
        }) {
      try {
        ResizePolicy.parseLongestEdge(value);
        fail(String.valueOf(value));
      } catch (IllegalArgumentException expected) {
      }
    }
  }

  @Test
  public void pathologicalAspectRatiosStayBoundedAndNonzero() {
    for (int[] dims :
        new int[][] {
          {100000, 1}, {1, 100000}, {Integer.MAX_VALUE, Integer.MAX_VALUE}, {8193, 4097}
        }) {
      int[] size = ResizePolicy.previewSize(dims[0], dims[1], 8192);
      assertTrue(size[0] >= 1 && size[1] >= 1);
      assertTrue(size[0] <= dims[0] && size[1] <= dims[1]);
      assertTrue(Math.max(size[0], size[1]) <= 8192);
      assertTrue((long) size[0] * size[1] <= ResizePolicy.MAX_PIXELS);
    }
  }
}
