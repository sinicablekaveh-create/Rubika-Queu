package ir.sinicable.rubikaqueue;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.json.*;
import static org.junit.Assert.*;
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
public class StoreTest {
 @Test public void persistenceIsolationAndDeletion() throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication(); c.deleteDatabase("ai-discovery.db");
  try(AiDiscoveryStore s=new AiDiscoveryStore(c)) {
   s.save("account-a","power",new JSONArray().put(new JSONObject().put("guid","g1").put("name","Power")));
   s.save("account-b","tools",new JSONArray().put(new JSONObject().put("guid","g1").put("name","Tools")));
   s.save("account-a","updated",new JSONArray().put(new JSONObject().put("guid","g1").put("name","Updated")));
  }
  try(AiDiscoveryStore s=new AiDiscoveryStore(c)) {
   assertEquals(1,s.groups("account-a").length());
   assertEquals("Updated",s.groups("account-a").getJSONObject(0).getString("name"));
   assertEquals("Tools",s.groups("account-b").getJSONObject(0).getString("name"));
   assertTrue(s.history("account-a").contains("updated"));
   assertFalse(s.history("account-a").contains("tools"));
   s.clear("account-a"); assertEquals(0,s.groups("account-a").length());
   assertEquals(1,s.groups("account-b").length());
  }
 }
 @Test public void historyBoundedAndEmptyResultsRecorded() throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication(); c.deleteDatabase("ai-discovery.db");
  try(AiDiscoveryStore s=new AiDiscoveryStore(c)) {
   for(int i=0;i<105;i++) s.save("a","query-"+i,new JSONArray());
   assertEquals(100,s.history("a").split("\n").length);
   assertFalse(s.history("a").contains("query-0 •"));
   assertEquals(0,s.groups("a").length());
  }
 }
 @Test public void guidAndLinkAliasesDoNotDuplicateGroups() throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication(); c.deleteDatabase("ai-discovery.db");
  String link="https://rubika.ir/joinc/example";
  try(AiDiscoveryStore s=new AiDiscoveryStore(c)) {
   s.save("a","link-only",new JSONArray().put(new JSONObject().put("link",link).put("name","Cable")));
   s.save("a","resolved",new JSONArray().put(new JSONObject().put("link",link).put("guid","guid-cable").put("name","Cable Updated")));
   assertEquals(1,s.groups("a").length());
   s.save("a","guid-only",new JSONArray().put(new JSONObject().put("guid","guid-cable").put("name","Newest")));
   assertEquals(1,s.groups("a").length());
   assertEquals(link,s.groups("a").getJSONObject(0).getString("link"));
   s.save("a","link-again",new JSONArray().put(new JSONObject().put("link",link).put("name","Cable Revisited")));
   assertEquals(1,s.groups("a").length());
   assertEquals("guid-cable",s.groups("a").getJSONObject(0).getString("guid"));
   s.save("b","isolated",new JSONArray().put(new JSONObject().put("link",link).put("name","Other")));
   assertEquals(1,s.groups("b").length());
   assertEquals(1,s.groups("a").length());
  }
 }
 @Test public void matchingAliasesWithinSingleBatchCoalesce() throws Exception {
  android.content.Context c=RuntimeEnvironment.getApplication(); c.deleteDatabase("ai-discovery.db");
  try(AiDiscoveryStore s=new AiDiscoveryStore(c)) {
   s.save("a","topic",new JSONArray()
       .put(new JSONObject().put("link","https://example.org/group").put("name","Before"))
       .put(new JSONObject().put("link","https://example.org/group").put("guid","g2").put("name","After")));
   assertEquals(1,s.groups("a").length());
   assertEquals("After",s.groups("a").getJSONObject(0).getString("name"));
  }
 }
}
