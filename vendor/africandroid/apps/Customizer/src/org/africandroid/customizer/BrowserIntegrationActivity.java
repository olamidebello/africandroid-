package org.africandroid.customizer;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BrowserIntegrationActivity extends Activity {
    private BiometricGate gate;
    private static final String BROWSER="org.africanbrowser.browser";
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);gate=new BiometricGate(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(20,20,20,20);setContentView(panel);
        TextView note=new TextView(this);note.setText(R.string.browser_scope);panel.addView(note);
        Button select=new BiometricGate.GuardedButton(this,gate);select.setText(R.string.browser_select);panel.addView(select);select.setOnClickListener(v->selectBrowser());
        Button defaults=new BiometricGate.GuardedButton(this,gate);defaults.setText(R.string.dialer_defaults);panel.addView(defaults);defaults.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)));
    }
    private void selectBrowser(){
        RoleManager roles=getSystemService(RoleManager.class);
        for(String scheme:new String[]{"http","https"}){
            Intent link=new Intent(Intent.ACTION_VIEW,Uri.parse(scheme+"://example.org/")).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(BROWSER);
            if(getPackageManager().resolveActivity(link,0)==null){toast(R.string.browser_missing);return;}
        }
        if(roles==null||!roles.isRoleAvailable(RoleManager.ROLE_BROWSER)){toast(R.string.browser_missing);return;}
        try{roles.addRoleHolderAsUser(RoleManager.ROLE_BROWSER,BROWSER,0,android.os.Process.myUserHandle(),getMainExecutor(),success->toast(success?R.string.browser_selected:R.string.browser_missing));}
        catch(SecurityException|IllegalArgumentException error){toast(R.string.browser_missing);}
    }
    private void toast(int id){Toast.makeText(this,id,Toast.LENGTH_LONG).show();}
    @Override public void onStop(){if(gate!=null)gate.cancel();super.onStop();}
}
