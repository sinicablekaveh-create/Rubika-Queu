package ir.sinicable.rubikaqueue;
import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.json.*;
public final class JobStore {
 public static synchronized JSONObject load(Context c)throws Exception {
  AtomicFile f=new AtomicFile(new File(c.getFilesDir(),"job.json"));if(!f.getBaseFile().exists())return null;
  return new JSONObject(new String(f.readFully(),StandardCharsets.UTF_8));
 }
 public static synchronized void save(Context c,JSONObject j)throws Exception {
  AtomicFile f=new AtomicFile(new File(c.getFilesDir(),"job.json"));FileOutputStream out=null;
  try{out=f.startWrite();out.write(j.toString().getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}
 }
 public static JSONObject create(ExcelReader.Result r,String message,String owner)throws Exception {
  JSONArray a=new JSONArray();SecureRandom random=new SecureRandom();
  for(ExcelReader.Entry e:r.entries)a.put(RubikaClient.obj("phone",e.phone,"name",e.name,"row",e.row,"status","pending","rnd",random.nextInt(999999999)+1,"error",""));
  return RubikaClient.obj("recipients",a,"message",message,"owner",owner,"last_attempt",0,"interval_seconds",SendInterval.MIN_SECONDS,"note","آماده؛ هنوز پیامی ارسال نشده","created",System.currentTimeMillis());
 }
 public static int next(JSONObject j)throws Exception {JSONArray a=j.getJSONArray("recipients");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("status").equals("pending"))return i;return -1;}
 public static JSONObject createGroups(JSONArray selected,String message,String owner)throws Exception {
  JSONArray a=new JSONArray();java.util.Set<String> seen=new java.util.HashSet<>();SecureRandom random=new SecureRandom();
  for(int i=0;i<selected.length();i++){JSONObject g=selected.getJSONObject(i);String id=g.optString("guid");if(!id.startsWith("g0")||!seen.add(id))continue;
   a.put(RubikaClient.obj("kind","group","guid",id,"name",g.optString("name"),"link",g.optString("link"),"phone","","row",i+1,"status","pending","rnd",random.nextInt(999999999)+1,"error",""));}
  if(a.length()==0)throw new IOException("هیچ گروه دارای شناسه انتخاب نشده است");
  return RubikaClient.obj("recipients",a,"message",message,"owner",owner,"last_attempt",0,"interval_seconds",SendInterval.MIN_SECONDS,"note","صف گروه‌ها آماده است؛ هنوز ارسال نشده","created",System.currentTimeMillis());
 }
 public static String destination(JSONObject r){return r.optString("kind").equals("group")?"گروه: "+r.optString("name")+"\n"+r.optString("link"):"شماره: +"+r.optString("phone")+"\nنام: "+r.optString("name");}
 public static boolean needsReview(JSONObject r){String s=r.optString("status");return s.equals("sending")||s.equals("uncertain")||(s.equals("skipped")&&!r.optBoolean("reviewed")&&r.optString("error").contains("نتیجه نامشخص"));}
 public static int reviewIndex(JSONObject j)throws Exception {JSONArray a=j.getJSONArray("recipients");for(int i=0;i<a.length();i++)if(needsReview(a.getJSONObject(i)))return i;return -1;}
 public static boolean uncertain(JSONObject j)throws Exception {return reviewIndex(j)>=0;}
 public static void recordSuccess(JSONObject r,String messageId,long now)throws Exception {
  r.put("status",messageId.isEmpty()?"accepted":"sent");r.put("confirmation",messageId.isEmpty()?"server_ok":"message_id");
  r.put("message_id",messageId);r.put("sent_at",now);r.put("error","");
 }
 public static void review(JSONObject job,int index,boolean seen,long now)throws Exception {
  JSONObject r=job.getJSONArray("recipients").getJSONObject(index);if(!needsReview(r))throw new IOException("وضعیت این مورد تغییر کرده است");
  r.put("previous_error",r.optString("error"));r.put("reviewed",true);r.put("reviewed_at",now);
  r.put("status",seen?"sent_manual":"skipped");r.put("confirmation",seen?"user_verified":"user_skipped");
  if(seen)r.put("sent_at",now);
  r.put("error",seen?"":"کاربر بدون تأیید ارسال از این مورد عبور کرد");
  job.put("note",seen?"ارسال این مورد با مشاهدهٔ شما تأیید شد؛ برای باقی‌مانده‌ها ادامه را بزنید":"از این مورد عبور شد؛ برای باقی‌مانده‌ها ادامه را بزنید");
 }
 public static String statusLabel(JSONObject r){
  if(needsReview(r))return "نیازمند بررسی؛ نتیجه ارسال نامشخص";
  switch(r.optString("status")){
   case "sent":return "ارسال با شناسه پیام ثبت شد";
   case "accepted":return "پذیرفته‌شده توسط سرور؛ بدون شناسه پیام";
   case "sent_manual":return "مشاهده پیام در روبیکا توسط کاربر تأیید شد";
   case "skipped":return "عبور داده‌شده؛ تأیید ارسال ثبت نشده";
   case "pending":return "در انتظار ارسال";
   case "retry_wait":return "در انتظار تلاش مجدد";
   default:return "نیازمند بررسی";
  }
 }
 public static String summary(JSONObject j)throws Exception {
  JSONArray a=j.getJSONArray("recipients");int sent=0,accepted=0,manual=0,skip=0,pending=0,unknown=0;
  for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);String s=r.optString("status");if(needsReview(r))unknown++;else if(s.equals("sent"))sent++;else if(s.equals("accepted"))accepted++;else if(s.equals("sent_manual"))manual++;else if(s.equals("skipped"))skip++;else if(s.equals("pending")||s.equals("retry_wait"))pending++;else unknown++;}
  int done=sent+accepted+manual;
  return "کل: "+a.length()+" | ثبت ارسال: "+done+" | در انتظار: "+pending+"\nشناسهٔ پیام: "+sent+" | پذیرفتهٔ سرور: "+accepted+" | تأیید دستی: "+manual+"\nناموفق/عبور شده: "+skip+" | نیازمند بررسی: "+unknown+"\n"+j.optString("note");
 }
}
