package com.kreativ.sharesafe;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import androidx.exifinterface.media.ExifInterface;
import java.io.BufferedInputStream;
import java.io.InputStream;

/** Presence-only EXIF checks: never retains coordinates, names or timestamps. */
final class PrivacyCheck {
    static final int NOT_FOUND = 0, FOUND = 1, UNKNOWN = 2;
    static final String[] LABELS = {"Location tags", "Date/time tags", "Camera/device tags"};
    private static final String[][] TAGS = {
        {"GPSLatitude", "GPSLongitude", "GPSLatitudeRef", "GPSLongitudeRef", "GPSAltitude", "GPSAltitudeRef",
         "GPSTimeStamp", "GPSDateStamp", "GPSProcessingMethod", "GPSAreaInformation", "GPSDestLatitude", "GPSDestLongitude"},
        {"DateTimeOriginal", "DateTimeDigitized", "DateTime", "OffsetTime", "OffsetTimeOriginal", "OffsetTimeDigitized",
         "SubSecTime", "SubSecTimeOriginal", "SubSecTimeDigitized"},
        {"Make", "Model", "BodySerialNumber", "CameraOwnerName", "LensMake", "LensModel", "LensSerialNumber"}
    };
    static final String LIMITS = "Checks selected EXIF fields only, not every metadata format. Android or your photo provider may hide metadata. Not found does not mean a photo is safe; review visible details too.";
    private PrivacyCheck() { }

    static int[] inspect(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return unknown();
                BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outMimeType == null
                    || !ExifInterface.isSupportedMimeType(bounds.outMimeType)) return unknown();
            try (InputStream raw = context.getContentResolver().openInputStream(uri)) {
                if (raw == null) return unknown();
                ExifInterface exif = new ExifInterface(new BufferedInputStream(raw));
                int[] result = new int[3];
                for (int i = 0; i < TAGS.length; i++) for (String tag : TAGS[i]) {
                    String value = exif.getAttribute(tag);
                    if (value != null && !value.trim().isEmpty()) { result[i] = FOUND; break; }
                }
                return result;
            }
        } catch (Exception | OutOfMemoryError ignored) { return unknown(); }
    }
    static int[] unknown() { return new int[] {UNKNOWN, UNKNOWN, UNKNOWN}; }
    static boolean verified(int[] fields) {
        return fields.length == 3 && fields[0] == NOT_FOUND && fields[1] == NOT_FOUND && fields[2] == NOT_FOUND;
    }

    static final class Report {
        int total;
        final int[] found = new int[3], unknown = new int[3];
        void add(int[] fields) {
            total++;
            for (int i = 0; i < 3; i++) {
                if (fields[i] == FOUND) found[i]++;
                else if (fields[i] != NOT_FOUND) unknown[i]++;
            }
        }
        String summary(boolean exported) {
            StringBuilder text = new StringBuilder(exported ? "Exported copies · " : "Before cleaning · ");
            text.append(total).append(total == 1 ? " photo" : " photos");
            for (int i = 0; i < 3; i++) {
                text.append("\n").append(LABELS[i]).append(": ");
                if (found[i] == 0 && unknown[i] == 0) text.append("not found in readable metadata");
                else {
                    text.append("found in ").append(found[i]).append(" of ").append(total);
                    if (unknown[i] > 0) text.append(" · ").append(unknown[i]).append(" unknown / unreadable");
                }
            }
            return text.toString();
        }
        void save(Bundle out, String key) {
            out.putIntArray(key, new int[] {total, found[0], found[1], found[2], unknown[0], unknown[1], unknown[2]});
        }
        static Report restore(Bundle saved, String key) {
            int[] data = saved == null ? null : saved.getIntArray(key);
            if (data == null || data.length != 7 || data[0] < 1) return null;
            Report report = new Report(); report.total = data[0];
            for (int i = 0; i < 3; i++) {
                if (data[i + 1] < 0 || data[i + 4] < 0 || (long) data[i + 1] + data[i + 4] > report.total) return null;
                report.found[i] = data[i + 1]; report.unknown[i] = data[i + 4];
            }
            return report;
        }
    }
}
