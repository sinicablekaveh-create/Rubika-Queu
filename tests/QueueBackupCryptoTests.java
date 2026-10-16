package ir.sinicable.rubikaqueue;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class QueueBackupCryptoTests {
 private static void require(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 private static void rejected(byte[] backup,char[] pass){
  try{QueueBackupCrypto.decrypt(backup,pass);throw new AssertionError("Unsafe backup accepted");}
  catch(GeneralSecurityException expected){}
 }
 public static void main(String[] args)throws Exception{
  char[] password="correct horse battery staple!".toCharArray();
  byte[] plain="{\"owner\":\"g1000\",\"message\":\"آزمایش\",\"recipients\":[{\"rnd\":311}]}".getBytes(StandardCharsets.UTF_8);
  byte[] one=QueueBackupCrypto.encrypt(plain,password);
  byte[] two=QueueBackupCrypto.encrypt(plain,password);
  require(!Arrays.equals(one,two),"Fresh salt/nonce required for every backup");
  require(Arrays.equals(plain,QueueBackupCrypto.decrypt(one,password)),"Unicode round trip");
  rejected(one,"incorrect password 1234".toCharArray());
  byte[] changed=one.clone();changed[changed.length-1]^=0x01;
  rejected(changed,password);
  changed=one.clone();changed[0]^=1;rejected(changed,password);
  rejected(new byte[]{1,2,3},password);
  try{QueueBackupCrypto.encrypt(plain,"short".toCharArray());throw new AssertionError("short password");}
  catch(GeneralSecurityException expected){}
  try{QueueBackupCrypto.encrypt(new byte[QueueBackupCrypto.MAX_PLAINTEXT+1],password);
      throw new AssertionError("oversized input");}
  catch(GeneralSecurityException expected){}
  require(one.length<QueueBackupCrypto.MAX_FILE,"Valid file limit");
  System.out.println("Password-encrypted queue backup offline tests passed");
 }
}
