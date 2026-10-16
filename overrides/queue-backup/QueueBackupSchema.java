package ir.sinicable.rubikaqueue;

import org.json.JSONArray;
import org.json.JSONObject;

/** Strict allowlist: queue state only, never session material from Vault. */
public final class QueueBackupSchema {
 private static final String[] HEADER={"owner","message","created","last_attempt","interval_seconds",
   "retry_count","next_retry_time","scheduled_start_at","note"};
 private static final String[] ROW={"phone","name","row","status","rnd","error","kind","guid",
   "link","message_id","confirmation","sent_at","reviewed","reviewed_at"};
 private QueueBackupSchema(){}

 private static JSONObject only(JSONObject src,String[] fields)throws Exception {
  JSONObject out=new JSONObject();
  for(String f:fields)if(src.has(f)&&!src.isNull(f))out.put(f,src.get(f));
  return out;
 }

 public static JSONObject sanitize(JSONObject source)throws Exception {
  if(source==null)throw new IllegalArgumentException("No queue");
  JSONObject out=only(source,HEADER);
  if(out.optString("owner").isEmpty()||out.optString("message").trim().isEmpty())
   throw new IllegalArgumentException("Missing queue owner or message");
  if(out.optString("message").length()>30000)throw new IllegalArgumentException("Message too large");
  JSONArray original=source.getJSONArray("recipients");
  if(original.length()<1||original.length()>5000)throw new IllegalArgumentException("Unexpected queue size");
  JSONArray recipients=new JSONArray();
  for(int i=0;i<original.length();i++){
   JSONObject r=only(original.getJSONObject(i),ROW);
   String state=r.optString("status"),kind=r.optString("kind","contact");
   if(!("pending".equals(state)||"sending".equals(state)||"uncertain".equals(state)
        ||"retry_wait".equals(state)||"sent".equals(state)||"accepted".equals(state)
        ||"skipped".equals(state)||"failed".equals(state)))
    throw new IllegalArgumentException("Unexpected queue state at row "+(i+1));
   if(r.optLong("rnd")<=0)throw new IllegalArgumentException("Missing deduplication id");
   if("group".equals(kind)){
    if(r.optString("guid").isEmpty())throw new IllegalArgumentException("Group identity missing");
   }else if(!"contact".equals(kind)||r.optString("phone").isEmpty()){
    throw new IllegalArgumentException("Contact identity missing");
   }
   recipients.put(r);
  }
  out.put("recipients",recipients);
  return out;
 }

 public static JSONObject forManualRestore(JSONObject queue)throws Exception {
  JSONObject restored=sanitize(queue);
  JSONArray rows=restored.getJSONArray("recipients");
  for(int i=0;i<rows.length();i++){
   JSONObject row=rows.getJSONObject(i);
   if("retry_wait".equals(row.optString("status"))){
    // We cannot safely re-dispatch an interrupted retry automatically.
    row.put("status","uncertain");
    row.put("error","بازیابی صف؛ این مورد باید قبل از ارسال دوباره بررسی شود");
   }
  }
  restored.put("scheduled_start_at",0);
  restored.put("next_retry_time",0);
  restored.put("retry_count",0);
  restored.put("note","بازیابی انجام شد؛ صف متوقف است و ارسال فقط با تأیید دستی آغاز می‌شود");
  return restored;
 }

 public static JSONObject envelope(JSONObject queue,String sourcePackage)throws Exception {
  JSONObject result=new JSONObject();
  result.put("backup_version",1);
  result.put("exported_at",System.currentTimeMillis());
  result.put("source_package",sourcePackage);
  result.put("queue",sanitize(queue));
  return result;
 }

 public static JSONObject fromEnvelope(JSONObject envelope)throws Exception {
  if(envelope.optInt("backup_version",-1)!=1)
   throw new IllegalArgumentException("Unsupported backup version");
  return forManualRestore(envelope.getJSONObject("queue"));
 }
}
