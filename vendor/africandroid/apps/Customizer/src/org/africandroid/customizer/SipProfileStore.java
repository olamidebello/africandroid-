package org.africandroid.customizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.net.URI;

final class SipProfileStore {
    private static final String KEY="AfricAndroidSipVaultV1";
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("sip_profiles",Context.MODE_PRIVATE);}
    static JSONArray metadata(Context c){try{return new JSONArray(prefs(c).getString("profiles","[]"));}catch(Exception e){return new JSONArray();}}
    private static SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(KEY))return (SecretKey)store.getKey(KEY,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true).setUserAuthenticationParameters(30,KeyProperties.AUTH_BIOMETRIC_STRONG|KeyProperties.AUTH_DEVICE_CREDENTIAL).build());
        return generator.generateKey();
    }
    static void validate(JSONObject p)throws Exception{
        String name=p.getString("name").trim(),user=p.getString("username"),domain=p.getString("domain"),server=p.getString("wssUrl");
        if(name.isEmpty()||name.length()>80||!user.matches("[A-Za-z0-9_.+%-]{1,128}")||!domain.matches("[A-Za-z0-9.-]{1,253}"))throw new IllegalArgumentException();
        String transport=p.optString("transport","WSS");
        if("WSS".equals(transport)){
            URI uri=new URI(server);
            if(!"wss".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null||uri.getPort()>65535||uri.getPort()==0||uri.getPort()<-1)throw new IllegalArgumentException();
        }else{
            if(!java.util.Arrays.asList("TLS","TCP","UDP").contains(transport)||!server.matches("sips?:[A-Za-z0-9.-]+:[0-9]{1,5}"))throw new IllegalArgumentException();
            int port=Integer.parseInt(server.substring(server.lastIndexOf(':')+1));if(port<1||port>65535)throw new IllegalArgumentException();
        }
        if(p.getString("password").isEmpty()||p.getString("password").length()>1024)throw new IllegalArgumentException();
    }
    static void save(Context c,JSONObject p)throws Exception{
        validate(p);String id=p.getString("id");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        byte[] encrypted=cipher.doFinal(p.toString().getBytes(StandardCharsets.UTF_8));
        JSONObject envelope=new JSONObject().put("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).put("ciphertext",Base64.encodeToString(encrypted,Base64.NO_WRAP));
        JSONArray existing=metadata(c),next=new JSONArray();
        for(int i=0;i<existing.length();i++){JSONObject item=existing.getJSONObject(i);if(!item.getString("id").equals(id))next.put(item);}
        if(next.length()>=25)throw new IllegalArgumentException();
        next.put(new JSONObject().put("id",id).put("name",p.getString("name")));
        prefs(c).edit().putString("profiles",next.toString()).putString("profile_"+id,envelope.toString()).apply();
    }
    static JSONObject read(Context c,String id)throws Exception{
        JSONObject e=new JSONObject(prefs(c).getString("profile_"+id,""));
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(e.getString("iv"),Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(e.getString("ciphertext"),Base64.NO_WRAP)),StandardCharsets.UTF_8));
    }
    static void delete(Context c,String id)throws Exception{
        JSONArray next=new JSONArray(),existing=metadata(c);
        for(int i=0;i<existing.length();i++){JSONObject item=existing.getJSONObject(i);if(!item.getString("id").equals(id))next.put(item);}
        SharedPreferences.Editor edit=prefs(c).edit().putString("profiles",next.toString()).remove("profile_"+id);
        if(id.equals(prefs(c).getString("default","")))edit.remove("default");edit.apply();
    }
}
