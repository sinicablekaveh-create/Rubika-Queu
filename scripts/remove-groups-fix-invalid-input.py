#!/usr/bin/env python3
"""Fail-closed v1.4 overlay: remove group discovery; validate personal-message inputs."""
from pathlib import Path

ROOT = Path("app/src/main/java/ir/sinicable/rubikaqueue")
def replace_once(text, old, new, note):
    if text.count(old) != 1:
        raise RuntimeError(f"{note}: expected once, found {text.count(old)}")
    return text.replace(old, new, 1)

def update(name, transform):
    p = ROOT / name
    old = p.read_text(encoding="utf-8")
    new = transform(old)
    if old == new:
        raise RuntimeError(f"No change: {name}")
    p.write_text(new, encoding="utf-8")
    print("Patched", name)

def main_screen(s):
    s = replace_once(s,
        "EditText searchTerms;CheckBox searchGroups,searchContacts;Button researchStart,researchExport,researchView,groupSend;",
        "EditText searchTerms;CheckBox searchContacts;Button researchStart,researchExport,researchView;",
        "field removal")
    s = replace_once(s,
        '  button(root,"کشف هوشمند گروه‌ها و تاریخچه",()->startActivity(new Intent(this,AiDiscoveryActivity.class)));\n',
        "", "AI group launcher")
    s = replace_once(s,
        'groupSend.setEnabled(!locked&&signed);searchTerms.setEnabled(!locked);searchGroups.setEnabled(!locked);',
        'searchTerms.setEnabled(!locked);', "refresh state")
    s = replace_once(s,
        '  searchGroups=new CheckBox(this);searchGroups.setText("گروه‌ها؛ عمومی و گروه‌های خودم");searchGroups.setChecked(getSharedPreferences("discovery",0).getBoolean("groups",true));d.addView(searchGroups);\n',
        "", "group checkbox")
    s = replace_once(s,
        '  groupSend=button(d,"ارسال جداگانه به گروه‌های انتخابی",()->chooseGroups());\n',
        "", "group-send button")
    s = replace_once(s,
        '  if(!searchGroups.isChecked()&&!searchContacts.isChecked()){toast("گروه‌ها یا شماره تماس‌ها را انتخاب کنید");return;}',
        '  if(!searchContacts.isChecked()){toast("جست‌وجوی شماره تماس را فعال کنید");return;}',
        "research form")
    s = replace_once(s,
        '.putBoolean("groups",searchGroups.isChecked())',
        '.putBoolean("groups",false)', "persist no-groups")
    s=s.replace('گروه‌ها و شماره تماس‌ها','شماره‌های تماس کاری عمومی')
    s=s.replace('جست‌وجوی گروه و تماس عمومی با شروع صف','جست‌وجوی شماره تماس عمومی با شروع صف')
    s=s.replace('گروه‌ها و شماره‌های تماس','شماره‌های تماس')
    s=s.replace('Rubika-groups-contacts.xlsx','Rubika-public-contacts.xlsx')
    s=s.replace('منابع: جست‌وجوی روبیکا، توضیح کانال‌های عمومی و صفحات عمومی برق.',
                'منابع: توضیح کانال‌های عمومی برق.')
    begin = s.find(' void showResearch(){')
    end = s.find('\n String timeText(', begin)
    if begin == -1 or end == -1:
        raise RuntimeError("Cannot isolate group-only view and dispatch methods")
    newview = ''' void showResearch(){
  try{
   JSONObject state=DiscoveryStore.load(this);
   JSONArray contacts=state.getJSONArray("contacts");
   StringBuilder out=new StringBuilder(DiscoveryData.summary(state)+"\\n\\n");
   for(int i=0;i<Math.min(80,contacts.length());i++){
    JSONObject r=contacts.getJSONObject(i);
    out.append("+").append(r.optString("phone")).append(" • ").append(r.optString("name"))
       .append("\\n").append(r.optString("source")).append("\\n\\n");
   }
   TextView text=new TextView(this);text.setText(out.toString());text.setTextSize(15);
   text.setPadding(dp(18),dp(14),dp(18),dp(14));text.setTextIsSelectable(true);
   ScrollView scroll=new ScrollView(this);scroll.addView(text);
   new AlertDialog.Builder(this).setTitle("شماره‌های تماس عمومی")
      .setView(scroll).setPositiveButton("بستن",null).show();
  }catch(Exception e){toast(error(e));}
 }
'''
    s=s[:begin]+newview+s[end:]
    old='if(e instanceof RubikaClient.ApiError){String code=((RubikaClient.ApiError)e).code;'
    if old not in s:
        raise RuntimeError("Expected transformed API error handler was not found")
    s=replace_once(s,old,old+
        'if("INVALID_INPUT".equalsIgnoreCase(code))return "روبیکا ورودی درخواست «"+'
        '((RubikaClient.ApiError)e).method+"» را رد کرد (INVALID_INPUT). شماره، کد ورود یا متن را بررسی کنید؛ '
        'برای جلوگیری از ارسال تکراری، صف متوقف شد";',
        "API error explanation")
    s=s.replace('getBoolean(QueueStartDiscoveryPolicy.PREFERENCE,true)',
                'getBoolean(QueueStartDiscoveryPolicy.PREFERENCE,false)')
    return s

