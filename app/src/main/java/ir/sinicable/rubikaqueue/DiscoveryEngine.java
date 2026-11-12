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
 private boolean wantGroups=false,wantContacts=true;
 public DiscoveryEngine(ReadApi a,Pages p,long milliseconds){api=a;pages=p;delay=milliseconds;}
 public static final String[] PUBLIC_SOURCES={};
 private final Set<String> checkedChannels=new HashSet<>(),checkedLinks=new HashSet<>();private int inspected=0;
 private JSONObject read(String method,JSONObject input)throws Exception {
  if(!Arrays.asList("searchGlobalObjects","getChannelInfo").contains(method))throw new IllegalArgumentException("Read-only discovery method required");
  if(delay>0)Thread.sleep(delay);return api.read(method,input);
 }
 public void run(JSONObject state,String[] terms,Progress progress)throws Exception {
  String stamp=DiscoveryData.stamp();state.put("last_run",stamp);state.put("keywords",String.join("، ",terms));state.put("groups",new JSONArray());checkedChannels.clear();checkedLinks.clear();inspected=0;
  // Private account chats are never public discovery results.
  for(String term:terms){if(progress.stopped())break;progress.update(state,"جست‌وجوی روبیکا: "+term);
   try{
    JSONObject r=read("searchGlobalObjects",RubikaClient.obj("search_text",term));
    for(JSONObject c:LeadExtractor.objects(r,"c")){
     if(progress.stopped()||inspected>=36)break;
     String id=LeadExtractor.id(c,"channel");if(!checkedChannels.add(id))continue;inspected++;
     try{
      JSONObject cr=read("getChannelInfo",RubikaClient.obj("channel_guid",id));JSONObject channel=cr.optJSONObject("channel");if(channel==null)continue;
      String username=channel.optString("username",c.optString("username",""));if(!username.matches("[A-Za-z0-9_]{3,64}")||channel.optBoolean("is_private"))continue;
      String title=LeadExtractor.objectTitle(channel),description=channel.optString("description",channel.optString("about",""));
      if(!LeadExtractor.relevant(title+" "+description,terms))continue;
      String publicUrl="https://rubika.ir/"+username;
      String publicHtml=pages.get(publicUrl);
      if(!PublicPages.hasPublicRubikaLanding(publicHtml))continue;
      String publicTitle=LeadExtractor.title(publicHtml);
      String publicDescription=LeadExtractor.plain(publicHtml);
      if(!LeadExtractor.relevant(publicTitle+" "+publicDescription,terms))continue;
      collect(state,publicTitle,publicDescription,publicUrl,term,stamp,progress);progress.update(state,"نتایج منبع «"+publicTitle+"» ذخیره شد");
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
  // Public results come only from an explicit query and live page checks.
  progress.update(state,progress.stopped()?"جست‌وجو متوقف شد؛ یافته‌ها نگه داشته شدند":"جست‌وجو تمام شد؛ تعداد نتایج به منابع در دسترس بستگی دارد");
 }
 public void run(JSONObject state,String[] terms,boolean groups,boolean contacts,Progress progress)throws Exception {wantGroups=false;wantContacts=contacts;run(state,terms,progress);}
 private void collect(JSONObject state,String title,String text,String source,String term,String stamp,Progress progress)throws Exception {
  if(wantContacts){JSONArray phones=LeadExtractor.publicBusinessPhones(text,title,source,term);for(int i=0;i<phones.length();i++)DiscoveryData.merge(state,"contacts",phones.getJSONObject(i),stamp);}

 }
 static boolean rate(Exception e){return e instanceof RubikaClient.ApiError&&((RubikaClient.ApiError)e).code.matches("(?i).*(REQUEST|LIMIT|FLOOD|AUTH|REGISTER).*");}
 static String problem(Exception e){if(e instanceof RubikaClient.ApiError)return e.getMessage();if(e instanceof java.net.SocketTimeoutException)return "مهلت ارتباط تمام شد";if(e instanceof java.net.UnknownHostException)return "منبع در دسترس نیست";return "منبع بررسی نشد: "+e.getClass().getSimpleName();}
 public static class PublicPages implements Pages {
  public static boolean hasPublicPosts(String html){return html.contains("tgme_widget_message_text");}
  public static boolean hasPublicRubikaLanding(String html){
   return html!=null&&(html.contains("user-title")||html.contains("l-title"))&&html.contains("l-desc");
  }
  public String get(String url)throws Exception {
   boolean telegramSource=Arrays.asList(PUBLIC_SOURCES).contains(url);
   boolean rubikaPublicPage=!DiscoveryLinks.rubika(url).isEmpty()&&url.equals(DiscoveryLinks.rubika(url));
   if(!telegramSource&&!rubikaPublicPage)throw new IOException("منبع عمومی مجاز نیست");
   HttpsURLConnection c=(HttpsURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","RubikaQueue/0.3 public business research");
   try{if(c.getResponseCode()!=200)throw new IOException("منبع عمومی در دسترس نیست");try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(b.size()+n>2*1024*1024)throw new IOException("صفحه بزرگ است");b.write(buf,0,n);}String html=new String(b.toByteArray(),StandardCharsets.UTF_8);if(telegramSource&&!hasPublicPosts(html))throw new IOException("متن منبع عمومی در دسترس نیست");if(rubikaPublicPage&&!hasPublicRubikaLanding(html))throw new IOException("صفحهٔ عمومی روبیکا تأیید نشد");return html;}}finally{c.disconnect();}
  }
 }
}
