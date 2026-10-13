package org.africandroid.customizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

public class SipProfilesActivity extends Activity {
    private BiometricGate gate;private LinearLayout panel;
    @Override public void onCreate(Bundle state){super.onCreate(state);gate=new BiometricGate(this);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);render();}
    private Button button(int label){Button b=new BiometricGate.GuardedButton(this,gate);b.setText(label);panel.addView(b);return b;}
    private void render(){
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(20,20,20,20);ScrollView scroll=new ScrollView(this);scroll.addView(panel);setContentView(scroll);
        TextView note=new TextView(this);note.setText(R.string.sip_scope);panel.addView(note);
        button(R.string.sip_add).setOnClickListener(v->edit(null));
        button(R.string.sip_dial_default).setOnClickListener(v->handoff(SipProfileStore.prefs(this).getString("default","")));
        button(R.string.dialer_defaults).setOnClickListener(v->startActivity(new Intent(this,DialerIntegrationActivity.class)));
        JSONArray rows=SipProfileStore.metadata(this);
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;String id=row.optString("id");
            Button b=new BiometricGate.GuardedButton(this,gate);b.setText(row.optString("name")+(id.equals(SipProfileStore.prefs(this).getString("default",""))?getString(R.string.sip_default_marker):""));panel.addView(b);
            b.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(row.optString("name"))
                .setItems(new String[]{getString(R.string.sip_edit),getString(R.string.sip_set_default),getString(R.string.sip_dial),getString(R.string.sip_delete)},(dialog,choice)->gate.run(()->{
                    try{
                        if(choice==0)edit(SipProfileStore.read(this,id));
                        else if(choice==1){SipProfileStore.read(this,id);SipProfileStore.prefs(this).edit().putString("default",id).apply();render();}
                        else if(choice==2)handoff(id);
                        else new AlertDialog.Builder(this).setMessage(R.string.sip_delete_confirm).setPositiveButton(R.string.sip_delete,(d,w)->gate.run(()->{try{SipProfileStore.delete(this,id);render();}catch(Exception e){toast(R.string.sip_failed);}})).setNegativeButton(R.string.close,null).show();
                    }catch(Exception e){toast(R.string.sip_failed);}
                })).show());
        }
    }
    private EditText input(LinearLayout form,int hint,String value,boolean password){EditText f=new EditText(this);f.setHint(hint);f.setText(value);if(password)f.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);else f.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);form.addView(f);return f;}
    private void edit(JSONObject prior){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        EditText name=input(form,R.string.sip_name,prior==null?"":prior.optString("name"),false);
        EditText user=input(form,R.string.sip_user,prior==null?"":prior.optString("username"),false);
        EditText domain=input(form,R.string.sip_domain,prior==null?"":prior.optString("domain"),false);
        android.widget.Spinner transport=new android.widget.Spinner(this);
        String[] transports={"TLS","TCP","UDP","WSS"};transport.setAdapter(new android.widget.ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,transports));
        if(prior!=null)for(int i=0;i<transports.length;i++)if(transports[i].equals(prior.optString("transport","WSS")))transport.setSelection(i);
        form.addView(transport);
        EditText server=input(form,R.string.sip_wss,prior==null?"sip:sip.dobhrap.com:5061":prior.optString("wssUrl"),false);
        // Existing passwords stay masked and are not placed in editable text widgets.
        EditText password=input(form,R.string.sip_password,"",true);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.sip_add).setView(form).setPositiveButton(R.string.geo_save,null).setNegativeButton(R.string.close,null).create();
        dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->gate.run(()->{
            try{
                JSONObject p=new JSONObject().put("id",prior==null?UUID.randomUUID().toString():prior.getString("id")).put("name",name.getText().toString().trim()).put("username",user.getText().toString().trim()).put("domain",domain.getText().toString().trim()).put("wssUrl",server.getText().toString().trim());
                String secret=password.getText().toString();if(secret.isEmpty()&&prior!=null)secret=prior.getString("password");p.put("password",secret);
                p.put("transport",transport.getSelectedItem().toString());SipProfileStore.save(this,p);password.setText("");dialog.dismiss();render();
            }catch(Exception e){toast(R.string.sip_failed);}
        })));dialog.getWindow();dialog.show();dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }
    private void handoff(String id){
        if(id.isEmpty()){toast(R.string.sip_no_default);return;}
        new AlertDialog.Builder(this).setTitle(R.string.sip_dial).setMessage(R.string.sip_transfer_confirm)
            .setPositiveButton(R.string.sip_dial,(dialog,which)->gate.run(()->{
                try{
                    JSONObject profile=SipProfileStore.read(this,id);Uri uri=SipProfileProvider.issue(this,profile);
                    Intent dial=new Intent(Intent.ACTION_DIAL,Uri.parse("tel:")).setPackage(SipProfileProvider.PACKAGE);
                    dial.putExtra("org.africandroid.SIP_PROFILE_URI",uri.toString());dial.setClipData(android.content.ClipData.newRawUri("SIP profile",uri));dial.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(dial);
                }catch(Exception e){toast(R.string.sip_bridge_missing);}
            })).setNegativeButton(R.string.close,null).show();
    }
    private void toast(int value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    @Override public void onStop(){if(gate!=null)gate.cancel();super.onStop();}
}
