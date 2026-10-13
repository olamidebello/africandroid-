package org.africandroid.customizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DialerIntegrationActivity extends Activity {
    private BiometricGate gate;
    private static final String OLAMIDE = "com.dobhrap.olamide";
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);gate=new BiometricGate(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(20,20,20,20);setContentView(panel);
        TextView note=new TextView(this);note.setText(R.string.dialer_scope);panel.addView(note);
        Button set=new BiometricGate.GuardedButton(this,gate);set.setText(R.string.dialer_select);panel.addView(set);
        set.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(R.string.dialer_select).setMessage(R.string.dialer_confirm)
            .setPositiveButton(R.string.dialer_select,(dialog,which)->gate.run(()->selectOlamide())).setNegativeButton(R.string.close,null).show());
        Button dial=new BiometricGate.GuardedButton(this,gate);dial.setText(R.string.dialer_open);panel.addView(dial);
        dial.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_DIAL,android.net.Uri.parse("tel:")).setPackage(OLAMIDE));}catch(RuntimeException e){toast(R.string.dialer_missing);}});
        Button defaults=new BiometricGate.GuardedButton(this,gate);defaults.setText(R.string.dialer_defaults);panel.addView(defaults);
        defaults.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)));
    }
    private void selectOlamide(){
        RoleManager roles=getSystemService(RoleManager.class);
        Intent dial=new Intent(Intent.ACTION_DIAL,android.net.Uri.parse("tel:")).setPackage(OLAMIDE);
        boolean valid=getPackageManager().resolveActivity(dial,0)!=null;
        boolean inCall=false;
        for(ResolveInfo service:getPackageManager().queryIntentServices(new Intent("android.telecom.InCallService").setPackage(OLAMIDE),0)){
            if(service.serviceInfo!=null && service.serviceInfo.exported && "android.permission.BIND_INCALL_SERVICE".equals(service.serviceInfo.permission))inCall=true;
        }
        if(!valid || !inCall || roles==null || !roles.isRoleAvailable(RoleManager.ROLE_DIALER)){toast(R.string.dialer_missing);return;}
        try {roles.addRoleHolderAsUser(RoleManager.ROLE_DIALER,OLAMIDE,0,android.os.Process.myUserHandle(),getMainExecutor(),success->toast(success?R.string.dialer_selected:R.string.dialer_missing));}
        catch(SecurityException | IllegalArgumentException error){toast(R.string.dialer_missing);}
    }
    private void toast(int value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    @Override public void onStop(){if(gate!=null)gate.cancel();super.onStop();}
}
