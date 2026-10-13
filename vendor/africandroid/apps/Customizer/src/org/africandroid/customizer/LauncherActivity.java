package org.africandroid.customizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Public Android API launcher. Profiles alter this launcher only, not Android's window manager. */
public class LauncherActivity extends Activity {
    private final ArrayList<ResolveInfo> apps = new ArrayList<>();
    private GridLayout grid;
    private SharedPreferences preferences;
    private String query = "";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("launcher", MODE_PRIVATE);
        render();
    }
    @Override public void onResume() {
        super.onResume();
        if (grid != null) { loadApps(); showApps(); }
    }
    private void render() {
        String profile = preferences.getString("profile", "Mobile");
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding); page.setBackgroundColor(0xffedf5ef);
        TextView title = new TextView(this); title.setText("AfricAndroid • " + profile); title.setTextSize(28); title.setTextColor(0xff006b54); page.addView(title);
        Button profiles = new Button(this); profiles.setText("Choose layout"); page.addView(profiles);
        profiles.setOnClickListener(v -> {
            String[] choices = {"Mobile", "Tablet", "Desktop"};
            new AlertDialog.Builder(this).setTitle("Workspace layout").setItems(choices, (dialog, index) -> {
                preferences.edit().putString("profile", choices[index]).apply(); query = ""; render();
            }).show();
        });
        EditText search = new EditText(this); search.setHint("Search installed apps"); page.addView(search);
        ScrollView scroll = new ScrollView(this);
        grid = new GridLayout(this);
        int widthDp = getResources().getConfiguration().screenWidthDp;
        int requested = profile.equals("Desktop") ? 6 : profile.equals("Tablet") ? 4 : 3;
        grid.setColumnCount(Math.max(2, Math.min(requested, widthDp / 100)));
        scroll.addView(grid); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout dock = new LinearLayout(this); dock.setOrientation(LinearLayout.HORIZONTAL);
        dockButton(dock, "Files", new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        dockButton(dock, "Settings", new Intent(Settings.ACTION_SETTINGS));
        dockButton(dock, "Customize", new Intent(this, MainActivity.class));
        page.addView(dock);
        setContentView(page);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); showApps(); }
            public void afterTextChanged(Editable e) {}
        });
        loadApps(); showApps();
    }
    private void dockButton(LinearLayout dock, String name, Intent intent) {
        Button b = new Button(this); b.setText(name);
        dock.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        b.setOnClickListener(v -> launch(intent));
    }
    private void loadApps() {
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = getPackageManager().queryIntentActivities(main, 0);
        apps.clear(); apps.addAll(found);
        apps.sort((a, b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
    }
    private void showApps() {
        grid.removeAllViews();
        for (ResolveInfo app : apps) {
            String label = app.loadLabel(getPackageManager()).toString();
            if (!label.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            Button button = new Button(this); button.setText(label);
            android.graphics.drawable.Drawable icon = app.loadIcon(getPackageManager());
            int iconSize = (int)(40 * getResources().getDisplayMetrics().density);
            icon.setBounds(0, 0, iconSize, iconSize); button.setCompoundDrawables(null, icon, null, null);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; params.height = -2; params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            button.setLayoutParams(params); grid.addView(button);
            button.setOnClickListener(v -> launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.activityInfo.packageName, app.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)));
            button.setOnLongClickListener(v -> {
                launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + app.activityInfo.packageName))); return true;
            });
        }
    }
    private void launch(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) { Toast.makeText(this, "This action is unavailable on this build.", Toast.LENGTH_SHORT).show(); }
    }
}
