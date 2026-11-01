package com.kreativ.photofeather;

import com.kreativ.imageengine.ImageEngine;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Creates a private archive from already verified, exportable JPEGs. */
final class ZipExporter {
  static void write(File destination, List<PhotoItem> photos, List<String> names, ImageEngine.Cancellation cancel)
      throws IOException {
    write(destination, photos, names, cancel, () -> destination.getParentFile().getUsableSpace());
  }

  static void write(File destination, List<PhotoItem> photos, List<String> names,
      ImageEngine.Cancellation cancel, java.util.function.LongSupplier availableSpace)
      throws IOException {
    boolean complete = false;
    try {
      long required = 2_000_000;
      for (PhotoItem photo : photos) {
        if (photo.after < 0 || required > Long.MAX_VALUE - photo.after - 1024)
          throw new IOException("The selected copies are too large.");
        required += photo.after + 1024;
      }
      if (availableSpace.getAsLong() < required)
        throw new IOException("Low storage. Free some space and try again.");
      if (photos.isEmpty() || names.size() != photos.size())
        throw new IOException("Select at least one prepared copy.");
      List<byte[]> fingerprints = new ArrayList<>();
      try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destination))) {
        int number = 0;
        byte[] buffer = new byte[65536];
        for (PhotoItem photo : photos) {
          cancel.check();
          if (!photo.exportable()) throw new IOException("A selected preview is unavailable.");
          File source = new File(photo.output);
          ImageEngine.verifyOutput(source, photo.w, photo.h);
          if (source.length() != photo.after) throw new IOException("A prepared copy changed.");
          zip.putNextEntry(new ZipEntry(names.get(number++)));
          MessageDigest digest = sha256();
          try (InputStream in = new FileInputStream(source)) {
            int count;
            while ((count = in.read(buffer)) != -1) {
              cancel.check();
              zip.write(buffer, 0, count);
              digest.update(buffer, 0, count);
            }
          }
          fingerprints.add(digest.digest());
          zip.closeEntry();
        }
        cancel.check();
      }
      try (ZipFile zip = new ZipFile(destination)) {
        if (zip.size() != photos.size()) throw new IOException("ZIP verification failed.");
        byte[] buffer = new byte[65536];
        for (int i = 0; i < names.size(); i++) {
          cancel.check();
          ZipEntry entry = zip.getEntry(names.get(i));
          if (entry == null) throw new IOException("ZIP verification failed.");
          MessageDigest digest = sha256();
          try (InputStream in = zip.getInputStream(entry)) {
            int count;
            while ((count = in.read(buffer)) != -1) {
              cancel.check();
              digest.update(buffer, 0, count);
            }
          }
          if (!MessageDigest.isEqual(digest.digest(), fingerprints.get(i)))
            throw new IOException("ZIP verification failed.");
        }
      }
      complete = true;
    } finally {
      if (!complete) destination.delete();
    }
  }

  private static MessageDigest sha256() throws IOException {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }
}
