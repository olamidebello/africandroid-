package com.dobhrap.olamide;

import android.telecom.InCallService;
import android.telecom.Call;
import android.app.*;
import android.content.Intent;
import java.util.ArrayList;

public class NativeInCallService extends InCallService {
    static final ArrayList<Call> calls=new ArrayList<>();
    private final Call.Callback listener=new Call.Callback(){@Override public void onStateChanged(Call call,int state){notifyUi();}};
    void notifyUi(){
        NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("telecom_calls","Phone calls",NotificationManager.IMPORTANCE_HIGH));
        if(calls.isEmpty())manager.cancel(5002);else manager.notify(5002,new Notification.Builder(this,"telecom_calls").setSmallIcon(android.R.drawable.sym_call_incoming).setContentTitle("Olamide phone call").setContentText("Open call controls").setOngoing(true).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build());
        if(MainActivity.current!=null)MainActivity.current.refreshStatus();
    }
    @Override public void onCallAdded(Call call){calls.add(call);call.registerCallback(listener);notifyUi();}
    @Override public void onCallRemoved(Call call){calls.remove(call);call.unregisterCallback(listener);notifyUi();}
}
