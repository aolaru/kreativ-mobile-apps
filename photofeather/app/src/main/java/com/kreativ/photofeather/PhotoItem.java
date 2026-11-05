package com.kreativ.photofeather;

import java.io.File;
import org.json.*;

final class PhotoItem {
  final String id;
  volatile String name,
      source = "",
      output = "",
      status = "Selected",
      error = "",
      saveError = "",
      savedUri = "";
  volatile long before, after, requestTarget = -1;
  volatile int requestEdge, requestQuality;
  volatile int ow, oh, w, h, orientation = 1, quality, attempts;
  volatile boolean alpha, allowLarger, selectedForExport = true;

  PhotoItem(String id, String name) {
    this.id = id;
    this.name = name;
  }

  boolean hasOutput() {
    return !output.isEmpty() && new File(output).isFile();
  }

  boolean exportable() {
    return hasOutput()
        && (status.equals("Ready")
            || ((status.equals("Larger copy") || status.equals("Same size")) && allowLarger));
  }

  boolean needsRetry() {
    return !source.isEmpty()
        && (status.equals("Failed")
            || status.equals("Cancelled")
            || status.equals("Interrupted")
            || status.equals("Target not reached")
            || (exportable() && !error.isEmpty()));
  }

  JSONObject json() throws JSONException {
    return new JSONObject()
        .put("id", id)
        .put("name", name)
        .put("source", source)
        .put("output", output)
        .put("status", status)
        .put("error", error)
        .put("saveError", saveError)
        .put("savedUri", savedUri)
        .put("requestTarget", requestTarget)
        .put("requestEdge", requestEdge)
        .put("requestQuality", requestQuality)
        .put("before", before)
        .put("after", after)
        .put("ow", ow)
        .put("oh", oh)
        .put("w", w)
        .put("h", h)
        .put("orientation", orientation)
        .put("quality", quality)
        .put("attempts", attempts)
        .put("alpha", alpha)
        .put("allowLarger", allowLarger)
        .put("selectedForExport", selectedForExport);
  }

  static PhotoItem read(JSONObject j) {
    PhotoItem p = new PhotoItem(j.optString("id"), j.optString("name", "Photo"));
    p.source = j.optString("source");
    p.output = j.optString("output");
    p.status = j.optString("status");
    p.error = j.optString("error");
    p.saveError = j.optString("saveError");
    p.savedUri = j.optString("savedUri");
    p.requestTarget = j.optLong("requestTarget", -1);
    p.requestEdge = j.optInt("requestEdge");
    p.requestQuality = j.optInt("requestQuality");
    p.before = j.optLong("before");
    p.after = j.optLong("after");
    p.ow = j.optInt("ow");
    p.oh = j.optInt("oh");
    p.w = j.optInt("w");
    p.h = j.optInt("h");
    p.orientation = j.optInt("orientation", 1);
    p.quality = j.optInt("quality");
    p.attempts = j.optInt("attempts");
    p.alpha = j.optBoolean("alpha");
    p.allowLarger = j.optBoolean("allowLarger");
    p.selectedForExport = j.optBoolean("selectedForExport", true);
    return p;
  }
}
