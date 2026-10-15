package com.kreativ.paperbuild;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class PresetStore {
    static final class Preset {
        final String name;
        final int paper, enhancement, compression;
        final ExportOptions options;
        Preset(String name, int paper, int enhancement, int compression, ExportOptions options) {
            this.name = DocumentProcessor.safeName(name); this.paper = paper == 1 ? 1 : 0;
            this.enhancement = Math.max(0, Math.min(2, enhancement)); this.compression = compression == 1 ? 1 : 0;
            this.options = options.withoutPassword();
        }
    }
    private final SharedPreferences prefs;
    PresetStore(Context context) { prefs = context.getSharedPreferences("paperbuild_presets", Context.MODE_PRIVATE); }
    synchronized List<Preset> list() {
        ArrayList<Preset> result = new ArrayList<>();
        try {
            JSONArray all = new JSONArray(prefs.getString("items", "[]"));
            for (int i = 0; i < all.length(); i++) {
                JSONObject p = all.getJSONObject(i);
                result.add(new Preset(p.getString("name"), p.optInt("paper"), p.optInt("style"), p.optInt("compression"),
                        new ExportOptions(p.optString("watermark"), p.optBoolean("numbers"), p.optBoolean("protect"), "")));
            }
        } catch (JSONException | IllegalArgumentException ignored) { }
        return result;
    }
    synchronized boolean save(Preset preset) {
        List<Preset> all = list();
        all.removeIf(p -> p.name.equalsIgnoreCase(preset.name));
        if (all.size() >= 30) throw new IllegalArgumentException("You can save up to 30 presets. Remove one first.");
        all.add(preset); return write(all);
    }
    synchronized boolean remove(String name) {
        List<Preset> all = list(); all.removeIf(p -> p.name.equals(name)); return write(all);
    }
    private boolean write(List<Preset> all) {
        JSONArray data = new JSONArray();
        try {
            for (Preset p : all) data.put(new JSONObject().put("name", p.name).put("paper", p.paper)
                    .put("style", p.enhancement).put("compression", p.compression)
                    .put("watermark", p.options.watermark).put("numbers", p.options.pageNumbers).put("protect", p.options.protect));
        } catch (JSONException error) { return false; }
        return prefs.edit().putString("items", data.toString()).commit();
    }
}
