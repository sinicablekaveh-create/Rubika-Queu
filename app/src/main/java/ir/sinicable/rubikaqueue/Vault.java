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
 static final String ALIAS="rubika_queue_session_v1";
 static SecretKey key()throws Exception {
  KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);
  if(!k.containsAlias(ALIAS)){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  return (SecretKey)k.getKey(ALIAS,null);
 }
 public static void save(Context context,JSONObject data)throws Exception {
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] encrypted=c.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
  String text=Base64.getEncoder().encodeToString(c.getIV())+":"+Base64.getEncoder().encodeToString(encrypted);
  if(!context.getSharedPreferences("vault",0).edit().putString("session",text).commit())throw new java.io.IOException("ذخیره ورود ناموفق بود");
 }
 public static RubikaClient load(Context context)throws Exception {
  String s=context.getSharedPreferences("vault",0).getString("session","");if(s.isEmpty())return null;String[] parts=s.split(":");
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));
  return RubikaClient.restore(new JSONObject(new String(c.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8)));
 }
 public static void clear(Context c)throws Exception {if(!c.getSharedPreferences("vault",0).edit().clear().commit())throw new java.io.IOException("حذف نشست ناموفق بود");KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);k.deleteEntry(ALIAS);}
}
