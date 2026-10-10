package ir.sinicable.rubikaqueue;
import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class DiscoveryStore {
 public static synchronized JSONObject load(Context c)throws Exception {AtomicFile f=new AtomicFile(new File(c.getFilesDir(),"discovery.json"));return f.getBaseFile().exists()?new JSONObject(new String(f.readFully(),StandardCharsets.UTF_8)):DiscoveryData.empty();}
 public static synchronized void save(Context c,JSONObject s)throws Exception {AtomicFile f=new AtomicFile(new File(c.getFilesDir(),"discovery.json"));FileOutputStream out=null;try{out=f.startWrite();out.write(s.toString().getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}}
}
