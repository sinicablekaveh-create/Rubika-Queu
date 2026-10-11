package ir.sinicable.rubikaqueue;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

/** Rubika protocol interoperability derived from Rubpy 7.3.5 (GPL-3.0). */
public final class CryptoCore {
 /** Applies Rubika's character substitution to protocol authentication or key text. */
 public static String transform(String s) {
  StringBuilder b=new StringBuilder();
  for(char c:s.toCharArray()) {
   if(c>='a'&&c<='z')c=(char)('a'+Math.floorMod(32-(c-'a'),26));
   else if(c>='A'&&c<='Z')c=(char)('A'+Math.floorMod(29-(c-'A'),26));
   else if(c>='0'&&c<='9')c=(char)('0'+Math.floorMod(13-(c-'0'),10));
   b.append(c);
  } return b.toString();
 }
 /** Derives the AES key from a 32-character session secret; rejects other lengths. */
 public static byte[] key(String a) {
  if(a.length()!=32)throw new IllegalArgumentException("Invalid session length");
  String t=a.substring(16,24)+a.substring(0,8)+a.substring(24)+a.substring(8,16);
  StringBuilder b=new StringBuilder();for(char c:t.toCharArray())b.append((char)('a'+Math.floorMod(c-'a'+9,26)));
  return b.toString().getBytes(StandardCharsets.UTF_8);
 }
 /** Generates a random 32-character lowercase temporary session secret. */
 public static String secret() {SecureRandom r=new SecureRandom();StringBuilder b=new StringBuilder();for(int i=0;i<32;i++)b.append((char)('a'+r.nextInt(26)));return b.toString();}
 /** Encrypts UTF-8 text using the protocol's AES-CBC key and zero IV, returning Base64. */
 public static String encrypt(String text,String auth)throws Exception {
  Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key(auth),"AES"),new IvParameterSpec(new byte[16]));
  return Base64.getEncoder().encodeToString(c.doFinal(text.getBytes(StandardCharsets.UTF_8)));
 }
 /** Decrypts a Base64 protocol payload with the session-derived AES key and zero IV. */
 public static String decrypt(String text,String auth)throws Exception {
  Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key(auth),"AES"),new IvParameterSpec(new byte[16]));
  return new String(c.doFinal(Base64.getDecoder().decode(text.replace('-','+').replace('_','/'))),StandardCharsets.UTF_8);
 }
 /** Generates the 1024-bit RSA key pair used by the sign-in protocol. */
 public static KeyPair generate()throws Exception {KeyPairGenerator k=KeyPairGenerator.getInstance("RSA");k.initialize(1024);return k.generateKeyPair();}
 /** Encodes an RSA public key as PEM, then Base64 and protocol character substitution. */
 public static String publicWire(PublicKey key) {
  String pem="-----BEGIN PUBLIC KEY-----\n"+Base64.getMimeEncoder(64,new byte[]{10}).encodeToString(key.getEncoded())+"\n-----END PUBLIC KEY-----";
  return transform(Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.US_ASCII)));
 }
 /** Returns the private key's PKCS#8 encoding as Base64 for session storage. */
 public static String privateEncoded(PrivateKey key){return Base64.getEncoder().encodeToString(key.getEncoded());}
 /** Restores an RSA private key from its Base64 PKCS#8 encoding. */
 public static PrivateKey privateKey(String encoded)throws Exception{return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));}
 /** Returns a Base64 SHA256withRSA signature over the UTF-8 request text. */
 public static String sign(String text,PrivateKey key)throws Exception {Signature s=Signature.getInstance("SHA256withRSA");s.initSign(key);s.update(text.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(s.sign());}
 /** Decrypts the Base64 RSA-OAEP sign-in secret using SHA-1 and returns UTF-8 text. */
 public static String unwrap(String text,PrivateKey key)throws Exception {
  Cipher c=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
  c.init(Cipher.DECRYPT_MODE,key,new OAEPParameterSpec("SHA-1","MGF1",MGF1ParameterSpec.SHA1,PSource.PSpecified.DEFAULT));
  return new String(c.doFinal(Base64.getDecoder().decode(text)),StandardCharsets.UTF_8);
 }
}
