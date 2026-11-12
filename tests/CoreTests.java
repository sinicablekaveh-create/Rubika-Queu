package ir.sinicable.rubikaqueue;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import org.json.*;
public class CoreTests {
 static int count=0;
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);count++;System.out.println("PASS "+name);}
 interface Check{void run()throws Exception;}
 static void fails(Check c,String name)throws Exception{try{c.run();throw new AssertionError("Expected failure: "+name);}catch(AssertionError e){throw e;}catch(Exception e){count++;System.out.println("PASS "+name);}}
 public static void main(String[]args)throws Exception{
  check(Phones.normalize("۰۹۱۲۳۴۵۶۷۸۹").equals("989123456789"),"Persian digits");
  check(Phones.normalize("٠٩١٢٣٤٥٦٧٨٩").equals("989123456789"),"Arabic digits");
  check(Phones.normalize("+98 (912) 345-6789").equals("989123456789"),"International formatting");
  check(Phones.normalize("00989123456789").equals("989123456789"),"International 00 prefix");
  check(Phones.normalize("9.123456789E9").equals("989123456789"),"Excel scientific notation");
  check(Phones.normalize("9123456789.5").isEmpty(),"Fraction rejected");
  check(Phones.normalize("+499123456789").isEmpty(),"Unsupported country rejected");
  check(Phones.normalize("0912345678").isEmpty(),"Short number rejected");
  File base=new File("tests/fixtures");
  ExcelReader.Result r=ExcelReader.read(new File(base,"mixed.xlsx"));
  check(r.entries.size()==2&&r.duplicates==1&&r.invalid==2,"Mixed Excel filtering and first sheet order");
  check(r.entries.get(0).name.equals("آزمایش")&&r.entries.get(0).row==2,"Persian names and source row retained");
  check(ExcelReader.read(new File(base,"shared.xlsx")).entries.size()==1,"Shared strings");
  check(ExcelReader.read(new File(base,"thousand.xlsx")).entries.size()==1000,"1000 rows accepted");
  fails(()->ExcelReader.read(new File(base,"too_many.xlsx")),"1001 unique rows rejected without truncation");
  fails(()->ExcelReader.read(new File(base,"xxe.xlsx")),"External XML entities blocked");
  JSONObject v=new JSONObject(new String(Files.readAllBytes(new File(base,"vector.json").toPath()),StandardCharsets.UTF_8));
  check(new String(CryptoCore.key(v.getString("auth")),StandardCharsets.UTF_8).equals(v.getString("key")),"Key derivation matches Rubpy");
  check(CryptoCore.transform(v.getString("auth")).equals(v.getString("transform")),"Auth encoding matches Rubpy");
  check(CryptoCore.encrypt(v.getString("plain"),v.getString("auth")).equals(v.getString("encrypted")),"AES wire bytes match Rubpy");
  check(CryptoCore.decrypt(v.getString("encrypted"),v.getString("auth")).equals(v.getString("plain")),"AES response Unicode round-trip");
  KeyPair keys=CryptoCore.generate();String encoded=CryptoCore.privateEncoded(keys.getPrivate());PrivateKey decoded=CryptoCore.privateKey(encoded);
  Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(keys.getPublic());verifier.update("body".getBytes(StandardCharsets.UTF_8));check(verifier.verify(Base64.getDecoder().decode(CryptoCore.sign("body",decoded))),"RSA request signatures");
  Cipher rsa=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");rsa.init(Cipher.ENCRYPT_MODE,keys.getPublic());check(CryptoCore.unwrap(Base64.getEncoder().encodeToString(rsa.doFinal(v.getString("auth").getBytes(StandardCharsets.UTF_8))),decoded).equals(v.getString("auth")),"RSA login auth decryption");
  RubikaClient.validateEndpoint("https://messengerg2c1.iranlms.ir/");
  fails(()->RubikaClient.validateEndpoint("https://iranlms.ir.evil.example/"),"Foreign endpoint rejected");
  fails(()->RubikaClient.validateEndpoint("http://messengerg2c1.iranlms.ir/"),"Cleartext endpoint rejected");
  JSONObject job=JobStore.create(r,"سلام","owner");check(JobStore.next(job)==0&&!JobStore.uncertain(job),"New queue starts with first record");
  job.getJSONArray("recipients").getJSONObject(0).put("status","sent");check(JobStore.next(job)==1,"Successful record excluded on resume");
  job.getJSONArray("recipients").getJSONObject(1).put("status","sending");
  JSONObject restored=new JSONObject(job.toString());check(JobStore.uncertain(restored)&&JobStore.next(restored)==-1,"Crash during sending blocks automatic retry");
  restored.getJSONArray("recipients").getJSONObject(1).put("status","skipped");check(!JobStore.uncertain(restored)&&JobStore.next(restored)==-1,"Reviewed uncertainty remains excluded");
  final RubikaClient[] holder=new RubikaClient[1];final String recipient="u0testrecipient00000000000000";final int[] phase={0};
  RubikaClient api=new RubikaClient((url,body)->{
   RubikaClient c=holder[0];JSONObject inner=new JSONObject(CryptoCore.decrypt(body.getString("data_enc"),c.auth));String method=inner.getString("method");
   Signature sig=Signature.getInstance("SHA256withRSA");sig.initVerify(keys.getPublic());sig.update(body.getString("data_enc").getBytes(StandardCharsets.UTF_8));check(sig.verify(Base64.getDecoder().decode(body.getString("sign"))),"API envelope signature "+method);
   JSONObject data;
   if(method.equals("addAddressBook"))data=RubikaClient.obj("user",RubikaClient.obj("user_guid",recipient));
   else if(method.equals("getUserInfo"))data=RubikaClient.obj("user",RubikaClient.obj("user_guid",recipient,"phone",phase[0]==0?"989123456789":"989123456788"));
   else {check(inner.getJSONObject("input").getLong("rnd")==12345L,"Stable message request id");data=RubikaClient.obj("message",RubikaClient.obj("message_id","765"));}
   return RubikaClient.obj("data_enc",CryptoCore.encrypt(RubikaClient.obj("status","OK","status_det","OK","data",data).toString(),c.auth));
  });holder[0]=api;api.endpoint="https://messengerg2c1.iranlms.ir/";api.privateKey=decoded;
  check(api.resolve("989123456789","").equals(recipient),"Recipient resolution verifies phone and ID");
  phase[0]=1;fails(()->api.resolve("989123456789",""),"Mismatched recipient stops before send");
  check(api.send(recipient,"test",12345L).equals("765"),"Message ID retained when supplied by server");
  RubikaClient limited=new RubikaClient((url,body)->RubikaClient.obj("status","ERROR","status_det","TOO_REQUESTS"));limited.endpoint=api.endpoint;limited.privateKey=decoded;
  fails(()->limited.send(recipient,"test",1),"Rate limit stops with no automatic retry");
  RubikaClient singular=new RubikaClient((url,body)->RubikaClient.obj("status","OK","status_det","OK","data",RubikaClient.obj("message_update",RubikaClient.obj("object_guid",recipient,"message",RubikaClient.obj("message_id","901")))));
  singular.endpoint=api.endpoint;singular.privateKey=decoded;
  check(singular.send(recipient,"test",12345L).equals("901"),"Regression: singular message_update acknowledgement must not stop queue");
  RubikaClient noReceipt=new RubikaClient((url,body)->RubikaClient.obj("status","OK","status_det","OK","data",new JSONObject()));
  noReceipt.endpoint=api.endpoint;noReceipt.privateKey=decoded;
  String missingId=noReceipt.send(recipient,"test",12345L);
  check(missingId.isEmpty(),"Explicit server OK without optional receipt is accepted without invented ID");
  JSONObject continued=JobStore.create(r,"سلام","owner");JSONObject first=continued.getJSONArray("recipients").getJSONObject(0);
  JobStore.recordSuccess(first,missingId,100L);
  check(first.getString("status").equals("accepted")&&first.getString("confirmation").equals("server_ok"),"Server acceptance has a distinct report status");
  check(JobStore.next(new JSONObject(continued.toString()))==1&&!JobStore.uncertain(continued),"Accepted record is never resent after restart; next recipient remains available");
  check(JobStore.summary(continued).contains("ثبت ارسال: 1")&&JobStore.statusLabel(first).contains("پذیرفته"),"Accepted status is reported as acceptance rather than rejection");
  JSONObject multi=RubikaClient.obj("message_updates",new JSONArray()
   .put(RubikaClient.obj("object_guid","u0other000000000000000000000","message",RubikaClient.obj("message_id","888")))
   .put(RubikaClient.obj("object_guid",recipient,"rnd",12345L,"message",RubikaClient.obj("message_id","902"))));
  check(SendResponse.messageId(multi,recipient,12345L).equals("902"),"Multiple updates select only the matching recipient and request");
  check(SendResponse.messageId(multi,recipient,99L).isEmpty(),"Mismatched request ID is not used as a receipt");
  check(SendResponse.messageId(RubikaClient.obj("message_id",JSONObject.NULL),recipient,1).isEmpty(),"JSON null is not a fake receipt ID");
  check(SendResponse.messageId(RubikaClient.obj("chat_update",RubikaClient.obj("last_message",RubikaClient.obj("message_id","old")),"reply_to_message_id","old"),recipient,1).isEmpty(),"Old chat and reply message IDs are not mistaken for sent-message receipt");
  RubikaClient innerFailure=new RubikaClient((url,body)->RubikaClient.obj("status","OK","status_det","OK","data",RubikaClient.obj("status","ERROR")));
  innerFailure.endpoint=api.endpoint;innerFailure.privateKey=decoded;
  fails(()->innerFailure.send(recipient,"test",1),"Explicit method error is not promoted to server acceptance");
  final int[] attempts={0};
  RubikaClient timedOut=new RubikaClient((url,body)->{attempts[0]++;throw new java.net.SocketTimeoutException("simulation");});
  timedOut.endpoint=api.endpoint;timedOut.privateKey=decoded;
  fails(()->timedOut.send(recipient,"test",1),"Network timeout remains uncertain rather than successful");
  check(attempts[0]==1,"Timeout never triggers an automatic duplicate-send attempt");
  JSONObject recovering=JobStore.create(r,"سلام","owner");JSONObject old=recovering.getJSONArray("recipients").getJSONObject(0);
  old.put("status","uncertain");old.put("error","ممکن است پیام ارسال شده باشد");
  JobStore.review(recovering,0,true,200L);
  check(old.getString("status").equals("sent_manual")&&old.getString("confirmation").equals("user_verified")&&!old.has("sent_at"),"User-observed old send is recorded without fabricating server receipt or send time");
  check(JobStore.next(recovering)==1&&!JobStore.uncertain(recovering),"Reviewed old message excluded from resumed queue");
  fails(()->JobStore.review(recovering,0,true,300L),"Already reviewed item cannot be changed by a stale dialog");
  JSONObject legacy=recovering.getJSONArray("recipients").getJSONObject(1);
  legacy.put("status","skipped");legacy.put("error","نتیجه نامشخص؛ کاربر پس از بررسی از این مورد عبور کرد");
  check(JobStore.needsReview(legacy)&&JobStore.reviewIndex(recovering)==1,"Version 0.1 skipped-unknown reports can be corrected individually");
  JobStore.review(recovering,1,false,400L);
  check(!JobStore.needsReview(legacy)&&JobStore.next(recovering)==-1&&legacy.getString("status").equals("skipped"),"Skip without confirmation neither claims success nor resends");
  System.out.println("ALL "+count+" CHECKS PASSED; no real accounts or messages used.");
 }
}
