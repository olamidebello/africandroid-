package com.dobhrap.olamide;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import org.json.JSONObject;
import org.linphone.core.*;

public class SipEngineService extends Service {
    static SipEngineService instance;Core core;static String status="Disconnected";String domain="";
    @Override public void onCreate(){super.onCreate();instance=this;getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("sip_engine","Olamide SIP",NotificationManager.IMPORTANCE_DEFAULT));}
    private Notification notification(){return new Notification.Builder(this,"sip_engine").setSmallIcon(android.R.drawable.sym_call_incoming).setContentTitle("Olamide SIP").setContentText(status).setOngoing(true).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build();}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){stopSelf();return START_NOT_STICKY;}
        try{
            startForeground(5001,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            if(core==null){
                Factory.instance().setDebugMode(false,"Olamide");core=Factory.instance().createCore(null,null,this);
                core.getConfig().setBool("sip","store_auth_info",false);core.verifyServerCertificates(true);core.verifyServerCn(true);
                core.setMediaEncryption(MediaEncryption.SRTP);core.setMediaEncryptionMandatory(true);
                core.addListener(new CoreListenerStub(){
                    @Override public void onAccountRegistrationStateChanged(Core c,Account a,RegistrationState state,String message){status="Registration: "+state.toString();update();}
                    @Override public void onCallStateChanged(Core c,Call call,Call.State state,String message){status="Call: "+state.toString();update();}
                });core.start();
            }
            if(intent!=null&&intent.hasExtra("profile_id"))configure(SipProfileStore.read(this,intent.getStringExtra("profile_id")));
        }catch(Exception error){status="Unable to initialize/unlock profile";update();stopSelf();}
        return START_NOT_STICKY;
    }
    private void configure(JSONObject p)throws Exception{
        if(core.getCallsNb()>0)throw new IllegalStateException();
        SipProfileStore.validate(p);String transport=p.optString("transport","WSS");if("WSS".equals(transport))throw new IllegalArgumentException();
        core.clearAccounts();core.clearAllAuthInfo();domain=p.getString("domain");
        Address identity=Factory.instance().createAddress("sip:"+p.getString("username")+"@"+domain);
        Address server=Factory.instance().createAddress(p.getString("wssUrl"));if(identity==null||server==null)throw new IllegalArgumentException();
        server.setTransport("TLS".equals(transport)?TransportType.Tls:"TCP".equals(transport)?TransportType.Tcp:TransportType.Udp);
        core.addAuthInfo(Factory.instance().createAuthInfo(p.getString("username"),null,p.getString("password"),null,null,domain));
        AccountParams params=core.createAccountParams();params.setIdentityAddress(identity);params.setServerAddress(server);params.setRegisterEnabled(true);
        Account account=core.createAccount(params);core.addAccount(account);core.setDefaultAccount(account);status="Registering selected profile";update();
    }
    void call(String destination){
        if(core==null||core.getDefaultAccount()==null||core.getDefaultAccount().getState()!=RegistrationState.Ok)throw new IllegalStateException();
        if(!destination.matches("[+0-9*#]{1,40}"))throw new IllegalArgumentException();
        Address target=Factory.instance().createAddress("sip:"+destination+"@"+domain);CallParams params=core.createCallParams(null);params.setVideoEnabled(false);
        if(core.inviteAddressWithParams(target,params)==null)throw new IllegalStateException();
    }
    void update(){getSystemService(NotificationManager.class).notify(5001,notification());if(MainActivity.current!=null)MainActivity.current.refreshStatus();}
    @Override public void onDestroy(){if(core!=null){core.terminateAllCalls();core.clearAccounts();core.clearAllAuthInfo();core.stop();core=null;}instance=null;status="Disconnected";super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
