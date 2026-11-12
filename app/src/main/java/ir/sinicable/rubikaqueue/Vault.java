package ir.sinicable.rubikaqueue;
import android.content.Context;
import android.security.keystore.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public final class Vault {
 static final String LEGACY_ALIAS="rubika_queue_session_v1";
 private Vault(){}
 private static int valid(int slot){if(slot<1||slot>3)throw new IllegalArgumentException("slot");return slot;}
 static String alias(int slot){slot=valid(slot);return slot==1?LEGACY_ALIAS:"rubika_queue_session_v2_slot_"+slot;}
 static String prefs(int slot){slot=valid(slot);return slot==1?"vault":"vault_"+slot;}
 static SecretKey key(int slot)throws Exception {
  String alias=alias(slot);
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);
  if(!k.containsAlias(alias)){
   KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
   g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
       .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
       .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
   g.generateKey();
  }
  return (SecretKey)k.getKey(alias,null);
 }
 public static void save(Context context,JSONObject data)throws Exception {save(context,1,data);}
 public static void save(Context context,int slot,JSONObject data)throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key(slot));
  byte[] encrypted=c.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
  String text=Base64.getEncoder().encodeToString(c.getIV())+":"+Base64.getEncoder().encodeToString(encrypted);
  if(!context.getSharedPreferences(prefs(slot),0).edit().putString("session",text).commit())
   throw new java.io.IOException("ذخیره ورود ناموفق بود");
 }
 public static RubikaClient load(Context context)throws Exception {return load(context,1);}
 public static RubikaClient load(Context context,int slot)throws Exception {
  String s=context.getSharedPreferences(prefs(slot),0).getString("session","");
  if(s.isEmpty())return null;
  String[] parts=s.split(":");
  if(parts.length!=2)throw new java.io.IOException("نشست ذخیره‌شده نامعتبر است");
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
  c.init(Cipher.DECRYPT_MODE,key(slot),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));
  return RubikaClient.restore(new JSONObject(new String(c.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8)));
 }
 public static void clear(Context c)throws Exception {clear(c,1);}
 public static void clear(Context c,int slot)throws Exception {
  if(!c.getSharedPreferences(prefs(slot),0).edit().clear().commit())throw new java.io.IOException("حذف نشست ناموفق بود");
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);String alias=alias(slot);
  if(k.containsAlias(alias))k.deleteEntry(alias);
 }
}
