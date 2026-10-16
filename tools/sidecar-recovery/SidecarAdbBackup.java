package ir.sinicable.rubikaqueue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Console;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.Arrays;
import java.util.EnumSet;

/** Desktop-only local rescue for an owner-authorized *debuggable* installed sidecar. */
public final class SidecarAdbBackup {
 private static final String PACKAGE="ir.sinicable.rubikaqueue.sidecar";
 private static final int MAX=QueueBackupCrypto.MAX_PLAINTEXT-256;
 private SidecarAdbBackup(){}

 private static byte[] readAll(InputStream stream,int max)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  byte[] b=new byte[8192];int total=0,n;
  while((n=stream.read(b))>=0){
   total+=n;if(total>max)throw new IllegalArgumentException("Job data too large");
   out.write(b,0,n);
  }
  return out.toByteArray();
 }

 private static byte[] invoke(String... argv)throws Exception{
  // Argument list, not shell: user data is never part of a command string.
  ProcessBuilder builder=new ProcessBuilder(argv);
  builder.redirectErrorStream(true);
  Process process=builder.start();
  byte[] captured;
  try(InputStream input=process.getInputStream()){captured=readAll(input,MAX);}
  if(process.waitFor()!=0){
   Arrays.fill(captured,(byte)0);
   throw new IllegalStateException("ADB failed: check USB authorization and whether the installed app is debuggable");
  }
  return captured;
 }

 private static byte[] snapshot()throws Exception {
  return invoke("adb","exec-out","run-as",PACKAGE,"cat","files/job.json");
 }

 /** Never return the raw queue to stdout, files, loggers or shell arguments. */
 public static byte[] envelope(byte[] job)throws Exception{
  if(job==null||job.length<20||job.length>MAX)
   throw new IllegalArgumentException("Missing or oversized queue");
  String preview=new String(job,0,Math.min(job.length,512),StandardCharsets.UTF_8).trim();
  if(!preview.startsWith("{")||!preview.contains("\"recipients\""))
   throw new IllegalArgumentException("Not a saved queue file");
  String prefix="{\"backup_version\":1,\"exported_at\":"+System.currentTimeMillis()
    +",\"source_package\":\""+PACKAGE+"\",\"queue\":";
  byte[] beginning=prefix.getBytes(StandardCharsets.UTF_8);
  byte[] result=new byte[beginning.length+job.length+1];
  System.arraycopy(beginning,0,result,0,beginning.length);
  System.arraycopy(job,0,result,beginning.length,job.length);
  result[result.length-1]=(byte)'}';
  if(result.length>QueueBackupCrypto.MAX_PLAINTEXT)
   throw new IllegalArgumentException("Queue exceeds backup limit");
  return result;
 }

 public static void main(String[] args)throws Exception{
  if(args.length!=1){
   System.err.println("Usage: java ... SidecarAdbBackup <private-output.rqbak>");
   System.exit(2);
  }
  Console console=System.console();
  if(console==null){
   System.err.println("Run from an interactive private terminal, not CI or a redirected shell");
   System.exit(2);
  }
  Path destination=Path.of(args[0]).toAbsolutePath().normalize();
  if(Files.exists(destination))
   throw new IllegalStateException("Refusing to overwrite an existing backup");
  console.printf("This does NOT update or uninstall the existing app.\n");
  console.printf("Stop the queue and any schedule in the SECOND app first.\n");
  console.printf("Authorized computer and USB debugging must already be enabled.\n");
  String consent=console.readLine("Type STOPPED after verifying that sending is stopped: ");
  if(!"STOPPED".equals(consent))
   throw new IllegalStateException("No backup created: sending was not confirmed stopped");
  byte[] check=invoke("adb","shell","run-as",PACKAGE,"id");
  Arrays.fill(check,(byte)0);
  // Two identical snapshots guard against a concurrently changing queue.
  byte[] first=snapshot(),second=null,plain=null,cipher=null;
  char[] pass=null,confirmation=null;
  try{
   Thread.sleep(1300);
   second=snapshot();
   if(!Arrays.equals(first,second))
    throw new IllegalStateException("Queue changed while backing up. Stop sending and try again.");
   pass=console.readPassword("Enter a NEW backup password (12-256 characters): ");
   confirmation=console.readPassword("Repeat the backup password: ");
   if(pass==null||confirmation==null||pass.length<12||!Arrays.equals(pass,confirmation))
    throw new IllegalArgumentException("Passwords do not match or are too short");
   plain=envelope(first);
   cipher=QueueBackupCrypto.encrypt(plain,pass);
   try {
    // CREATE_NEW ensures an earlier good backup is never replaced.
    Files.write(destination,cipher,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
    if(Files.getFileAttributeView(destination,PosixFileAttributeView.class)!=null){
     Files.setPosixFilePermissions(destination,EnumSet.of(PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE));
    }
   }catch(Exception e){
    Files.deleteIfExists(destination);
    throw e;
   }
   console.printf("Encrypted queue-state backup saved. Keep the password separately.\n");
   console.printf("No login keys or Vault files were read. The sending app is untouched.\n");
  }finally{
   Arrays.fill(first,(byte)0);
   if(second!=null)Arrays.fill(second,(byte)0);
   if(plain!=null)Arrays.fill(plain,(byte)0);
   if(cipher!=null)Arrays.fill(cipher,(byte)0);
   if(pass!=null)Arrays.fill(pass,'\0');
   if(confirmation!=null)Arrays.fill(confirmation,'\0');
  }
 }
}
