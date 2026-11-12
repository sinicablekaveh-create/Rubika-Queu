package ir.sinicable.rubikaqueue;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class ExcelReaderCsvTest {
 @Test public void persianCsvBOMAndDuplicates()throws Exception{
  Path p=Files.createTempFile("rubika-contacts-",".csv");
  try{
   Files.write(p,("\uFEFFشماره;نام\n۰۹۱۲۳۴۵۶۷۸۹;علی\n09123456789;Duplicate\n09123456780;سارا\n").getBytes(StandardCharsets.UTF_8));
   ExcelReader.Result r=ExcelReader.read(p.toFile());
   assertEquals(2,r.entries.size());assertEquals(1,r.duplicates);
   assertEquals("989123456789",r.entries.get(0).phone);
  } finally {Files.deleteIfExists(p);}
 }
}
