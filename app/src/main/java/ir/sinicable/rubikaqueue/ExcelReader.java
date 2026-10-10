package ir.sinicable.rubikaqueue;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public final class ExcelReader {
 public static class Entry {public String phone,name;public int row;Entry(String p,String n,int r){phone=p;name=n;row=r;}}
 public static class Result {public final List<Entry> entries=new ArrayList<>();public int invalid,duplicates;}
 static final int MAX_XML=16*1024*1024;
 static Document xml(ZipFile z,String path)throws Exception {
  ZipEntry e=z.getEntry(path);if(e==null)throw new IOException("بخش لازم فایل اکسل پیدا نشد: "+path);
  if(e.getSize()>MAX_XML)throw new IOException("فایل اکسل بیش از حد بزرگ است");
  ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=z.getInputStream(e)){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(b.size()+n>MAX_XML)throw new IOException("حجم بازشده اکسل بیش از حد مجاز است");b.write(buf,0,n);}}
  String raw=new String(b.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
  if(raw.contains("<!DOCTYPE")||raw.contains("<!ENTITY"))throw new IOException("ساختار XML مجاز نیست");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);f.setExpandEntityReferences(false);
  DocumentBuilder p=f.newDocumentBuilder();p.setEntityResolver((publicId,systemId)->{throw new org.xml.sax.SAXException("External entities disabled");});
  return p.parse(new ByteArrayInputStream(b.toByteArray()));
 }
 static NodeList tags(Element e,String t){return e.getElementsByTagNameNS("*",t);}
 static String text(Element e,String t){NodeList a=tags(e,t);return a.getLength()==0?"":a.item(0).getTextContent();}
 static int column(String s)throws IOException {int col=0;for(char c:s.toCharArray()){if(c<'A'||c>'Z')break;col=col*26+c-'A'+1;if(col>16384)throw new IOException("ستون اکسل نامعتبر");}return col-1;}
 static String header(String s){return Phones.digits(s).toLowerCase(Locale.ROOT).replaceAll("[\\s_\\u200c]","");}
 public static Result read(File file)throws Exception {
  if(file.length()>8*1024*1024)throw new IOException("حداکثر حجم فایل ۸ مگابایت است");
  try(ZipFile z=new ZipFile(file)){
   List<String> strings=new ArrayList<>();
   if(z.getEntry("xl/sharedStrings.xml")!=null){NodeList si=xml(z,"xl/sharedStrings.xml").getElementsByTagNameNS("*","si");for(int i=0;i<si.getLength();i++){NodeList ts=tags((Element)si.item(i),"t");StringBuilder s=new StringBuilder();for(int j=0;j<ts.getLength();j++)s.append(ts.item(j).getTextContent());strings.add(s.toString());}}
   Document workbook=xml(z,"xl/workbook.xml");NodeList sheets=workbook.getElementsByTagNameNS("*","sheet");if(sheets.getLength()==0)throw new IOException("اکسل شیت ندارد");
   Element sheet=(Element)sheets.item(0);String id=sheet.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");
   NodeList rels=xml(z,"xl/_rels/workbook.xml.rels").getElementsByTagNameNS("*","Relationship");String path="";
   for(int i=0;i<rels.getLength();i++){Element r=(Element)rels.item(i);if(id.equals(r.getAttribute("Id"))){if(r.getAttribute("TargetMode").equals("External"))throw new IOException("شیت خارجی مجاز نیست");String target=r.getAttribute("Target");path=target.startsWith("/")?target.substring(1):"xl/"+target;}}
   if(path.isEmpty()||path.contains(".."))throw new IOException("مسیر شیت نامعتبر است");
   NodeList rows=xml(z,path).getElementsByTagNameNS("*","row");if(rows.getLength()>20000)throw new IOException("فایل بیش از ۲۰ هزار ردیف دارد");
   Result out=new Result();Set<String> seen=new HashSet<>();int phoneCol=-1,nameCol=-1;boolean selected=false;
   for(int i=0;i<rows.getLength();i++){
    Element row=(Element)rows.item(i);NodeList cells=tags(row,"c");Map<Integer,String> values=new TreeMap<>();
    for(int j=0;j<cells.getLength();j++){
     Element c=(Element)cells.item(j);int col=column(c.getAttribute("r"));if(col<0)col=j;
     String v=text(c,"v"),type=c.getAttribute("t");
     if(tags(c,"f").getLength()>0)v="[FORMULA]";
     else if(type.equals("s")){int index=Integer.parseInt(v);if(index<0||index>=strings.size())throw new IOException("متن اکسل نامعتبر است");v=strings.get(index);}
     else if(type.equals("inlineStr")){NodeList ts=tags(c,"t");StringBuilder s=new StringBuilder();for(int k=0;k<ts.getLength();k++)s.append(ts.item(k).getTextContent());v=s.toString();}
     if(!v.trim().isEmpty())values.put(col,v.trim());
    }
    if(values.isEmpty())continue;
    if(!selected){
     for(Map.Entry<Integer,String> v:values.entrySet()){String h=header(v.getValue());if(Arrays.asList("phone","mobile","phonenumber","number","شماره","شمارهتلفن","شمارههمراه","موبایل","شمارهتماس","تلفن","تلفنهمراه").contains(h))phoneCol=v.getKey();if(Arrays.asList("name","fullname","نام","نامونامخانوادگی","ناممخاطب").contains(h))nameCol=v.getKey();}
     if(phoneCol>=0){selected=true;continue;}
     List<Integer> possible=new ArrayList<>();for(Map.Entry<Integer,String> v:values.entrySet())if(!Phones.normalize(v.getValue()).isEmpty())possible.add(v.getKey());
     if(possible.size()==1){phoneCol=possible.get(0);selected=true;}
     else throw new IOException("در ردیف اول، عنوان ستون شماره را phone و نام را name بنویسید");
    }
    String p=Phones.normalize(values.get(phoneCol));if(p.isEmpty()){out.invalid++;continue;}
    if(!seen.add(p)){out.duplicates++;continue;}
    String name=nameCol>=0?values.get(nameCol):"";if(name==null)name="";if(name.length()>100)name=name.substring(0,100);
    int sourceRow=i+1;try{sourceRow=Integer.parseInt(row.getAttribute("r"));}catch(Exception ignored){}
    out.entries.add(new Entry(p,name,sourceRow));if(out.entries.size()>1000)throw new IOException("حداکثر ۱۰۰۰ شماره یکتا در هر صف مجاز است");
   }
   if(out.entries.isEmpty())throw new IOException("هیچ شماره موبایل معتبر ایرانی پیدا نشد");return out;
  }catch(ZipException e){throw new IOException("فایل باید Excel با پسوند xlsx باشد؛ xls پشتیبانی نمی‌شود");}
 }
}
