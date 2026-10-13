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
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private BiometricGate gate;
    private final ArrayList<ResolveInfo> apps = new ArrayList<>();
    private GridLayout grid;
    private SharedPreferences preferences;
    private String query = "";
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        gate = new BiometricGate(this);
        preferences = getSharedPreferences("launcher", MODE_PRIVATE);
        render();
    }
    @Override public void onResume() {
        super.onResume();
        if (grid != null) { render(); }
    }
    private void render() {
        query = "";
        String profile = preferences.getString("profile", "Mobile");
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(16 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding); page.setBackgroundColor(Color.TRANSPARENT);
        TextView title = new TextView(this); title.setText(getString(R.string.workspace_title, profileLabel(profile))); title.setTextSize(28); title.setTextColor(0xffffd166); page.addView(title);
        TextView carrierBadge = new TextView(this);
        carrierBadge.setText(getString(R.string.carrier_local_badge, getSharedPreferences("carrier_display",MODE_PRIVATE).getString("badge","AfricAndroid")));
        carrierBadge.setTextColor(0xfffff3cf);page.addView(carrierBadge);
        Button profiles = new BiometricGate.GuardedButton(this, gate); profiles.setText(getString(R.string.choose_layout)); page.addView(profiles);
        profiles.setOnClickListener(v -> {
            String[] choices = {getString(R.string.profile_mobile), getString(R.string.profile_tablet), getString(R.string.profile_desktop)};
            String[] ids = {"Mobile", "Tablet", "Desktop"};
            new AlertDialog.Builder(this).setTitle(getString(R.string.layout_title)).setItems(choices, (dialog, index) -> gate.run(() -> {
                preferences.edit().putString("profile", ids[index]).apply(); query = ""; render();
            })).show();
        });
        EditText search = new EditText(this); search.setHint(getString(R.string.search_apps)); search.setTextColor(0xfffff3cf); search.setHintTextColor(0xffd6eadf); search.setBackgroundColor(0xe6032419); page.addView(search);
        ScrollView scroll = new ScrollView(this);
        grid = new GridLayout(this);
        int widthDp = getResources().getConfiguration().screenWidthDp;
        int requested = profile.equals("Desktop") ? 6 : profile.equals("Tablet") ? 4 : 3;
        grid.setColumnCount(Math.max(2, Math.min(requested, widthDp / 100)));
        scroll.addView(grid); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout dock = new LinearLayout(this); dock.setOrientation(LinearLayout.HORIZONTAL);
        dockButton(dock, getString(R.string.files), new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE));
        dockButton(dock, getString(R.string.languages_shortcut), new Intent(Settings.ACTION_LOCALE_SETTINGS));
        dockButton(dock, getString(R.string.settings), new Intent(Settings.ACTION_SETTINGS));
        dockButton(dock, getString(R.string.customize), new Intent(this, MainActivity.class));
        page.addView(dock);
        FrameLayout workspace = new FrameLayout(this);
        workspace.setBackgroundColor(Color.BLACK);
        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.africandroid_brand);
        background.setScaleType(ImageView.ScaleType.FIT_CENTER);
        background.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        workspace.addView(background, new FrameLayout.LayoutParams(-1, -1));
        workspace.addView(page, new FrameLayout.LayoutParams(-1, -1));
        setContentView(workspace);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); showApps(); }
            public void afterTextChanged(Editable e) {}
        });
        loadApps(); showApps();
    }
    private String profileLabel(String profile) {
        return getString(profile.equals("Desktop") ? R.string.profile_desktop : profile.equals("Tablet") ? R.string.profile_tablet : R.string.profile_mobile);
    }
    private void dockButton(LinearLayout dock, String name, Intent intent) {
        Button b = new BiometricGate.GuardedButton(this, gate); b.setText(name);
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
            Button button = new BiometricGate.GuardedButton(this, gate); button.setText(label);
            android.graphics.drawable.Drawable icon = app.loadIcon(getPackageManager());
            int iconSize = (int)(40 * getResources().getDisplayMetrics().density);
            icon.setBounds(0, 0, iconSize, iconSize); button.setCompoundDrawables(null, icon, null, null);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; params.height = -2; params.setMargins(4, 6, 4, 6); params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            button.setLayoutParams(params); grid.addView(button);
            button.setOnClickListener(v -> launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(app.activityInfo.packageName, app.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)));
            button.setOnLongClickListener(v -> {
                gate.run(() -> launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + app.activityInfo.packageName)))); return true;
            });
        }
    }
    @Override public void onStop() {
        if (gate != null) gate.cancel();
        super.onStop();
    }
    private void launch(Intent intent) {
        if (Intent.ACTION_MAIN.equals(intent.getAction()) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
                && intent.getComponent() != null && !getPackageName().equals(intent.getComponent().getPackageName())
                && !GeoStore.permitsAppLaunch(this)) {
            Toast.makeText(this, R.string.geo_launch_blocked, Toast.LENGTH_LONG).show(); return;
        }
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) { Toast.makeText(this, getString(R.string.action_unavailable), Toast.LENGTH_SHORT).show(); }
    }
}
