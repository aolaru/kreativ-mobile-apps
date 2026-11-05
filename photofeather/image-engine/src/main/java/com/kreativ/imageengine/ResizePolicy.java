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
