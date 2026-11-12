package ir.sinicable.rubikaqueue;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.net.ssl.HttpsURLConnection;

public class RubikaClient {
 public interface Transport {JSONObject exchange(String url,JSONObject body)throws Exception;}
 public static class ApiError extends IOException {
  public final String code,method;
  public ApiError(String s){this(s,"نامشخص");}
  public ApiError(String s,String m){super("پاسخ روبیکا: "+s+" | عملیات: "+m);code=s;method=m;}
 }
 public static class RateLimitError extends ApiError {
  public final long retryAfterMs;
  public RateLimitError(String code,String method,long retryAfterMs){super(code,method);this.retryAfterMs=Math.max(0L,retryAfterMs);}
 }
 public String auth=CryptoCore.secret(),guid="",phone="",endpoint="";
 public static final int MAX_MESSAGE_RND=1000000;
 private static final String PWA_USER_AGENT="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36";
 public PrivateKey privateKey;
 private final Transport transport;
 private final ArrayList<String> apiEndpoints=new ArrayList<>();
 private int endpointIndex=0;
 static class HostTransportError extends IOException {HostTransportError(String s){super(s);}}
 public RubikaClient(){this(new HttpTransport());}
 public RubikaClient(Transport t){transport=t;}
 public static JSONObject obj(Object... a)throws JSONException {JSONObject j=new JSONObject();for(int i=0;i<a.length;i+=2)j.put((String)a[i],a[i+1]);return j;}
 public static String safeCode(String s){return s.replaceAll("[^A-Za-z0-9_ -]","").substring(0,Math.min(80,s.replaceAll("[^A-Za-z0-9_ -]","").length()));}
 public void discover()throws Exception {
  JSONObject d=transport.exchange("https://getdcmess.iranlms.ir/",null).getJSONObject("data");
  JSONObject apis=d.getJSONObject("API");apiEndpoints.clear();endpointIndex=0;
  addEndpointByCode(apis,d.optString("default_api",""));
  JSONArray preferred=d.optJSONArray("default_apis");
  if(preferred!=null)for(int i=0;i<preferred.length();i++)addEndpointByCode(apis,preferred.optString(i));
  if(apiEndpoints.isEmpty()){Iterator<String> keys=apis.keys();while(keys.hasNext()&&apiEndpoints.size()<5)addEndpointByCode(apis,keys.next());}
  if(apiEndpoints.isEmpty())throw new IOException("سرور روبیکا پیدا نشد");
  endpoint=apiEndpoints.get(0);
 }
 private void addEndpointByCode(JSONObject apis,String code)throws Exception {
  if(code==null||code.isEmpty()||!apis.has(code))return;
  String url=apis.optString(code,"");if(url.isEmpty())return;validateEndpoint(url);
  if(!apiEndpoints.contains(url))apiEndpoints.add(url);
 }
 private boolean safeFailoverMethod(String method){
  return "getUserInfo".equals(method)||"getContacts".equals(method)||"getGroupInfo".equals(method);
 }
 private boolean hostSpecificFailure(Throwable e){
  if(e instanceof ApiError||e instanceof javax.net.ssl.SSLException)return false;
  return e instanceof HostTransportError||e instanceof UnknownHostException||e instanceof ConnectException||
      e instanceof SocketTimeoutException||e instanceof NoRouteToHostException||e instanceof EOFException||e instanceof JSONException;
 }
 public static void validateEndpoint(String s)throws Exception {
  URL u=new URL(s);String h=u.getHost().toLowerCase(Locale.ROOT);
  if(!u.getProtocol().equals("https")||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!(h.endsWith(".iranlms.ir")||h.equals("iranlms.ir")||h.endsWith(".rubika.ir")||h.equals("rubika.ir")))throw new IOException("نشانی سرور معتبر نیست");
 }
 public JSONObject call(String method,JSONObject input,boolean temporary)throws Exception {
  if(endpoint.isEmpty())discover();
  JSONObject client=obj("app_name","Main","app_version","2.4.6","platform","PWA","package","m.rubika.ir","lang_code","fa");
  String encrypted=CryptoCore.encrypt(obj("client",client,"method",method,"input",input).toString(),auth);
  JSONObject request=obj("api_version","6","data_enc",encrypted);
  request.put(temporary?"tmp_session":"auth",temporary?auth:CryptoCore.transform(auth));
  if(!temporary){if(privateKey==null)throw new IOException("ابتدا وارد حساب شوید");request.put("sign",CryptoCore.sign(encrypted,privateKey));}
  int attempts=safeFailoverMethod(method)?Math.max(1,apiEndpoints.size()):1;Exception last=null;
  for(int n=0;n<attempts;n++){
   if(!apiEndpoints.isEmpty()){endpointIndex=Math.floorMod(endpointIndex,apiEndpoints.size());endpoint=apiEndpoints.get(endpointIndex);}
   try{
    JSONObject response=transport.exchange(endpoint,request);
    if(response.has("data_enc"))response=new JSONObject(CryptoCore.decrypt(response.getString("data_enc"),auth));
    if(!response.optString("status").equals("OK")||!response.optString("status_det").equals("OK")){
     String detail=response.optString("status_det","");
     if(detail.isEmpty()||"OK".equals(detail))detail=response.optString("error_code","");
     if(detail.isEmpty())detail=response.optString("error","");
     if(detail.isEmpty())detail=response.optString("message","");
     if(detail.isEmpty())detail=response.optString("status","UNKNOWN");
     throw new ApiError(safeCode(detail),method);
    }
    JSONObject data=response.optJSONObject("data");if(data==null)throw new JSONException("missing data");return data;
   }catch(Exception problem){
    last=problem;
    if(!safeFailoverMethod(method)||!hostSpecificFailure(problem)||n+1>=attempts)throw problem;
    endpointIndex=(endpointIndex+1)%apiEndpoints.size();
   }
  }
  throw last==null?new IOException("سرور روبیکا در دسترس نیست"):last;
 }
 public JSONObject sendCode(String number,String password)throws Exception {
  if(number==null||!number.matches("989[0-9]{9}"))throw new IllegalArgumentException("شماره حساب صحیح نیست");
  if(password==null)password="";
  phone=number;return call("sendCode",obj("phone_number",number,"pass_key",password.isEmpty()?JSONObject.NULL:password,"send_type","SMS"),true);}
 public JSONObject signIn(String code,String hash)throws Exception {
  if(code==null||!code.matches("[0-9]{4,10}")||hash==null||hash.trim().isEmpty())
   throw new IllegalArgumentException("کد ورود یا شناسه تأیید معتبر نیست");
  KeyPair keys=CryptoCore.generate();JSONObject r=call("signIn",obj("phone_code",code,"phone_number",phone,"phone_code_hash",hash,"public_key",CryptoCore.publicWire(keys.getPublic())),true);
  if(!r.optString("status").equals("OK"))throw new ApiError(safeCode(r.optString("status","SIGN_IN_FAILED")));
  String a=CryptoCore.unwrap(r.getString("auth"),keys.getPrivate());if(a.length()!=32)throw new IOException("پاسخ ورود نامعتبر");
  JSONObject user=r.getJSONObject("user");String g=user.getString("user_guid");
  auth=a;privateKey=keys.getPrivate();guid=g;return r;
 }
 public void register()throws Exception {call("registerDevice",obj("token","","lang_code","fa","token_type","Firebase","app_version","PW_2.4.6","system_version","Windows 10","device_model","Chrome 102","device_hash","25010064645373610200053736"),false);}
 public JSONObject me()throws Exception{return call("getUserInfo",obj("user_guid",guid),false);}
 public JSONObject verifySession()throws Exception {
  if(privateKey==null||guid==null||!guid.startsWith("u"))throw new IOException("نشست حساب کامل نیست؛ دوباره وارد شوید");
  JSONObject data=me();
  JSONObject user=data.optJSONObject("user");
  String returned=user==null?data.optString("user_guid"):user.optString("user_guid");
  if(!returned.isEmpty()&&!guid.equals(returned))throw new IOException("پاسخ حساب با نشست ذخیره‌شده تطبیق ندارد");
  return data;
 }
 public String groupTarget(String id)throws Exception {
  if(!id.startsWith("g0")||id.length()<12)throw new IOException("شناسه گروه معتبر نیست");
  JSONObject r=call("getGroupInfo",obj("group_guid",id),false),g=r.optJSONObject("group"),chat=r.optJSONObject("chat");
  if(g==null||!id.equals(g.optString("group_guid")))throw new IOException("گروه قابل تأیید نیست");
  if(g.optBoolean("is_deleted")||(chat!=null&&chat.has("in_chat")&&!chat.optBoolean("in_chat")))throw new IOException("ابتدا عضویت گروه را در روبیکا بررسی کنید");
  JSONArray access=chat==null?null:chat.optJSONArray("access");if(access==null)access=g.optJSONArray("access");if(access==null)access=r.optJSONArray("access");
  boolean permitted=false;if(access!=null)for(int i=0;i<access.length();i++)if("SendMessages".equals(access.optString(i)))permitted=true;
  if(!permitted)throw new IOException("عضویت یا اجازه ارسال گروه تأیید نشد؛ در روبیکا بررسی کنید");return id;
 }
 public String resolve(String number,String name)throws Exception {
  if(number==null||!number.matches("989[0-9]{9}"))
   throw new IllegalArgumentException("شماره تماس Excel معتبر نیست");
  String clean=name==null?"":name.replaceAll("[\\p{Cntrl}\\p{Cf}]","").trim();
  if(clean.length()>60)clean=clean.substring(0,60);if(clean.isEmpty())clean=number;
  JSONObject r=call("addAddressBook",obj("phone",number,"first_name",clean,"last_name",""),false);
  LinkedHashSet<String> ids=new LinkedHashSet<>();collectIds(r,ids,0);
  if(ids.isEmpty()){String fallback=findContactByPhone(number);if(fallback.isEmpty())return "";return verifyResolved(number,fallback);}
  if(ids.size()!=1)throw new IOException("بیش از یک مخاطب پیدا شد؛ ارسال متوقف شد");
  return verifyResolved(number,ids.iterator().next());
 }
 private String findContactByPhone(String number)throws Exception {
  String start="";
  for(int page=0;page<3;page++){
   JSONObject input=start.isEmpty()?new JSONObject():obj("start_id",start);
   JSONObject data=call("getContacts",input,false);String found=findPhoneGuid(data,number,0);if(!found.isEmpty())return found;
   boolean more=data.optBoolean("has_continue",false);String next=data.optString("next_start_id","");
   if(!more||next.isEmpty()||next.equals(start))break;start=next;
  }
  return "";
 }
 private static String findPhoneGuid(Object value,String number,int depth)throws JSONException {
  if(depth>8||value==null)return "";
  if(value instanceof JSONObject){JSONObject o=(JSONObject)value;String id=o.optString("user_guid","");String phone=Phones.normalize(o.optString("phone",""));
   if(id.startsWith("u")&&number.equals(phone))return id;Iterator<String> it=o.keys();while(it.hasNext()){String f=findPhoneGuid(o.opt(it.next()),number,depth+1);if(!f.isEmpty())return f;}}
  else if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++){String f=findPhoneGuid(a.opt(i),number,depth+1);if(!f.isEmpty())return f;}}
  return "";
 }
 private String verifyResolved(String number,String id)throws Exception {
  JSONObject info=call("getUserInfo",obj("user_guid",id),false);JSONObject u=info.optJSONObject("user");
  if(u==null||!id.equals(u.optString("user_guid")))throw new IOException("هویت گیرنده تأیید نشد؛ ارسال متوقف شد");
  String visiblePhone=Phones.normalize(u.optString("phone"));
  if(number.equals(visiblePhone))return id;
  // Hidden profile phone is not proof of mismatch. Require independent matching contact record.
  if(visiblePhone.isEmpty()&&id.equals(findContactByPhone(number)))return id;
  throw new IOException("تطبیق شماره و حساب ممکن نشد؛ ارسال متوقف شد");
 }
 private static void collectIds(Object v,Set<String>s,int depth)throws JSONException {
  if(depth>8)return;
  if(v instanceof JSONObject){JSONObject o=(JSONObject)v;String id=o.optString("user_guid");if(id.startsWith("u")&&id.length()>12)s.add(id);Iterator<String> it=o.keys();while(it.hasNext())collectIds(o.get(it.next()),s,depth+1);}
  else if(v instanceof JSONArray){JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++)collectIds(a.get(i),s,depth+1);}
 }
 public static long retryAfterMillis(String value,long nowMs){
  if(value==null)return 0L;
  String v=value.trim();if(v.isEmpty())return 0L;
  try{
   long seconds=Long.parseLong(v);
   if(seconds<0)return 0L;
   long capped=Math.min(seconds,7L*24L*60L*60L);
   return capped*1000L;
  }catch(Exception ignored){}
  try{
   long target=ZonedDateTime.parse(v,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
   return Math.max(0L,Math.min(target-nowMs,7L*24L*60L*60L*1000L));
  }catch(Exception ignored){return 0L;}
 }
 public static long freshMessageRnd(){return new SecureRandom().nextInt(MAX_MESSAGE_RND)+1L;}
 public static boolean validMessageRnd(long rnd){return rnd>=1&&rnd<=MAX_MESSAGE_RND;}
 public String send(String to,String text,long randomId)throws Exception {
  if(to==null||!to.matches("u[A-Za-z0-9_]{11,64}"))
   throw new IllegalArgumentException("شناسه گیرنده روبیکا معتبر نیست");
  String cleanText=text==null?"":text.trim();
  if(cleanText.isEmpty()||cleanText.length()>4200)
   throw new IllegalArgumentException("متن پیام خالی یا بیش از حد مجاز است");
  if(!validMessageRnd(randomId))throw new IllegalArgumentException("شناسه یکتای پیام معتبر نیست");
  JSONObject r=call("sendMessage",obj("object_guid",to,"text",cleanText,"rnd",randomId),false);
  // call() has already verified BOTH status=OK and status_det=OK.
  // An absent optional message ID must not turn this acknowledgement into a failure.
  // Empty string means server-accepted without a receipt ID, not delivery/read confirmation.
  return SendResponse.messageId(r,to,randomId);
 }
 public JSONObject session()throws Exception{return obj("auth",auth,"private",CryptoCore.privateEncoded(privateKey),"guid",guid,"phone",phone);}
 public static RubikaClient restore(JSONObject s)throws Exception {RubikaClient c=new RubikaClient();c.auth=s.getString("auth");c.privateKey=CryptoCore.privateKey(s.getString("private"));c.guid=s.getString("guid");c.phone=s.getString("phone");return c;}
 static class HttpTransport implements Transport {
  public JSONObject exchange(String endpoint,JSONObject body)throws Exception {
   validateEndpoint(endpoint);HttpsURLConnection c=(HttpsURLConnection)new URL(endpoint).openConnection();
   c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(25000);
   c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("Origin","https://m.rubika.ir");c.setRequestProperty("Referer","https://m.rubika.ir/");c.setRequestProperty("User-Agent",PWA_USER_AGENT);
   try{
    if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream os=c.getOutputStream()){os.write(bytes);}}
    int status=c.getResponseCode();
    if(status==429)throw new RateLimitError("RATE_LIMIT_429","HTTP_429",retryAfterMillis(c.getHeaderField("Retry-After"),System.currentTimeMillis()));
    if(status>=500&&status<=599)throw new HostTransportError("HTTP "+status);
    if(status!=200)throw new IOException("HTTP "+status+"؛ درخواست رد شد");
    try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(b.size()+n>4*1024*1024)throw new IOException("پاسخ سرور بزرگ‌تر از حد مجاز است");b.write(buf,0,n);}return new JSONObject(new String(b.toByteArray(),StandardCharsets.UTF_8));}
   }finally{c.disconnect();}
  }
 }
}
