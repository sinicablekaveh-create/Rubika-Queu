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
 @Test public void distinctGuidGroupsMustNotMergeWhenLinksMatch() throws Exception {
  android.content.Context context=RuntimeEnvironment.getApplication();
  context.deleteDatabase("ai-discovery.db");
  String link="https://rubika.ir/joinc/reused";
  try(AiDiscoveryStore store=new AiDiscoveryStore(context)) {
   store.save("account","first",new JSONArray().put(
       new JSONObject().put("guid","group-1").put("link",link).put("name","First")));
   store.save("account","second",new JSONArray().put(
       new JSONObject().put("guid","group-2").put("link",link).put("name","Second")));
   JSONArray actual=store.groups("account");
   assertEquals(2,actual.length());
   java.util.Set<String> identifiers=new java.util.HashSet<>();
   for(int i=0;i<actual.length();i++) identifiers.add(actual.getJSONObject(i).getString("guid"));
   assertTrue(identifiers.contains("group-1"));
   assertTrue(identifiers.contains("group-2"));
  }
 }

 @Test public void ambiguousLinkOnlyResultCannotOverwriteDistinctGuidGroups() throws Exception {
  android.content.Context context=RuntimeEnvironment.getApplication();
  context.deleteDatabase("ai-discovery.db");
  String link="https://rubika.ir/joing/reused";
  try(AiDiscoveryStore store=new AiDiscoveryStore(context)) {
   store.save("account","first",new JSONArray().put(
       new JSONObject().put("guid","group-1").put("link",link).put("name","Owner One")));
   store.save("account","second",new JSONArray().put(
       new JSONObject().put("guid","group-2").put("link",link).put("name","Owner Two")));
   store.save("account","ambiguous",new JSONArray().put(
       new JSONObject().put("link",link).put("name","Unverified Name")));
   JSONArray groups=store.groups("account");
   assertEquals(2,groups.length());
   java.util.Set<String> names=new java.util.HashSet<>();
   for(int i=0;i<groups.length();i++) names.add(groups.getJSONObject(i).getString("name"));
   assertTrue(names.contains("Owner One"));
   assertTrue(names.contains("Owner Two"));
   assertFalse(names.contains("Unverified Name"));
   assertTrue(store.history("account").contains("ambiguous"));
  }
 }
 @Test public void suggestedTermsPersistPerAccountAndQuery() throws Exception {
  android.content.Context context=RuntimeEnvironment.getApplication();
  context.deleteDatabase("ai-discovery.db");
  try(AiDiscoveryStore store=new AiDiscoveryStore(context)) {
   store.save("a","سینی کابل",new JSONArray(),new String[]{"لدر کابل","نردبان کابل"});
   store.save("b","سینی کابل",new JSONArray(),new String[]{"برق صنعتی"});
   store.save("a","تابلو برق",new JSONArray(),new String[]{"پیمانکار برق"});
  }
  try(AiDiscoveryStore reopened=new AiDiscoveryStore(context)) {
   AiDiscoveryStore.SearchContext match=reopened.searchContext("a","سینی کابل");
   assertEquals("سینی کابل",match.query);
   assertArrayEquals(new String[]{"لدر کابل","نردبان کابل"},match.suggestions);
   assertArrayEquals(new String[]{"برق صنعتی"},
       reopened.searchContext("b","سینی کابل").suggestions);
   assertEquals("تابلو برق",reopened.searchContext("a","").query);
   assertArrayEquals(new String[]{"پیمانکار برق"},
       reopened.searchContext("a","").suggestions);
   assertEquals(0,reopened.searchContext("a","missing").suggestions.length);
  }
 }
 @Test public void legacyDatabaseMigrationKeepsHistoryAndGroups() throws Exception {
  android.content.Context context=RuntimeEnvironment.getApplication();
  context.deleteDatabase("ai-discovery.db");
  android.database.sqlite.SQLiteDatabase db=context.openOrCreateDatabase(
      "ai-discovery.db",android.content.Context.MODE_PRIVATE,null);
  db.execSQL("CREATE TABLE groups_cache(owner TEXT NOT NULL, identity TEXT NOT NULL, data TEXT NOT NULL, PRIMARY KEY(owner,identity))");
  db.execSQL("CREATE TABLE searches(id INTEGER PRIMARY KEY, owner TEXT NOT NULL, query TEXT NOT NULL, searched_at INTEGER NOT NULL, result_count INTEGER NOT NULL)");
  db.execSQL("INSERT INTO searches(owner,query,searched_at,result_count) VALUES('a','old',100,1)");
  db.execSQL("INSERT INTO groups_cache(owner,identity,data) VALUES('a','guid-old','{\"guid\":\"guid-old\",\"name\":\"Legacy\"}')");
  db.setVersion(1);
  db.close();
  try(AiDiscoveryStore store=new AiDiscoveryStore(context)) {
   assertEquals(1,store.groups("a").length());
   assertTrue(store.history("a").contains("old"));
   assertEquals("old",store.searchContext("a","").query);
   assertEquals(0,store.searchContext("a","").suggestions.length);
   store.save("a","new",new JSONArray(),new String[]{"لدر کابل"});
   assertArrayEquals(new String[]{"لدر کابل"},store.searchContext("a","").suggestions);
  }
 }

}
