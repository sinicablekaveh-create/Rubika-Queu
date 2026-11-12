package ir.sinicable.rubikaqueue;

import org.json.*;
import java.util.LinkedHashSet;
import java.util.Set;

/** Optional receipt extraction, used only after an explicit successful API response. */
public final class SendResponse {
 public static String messageId(JSONObject data,String target,long requestId)throws Exception {
  if(data.has("status")&&!data.isNull("status")&&!"OK".equals(data.optString("status")))
   throw new RubikaClient.ApiError(RubikaClient.safeCode(data.optString("status")));
  Set<String> ids=new LinkedHashSet<>();collect(data,target,requestId,ids,0);
  return ids.size()==1?ids.iterator().next():"";
 }
 private static void collect(Object value,String target,long requestId,Set<String> ids,int depth)throws JSONException {
  if(depth>6)return;
  if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++)collect(a.opt(i),target,requestId,ids,depth+1);return;}
  if(!(value instanceof JSONObject))return;
  JSONObject o=(JSONObject)value;
  if(o.has("object_guid")&&!o.isNull("object_guid")&&!target.equals(o.optString("object_guid")))return;
  if(o.has("rnd")&&!o.isNull("rnd")&&!Long.toString(requestId).equals(String.valueOf(o.opt("rnd"))))return;
  Object id=o.opt("message_id");
  if(id instanceof String||id instanceof Number){String s=String.valueOf(id).trim();if(s.matches("[A-Za-z0-9_-]{1,128}")&&!s.equals("null")&&!s.equals("0"))ids.add(s);}
  // Do not traverse chat history, reply metadata or other unrelated message identifiers.
  for(String key:new String[]{"message","message_update","message_updates"})collect(o.opt(key),target,requestId,ids,depth+1);
 }
}
