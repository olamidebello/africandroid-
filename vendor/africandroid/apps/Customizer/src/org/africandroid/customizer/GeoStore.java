package org.africandroid.customizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;

final class GeoStore {
    private static volatile long sampleNanos;
    private static volatile boolean withinZone;
    static SharedPreferences prefs(Context context) { return context.getSharedPreferences("geofencing", Context.MODE_PRIVATE); }
    static JSONArray zones(Context context) {
        try { return new JSONArray(prefs(context).getString("zones", "[]")); }
        catch (Exception error) { return new JSONArray(); }
    }
    static void save(Context context, JSONArray zones) {
        invalidate(); prefs(context).edit().putString("zones", zones.toString()).apply();
    }
    static void invalidate() { sampleNanos = 0; withinZone = false; }
    static void sample(long elapsedNanos, boolean inside) { withinZone = inside; sampleNanos = elapsedNanos; }
    static boolean permitsAppLaunch(Context context) {
        if (!prefs(context).getBoolean("restrict_apps", false)) return true;
        long age = SystemClock.elapsedRealtimeNanos() - sampleNanos;
        return sampleNanos > 0 && age >= 0 && age <= context.getResources().getInteger(R.integer.geo_max_sample_age_ms) * 1000000L && withinZone;
    }
    static synchronized void log(Context context, String name, String event) {
        try {
            JSONArray existing = new JSONArray(prefs(context).getString("events", "[]"));
            JSONArray kept = new JSONArray();
            int limit = context.getResources().getInteger(R.integer.geo_event_history_limit);
            for (int i = Math.max(0, existing.length() - (limit - 1)); i < existing.length(); i++) kept.put(existing.get(i));
            JSONObject row = new JSONObject(); row.put("time", System.currentTimeMillis()); row.put("zone", name); row.put("event", event); kept.put(row);
            prefs(context).edit().putString("events", kept.toString()).apply();
        } catch (Exception ignored) { /* Malformed local history must not permit app launches. */ }
    }
}
