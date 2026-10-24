from pathlib import Path
import re
import sys

SOURCE = Path('app/src/main/java/ir/sinicable/rubikaqueue/DiscoveryEngine.java')


def transform(text: str) -> str:
    if 'Private account chats are never public discovery results.' in text:
        return text
    text = re.sub(
        r' public static final String\[\] PUBLIC_SOURCES=\{[^}]*\};',
        ' public static final String[] PUBLIC_SOURCES={};', text, count=1)
    start = text.find('  String cursor="";')
    end = text.find('  for(String term:terms){', start)
    if start < 0 or end < 0:
        raise ValueError('Could not locate private-chat scan boundary')
    text = text[:start] + '  // Private account chats are never public discovery results.\n' + text[end:]
    text = re.sub(
        r'    if\(wantGroups\)for\(JSONObject g:LeadExtractor\.objects\(r,"g"\)\)\{.*?\}\n    for\(JSONObject c:',
        '    if(wantGroups)for(JSONObject g:LeadExtractor.objects(r,"g")) {\n'
        '     if(progress.stopped())break;\n'
        '     String name=LeadExtractor.objectTitle(g);\n'
        '     String username=g.optString("username");\n'
        '     if(username.isEmpty()||g.optBoolean("is_private")||!username.matches("[A-Za-z0-9_]{3,64}"))continue;\n'
        '     String link="https://rubika.ir/"+username;\n'
        '     String html=pages.get(link);\n'
        '     if(!PublicPages.hasPublicRubikaLanding(html))continue;\n'
        '     if(name.isEmpty())name=LeadExtractor.title(html);\n'
        '     String description=LeadExtractor.plain(html);\n'
        '     if(!LeadExtractor.relevant(name+" "+description,terms))continue;\n'
        '     JSONObject item=RubikaClient.obj("guid",LeadExtractor.id(g,"group"),"name",name,"link",link,"source","جست‌وجوی عمومی روبیکا: "+term,"keyword",term,"visibility","صفحهٔ عمومی روبیکا تأیید شد","availability","فقط برای بررسی؛ هنوز مجوز پیام ثبت نشده است");\n'
        '     DiscoveryData.merge(state,"groups",item,stamp);\n'
        '    }\n'
        '    for(JSONObject c:', text, count=1, flags=re.S)
    if 'صفحهٔ عمومی روبیکا تأیید شد' not in text:
        raise ValueError('Could not replace unverified public group classification')
    old = '      collect(state,title,description,"https://rubika.ir/"+username,term,stamp,progress);progress.update(state,"نتایج منبع «"+title+"» ذخیره شد");'
    new = ('      String publicUrl="https://rubika.ir/"+username;\n'
           '      String publicHtml=pages.get(publicUrl);\n'
           '      if(!PublicPages.hasPublicRubikaLanding(publicHtml))continue;\n'
           '      String publicTitle=LeadExtractor.title(publicHtml);\n'
           '      String publicDescription=LeadExtractor.plain(publicHtml);\n'
           '      if(!LeadExtractor.relevant(publicTitle+" "+publicDescription,terms))continue;\n'
           '      collect(state,publicTitle,publicDescription,publicUrl,term,stamp,progress);progress.update(state,"نتایج منبع «"+publicTitle+"» ذخیره شد");')
    if old not in text:
        raise ValueError('Could not locate public channel verification point')
    text = text.replace(old, new, 1)
    start = text.find('  // Published seed found in this request')
    end = text.find('  progress.update(state,progress.stopped()', start)
    if start < 0 or end < 0:
        raise ValueError('Could not locate unverified hardcoded seed')
    text = text[:start] + "  // Public results come only from an explicit query and live page checks.\n" + text[end:]
    old = '   JSONObject item=RubikaClient.obj("link",link,"name",title,"source",source,"keyword",term,"visibility","لینک گروه منتشرشدهٔ عمومی","availability","لینک منتشرشده؛ اعتبار تأیید نشده");'
    if old not in text:
        raise ValueError('Could not locate invite-link validation block')
    text = text.replace(old, '   String publicLink=DiscoveryLinks.rubika(link);if(publicLink.isEmpty())continue;\n   JSONObject item=RubikaClient.obj("link",publicLink,"name",title,"source",source,"keyword",term,"visibility","لینک عمومی؛ پیش‌نمایش روبیکا تأیید شد","availability","برای بررسی؛ بدون عضویت یا ارسال");', 1)
    old = '   try{String hash=link.substring(link.lastIndexOf(\'/\')+1);JSONObject r=read("groupPreviewByJoinLink",RubikaClient.obj("hash_link",hash));List<JSONObject> gs=LeadExtractor.objects(r,"g");if(gs.size()==1){JSONObject g=gs.get(0);item.put("guid",LeadExtractor.id(g,"group"));String n=LeadExtractor.objectTitle(g);if(!n.isEmpty())item.put("name",n);item.put("availability","پیش‌نمایش از روبیکا دریافت شد؛ عضویت انجام نشد");}}'
    new = '   boolean verified=false;try{String hash=publicLink.substring(publicLink.lastIndexOf(\'/\')+1);JSONObject r=read("groupPreviewByJoinLink",RubikaClient.obj("hash_link",hash));List<JSONObject> gs=LeadExtractor.objects(r,"g");if(gs.size()==1&&!gs.get(0).optBoolean("is_private")){JSONObject g=gs.get(0);item.put("guid",LeadExtractor.id(g,"group"));String n=LeadExtractor.objectTitle(g);if(!n.isEmpty())item.put("name",n);item.put("availability","پیش‌نمایش روبیکا تأیید شد؛ عضویت انجام نشد");verified=true;}}'
    if old not in text:
        raise ValueError('Could not locate invite preview check')
    text = text.replace(old, new, 1)
    text = text.replace('   DiscoveryData.merge(state,"groups",item,stamp);\n  }\n }', '   if(verified)DiscoveryData.merge(state,"groups",item,stamp);\n  }\n }', 1)
    marker = '  public String get(String url)throws Exception {\n   if(!Arrays.asList(PUBLIC_SOURCES).contains(url))throw new IOException("منبع عمومی مجاز نیست");'
    replacement = ('  public static boolean hasPublicRubikaLanding(String html){\n'
        '   return html!=null&&(html.contains("user-title")||html.contains("l-title"))&&html.contains("l-desc");\n'
        '  }\n'
        '  public String get(String url)throws Exception {\n'
        '   boolean telegramSource=Arrays.asList(PUBLIC_SOURCES).contains(url);\n'
        '   boolean rubikaPublicPage=!DiscoveryLinks.rubika(url).isEmpty()&&url.equals(DiscoveryLinks.rubika(url));\n'
        '   if(!telegramSource&&!rubikaPublicPage)throw new IOException("منبع عمومی مجاز نیست");')
    if marker not in text:
        raise ValueError('Could not locate public HTTP allowlist')
    text = text.replace(marker, replacement, 1)
    text = text.replace('if(!hasPublicPosts(html))throw new IOException("متن منبع عمومی در دسترس نیست");return html;',
        'if(telegramSource&&!hasPublicPosts(html))throw new IOException("متن منبع عمومی در دسترس نیست");if(rubikaPublicPage&&!hasPublicRubikaLanding(html))throw new IOException("صفحهٔ عمومی روبیکا تأیید نشد");return html;',1)
    return text


def main() -> None:
    source = Path(sys.argv[1]) if len(sys.argv) > 1 else SOURCE
    if not source.exists():
        raise SystemExit('DiscoveryEngine.java missing; run after extracting the audited Android source')
    original = source.read_text(encoding='utf-8')
    if 'Private account chats are never public discovery results.' in original:
        print('Public discovery safety already applied')
        return
    source.write_text(transform(original), encoding='utf-8')
    print('Applied public-only discovery checks')

if __name__ == '__main__':
    main()
