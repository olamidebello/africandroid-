package com.dobhrap.olamide;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.telecom.TelecomManager;
import android.text.InputType;
import android.widget.*;
import org.json.*;
import org.linphone.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends Activity {
    static MainActivity current;private BiometricGate gate;private TextView status;private EditText number;private LinearLayout panel;
    @Override public void onCreate(Bundle state){super.onCreate(state);gate=new BiometricGate(this);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);render();handleIntent(getIntent());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent);}
    private Button button(String label,Runnable task){Button b=new BiometricGate.GuardedButton(this,gate);b.setText(label);panel.addView(b);b.setOnClickListener(v->{try{task.run();}catch(Exception e){toast("Action unavailable; check profile, registration and permissions.");}});return b;}
    private void render(){
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(20,20,20,20);ScrollView scroll=new ScrollView(this);scroll.addView(panel);setContentView(scroll);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.africandroid_brand);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);panel.addView(logo,new LinearLayout.LayoutParams(-1,180));
        status=new TextView(this);status.setTextColor(0xffffd166);status.setTextSize(20);panel.addView(status);refreshStatus();
        number=new EditText(this);number.setHint("Phone number");number.setInputType(InputType.TYPE_CLASS_PHONE);panel.addView(number);
        LinearLayout keys=new LinearLayout(this);keys.setOrientation(LinearLayout.VERTICAL);panel.addView(keys);
        for(String digits:new String[]{"123","456","789","*0#"}){LinearLayout row=new LinearLayout(this);keys.addView(row);for(char digit:digits.toCharArray()){Button key=new BiometricGate.GuardedButton(this,gate);key.setText(String.valueOf(digit));row.addView(key,new LinearLayout.LayoutParams(0,-2,1));key.setOnClickListener(v->{number.append(String.valueOf(digit));SipEngineService s=SipEngineService.instance;if(s!=null&&s.core!=null&&s.core.getCurrentCall()!=null)s.core.getCurrentCall().sendDtmf(digit);});}}
        button("SIP profiles / default account",()->profiles());
        button("Register default SIP profile",()->register());
        button("Call using default SIP profile",()->placeCall());
        button("Answer",()->{if(!NativeInCallService.calls.isEmpty())NativeInCallService.calls.get(0).answer(0);else active().accept();});
        button("Hang up / reject",()->{if(!NativeInCallService.calls.isEmpty())NativeInCallService.calls.get(0).disconnect();else active().terminate();});
        button("Mute / unmute SIP microphone",()->{SipEngineService s=engine();s.core.setMicEnabled(!s.core.isMicEnabled());});
        button("Hold / resume",()->{if(!NativeInCallService.calls.isEmpty()){android.telecom.Call c=NativeInCallService.calls.get(0);if(c.getState()==android.telecom.Call.STATE_HOLDING)c.unhold();else c.hold();}else{org.linphone.core.Call c=active();if(c.getState()==org.linphone.core.Call.State.Paused)c.resume();else c.pause();}});
        button("Speaker / earpiece (SIP)",()->{SipEngineService s=engine();AudioDevice out=s.core.getOutputAudioDevice();boolean speaker=out!=null&&out.getType()==AudioDevice.Type.Speaker;for(AudioDevice d:s.core.getAudioDevices())if(d.getType()==(speaker?AudioDevice.Type.Earpiece:AudioDevice.Type.Speaker)){s.core.setOutputAudioDevice(d);break;}});
        button("Disconnect SIP",()->stopService(new Intent(this,SipEngineService.class)));
        button("Make Olamide the default phone app",()->{RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_DIALER))startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_DIALER),90);});
    }
    void refreshStatus(){if(status!=null)status.setText(SipEngineService.status+(NativeInCallService.calls.isEmpty()?"":" • Cellular call active"));}
    private SipEngineService engine(){SipEngineService s=SipEngineService.instance;if(s==null||s.core==null)throw new IllegalStateException();return s;}
    private org.linphone.core.Call active(){org.linphone.core.Call c=engine().core.getCurrentCall();if(c==null)throw new IllegalStateException();return c;}
    private void register(){
        SipEngineService active=SipEngineService.instance;if(active!=null&&active.core!=null&&active.core.getCallsNb()>0){toast("Finish the active call before switching or registering profiles.");return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.CALL_PHONE},91);return;}
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},92);return;}
        String id=SipProfileStore.prefs(this).getString("default","");if(id.isEmpty()){toast("Select a default SIP profile.");return;}
        startForegroundService(new Intent(this,SipEngineService.class).putExtra("profile_id",id));
    }
    private void placeCall(){String digits=number.getText().toString().trim();TelecomManager telecom=getSystemService(TelecomManager.class);
        if(telecom!=null&&getSystemService(android.telephony.TelephonyManager.class).isEmergencyNumber(digits)){if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CALL_PHONE},93);return;}telecom.placeCall(Uri.fromParts("tel",digits,null),new Bundle());return;}
        engine().call(digits);
    }
    private EditText field(LinearLayout form,String hint,String value,boolean password){EditText f=new EditText(this);f.setHint(hint);f.setText(value);f.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);form.addView(f);return f;}
    private void profiles(){
        JSONArray rows=SipProfileStore.metadata(this);String[] labels=new String[rows.length()+1];labels[0]="Add native SIP profile";
        for(int i=0;i<rows.length();i++)labels[i+1]=rows.optJSONObject(i).optString("name");
        new AlertDialog.Builder(this).setTitle("SIP profiles").setItems(labels,(dialog,index)->gate.run(()->{
            if(index==0){edit(null);return;}JSONObject row=rows.optJSONObject(index-1);String id=row.optString("id");
            new AlertDialog.Builder(this).setTitle(row.optString("name")).setItems(new String[]{"Set default","Edit","Delete"},(d,choice)->gate.run(()->{
                try{if(choice==0){SipProfileStore.read(this,id);SipProfileStore.prefs(this).edit().putString("default",id).apply();toast("Default SIP profile selected.");}
                else if(choice==1)edit(SipProfileStore.read(this,id));else new AlertDialog.Builder(this).setMessage("Delete profile and credentials?").setPositiveButton("Delete",(a,b)->gate.run(()->{try{SipProfileStore.delete(this,id);}catch(Exception e){toast("Unable to delete profile.");}})).setNegativeButton("Cancel",null).show();}
                catch(Exception e){toast("Unlock failed. Re-enter credentials if device security changed.");}
            })).show();
        })).show();
    }
    private void edit(JSONObject prior){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        EditText label=field(form,"Profile name",prior==null?"":prior.optString("name"),false);
        EditText username=field(form,"SIP username",prior==null?"":prior.optString("username"),false);
        EditText domain=field(form,"SIP domain",prior==null?"sip.dobhrap.com":prior.optString("domain"),false);
        EditText server=field(form,"sip:host:port",prior==null?"sip:sip.dobhrap.com:5061":prior.optString("wssUrl"),false);
        Spinner transport=new Spinner(this);String[] types={"TLS","TCP","UDP"};transport.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,types));form.addView(transport);
        if(prior!=null)for(int i=0;i<types.length;i++)if(types[i].equals(prior.optString("transport")))transport.setSelection(i);
        EditText password=field(form,"Password (blank keeps existing)","",true);
        TextView warning=new TextView(this);warning.setText("TLS is recommended. TCP/UDP signaling is unencrypted. SRTP is required for audio; incompatible servers will fail.");form.addView(warning);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Native SIP profile").setView(form).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(shown->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->gate.run(()->{
            try{JSONObject p=new JSONObject().put("id",prior==null?UUID.randomUUID().toString():prior.getString("id")).put("name",label.getText().toString().trim()).put("username",username.getText().toString().trim()).put("domain",domain.getText().toString().trim()).put("wssUrl",server.getText().toString().trim()).put("transport",transport.getSelectedItem().toString());
                String secret=password.getText().toString();if(secret.isEmpty()&&prior!=null)secret=prior.getString("password");p.put("password",secret);SipProfileStore.save(this,p);password.setText("");dialog.dismiss();}
            catch(Exception e){toast("Check profile fields, authentication and profile limit.");}
        })));dialog.show();dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    }
    private void handleIntent(Intent intent){
        if(intent.getData()!=null&&"tel".equals(intent.getData().getScheme()))number.setText(intent.getData().getSchemeSpecificPart());
        String raw=intent.getStringExtra("org.africandroid.SIP_PROFILE_URI");if(raw==null)return;intent.removeExtra("org.africandroid.SIP_PROFILE_URI");
        gate.run(()->{
            try{Uri uri=Uri.parse(raw);if(!"content".equals(uri.getScheme())||!"org.africandroid.customizer.sipprofiles".equals(uri.getAuthority()))throw new SecurityException();
                try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[1024];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>16384)throw new IOException();out.write(buffer,0,count);}
                    JSONObject p=new JSONObject(out.toString(StandardCharsets.UTF_8.name()));SipProfileStore.validate(p);if("WSS".equals(p.optString("transport","WSS")))throw new IllegalArgumentException();
                    SipProfileStore.save(this,p);SipProfileStore.prefs(this).edit().putString("default",p.getString("id")).apply();toast("Native SIP profile imported. Tap Register to connect.");
                }
            }catch(Exception e){toast("Profile import failed or expired. Native engine requires TLS/TCP/UDP profile.");}
        });
    }
    @Override public void onRequestPermissionsResult(int code,String[] names,int[] results){super.onRequestPermissionsResult(code,names,results);if(code==91||code==92)gate.run(()->{if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){if(code==91)register();else{String id=SipProfileStore.prefs(this).getString("default","");if(!id.isEmpty())startForegroundService(new Intent(this,SipEngineService.class).putExtra("profile_id",id));}}else toast("Microphone permission is required.");});}
    @Override protected void onResume(){super.onResume();current=this;refreshStatus();}
    @Override public void onStop(){current=null;if(gate!=null)gate.cancel();super.onStop();}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
