package org.africandroid.customizer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import java.io.FileNotFoundException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class SipProfileProvider extends ContentProvider {
    static final String PACKAGE="com.dobhrap.olamide";
    static final String AUTHORITY="org.africandroid.customizer.sipprofiles";
    private static final HashMap<String,Ticket> tickets=new HashMap<>();
    private static final class Ticket{byte[] bytes;long expiry;Ticket(byte[] b){bytes=b;expiry=SystemClock.elapsedRealtime()+45000;}}
    static boolean trusted(Context c){
        try{
            String wanted=c.getString(R.string.sip_trusted_signer).toLowerCase(java.util.Locale.ROOT);
            if(!wanted.matches("[a-f0-9]{64}"))return false;
            PackageInfo info=c.getPackageManager().getPackageInfo(PACKAGE,PackageManager.GET_SIGNING_CERTIFICATES);
            if(info.signingInfo==null||info.signingInfo.hasMultipleSigners())return false;
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(info.signingInfo.getApkContentsSigners()[0].toByteArray());
            StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return wanted.equals(hex.toString());
        }catch(Exception e){return false;}
    }
    static synchronized Uri issue(Context c,org.json.JSONObject profile)throws Exception{
        if(!trusted(c))throw new SecurityException();
        PackageInfo info=c.getPackageManager().getPackageInfo(PACKAGE,PackageManager.GET_META_DATA);
        if(info.applicationInfo==null||info.applicationInfo.metaData==null||info.applicationInfo.metaData.getInt("org.africandroid.SIP_PROFILE_IMPORT",0)!=1)throw new SecurityException();
        if(tickets.size()>=8)throw new IllegalStateException();
        String token=UUID.randomUUID().toString();Ticket ticket=new Ticket(profile.toString().getBytes(StandardCharsets.UTF_8));tickets.put(token,ticket);
        Uri uri=Uri.parse("content://"+AUTHORITY+"/handoff/"+token);
        c.grantUriPermission(PACKAGE,uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        new Handler(Looper.getMainLooper()).postDelayed(()->expire(c,token,uri),45000);
        return uri;
    }
    private static synchronized void expire(Context c,String token,Uri uri){Ticket t=tickets.remove(token);if(t!=null)Arrays.fill(t.bytes,(byte)0);c.revokeUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        Context c=getContext();String[] names=c.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if(!"r".equals(mode)||!AUTHORITY.equals(uri.getAuthority())||uri.getPathSegments().size()!=2||!"handoff".equals(uri.getPathSegments().get(0))||names==null||!Arrays.asList(names).contains(PACKAGE)||!trusted(c))throw new FileNotFoundException("Unauthorized or unavailable handoff");
        Ticket ticket; synchronized(SipProfileProvider.class){ticket=tickets.remove(uri.getLastPathSegment());}
        if(ticket==null||ticket.expiry<SystemClock.elapsedRealtime()){if(ticket!=null)Arrays.fill(ticket.bytes,(byte)0);throw new FileNotFoundException("Handoff expired or used");}
        try{
            ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();
            new Thread(()->{try(ParcelFileDescriptor.AutoCloseOutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])){out.write(ticket.bytes);}catch(Exception ignored){}finally{Arrays.fill(ticket.bytes,(byte)0);c.revokeUriPermission(uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);}}).start();
            return pipe[0];
        }catch(Exception e){Arrays.fill(ticket.bytes,(byte)0);throw new FileNotFoundException("Handoff unavailable");}
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/json";}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
