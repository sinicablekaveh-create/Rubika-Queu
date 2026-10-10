package ir.sinicable.rubikaqueue;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

public class RubikaClient {
 public interface Transport {JSONObject exchange(String url,JSONObject body)throws Exception;}
 public static class ApiError extends IOException {public final String code;public ApiError(String s){super("پاسخ روبیکا: "+s);code=s;}}
 public String auth=CryptoCore.secret(),guid="",phone="",endpoint="";
 public PrivateKey privateKey;
 private final Transport transport;
 public RubikaClient(){this(new HttpTransport());}
 public RubikaClient(Transport t){transport=t;}
 public static JSONObject obj(Object... a)throws JSONException {JSONObject j=new JSONObject();for(int i=0;i<a.length;i+=2)j.put((String)a[i],a[i+1]);return j;}
 public static String safeCode(String s){return s.replaceAll("[^A-Za-z0-9_ -]","").substring(0,Math.min(80,s.replaceAll("[^A-Za-z0-9_ -]","").length()));}
 public void discover()throws Exception {
  JSONObject d=transport.exchange("https://getdcmess.iranlms.ir/",null).getJSONObject("data");JSONObject apis=d.getJSONObject("API");
  String code=d.optString("default_api","");
  if(!apis.has(code)){JSONArray a=d.optJSONArray("default_apis");if(a!=null&&a.length()>0)code=a.getString(0);}
  if(!apis.has(code)){Iterator<String> k=apis.keys();if(!k.hasNext())throw new IOException("سرور روبیکا پیدا نشد");code=k.next();}
  endpoint=apis.getString(code);validateEndpoint(endpoint);
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
  JSONObject response=transport.exchange(endpoint,request);
  if(response.has("data_enc"))response=new JSONObject(CryptoCore.decrypt(response.getString("data_enc"),auth));
  if(!response.optString("status").equals("OK")||!response.optString("status_det").equals("OK"))throw new ApiError(safeCode(response.optString("status_det",response.optString("status","UNKNOWN"))));
  JSONObject data=response.optJSONObject("data");if(data==null)throw new IOException("ساختار پاسخ روبیکا تغییر کرده است");return data;
 }
 public JSONObject sendCode(String number,String password)throws Exception {phone=number;return call("sendCode",obj("phone_number",number,"pass_key",password.isEmpty()?JSONObject.NULL:password,"send_type","SMS"),true);}
 public JSONObject signIn(String code,String hash)throws Exception {
  KeyPair keys=CryptoCore.generate();JSONObject r=call("signIn",obj("phone_code",code,"phone_number",phone,"phone_code_hash",hash,"public_key",CryptoCore.publicWire(keys.getPublic())),true);
  if(!r.optString("status").equals("OK"))throw new ApiError(safeCode(r.optString("status","SIGN_IN_FAILED")));
  String a=CryptoCore.unwrap(r.getString("auth"),keys.getPrivate());if(a.length()!=32)throw new IOException("پاسخ ورود نامعتبر");
  JSONObject user=r.getJSONObject("user");String g=user.getString("user_guid");
  auth=a;privateKey=keys.getPrivate();guid=g;return r;
 }
 public void register()throws Exception {call("registerDevice",obj("token","","lang_code","fa","token_type","Firebase","app_version","PW_2.4.6","system_version","Android","device_model","Rubika Queue (unofficial)","device_hash","2"+Math.abs(guid.hashCode())),false);}
 public JSONObject me()throws Exception{return call("getUserInfo",new JSONObject(),false);}
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
  JSONObject r=call("addAddressBook",obj("phone",number,"first_name",name.isEmpty()?number:name,"last_name",""),false);
  LinkedHashSet<String> ids=new LinkedHashSet<>();collectIds(r,ids,0);
  if(ids.isEmpty())return "";
  if(ids.size()!=1)throw new IOException("بیش از یک مخاطب پیدا شد؛ ارسال متوقف شد");
  String id=ids.iterator().next();JSONObject info=call("getUserInfo",obj("user_guid",id),false);JSONObject u=info.optJSONObject("user");
  if(u==null||!id.equals(u.optString("user_guid"))||!number.equals(Phones.normalize(u.optString("phone"))))throw new IOException("تطبیق شماره و حساب ممکن نشد؛ ارسال متوقف شد");
  return id;
 }
 private static void collectIds(Object v,Set<String>s,int depth)throws JSONException {
  if(depth>8)return;
  if(v instanceof JSONObject){JSONObject o=(JSONObject)v;String id=o.optString("user_guid");if(id.startsWith("u")&&id.length()>12)s.add(id);Iterator<String> it=o.keys();while(it.hasNext())collectIds(o.get(it.next()),s,depth+1);}
  else if(v instanceof JSONArray){JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++)collectIds(a.get(i),s,depth+1);}
 }
 public String send(String to,String text,long randomId)throws Exception {
  JSONObject r=call("sendMessage",obj("object_guid",to,"text",text,"rnd",randomId),false);
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
   c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setRequestProperty("Origin","https://m.rubika.ir");c.setRequestProperty("Referer","https://m.rubika.ir/");c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");
   try{
    if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream os=c.getOutputStream()){os.write(bytes);}}
    int status=c.getResponseCode();if(status==429)throw new ApiError("RATE_LIMIT_429");if(status!=200)throw new IOException("HTTP "+status+"؛ صف متوقف شد");
    try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(b.size()+n>4*1024*1024)throw new IOException("پاسخ سرور بزرگ‌تر از حد مجاز است");b.write(buf,0,n);}return new JSONObject(new String(b.toByteArray(),StandardCharsets.UTF_8));}
   }finally{c.disconnect();}
  }
 }
}
