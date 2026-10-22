package com.kreativ.imageengine;

/** Immutable engine contract. Decimal units: 1 KB = 1,000 bytes; 1 MB = 1,000,000 bytes. */
public final class CompressionOptions {
  public final long targetBytes; // Zero means resize/quality only.
  public final int longestEdge, maxQuality, background;
  public final boolean allowFlatten;

  public CompressionOptions(
      long targetBytes, int longestEdge, int maxQuality, int background, boolean allowFlatten) {
    if (targetBytes != 0 && (targetBytes < 1000 || targetBytes > 50_000_000))
      throw new IllegalArgumentException("Enter a target from 1 to 50,000 KB.");
    if (longestEdge < 64 || longestEdge > 8192)
      throw new IllegalArgumentException("Enter a longest edge from 64 to 8,192 pixels.");
    if (maxQuality < 10 || maxQuality > 100)
      throw new IllegalArgumentException("JPEG quality must be 10–100.");
    if ((background >>> 24) != 255)
      throw new IllegalArgumentException("JPEG background must be opaque.");
    this.targetBytes = targetBytes;
    this.longestEdge = longestEdge;
    this.maxQuality = maxQuality;
    this.background = background;
    this.allowFlatten = allowFlatten;
  }

  public static long parseKilobytes(String value) {
    try {
      if (!value.matches("[0-9]{1,5}")) throw new NumberFormatException();
      long bytes = Long.parseLong(value) * 1000;
      if (bytes < 1000 || bytes > 50_000_000) throw new NumberFormatException();
      return bytes;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Enter a whole number from 1 to 50,000 KB.");
    }
  }
}
