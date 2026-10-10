package ir.sinicable.rubikaqueue;
import org.json.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.HttpsURLConnection;
public class DiscoveryEngine {
 public interface ReadApi{JSONObject read(String method,JSONObject input)throws Exception;}
 public interface Pages{String get(String url)throws Exception;}
 public interface Progress{boolean stopped();void update(JSONObject state,String note)throws Exception;}
 private final ReadApi api;private final Pages pages;private final long delay;
 private boolean wantGroups=true,wantContacts=true;
 public DiscoveryEngine(ReadApi a,Pages p,long milliseconds){api=a;pages=p;delay=milliseconds;}
 public static final String[] PUBLIC_SOURCES={"https://t.me/s/linkbarghi","https://telegram.me/s/barghkaranejavan"};
 private final Set<String> checkedChannels=new HashSet<>(),checkedLinks=new HashSet<>();private int inspected=0;
 private JSONObject read(String method,JSONObject input)throws Exception {
  if(!Arrays.asList("getChats","searchGlobalObjects","getChannelInfo","groupPreviewByJoinLink").contains(method))throw new IllegalArgumentException("Read-only discovery method required");
  if(delay>0)Thread.sleep(delay);return api.read(method,input);
 }
 public void run(JSONObject state,String[] terms,Progress progress)throws Exception {
  String stamp=DiscoveryData.stamp();state.put("last_run",stamp);state.put("keywords",String.join("، ",terms));checkedChannels.clear();checkedLinks.clear();inspected=0;
  String cursor="";
  try{for(int page=0;wantGroups&&page<8&&!progress.stopped();page++){
   progress.update(state,"بررسی مشخصات گروه‌های خود حساب");
   JSONObject r=read("getChats",RubikaClient.obj("start_id",cursor.isEmpty()?JSONObject.NULL:cursor));
   for(JSONObject o:LeadExtractor.objects(r,"g")){
    String title=LeadExtractor.objectTitle(o);if(!LeadExtractor.relevant(title,terms))continue;
    DiscoveryData.merge(state,"groups",RubikaClient.obj("guid",LeadExtractor.id(o,"group"),"name",title,"visibility","گروه حساب شما؛ ممکن است خصوصی باشد","source","فهرست گفت‌وگوهای حساب","keyword","تطبیق عنوان با عبارت‌ها","availability","دسترسی ارسال هنگام شروع بررسی می‌شود"),stamp);
   }
   String next=r.optString("next_start_id","");if(next.isEmpty()||next.equals(cursor)||next.equals("null"))break;cursor=next;
  }}catch(InterruptedException e){throw e;}catch(Exception e){DiscoveryData.log(state,"گروه‌های حساب",problem(e));if(rate(e))throw e;}
  for(String term:terms){if(progress.stopped())break;progress.update(state,"جست‌وجوی روبیکا: "+term);
   try{
    JSONObject r=read("searchGlobalObjects",RubikaClient.obj("search_text",term));
    if(wantGroups)for(JSONObject g:LeadExtractor.objects(r,"g")){String title=LeadExtractor.objectTitle(g);if(title.isEmpty()||!LeadExtractor.relevant(title,terms))continue;DiscoveryData.merge(state,"groups",RubikaClient.obj("guid",LeadExtractor.id(g,"group"),"name",title,"source","جست‌وجوی عمومی روبیکا: "+term,"keyword",term,"visibility","نتیجه عمومی روبیکا","availability","نیازمند بررسی عضویت"),stamp);}
    for(JSONObject c:LeadExtractor.objects(r,"c")){
     if(progress.stopped()||inspected>=36)break;
     String id=LeadExtractor.id(c,"channel");if(!checkedChannels.add(id))continue;inspected++;
     try{
      JSONObject cr=read("getChannelInfo",RubikaClient.obj("channel_guid",id));JSONObject channel=cr.optJSONObject("channel");if(channel==null)continue;
      String username=channel.optString("username",c.optString("username",""));if(!username.matches("[A-Za-z0-9_]{3,64}")||channel.optBoolean("is_private"))continue;
      String title=LeadExtractor.objectTitle(channel),description=channel.optString("description",channel.optString("about",""));
      if(!LeadExtractor.relevant(title+" "+description,terms))continue;
      collect(state,title,description,"https://rubika.ir/"+username,term,stamp,progress);progress.update(state,"نتایج منبع «"+title+"» ذخیره شد");
     }catch(InterruptedException e){throw e;}catch(Exception e){DiscoveryData.log(state,"کانال عمومی",problem(e));if(rate(e))throw e;}
    }
    DiscoveryData.log(state,term,"بررسی شد");
   }catch(InterruptedException e){throw e;}catch(Exception e){DiscoveryData.log(state,term,problem(e));if(rate(e))throw e;}
   progress.update(state,"نتایج عبارت «"+term+"» ذخیره شد");
  }
  for(String source:PUBLIC_SOURCES){if(progress.stopped())break;progress.update(state,"بررسی منبع عمومی برق و تأسیسات");
   try{String html=pages.get(source);String title=LeadExtractor.title(html),text=LeadExtractor.plain(html);if(LeadExtractor.relevant(title+" "+text,terms))collect(state,title,text,source,"منبع عمومی برق و ساختمان",stamp,progress);DiscoveryData.log(state,source,"بررسی شد");}
   catch(InterruptedException e){throw e;}catch(Exception e){DiscoveryData.log(state,source,problem(e));if(rate(e))throw e;}
  }
  // Published seed found in this request's public web search, independently rechecked by Rubika.
  if(wantGroups&&!progress.stopped()&&LeadExtractor.relevant("گروه پرسش و پاسخ فنی برق",terms)){
   collect(state,"گروه پرسش و پاسخ فنی برق","گروه پرسش و پاسخ فنی برق https://rubika.ir/joing/BAAAJFHBB0YUNCTMZYGWRCIZUBIFLPAS","https://t.me/s/linkbarghi","لینک عمومی اولیه؛ پیش‌نمایش در هر نوبت بررسی می‌شود",stamp,progress);
  }
  progress.update(state,progress.stopped()?"جست‌وجو متوقف شد؛ یافته‌ها نگه داشته شدند":"جست‌وجو تمام شد؛ تعداد نتایج به منابع در دسترس بستگی دارد");
 }
 public void run(JSONObject state,String[] terms,boolean groups,boolean contacts,Progress progress)throws Exception {wantGroups=groups;wantContacts=contacts;run(state,terms,progress);}
 private void collect(JSONObject state,String title,String text,String source,String term,String stamp,Progress progress)throws Exception {
  if(wantContacts){JSONArray phones=LeadExtractor.publicBusinessPhones(text,title,source,term);for(int i=0;i<phones.length();i++)DiscoveryData.merge(state,"contacts",phones.getJSONObject(i),stamp);}
  if(wantGroups)for(String link:LeadExtractor.groupLinks(text)){
   if(progress.stopped()||checkedLinks.size()>=40)break;if(!checkedLinks.add(link))continue;
   JSONObject item=RubikaClient.obj("link",link,"name",title,"source",source,"keyword",term,"visibility","لینک گروه منتشرشدهٔ عمومی","availability","لینک منتشرشده؛ اعتبار تأیید نشده");
   try{String hash=link.substring(link.lastIndexOf('/')+1);JSONObject r=read("groupPreviewByJoinLink",RubikaClient.obj("hash_link",hash));List<JSONObject> gs=LeadExtractor.objects(r,"g");if(gs.size()==1){JSONObject g=gs.get(0);item.put("guid",LeadExtractor.id(g,"group"));String n=LeadExtractor.objectTitle(g);if(!n.isEmpty())item.put("name",n);item.put("availability","پیش‌نمایش از روبیکا دریافت شد؛ عضویت انجام نشد");}}
   catch(InterruptedException e){throw e;}catch(Exception e){item.put("availability","پیش‌نمایش در دسترس نبود");DiscoveryData.log(state,link,problem(e));if(rate(e))throw e;}
   DiscoveryData.merge(state,"groups",item,stamp);
  }
 }
 static boolean rate(Exception e){return e instanceof RubikaClient.ApiError&&((RubikaClient.ApiError)e).code.matches("(?i).*(REQUEST|LIMIT|FLOOD|AUTH|REGISTER).*");}
 static String problem(Exception e){if(e instanceof RubikaClient.ApiError)return e.getMessage();if(e instanceof java.net.SocketTimeoutException)return "مهلت ارتباط تمام شد";if(e instanceof java.net.UnknownHostException)return "منبع در دسترس نیست";return "منبع بررسی نشد: "+e.getClass().getSimpleName();}
 public static class PublicPages implements Pages {
  public static boolean hasPublicPosts(String html){return html.contains("tgme_widget_message_text");}
  public String get(String url)throws Exception {
   if(!Arrays.asList(PUBLIC_SOURCES).contains(url))throw new IOException("منبع عمومی مجاز نیست");
   HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","RubikaQueue/0.3 public business research");
   try{if(c.getResponseCode()!=200)throw new IOException("منبع عمومی در دسترس نیست");try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(b.size()+n>2*1024*1024)throw new IOException("صفحه بزرگ است");b.write(buf,0,n);}String html=new String(b.toByteArray(),StandardCharsets.UTF_8);if(!hasPublicPosts(html))throw new IOException("متن منبع عمومی در دسترس نیست");return html;}}finally{c.disconnect();}
  }
 }
}
