package com.kreativ.northlight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Compatible with existing saved bearings. Never silently evicts a user's data. */
final class BearingData {
    static final int LIMIT = 8;
    private final JSONArray values;
    BearingData(String json) throws JSONException { values = new JSONArray(json); }
    int size() { return values.length(); }
    String name(int index) throws JSONException { return values.getJSONObject(index).getString("name"); }
    int degrees(int index) throws JSONException { return values.getJSONObject(index).getInt("degrees"); }
    boolean add(String name, int degrees) throws JSONException {
        if (size() >= LIMIT) return false;
        values.put(new JSONObject().put("name", name).put("degrees", HeadingMath.rounded(degrees)));
        return true;
    }
    void rename(int index, String name) throws JSONException { values.getJSONObject(index).put("name", name); }
    void delete(int index) { values.remove(index); }
    @Override public String toString() { return values.toString(); }
}
