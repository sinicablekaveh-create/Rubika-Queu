package ir.sinicable.rubikaqueue;
import org.json.*;
import java.util.*;
import java.util.regex.*;
public final class LeadExtractor {
 public static final String DEFAULT_TERMS="برق\nبرق ساختمان\nبرقکار\nتاسیسات برقی\nسینی کابل\nتابلو برق\nپیمانکار برق\nلوازم برق\nهوشمندسازی ساختمان\nبرق صنعتی\nپیمانکار ساختمان\nسیم و کابل";
 public static String normalize(String s){return Phones.digits(s).replace('ي','ی').replace('ك','ک').replace('\u200c',' ').toLowerCase(Locale.ROOT);}
 public static boolean relevant(String s,String[] terms){String n=normalize(s);for(String t:terms)if(!t.trim().isEmpty()&&n.contains(normalize(t.trim())))return true;return false;}
 public static String[] terms(String s){LinkedHashSet<String> set=new LinkedHashSet<>();for(String t:s.split("[\\n،,;]+")){t=t.trim();if(t.length()>=2&&t.length()<=70)set.add(t);if(set.size()==12)break;}return set.toArray(new String[0]);}
 public static String plain(String html){return html.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>"," ").replaceAll("(?i)<br\\s*/?>|</(?:p|div|li)>","\n").replaceAll("<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">").replaceAll("[ \\t]+"," ");}
 public static String title(String html){Matcher m=Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);return m.find()?plain(m.group(1)).trim():"منبع عمومی حوزه برق";}
 public static List<String> groupLinks(String text){LinkedHashSet<String> links=new LinkedHashSet<>();Matcher m=Pattern.compile("(?i)(?:https?://)?(?:www\\.)?rubika\\.ir/joing/((?:\\+|%2b)?[a-z0-9]{16,80})(?![a-z0-9])").matcher(text);while(m.find())links.add("https://rubika.ir/joing/"+m.group(1).replaceAll("(?i)%2b","+"));return new ArrayList<>(links);}
 public static JSONArray publicBusinessPhones(String text,String title,String source,String query)throws Exception {
  // Call only on public business descriptions/posts, never member lists, chats or user profiles.
  JSONArray out=new JSONArray();String n=normalize(text);Set<String> seen=new HashSet<>();
  Matcher m=Pattern.compile("(?<![0-9])(?:\\+?98|0098|0)?9(?:[ \\t-]*[0-9]){9}(?![0-9])").matcher(n);
  while(m.find()){
   String p=Phones.normalize(m.group());if(p.isEmpty()||!seen.add(p))continue;
   int a=Math.max(0,m.start()-180),b=Math.min(n.length(),m.end()+120);String context=n.substring(a,b);
   if(!Pattern.compile("تماس|سفارش|فروش|واتساپ|واتس اپ|whatsapp|تلفن|مشاوره|اجرای|اجرا و|خدمات").matcher(context).find())continue;
   out.put(RubikaClient.obj("phone",p,"name",title,"source",source,"keyword",query,"excerpt",text.substring(Math.min(a,text.length()),Math.min(b,text.length())).trim(),"visibility","تماس کاری منتشرشده؛ نیازمند بررسی"));
  }return out;
 }
 public static List<JSONObject> objects(JSONObject response,String prefix){List<JSONObject> out=new ArrayList<>();Set<String> seen=new HashSet<>();walk(response,prefix,out,seen,0);return out;}
 private static void walk(Object v,String prefix,List<JSONObject> out,Set<String> seen,int depth){if(depth>8)return;
  if(v instanceof JSONObject){JSONObject o=(JSONObject)v;String id=o.optString(prefix.equals("g")?"group_guid":"channel_guid","");if(id.isEmpty())id=o.optString("object_guid","");
   if(id.startsWith(prefix+"0")&&seen.add(id))out.add(o);
   Iterator<String> keys=o.keys();while(keys.hasNext()){String key=keys.next();if(key.equals("last_message")||key.equals("messages")||key.equals("members")||key.equals("users"))continue;walk(o.opt(key),prefix,out,seen,depth+1);}}
  else if(v instanceof JSONArray){JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++)walk(a.opt(i),prefix,out,seen,depth+1);}
 }
 public static String id(JSONObject o,String type){String id=o.optString(type+"_guid","");return id.isEmpty()?o.optString("object_guid",""):id;}
 public static String objectTitle(JSONObject o){String s=o.optString("title","");if(s.isEmpty())s=o.optString("name","");for(String key:new String[]{"abs_object","group","channel","object"}){JSONObject abs=o.optJSONObject(key);if(s.isEmpty()&&abs!=null)s=objectTitle(abs);}return s;}
}
