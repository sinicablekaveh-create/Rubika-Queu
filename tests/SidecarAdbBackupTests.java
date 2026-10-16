package ir.sinicable.rubikaqueue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** No ADB process and no phone data: pure compatible envelope test. */
public final class SidecarAdbBackupTests {
 private static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  byte[] job=("{\"owner\":\"g1000\",\"message\":\"نمونه\",\"recipients\":["
    +"{\"phone\":\"09123456789\",\"row\":1,\"status\":\"sent\",\"rnd\":123}]}").getBytes(StandardCharsets.UTF_8);
  byte[] envelope=SidecarAdbBackup.envelope(job);
  String text=new String(envelope,StandardCharsets.UTF_8);
  require(text.startsWith("{\"backup_version\":1,"),"correct backup format");
  require(text.contains("\"source_package\":\"ir.sinicable.rubikaqueue.sidecar\""),"package identity");
  require(text.contains("\"queue\":"+new String(job,StandardCharsets.UTF_8)),"exact row status preservation");
  require(!text.contains("privateKey")&&!text.contains("session"),"no account secrets");
  char[] password="user-chosen-password-123!".toCharArray();
  byte[] encrypted=QueueBackupCrypto.encrypt(envelope,password);
  require(Arrays.equals(QueueBackupCrypto.decrypt(encrypted,password),envelope),"compatible with app crypto");
  try{SidecarAdbBackup.envelope("invalid input".getBytes(StandardCharsets.UTF_8));throw new AssertionError("invalid job");}
  catch(IllegalArgumentException expected){}
  System.out.println("ADB helper envelope tests passed without any phone connection");
 }
}
