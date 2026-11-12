package ir.sinicable.rubikaqueue;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
public final class ExcelReaderCsvTests {
  static int passed=0;
  static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);passed++;}
  static ExcelReader.Result parse(String csv)throws Exception {
    Path f=Files.createTempFile("test-rubika-csv", ".csv");
    try{Files.write(f,csv.getBytes(StandardCharsets.UTF_8));return ExcelReader.read(f.toFile());}
    finally{Files.deleteIfExists(f);}
  }
  public static void main(String[] args)throws Exception {
    ExcelReader.Result a=parse("phone,name\n09123456789,Ali\n09123456789,Ali again\n09123456780,Parsa\ninvalid,x\n");
    check(a.entries.size()==2,"CSV unique contacts");
    check(a.duplicates==1,"deduplication");
    check(a.invalid==1,"invalid row");
    check(a.entries.get(0).phone.equals("989123456789"),"E164 normalized");
    ExcelReader.Result b=parse("شماره;نام\n۰۹۱۲۳۴۵۶۷۸۹;\"علی؛ تست\"\n");
    check(b.entries.size()==1,"Persian digits semicolon separated");
    check(b.entries.get(0).name.equals("علی؛ تست"),"quoted separator");
    ExcelReader.Result c=parse("\uFEFFphone,name\n09123456789,Ali\n");
    check(c.entries.size()==1,"UTF8 BOM");
    ExcelReader.Result d=parse("sep=;\nphone;name\n09123456789;\"Name; Test\"\n");
    check(d.entries.size()==1,"Excel sep directive");
    ExcelReader.Result e=parse("phone,name\n09123456789,\"Ali\nHosseini\"\n");
    check(e.entries.get(0).name.equals("Ali\nHosseini"),"Multiline quoted CSV");
    boolean invalid=false;try{parse("phone,name\n09123456789,\"unclosed");}catch(IOException ex){invalid=true;}
    check(invalid,"Reject malformed quotes");
    System.out.println("PASS "+passed+" CSV checks");
  }
}