def engine(s):
    b=s.find('    if(wantGroups)for(JSONObject g:LeadExtractor.objects(r,"g"))')
    e=s.find('    for(JSONObject c:LeadExtractor.objects(r,"c"))',b)
    if b<0 or e<0:raise RuntimeError("Cannot isolate group search results")
    s=s[:b]+s[e:]
    b=s.find('  if(wantGroups)for(String link:LeadExtractor.groupLinks(text))')
    e=s.find('\n }\n static boolean rate(',b)
    if b<0 or e<0:raise RuntimeError("Cannot isolate group-link preview")
    s=s[:b]+s[e:]
    s=replace_once(s,'private boolean wantGroups=true,wantContacts=true;',
                   'private boolean wantGroups=false,wantContacts=true;',
                   "default discovery")
    s=replace_once(s,'wantGroups=groups;wantContacts=contacts;run(state,terms,progress);',
                   'wantGroups=false;wantContacts=contacts;run(state,terms,progress);',
                   "ignore legacy group preferences")
    s=replace_once(s,'checkedChannels.clear();checkedLinks.clear();inspected=0;',
                   'state.put("groups",new JSONArray());checkedChannels.clear();checkedLinks.clear();inspected=0;',
                   "clear legacy group results")
    s=s.replace('"getChats","searchGlobalObjects","getChannelInfo","groupPreviewByJoinLink"',
                '"searchGlobalObjects","getChannelInfo"')
    return s

def discovery_service(s):
    s=replace_once(s,
        'engine.run(data,terms,p.getBoolean("groups",true),p.getBoolean("contacts",true),',
        'engine.run(data,terms,false,p.getBoolean("contacts",true),',
        "discovery runtime flags")
    s=s.replace('جست‌وجوی گروه و تماس کاری','جست‌وجوی شماره تماس کاری')
    s=s.replace('جست‌وجوی گروه‌ها و شماره تماس‌ها','جست‌وجوی شماره‌های تماس کاری')
    return s

def queue_start(s):
    s=replace_once(s,'boolean groups = prefs.getBoolean("groups", true);',
                   'boolean groups = false;', "queue auto-discovery group flag")
    return s

def queue_start_policy(s):
    return s.replace('return enabled && (groups || contacts) && terms != null',
                     'return enabled && contacts && terms != null')

def queue(s):
    s=replace_once(s,'جست‌وجوی خودکار گروه و تماس عمومی پیش از ادامه صف',
                   'جست‌وجوی خودکار شماره‌های تماس عمومی پیش از ادامه صف',
                   "queue status text")
    s=replace_once(s,
        'String guid=current.optString("kind").equals("group")?client.groupTarget(current.getString("guid")):client.resolve(current.getString("phone"),current.optString("name"));',
        '''if("group".equals(current.optString("kind")))
      throw new java.io.IOException("صف گروهی قدیمی غیرفعال است؛ برای ارسال شخصی فایل اکسل جدید انتخاب کنید");
    String guid=client.resolve(current.getString("phone"),current.optString("name"));''',
        "block old group jobs")
    key='''     String c=api.code;
     if(c.contains("TOO_REQUESTS")'''
    replace='''     String c=api.code;
     // An explicit INVALID_INPUT response is a rejected request, not an ambiguous send.
     // Stop the queue before trying another recipient. Do not automatically retry it.
     if("INVALID_INPUT".equalsIgnoreCase(c)){
      dispatch=false;
      current.put("status","pending");
      current.put("error",MainActivity.error(api));
      checkpoint(job,"درخواست روبیکا رد شد (INVALID_INPUT)؛ صف متوقف و مورد در انتظار اصلاح ماند");
      return;
     }
     if(c.contains("TOO_REQUESTS")'''
    s=replace_once(s,key,replace,"invalid send preflight rejection")
    return s

