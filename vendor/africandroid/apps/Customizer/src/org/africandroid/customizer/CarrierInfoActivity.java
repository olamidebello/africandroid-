package org.africandroid.customizer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class CarrierInfoActivity extends Activity {
    private BiometricGate gate;
    private LinearLayout panel;
    private SharedPreferences prefs;
    @Override public void onCreate(Bundle state) { super.onCreate(state); gate = new BiometricGate(this); prefs = getSharedPreferences("carrier_display",MODE_PRIVATE); render(false); }
    private void text(String value) { TextView row = new TextView(this); row.setText(value); row.setTextSize(16); row.setPadding(12,12,12,12); panel.addView(row); }
    private Button button(int label) { Button b = new BiometricGate.GuardedButton(this,gate); b.setText(label); panel.addView(b); return b; }
    private void render(boolean readSim) {
        panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(20,20,20,20);
        ScrollView scroll = new ScrollView(this); scroll.addView(panel); setContentView(scroll);
        text(getString(R.string.carrier_title));text(getString(R.string.carrier_disclaimer));
        text(getString(R.string.carrier_local_badge, prefs.getString("badge", "AfricAndroid")));
        button(R.string.carrier_change_badge).setOnClickListener(v -> label("badge"));
        button(R.string.carrier_reset).setOnClickListener(v -> { prefs.edit().clear().apply(); render(false); });
        button(R.string.carrier_read).setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) requestPermissions(new String[] {Manifest.permission.READ_PHONE_STATE},801);
            else render(true);
        });
        button(R.string.carrier_network_settings).setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)); }
            catch (RuntimeException error) { startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); }
        });
        if (!readSim) return;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) { text(getString(R.string.carrier_no_sim)); return; }
        try {
            SubscriptionManager manager = getSystemService(SubscriptionManager.class);
            TelephonyManager base = getSystemService(TelephonyManager.class);
            List<SubscriptionInfo> subscriptions = manager == null ? null : manager.getActiveSubscriptionInfoList();
            if (subscriptions == null || subscriptions.isEmpty() || base == null) { text(getString(R.string.carrier_no_sim)); return; }
            for (SubscriptionInfo sub : subscriptions) {
                TelephonyManager phone = base.createForSubscriptionId(sub.getSubscriptionId());
                String key = "sim_" + sub.getSubscriptionId();
                text(getString(R.string.carrier_sim_details, sub.getSimSlotIndex()+1, String.valueOf(sub.getCarrierName()), phone.getNetworkOperatorName(), String.valueOf(sub.getMccString()),String.valueOf(sub.getMncString())));
                text(getString(R.string.carrier_local_badge, prefs.getString(key,String.valueOf(sub.getDisplayName()))));
                Button edit = button(R.string.carrier_change_sim);edit.setOnClickListener(v -> label(key));
            }
        } catch (RuntimeException error) { text(getString(R.string.carrier_permission)); }
    }
    private void label(String key) {
        EditText field = new EditText(this);field.setText(prefs.getString(key,""));field.setHint(R.string.carrier_label_hint);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.carrier_change_badge).setView(field).setPositiveButton(R.string.geo_save,null).setNegativeButton(R.string.close,null).create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> gate.run(() -> {
            String value = field.getText().toString().trim();
            if (value.isEmpty() || value.length()>60 || value.matches(".*[\\p{Cntrl}].*")) { Toast.makeText(this,R.string.carrier_invalid,Toast.LENGTH_SHORT).show();return; }
            prefs.edit().putString(key,value).apply(); dialog.dismiss();render(false);
        })));dialog.show();
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if (request==801) gate.run(() -> {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED) render(true);
            else Toast.makeText(this,R.string.carrier_permission,Toast.LENGTH_SHORT).show();
        });
    }
    @Override public void onStop() { if(gate!=null)gate.cancel();super.onStop(); }
}
