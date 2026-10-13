package org.africandroid.customizer;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.HashSet;

public class GeofenceService extends Service implements LocationListener {
    private LocationManager locations;
    private final HashMap<String, Boolean> states = new HashMap<>();
    private Notification notification(String message) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, GeofenceActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, "afric_geo")
            .setSmallIcon(R.drawable.ic_afric).setContentTitle(getString(R.string.geo_title))
            .setContentText(message).setContentIntent(open).setOngoing(true).build();
    }
    @Override public void onCreate() {
        super.onCreate(); locations = getSystemService(LocationManager.class);
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("afric_geo", getString(R.string.geo_title), NotificationManager.IMPORTANCE_LOW));
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        GeoStore.invalidate();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || locations == null || !locations.isLocationEnabled()) { stopSelf(); return START_NOT_STICKY; }
        try {
            startForeground(4100, notification(getString(R.string.geo_monitoring)), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            locations.removeUpdates(this);
            boolean subscribed = false;
            for (String provider : new String[] {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                if (locations.isProviderEnabled(provider)) { locations.requestLocationUpdates(provider, getResources().getInteger(R.integer.geo_sample_interval_ms), 0f, this); subscribed = true; }
            }
            if (!subscribed) stopSelf();
        } catch (SecurityException | IllegalArgumentException error) { stopSelf(); }
        return START_NOT_STICKY;
    }
    @Override public void onLocationChanged(Location location) {
        long age = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
        if (location.isFromMockProvider() || !location.hasAccuracy() || age < 0 || age > getResources().getInteger(R.integer.geo_max_sample_age_ms) * 1000000L) { GeoStore.invalidate(); return; }
        boolean anyInside = false;
        JSONArray zones = GeoStore.zones(this); HashSet<String> live = new HashSet<>();
        for (int i = 0; i < zones.length(); i++) {
            try {
                JSONObject zone = zones.getJSONObject(i); if (!zone.optBoolean("enabled", true)) continue;
                String id = zone.getString("id"); live.add(id);
                Location center = new Location("zone"); center.setLatitude(zone.getDouble("lat")); center.setLongitude(zone.getDouble("lon"));
                float distance = location.distanceTo(center); double radius = zone.getDouble("radius_m");
                boolean inside = distance + location.getAccuracy() <= radius;
                boolean outside = distance - location.getAccuracy() > radius;
                if (inside) anyInside = true;
                // An uncertain boundary sample cannot establish entry or exit.
                if (!inside && !outside) continue;
                Boolean previous = states.put(id, inside);
                if (previous == null || previous.booleanValue() != inside) {
                    String event = previous == null ? (inside ? "initial_inside" : "initial_outside") : (inside ? "entered" : "exited");
                    GeoStore.log(this, zone.getString("name"), event);
                    getSystemService(NotificationManager.class).notify(4100, notification(zone.getString("name") + " · " + event));
                }
            } catch (Exception error) { GeoStore.invalidate(); return; }
        }
        states.keySet().retainAll(live);
        GeoStore.sample(location.getElapsedRealtimeNanos(), anyInside);
    }
    @Override public void onProviderDisabled(String provider) { GeoStore.invalidate(); }
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onDestroy() {
        GeoStore.invalidate();
        if (locations != null) { try { locations.removeUpdates(this); } catch (SecurityException ignored) {} }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