def client(s):
    old='public static class ApiError extends IOException {public final String code;public ApiError(String s){super("پاسخ روبیکا: "+s);code=s;}}'
    new='''public static class ApiError extends IOException {
  public final String code,method;
  public ApiError(String s){this(s,"نامشخص");}
  public ApiError(String s,String m){super("پاسخ روبیکا: "+s+" | عملیات: "+m);code=s;method=m;}
 }'''
    s=replace_once(s,old,new,"method-specific API errors")
    s=replace_once(s,
        'new ApiError(safeCode(response.optString("status_det",response.optString("status","UNKNOWN"))))',
        'new ApiError(safeCode(response.optString("status_det",response.optString("status","UNKNOWN"))),method)',
        "method-specific server rejection")
    s=replace_once(s,
        'public JSONObject sendCode(String number,String password)throws Exception {phone=number;',
        '''public JSONObject sendCode(String number,String password)throws Exception {
  if(number==null||!number.matches("989[0-9]{9}"))throw new IllegalArgumentException("شماره حساب صحیح نیست");
  if(password==null)password="";
  phone=number;''',
        "code request validation")
    s=replace_once(s,
        'public JSONObject signIn(String code,String hash)throws Exception {\n  KeyPair keys=',
        '''public JSONObject signIn(String code,String hash)throws Exception {
  if(code==null||!code.matches("[0-9]{4,10}")||hash==null||hash.trim().isEmpty())
   throw new IllegalArgumentException("کد ورود یا شناسه تأیید معتبر نیست");
  KeyPair keys=''', "login validation")
    s=replace_once(s,
        '  JSONObject r=call("addAddressBook",obj("phone",number,"first_name",name.isEmpty()?number:name,"last_name",""),false);',
        '''  if(number==null||!number.matches("989[0-9]{9}"))
   throw new IllegalArgumentException("شماره تماس Excel معتبر نیست");
  String clean=name==null?"":name.replaceAll("[\\\\p{Cntrl}\\\\p{Cf}]","").trim();
  if(clean.length()>60)clean=clean.substring(0,60);
  if(clean.isEmpty())clean=number;
  JSONObject r=call("addAddressBook",obj("phone",number,"first_name",clean,"last_name",""),false);''',
        "address-book validation")
    s=replace_once(s,
        ' public String send(String to,String text,long randomId)throws Exception {\n  JSONObject r=',
        ''' public String send(String to,String text,long randomId)throws Exception {
  if(to==null||!to.matches("[ug][A-Za-z0-9_]{11,64}"))
   throw new IllegalArgumentException("شناسه گیرنده روبیکا معتبر نیست");
  if(text==null||text.trim().isEmpty()||text.length()>4200)
   throw new IllegalArgumentException("متن پیام خالی یا بیش از حد مجاز است");
  if(randomId<=0)throw new IllegalArgumentException("شناسه یکتای پیام معتبر نیست");
  JSONObject r=''', "message validation")
    return s

def summary(s):
    return replace_once(s,
        'return "گروه‌ها: "+s.getJSONArray("groups").length()+" | تماس کاری عمومی: "+s.getJSONArray("contacts").length()+"\\n"+s.optString("note");',
        'return "شماره‌های تماس کاری عمومی: "+s.getJSONArray("contacts").length()+"\\n"+s.optString("note");',
        "discovery summary")

def workbook(s):
    s=s.replace('گروه‌ها','راهنما')
    old='entry(z,"xl/worksheets/sheet2.xml",sheet(data.getJSONArray("groups"),'
    if old not in s:raise RuntimeError("group worksheet not found")
    start=s.index('   entry(z,"xl/worksheets/sheet2.xml",')
    end=s.index("\n",start)
    s=s[:start]+'   entry(z,"xl/worksheets/sheet2.xml",sheet(new JSONArray(),new String[]{"توضیح"},new String[]{"note"}));'+s[end:]
    if 'data.getJSONArray("groups")' in s:raise RuntimeError("group sheet still present")
    return s

def manifest(s):
    s=replace_once(s,'    <activity android:name=".AiDiscoveryActivity" android:exported="false"/>\n',
        '',"remove AI group activity")
    return s

update("MainActivity.java",main_screen)
update("DiscoveryEngine.java",engine)
update("DiscoveryService.java",discovery_service)
update("QueueStartDiscovery.java",queue_start)
update("QueueStartDiscoveryPolicy.java",queue_start_policy)
update("QueueService.java",queue)
update("RubikaClient.java",client)
update("DiscoveryData.java",summary)
update("ResearchXlsx.java",workbook)
p=Path("app/src/main/AndroidManifest.xml")
p.write_text(manifest(p.read_text(encoding="utf-8")),encoding="utf-8")
print("DONE: group discovery disabled, input rejection handling added")
