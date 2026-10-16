package ir.sinicable.rubikaqueue;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Password-encrypted self-contained queue backup. No Android dependency or session keys. */
public final class QueueBackupCrypto {
 private static final byte[] MAGIC = new byte[]{'R','B','Q','B','K','0','0','1'};
 private static final int SALT=16, NONCE=12, TAG=16, ITERATIONS=210000;
 public static final int MAX_PLAINTEXT=8*1024*1024;
 public static final int MAX_FILE=MAX_PLAINTEXT+1024;
 private QueueBackupCrypto(){}

 public static byte[] encrypt(byte[] plaintext,char[] password) throws GeneralSecurityException {
  if(plaintext==null||plaintext.length==0||plaintext.length>MAX_PLAINTEXT)
   throw new GeneralSecurityException("Invalid backup size");
  validatePassword(password);
  SecureRandom rng=new SecureRandom();
  byte[] salt=new byte[SALT],nonce=new byte[NONCE];
  rng.nextBytes(salt);rng.nextBytes(nonce);
  byte[] key=derive(password,salt);
  try{
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));
   cipher.updateAAD(MAGIC);
   byte[] ciphertext=cipher.doFinal(plaintext);
   ByteBuffer out=ByteBuffer.allocate(MAGIC.length+SALT+NONCE+ciphertext.length);
   out.put(MAGIC).put(salt).put(nonce).put(ciphertext);
   return out.array();
  }finally{Arrays.fill(key,(byte)0);}
 }

 public static byte[] decrypt(byte[] file,char[] password) throws GeneralSecurityException {
  if(file==null||file.length<MAGIC.length+SALT+NONCE+TAG
    ||file.length>MAX_FILE)throw new GeneralSecurityException("Invalid encrypted file size");
  validatePassword(password);
  ByteBuffer in=ByteBuffer.wrap(file);
  byte[] magic=new byte[MAGIC.length];in.get(magic);
  if(!Arrays.equals(magic,MAGIC))throw new GeneralSecurityException("Not a supported backup");
  byte[] salt=new byte[SALT],nonce=new byte[NONCE];
  in.get(salt);in.get(nonce);
  byte[] payload=new byte[in.remaining()];in.get(payload);
  byte[] key=derive(password,salt);
  try{
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));
   cipher.updateAAD(MAGIC);
   byte[] clear=cipher.doFinal(payload);
   if(clear.length==0||clear.length>MAX_PLAINTEXT)
    throw new GeneralSecurityException("Invalid decrypted size");
   return clear;
  }finally{Arrays.fill(key,(byte)0);}
 }

 private static void validatePassword(char[] password)throws GeneralSecurityException {
  if(password==null||password.length<12||password.length>256)
   throw new GeneralSecurityException("Backup password must contain 12-256 characters");
 }

 private static byte[] derive(char[] password,byte[] salt)throws GeneralSecurityException {
  PBEKeySpec spec=new PBEKeySpec(password,salt,ITERATIONS,256);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
  finally{spec.clearPassword();}
 }
}
