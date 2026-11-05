package com.kreativ.northlight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** User-selected places stay local and are never silently overwritten. */
final class PlaceData {
    static final int LIMIT = 24;
    private final JSONArray values;
    PlaceData(String json) throws JSONException {
        values = new JSONArray(json);
        if (values.length() > LIMIT) throw new JSONException("Too many places");
        for (int i = 0; i < values.length(); i++) {
            JSONObject p = values.getJSONObject(i);
            String name = p.getString("name");
            double lat = p.getDouble("lat"), lon = p.getDouble("lon"), accuracy = p.getDouble("accuracy");
            if (name.trim().isEmpty() || !PlaceMath.valid(lat, lon) || !Double.isFinite(accuracy) || accuracy < 0 || accuracy > 50)
                throw new JSONException("Invalid saved place");
        }
    }
    int size() { return values.length(); }
    String name(int i) throws JSONException { return values.getJSONObject(i).getString("name"); }
    double latitude(int i) throws JSONException { return values.getJSONObject(i).getDouble("lat"); }
    double longitude(int i) throws JSONException { return values.getJSONObject(i).getDouble("lon"); }
    double accuracy(int i) throws JSONException { return values.getJSONObject(i).getDouble("accuracy"); }
    boolean add(String name, double latitude, double longitude, double accuracy) throws JSONException {
        if (size() >= LIMIT) return false;
        if (name.trim().isEmpty() || !PlaceMath.valid(latitude, longitude) || !Double.isFinite(accuracy) || accuracy < 0 || accuracy > 50)
            throw new JSONException("Invalid place");
        values.put(new JSONObject().put("name", name).put("lat", latitude).put("lon", longitude).put("accuracy", accuracy));
        return true;
    }
    void rename(int i, String name) throws JSONException {
        if (name.trim().isEmpty()) throw new JSONException("Empty name");
        values.getJSONObject(i).put("name", name);
    }
    void delete(int i) { values.remove(i); }
    @Override public String toString() { return values.toString(); }
}
