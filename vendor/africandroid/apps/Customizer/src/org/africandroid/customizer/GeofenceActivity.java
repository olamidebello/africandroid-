package org.africandroid.customizer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

public class GeofenceActivity extends Activity {
    private BiometricGate gate;
    private LinearLayout panel;
    @Override public void onCreate(Bundle state) { super.onCreate(state); gate = new BiometricGate(this); render(); }
    private void text(String value) { TextView view = new TextView(this); view.setText(value); view.setTextSize(16); view.setPadding(12, 12, 12, 12); panel.addView(view); }
    private Button button(int label) { Button b = new BiometricGate.GuardedButton(this, gate); b.setText(label); panel.addView(b); return b; }
    private void render() {
        panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(20,20,20,20);
        ScrollView scroll = new ScrollView(this); scroll.addView(panel); setContentView(scroll);
        text(getString(R.string.geo_title)); text(getString(R.string.geo_consent));
        button(R.string.geo_add).setOnClickListener(v -> edit(null));
        button(R.string.geo_start).setOnClickListener(v -> requestMonitoring());
        button(R.string.geo_stop).setOnClickListener(v -> { stopService(new Intent(this, GeofenceService.class)); GeoStore.invalidate(); toast(R.string.geo_stopped); });
        text(getString(GeoStore.prefs(this).getBoolean("restrict_apps", false) ? R.string.geo_restrict_on : R.string.geo_restrict_off));
        button(R.string.geo_restrict_toggle).setOnClickListener(v -> {
            boolean next = !GeoStore.prefs(this).getBoolean("restrict_apps", false);
            GeoStore.invalidate(); GeoStore.prefs(this).edit().putBoolean("restrict_apps", next).apply(); render();
        });
        button(R.string.geo_location_settings).setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
        button(R.string.geo_history).setOnClickListener(v -> {
            StringBuilder history = new StringBuilder();
            try { JSONArray rows = new JSONArray(GeoStore.prefs(this).getString("events", "[]"));
                for (int i = rows.length()-1; i >= 0; i--) { JSONObject row = rows.getJSONObject(i); history.append(new java.util.Date(row.getLong("time"))).append(" · ").append(row.getString("zone")).append(" · ").append(row.getString("event")).append("\n"); }
            } catch (Exception ignored) {}
            new AlertDialog.Builder(this).setTitle(R.string.geo_history).setMessage(history.length() == 0 ? getString(R.string.geo_no_events) : history.toString()).setPositiveButton(R.string.close, null).show();
        });
        button(R.string.geo_clear_history).setOnClickListener(v -> GeoStore.prefs(this).edit().remove("events").apply());
        JSONArray zones = GeoStore.zones(this);
        for (int i = 0; i < zones.length(); i++) {
            try {
                JSONObject zone = zones.getJSONObject(i);
                Button row = new BiometricGate.GuardedButton(this, gate);
                row.setText(zone.getString("name") + " · " + zone.getDouble("radius_m") + " m · " + (zone.optBoolean("enabled", true) ? getString(R.string.geo_enabled) : getString(R.string.geo_disabled)));
                panel.addView(row); row.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(zone.optString("name"))
                    .setItems(new String[] {getString(R.string.geo_edit),getString(R.string.geo_toggle),getString(R.string.geo_delete)}, (dialog, option) -> gate.run(() -> {
                        if (option == 0) { edit(zone); return; }
                        JSONArray saved = new JSONArray(); JSONArray existing = GeoStore.zones(this);
                        for (int j = 0; j < existing.length(); j++) { JSONObject item = existing.optJSONObject(j); if (item == null) continue;
                            if (item.optString("id").equals(zone.optString("id"))) {
                                if (option == 2) continue;
                                try { item.put("enabled", !item.optBoolean("enabled", true)); } catch (Exception ignored) {}
                            } saved.put(item);
                        } GeoStore.save(this, saved); render();
                    })).show());
            } catch (Exception ignored) {}
        }
    }
    private EditText input(LinearLayout form, int hint, String value, boolean numeric) {
        EditText field = new EditText(this); field.setHint(hint); field.setText(value);
        if (numeric) field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(field); return field;
    }
    private void edit(JSONObject prior) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText name = input(form,R.string.geo_name,prior == null ? "" : prior.optString("name"),false);
        EditText lat = input(form,R.string.geo_lat,prior == null ? "" : prior.optString("lat"),true);
        EditText lon = input(form,R.string.geo_lon,prior == null ? "" : prior.optString("lon"),true);
        EditText radius = input(form,R.string.geo_radius,prior == null ? "500" : prior.optString("radius_m"),true);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.geo_add).setView(form).setPositiveButton(R.string.geo_save,null).setNegativeButton(R.string.close,null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> gate.run(() -> {
            try {
                double a = Double.parseDouble(lat.getText().toString()), o = Double.parseDouble(lon.getText().toString()), rad = Double.parseDouble(radius.getText().toString());
                String label = name.getText().toString().trim();
                if (!Double.isFinite(a) || !Double.isFinite(o) || !Double.isFinite(rad) || a < -90 || a > 90 || o < -180 || o > 180 || rad < 100 || rad > 100000 || label.isEmpty() || label.length()>80) throw new IllegalArgumentException();
                JSONArray existing = GeoStore.zones(this); if (prior == null && existing.length() >= 50) throw new IllegalArgumentException();
                JSONObject zone = new JSONObject(); String id = prior == null ? UUID.randomUUID().toString() : prior.getString("id");
                zone.put("id",id).put("name",label).put("lat",a).put("lon",o).put("radius_m",rad).put("enabled",prior == null || prior.optBoolean("enabled",true));
                JSONArray saved = new JSONArray(); for (int i = 0; i < existing.length(); i++) { JSONObject item = existing.getJSONObject(i); if (!item.getString("id").equals(id)) saved.put(item); } saved.put(zone);
                GeoStore.save(this,saved); dialog.dismiss(); render();
            } catch (Exception error) { toast(R.string.geo_invalid); }
        })));dialog.show();
    }
    private void requestMonitoring() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}, 701); return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 702); return;
        }
        startMonitoring();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code,permissions,results);
        if (code == 701 || code == 702) gate.run(() -> {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { toast(R.string.geo_permission); return; }
            if (code == 701) requestMonitoring(); else startMonitoring();
        });
    }
    private void startMonitoring() {
        try { startForegroundService(new Intent(this,GeofenceService.class)); toast(R.string.geo_start_requested); }
        catch (RuntimeException error) { toast(R.string.geo_permission); }
    }
    private void toast(int message) { Toast.makeText(this,message,Toast.LENGTH_SHORT).show(); }
    @Override public void onStop() { if (gate != null) gate.cancel(); super.onStop(); }
}
