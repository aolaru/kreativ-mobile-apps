package com.kreativ.imageengine;

public final class ResizePolicy {
  public static final long MAX_PIXELS = 8_000_000;

  private ResizePolicy() {}

  public static int[] fit(int width, int height, int longestEdge, long maxPixels) {
    if (width < 1 || height < 1 || longestEdge < 1 || maxPixels < 1)
      throw new IllegalArgumentException("Invalid image dimensions");
    double scale =
        Math.min(
            1,
            Math.min(
                longestEdge / (double) Math.max(width, height),
                Math.sqrt(maxPixels / ((double) width * height))));
    return new int[] {
      Math.max(1, (int) Math.floor(width * scale)), Math.max(1, (int) Math.floor(height * scale))
    };
  }

  /**
   * Upper-bound resize preview using oriented source dimensions and the safe decode policy. Actual
   * decoder rounding can be smaller; a byte target may reduce dimensions further.
   */
  public static int[] previewSize(int width, int height, int longestEdge) {
    if (width < 1 || height < 1 || longestEdge < 64 || longestEdge > 8192)
      throw new IllegalArgumentException("Invalid resize dimensions");
    int sample = sample(width, height);
    int[] widths = {Math.max(1, width / sample), (int) Math.ceil(width / (double) sample)};
    int[] heights = {Math.max(1, height / sample), (int) Math.ceil(height / (double) sample)};
    int[] upper = new int[2];
    // Codecs can round sampled dimensions down or up. Bound both possibilities before scaling.
    for (int w : widths)
      for (int h : heights) {
        int[] size = fit(w, h, longestEdge, MAX_PIXELS);
        upper[0] = Math.max(upper[0], size[0]);
        upper[1] = Math.max(upper[1], size[1]);
      }
    return upper;
  }

  public static int parseLongestEdge(String value) {
    try {
      if (value == null || !value.matches("[0-9]{1,4}")) throw new NumberFormatException();
      int edge = Integer.parseInt(value);
      if (edge < 64 || edge > 8192) throw new NumberFormatException();
      return edge;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Enter a whole number from 64 to 8192 pixels.");
    }
  }

  public static int sample(int width, int height) {
    int sample = 1;
    while (Math.ceil(width / (double) sample) * Math.ceil(height / (double) sample) > MAX_PIXELS
        || Math.max(width, height) / sample > 8192) sample *= 2;
    return sample;
  }

  public static int attemptEdge(int initial, int level) {
    if (level < 0 || level > 4) throw new IllegalArgumentException();
    int minimum = Math.min(initial, 320);
    return Math.max(
        minimum, (int) Math.floor(initial * Math.pow(minimum / (double) initial, level / 4.0)));
  }
}
