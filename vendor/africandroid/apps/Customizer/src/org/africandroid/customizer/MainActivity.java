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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends Activity {
    private LinearLayout panel;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        panel.setPadding(pad, pad, pad, pad);
        panel.setBackgroundColor(Color.rgb(244, 249, 246));
        scroll.addView(panel);
        setContentView(scroll);
        label("AfricAndroid", 32);
        label("Your phone. Your style.", 20);
        label("Android " + Build.VERSION.RELEASE + " • " + Build.MODEL, 14);
        label("Choose a wallpaper", 20);
        wallpaper("Forest & Gold", 0xff006b54, 0xffffd166);
        wallpaper("Midnight & Ocean", 0xff101b3a, 0xff10b9bd);
        wallpaper("Sunset & Earth", 0xff9b352d, 0xfff3b568);
        button("African language directory").setOnClickListener(v -> showLanguages());
        label("Personalize your system", 20);
        settings("Display, dark theme & font size", Settings.ACTION_DISPLAY_SETTINGS);
        settings("Language & keyboard", Settings.ACTION_LOCALE_SETTINGS);
        settings("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settings("Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        settings("Apps & permissions", Settings.ACTION_APPLICATION_SETTINGS);
        settings("Security", Settings.ACTION_SECURITY_SETTINGS);
        settings("Choose default home launcher", Settings.ACTION_HOME_SETTINGS);
        button("Preview AfricAndroid workspace").setOnClickListener(v -> startActivity(new Intent(this, LauncherActivity.class)));
        settings("All settings", Settings.ACTION_SETTINGS);
        label("Emulator development edition • 0.1.0", 14);
    }
    private void label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size);
        view.setTextColor(0xff12392f); view.setPadding(0, 16, 0, 16);
        panel.addView(view);
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); panel.addView(b); return b;
    }
    private void settings(String text, String action) {
        button(text).setOnClickListener(v -> {
            try { startActivity(new Intent(action)); }
            catch (ActivityNotFoundException e) { toast("This setting is unavailable on this build."); }
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
                    runOnUiThread(() -> toast("Wallpaper applied."));
                } catch (IOException | SecurityException | IllegalArgumentException e) {
                    runOnUiThread(() -> toast("Wallpaper could not be changed."));
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
        toast("Loading African language catalog…");
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
            } catch (Exception e) { runOnUiThread(() -> toast("Language catalog could not be loaded.")); }
        }).start();
    }
    private void languageDialog(ArrayList<String> labels) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView info = new TextView(this);
        info.setText("Catalog entries do not guarantee Android translations, keyboard or speech support. Search a name or language code.");
        layout.addView(info);
        EditText search = new EditText(this); search.setHint("Search African languages"); layout.addView(search);
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
            .setMessage("System translations: unverified\nKeyboard: unverified\nSpeech: unverified\n\nAndroid settings show the languages actually available in this build.")
            .setPositiveButton("Language settings", (dialog, which) -> {
                try { startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS)); }
                catch (ActivityNotFoundException e) { toast("Language settings unavailable."); }
            }).setNegativeButton("Close", null).show());
        new AlertDialog.Builder(this).setTitle("African language directory")
            .setView(layout).setNegativeButton("Close", null).show();
    }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
}
