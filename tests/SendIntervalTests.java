import ir.sinicable.rubikaqueue.SendInterval;
import org.json.JSONObject;
public final class SendIntervalTests {
 private static void check(boolean ok, String label) { if(!ok)throw new AssertionError(label); }
 public static void main(String[] args) throws Exception {
  check(SendInterval.requireValid("30")==30,"min");
  check(SendInterval.requireValid("60")==60,"chosen");
  check(SendInterval.requireValid("3600")==3600,"max");
  for(String s:new String[]{"29","3601","","abc"}) {
   try{SendInterval.requireValid(s);throw new AssertionError("accepted "+s);}catch(IllegalArgumentException expected){}
  }
  JSONObject job=new JSONObject();
  check(SendInterval.fromJob(job)==30,"legacy job fallback");
  job.put("interval_seconds",60);
  check(SendInterval.fromJob(job)==60,"persisted interval");
  check(SendInterval.remainingMs(1000,11000,30)==20000L,"remaining");
  check(SendInterval.remainingMs(1000,33000,30)==0,"elapsed");
  check(SendInterval.remainingMs(1000,500,30)==30000,"clock reversal safety");
  check(SendInterval.remainingMs(0,500,30)==0,"new job immediate");
  System.out.println("SendIntervalTests: PASS");
 }
}
