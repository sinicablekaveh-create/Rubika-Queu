package ir.sinicable.rubikaqueue;
import java.io.*;
import java.util.*;
import org.json.*;
public final class DiscoveryTests {
 static int checks=0;static final String GROUP="g0public000000000000000000",PRIVATE="g0private000000000000000000",CHANNEL="c0public000000000000000000";
 static final String LINK="https://rubika.ir/joing/BAAAJFHBB0YUNCTMZYGWRCIZUBIFLPAS";
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static class FakeRead implements DiscoveryEngine.ReadApi {List<String> methods=new ArrayList<>();boolean privateChannel=false;
  public JSONObject read(String m,JSONObject input)throws Exception {methods.add(m);
   if(m.equals("getChats"))return RubikaClient.obj("chats",new JSONArray().put(RubikaClient.obj("object_guid",PRIVATE,"abs_object",RubikaClient.obj("title","برق ساختمان خصوصی"),"last_message",RubikaClient.obj("text","تماس خصوصی 09123456786"))));
   if(m.equals("searchGlobalObjects"))return RubikaClient.obj("objects",new JSONArray().put(RubikaClient.obj("channel_guid",CHANNEL,"title","تامین برق","username","public_electric")),"users",new JSONArray().put(RubikaClient.obj("phone","09123456785","user_guid","u0private")));
   if(m.equals("getChannelInfo"))return RubikaClient.obj("channel",RubikaClient.obj("channel_guid",CHANNEL,"title","تامین برق ساختمان","username","public_electric","is_private",privateChannel,"description","فروش لوازم برق؛ تماس ۰۹۱۲۳۴۵۶۷۸۹ برای سفارش\nگروه برق "+LINK));
   if(m.equals("groupPreviewByJoinLink"))return RubikaClient.obj("group",RubikaClient.obj("group_guid",GROUP,"title","گروه برق ساختمان"));
   throw new AssertionError("Unexpected or mutating discovery call "+m);
  }
 }
 static DiscoveryEngine.Progress progress(boolean stopped){return new DiscoveryEngine.Progress(){public boolean stopped(){return stopped;}public void update(JSONObject s,String note)throws Exception{s.put("note",note);}};}
 public static void main(String[] args)throws Exception {
  ok(LeadExtractor.groupLinks("group "+LINK+" duplicate "+LINK).size()==1,"Duplicate published group links normalized");
  String hash=LINK.substring(LINK.lastIndexOf('/')+1);List<String> plus=LeadExtractor.groupLinks("https://rubika.ir/joing/+"+hash+" https://rubika.ir/joing/%2B"+hash);ok(plus.size()==1&&plus.get(0).endsWith("/+"+hash),"Published plus and encoded-plus invitation links normalize without losing the hash prefix");
  ok(!DiscoveryEngine.PublicPages.hasPublicPosts("<html>Site Unavailable</html>"),"HTTP 200 unavailable page is not reported as a checked public feed");
  ok(LeadExtractor.publicBusinessPhones("فروش برق تماس ۰۹۱۲۳۴۵۶۷۸۹","برق","https://rubika.ir/public_electric","برق").length()==1,"Public business contact cue and Persian digits accepted");
  ok(LeadExtractor.publicBusinessPhones("شماره عضو 09123456786","برق","source","برق").length()==0,"Unlabelled member number is not a business lead");
  FakeRead fake=new FakeRead();JSONObject data=DiscoveryData.empty();
  DiscoveryEngine engine=new DiscoveryEngine(fake,url->"<title>منبع برق</title><p>خدمات برق ساختمان؛ تماس 09123456789</p>",0);
  engine.run(data,new String[]{"برق"},true,true,progress(false));
  ok(data.getJSONArray("groups").length()==2,"Public link and user's private group discovered separately");
  ok(data.getJSONArray("contacts").length()==1,"Public phone deduplicated across sources");
  ok(data.getJSONArray("contacts").getJSONObject(0).getString("phone").equals("989123456789"),"No private member or user-profile phone extracted");
  ok(!fake.methods.contains("sendMessage")&&!fake.methods.contains("joinGroup")&&!fake.methods.contains("getGroupAllMembers")&&!fake.methods.contains("getUserInfo"),"Discovery does not send, join, enumerate members or inspect user profiles");
  int gc=data.getJSONArray("groups").length(),pc=data.getJSONArray("contacts").length();engine.run(data,new String[]{"برق"},true,true,progress(false));
  ok(data.getJSONArray("groups").length()==gc&&data.getJSONArray("contacts").length()==pc,"Refreshing preserves results without duplicate rows");
  JSONObject first=data.getJSONArray("contacts").getJSONObject(0);String born=first.getString("first_seen");DiscoveryData.merge(data,"contacts",RubikaClient.obj("phone",first.getString("phone"),"name","نام تازه"),"2030-01-01T00:00:00Z");
  ok(first.getString("first_seen").equals(born)&&first.getString("last_seen").equals("2030-01-01T00:00:00Z")&&first.getString("name").equals("نام تازه"),"Refresh updates last-seen while retaining first-seen");
  JSONObject bridge=DiscoveryData.empty();DiscoveryData.merge(bridge,"groups",RubikaClient.obj("guid",GROUP,"name","گروه برق"),"2026-01-02T00:00:00Z");DiscoveryData.merge(bridge,"groups",RubikaClient.obj("link",LINK,"source","public"),"2026-01-01T00:00:00Z");DiscoveryData.merge(bridge,"groups",RubikaClient.obj("guid",GROUP,"link",LINK),"2026-01-03T00:00:00Z");JSONObject merged=bridge.getJSONArray("groups").getJSONObject(0);ok(bridge.getJSONArray("groups").length()==1&&merged.getString("first_seen").equals("2026-01-01T00:00:00Z")&&merged.getString("source").equals("public"),"Verified group preview bridges separate GUID and link rows while preserving the first observation");
  List<String> limitedMethods=new ArrayList<>();boolean limited=false;try{new DiscoveryEngine((m,in)->{limitedMethods.add(m);throw new RubikaClient.ApiError("REQUEST_LIMIT");},url->{throw new AssertionError("Network call after rate limit");},0).run(DiscoveryData.empty(),new String[]{"برق"},true,true,progress(false));}catch(RubikaClient.ApiError e){limited=true;}ok(limited&&limitedMethods.size()==1&&limitedMethods.get(0).equals("getChats"),"Account chat rate limit stops discovery before further queries or web requests");
  JSONObject groupsOnly=DiscoveryData.empty();new DiscoveryEngine(new FakeRead(),url->"",0).run(groupsOnly,new String[]{"برق"},true,false,progress(false));ok(groupsOnly.getJSONArray("contacts").length()==0,"Group-only option collects no phone numbers");
  FakeRead contactsApi=new FakeRead();JSONObject contactsOnly=DiscoveryData.empty();new DiscoveryEngine(contactsApi,url->"",0).run(contactsOnly,new String[]{"برق"},false,true,progress(false));
  ok(contactsOnly.getJSONArray("groups").length()==0&&!contactsApi.methods.contains("getChats")&&!contactsApi.methods.contains("groupPreviewByJoinLink"),"Contact-only option does not inspect own groups");
  FakeRead hidden=new FakeRead();hidden.privateChannel=true;JSONObject protectedData=DiscoveryData.empty();new DiscoveryEngine(hidden,url->"",0).run(protectedData,new String[]{"برق"},false,true,progress(false));ok(protectedData.getJSONArray("contacts").length()==0,"Private channel description never used for phone extraction");
  FakeRead stopped=new FakeRead();new DiscoveryEngine(stopped,url->{throw new AssertionError("Request after stop");},0).run(data,new String[]{"برق"},true,true,progress(true));ok(stopped.methods.isEmpty(),"Stopped discovery starts no new network requests");
  ok(OperationGate.acquire("queue")&&!OperationGate.acquire("discovery"),"Sending and research cannot run simultaneously");OperationGate.release("discovery");ok(OperationGate.occupied(),"Unrelated service cannot release sending lock");OperationGate.release("queue");ok(OperationGate.acquire("discovery")&&!OperationGate.acquire("test"),"Test messages are blocked while researching");OperationGate.release("discovery");
  JSONArray selected=new JSONArray().put(RubikaClient.obj("guid",GROUP,"name","برق","link",LINK)).put(RubikaClient.obj("guid",GROUP,"name","duplicate"));JSONObject job=JobStore.createGroups(selected,"hello","owner");
  ok(job.getJSONArray("recipients").length()==1&&job.getJSONArray("recipients").getJSONObject(0).getString("kind").equals("group"),"Selected group queue deduplicates targets and remains separate from research");
  final boolean[] maySend={true};RubikaClient api=new RubikaClient((url,body)->RubikaClient.obj("status","OK","status_det","OK","data",RubikaClient.obj("group",RubikaClient.obj("group_guid",GROUP),"chat",RubikaClient.obj("in_chat",maySend[0],"access",new JSONArray().put("SendMessages")))));api.endpoint="https://messengerg2c1.iranlms.ir/";api.privateKey=CryptoCore.generate().getPrivate();
  ok(api.groupTarget(GROUP).equals(GROUP),"Group sending checks membership and send access");maySend[0]=false;boolean denied=false;try{api.groupTarget(GROUP);}catch(IOException e){denied=true;}ok(denied,"Not-member group cannot be sent to");
  first.put("name","=1+1 & آزمایش");try(OutputStream out=new FileOutputStream("tests/discovery-fixture.xlsx")){ResearchXlsx.write(data,out);}ok(new File("tests/discovery-fixture.xlsx").length()>1000,"Actual XLSX workbook generated");
  ExcelReader.Result roundtrip=ExcelReader.read(new File("tests/discovery-fixture.xlsx"));ok(roundtrip.entries.size()==1&&roundtrip.entries.get(0).name.equals("=1+1 & آزمایش"),"Exported public contact sheet reimports as literal text without formula execution");
  System.out.println("ALL "+checks+" DISCOVERY CHECKS PASSED; no live messages used.");
 }
}
