package org.africandroid.customizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.widget.Button;
import android.view.View;
import android.widget.Toast;

/** Fresh, system-owned authentication before each protected UI action. No cached approvals. */
final class BiometricGate {
    private final Activity activity;
    private CancellationSignal pending;
    private int generation;
    BiometricGate(Activity activity) { this.activity = activity; }
    void cancel() {
        generation++;
        if (pending != null) { pending.cancel(); pending = null; }
    }
    void run(Runnable action) {
        if (activity.isFinishing() || activity.isDestroyed() || pending != null) return;
        KeyguardManager keyguard = activity.getSystemService(KeyguardManager.class);
        if (keyguard == null || !keyguard.isDeviceSecure()) {
            new AlertDialog.Builder(activity).setTitle(R.string.auth_setup_title)
                .setMessage(R.string.auth_setup_message)
                // Bootstrap exception: opening enrollment settings must work before a credential exists.
                .setPositiveButton(R.string.security, (dialog, which) -> {
                    try { activity.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); }
                    catch (ActivityNotFoundException e) { toast(R.string.setting_unavailable); }
                }).setNegativeButton(R.string.close, null).show();
            return;
        }
        final int token = ++generation;
        pending = new CancellationSignal();
        try {
            BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                .setTitle(activity.getString(R.string.auth_title))
                .setSubtitle(activity.getString(R.string.auth_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .setConfirmationRequired(true).build();
            prompt.authenticate(pending, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    if (token != generation || pending == null) return;
                    pending = null; generation++;
                    if (!activity.isFinishing() && !activity.isDestroyed()) action.run();
                }
                @Override public void onAuthenticationError(int code, CharSequence message) {
                    if (token != generation) return;
                    pending = null; generation++; toast(R.string.auth_denied);
                }
                @Override public void onAuthenticationFailed() { toast(R.string.auth_retry); }
            });
        } catch (SecurityException | IllegalArgumentException e) {
            pending = null; generation++; toast(R.string.auth_denied);
        }
    }
    private void toast(int message) { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show(); }
    static final class GuardedButton extends Button {
        private final BiometricGate gate;
        GuardedButton(Activity activity, BiometricGate gate) { super(activity); this.gate = gate; }
        @Override public void setOnClickListener(View.OnClickListener listener) {
            super.setOnClickListener(listener == null ? null : view -> gate.run(() -> listener.onClick(view)));
        }
    }
}
