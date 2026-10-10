package ir.sinicable.rubikaqueue;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;
public final class DiscoveryData {
 public static JSONObject empty()throws Exception{return RubikaClient.obj("groups",new JSONArray(),"contacts",new JSONArray(),"logs",new JSONArray(),"note","هنوز جست‌وجویی انجام نشده");}
 public static String stamp(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
 public static void merge(JSONObject state,String collection,JSONObject item,String stamp)throws Exception {
  JSONArray a=state.getJSONArray(collection);JSONObject found=null;
  for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);boolean same=collection.equals("contacts")?r.optString("phone").equals(item.optString("phone")):(!item.optString("guid").isEmpty()&&item.optString("guid").equals(r.optString("guid")))||(!item.optString("link").isEmpty()&&item.optString("link").equals(r.optString("link")));if(same){
   if(found==null){found=r;continue;}
   String older=r.optString("first_seen"),current=found.optString("first_seen");if(!older.isEmpty()&&(current.isEmpty()||older.compareTo(current)<0))found.put("first_seen",older);
   Iterator<String> previous=r.keys();while(previous.hasNext()){String k=previous.next();if(found.optString(k).isEmpty())found.put(k,r.opt(k));}
   a.remove(i--);
  }}
  if(found==null){if(a.length()>=1000)return;found=new JSONObject();found.put("first_seen",stamp);a.put(found);}
  Iterator<String> keys=item.keys();while(keys.hasNext()){String k=keys.next();Object v=item.opt(k);if(v!=null&&v!=JSONObject.NULL&&!v.toString().isEmpty())found.put(k,v);}
  found.put("last_seen",stamp);
 }
 public static void log(JSONObject state,String source,String status)throws Exception {
  JSONArray a=state.getJSONArray("logs");a.put(RubikaClient.obj("time",stamp(),"source",source,"status",status));if(a.length()>100){JSONArray b=new JSONArray();for(int i=a.length()-100;i<a.length();i++)b.put(a.get(i));state.put("logs",b);}
 }
 public static String summary(JSONObject s)throws Exception{return "گروه‌ها: "+s.getJSONArray("groups").length()+" | تماس کاری عمومی: "+s.getJSONArray("contacts").length()+"\n"+s.optString("note");}
}
