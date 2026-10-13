package org.africandroid.customizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.text.TextWatcher;
import android.text.Editable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import android.app.WallpaperManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends Activity {
    private BiometricGate gate;
    private LinearLayout panel;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        gate = new BiometricGate(this);
        ScrollView scroll = new ScrollView(this);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackgroundColor(0xff031b13);
        scroll.addView(panel);
        setContentView(scroll);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.africandroid_brand);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackgroundColor(Color.BLACK);
        logo.setContentDescription(getString(R.string.brand_artwork));
        panel.addView(logo, new LinearLayout.LayoutParams(-1, (int)(240 * getResources().getDisplayMetrics().density)));
        label(getString(R.string.brand), 32);
        label(getString(R.string.afric_slogan), 20);
        label(getString(R.string.tagline), 16);
        label(getString(R.string.android_model, Build.VERSION.RELEASE, Build.MODEL), 14);
        label(getString(R.string.choose_wallpaper), 20);
        button(getString(R.string.restore_brand_wallpaper)).setOnClickListener(v -> new Thread(() -> {
            try {
                WallpaperManager.getInstance(this).setResource(R.drawable.africandroid_brand);
                runOnUiThread(() -> toast(getString(R.string.wallpaper_applied)));
            } catch (IOException | SecurityException | IllegalArgumentException e) {
                runOnUiThread(() -> toast(getString(R.string.wallpaper_failed)));
            }
        }).start());
        wallpaper(getString(R.string.wallpaper_forest), 0xff006b54, 0xffffd166);
        wallpaper(getString(R.string.wallpaper_ocean), 0xff101b3a, 0xff10b9bd);
        wallpaper(getString(R.string.wallpaper_sunset), 0xff9b352d, 0xfff3b568);
        button(getString(R.string.language_directory)).setOnClickListener(v -> showLanguages());
        label(getString(R.string.personalize), 20);
        settings(getString(R.string.display_settings), Settings.ACTION_DISPLAY_SETTINGS);
        settings(getString(R.string.language_keyboard), Settings.ACTION_LOCALE_SETTINGS);
        settings(getString(R.string.region_time), Settings.ACTION_DATE_SETTINGS);
        settings(getString(R.string.keyboard_settings), Settings.ACTION_INPUT_METHOD_SETTINGS);
        settings(getString(R.string.accessibility), Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settings(getString(R.string.wifi), Settings.ACTION_WIFI_SETTINGS);
        settings(getString(R.string.apps_permissions), Settings.ACTION_APPLICATION_SETTINGS);
        settings(getString(R.string.security), Settings.ACTION_SECURITY_SETTINGS);
        settings(getString(R.string.biometric_enroll), Settings.ACTION_BIOMETRIC_ENROLL);
        settings(getString(R.string.choose_home), Settings.ACTION_HOME_SETTINGS);
        button(getString(R.string.preview_workspace)).setOnClickListener(v -> startActivity(new Intent(this, LauncherActivity.class)));
        settings(getString(R.string.all_settings), Settings.ACTION_SETTINGS);
        label(getString(R.string.edition), 14);
    }
    private void label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size);
        view.setTextColor(0xffffd166); view.setPadding(0, 16, 0, 16);
        panel.addView(view);
    }
    private Button button(String text) {
        Button b = new BiometricGate.GuardedButton(this, gate); b.setText(text); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, 6, 0, 6); panel.addView(b, params); return b;
    }
    private void settings(String text, String action) {
        button(text).setOnClickListener(v -> {
            try { startActivity(new Intent(action)); }
            catch (ActivityNotFoundException e) { toast(getString(R.string.setting_unavailable)); }
        });
    }
    private void wallpaper(String text, int first, int second) {
        button(text).setOnClickListener(v -> {
            new Thread(() -> {
                Bitmap bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888);
                try {
                    Paint paint = new Paint();
                    paint.setShader(new LinearGradient(0, 0, 1080, 1920, first, second, Shader.TileMode.CLAMP));
                    new Canvas(bitmap).drawRect(0, 0, 1080, 1920, paint);
                    WallpaperManager.getInstance(this).setBitmap(bitmap);
                    runOnUiThread(() -> toast(getString(R.string.wallpaper_applied)));
                } catch (IOException | SecurityException | IllegalArgumentException e) {
                    runOnUiThread(() -> toast(getString(R.string.wallpaper_failed)));
                } finally { bitmap.recycle(); }
            }).start();
        });
    }
    private String readAsset(String name) throws IOException {
        try (InputStream in = getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    private void showLanguages() {
        toast(getString(R.string.catalog_loading));
        new Thread(() -> {
            ArrayList<String> labels = new ArrayList<>();
            try {
                JSONArray files = new JSONObject(readAsset("languages/index.json")).getJSONArray("files");
                for (int i = 0; i < files.length(); i++) {
                    JSONArray rows = new JSONArray(readAsset("languages/" + files.getString(i)));
                    for (int j = 0; j < rows.length(); j++) {
                        JSONObject row = rows.getJSONObject(j);
                        labels.add(row.getString("name") + " · " + row.getString("iso639_3") + " · " + row.getString("id") + " · " + row.getString("level"));
                    }
                }
                runOnUiThread(() -> languageDialog(labels));
            } catch (Exception e) { runOnUiThread(() -> toast(getString(R.string.catalog_failed))); }
        }).start();
    }
    private void languageDialog(ArrayList<String> labels) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView info = new TextView(this);
        info.setText(getString(R.string.catalog_disclaimer));
        layout.addView(info);
        EditText search = new EditText(this); search.setHint(getString(R.string.search_languages)); layout.addView(search);
        ListView list = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);
        layout.addView(list, new LinearLayout.LayoutParams(-1, (int)(360 * getResources().getDisplayMetrics().density)));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.getFilter().filter(s); }
            public void afterTextChanged(Editable s) {}
        });
        list.setOnItemClickListener((parent, view, position, id) -> new AlertDialog.Builder(this)
            .setTitle(adapter.getItem(position))
            .setMessage(getString(R.string.language_status))
            .setPositiveButton(getString(R.string.language_settings), (dialog, which) -> gate.run(() -> {
                try { startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS)); }
                catch (ActivityNotFoundException e) { toast(getString(R.string.language_unavailable)); }
            })).setNegativeButton(getString(R.string.close), null).show());
        new AlertDialog.Builder(this).setTitle(getString(R.string.language_directory))
            .setView(layout).setNegativeButton(getString(R.string.close), null).show();
    }
    @Override public void onStop() {
        if (gate != null) gate.cancel();
        super.onStop();
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
}
