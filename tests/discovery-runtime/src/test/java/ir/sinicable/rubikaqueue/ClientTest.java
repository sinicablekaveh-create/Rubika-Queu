package ir.sinicable.rubikaqueue;
import org.junit.*;
import static org.junit.Assert.*;
import java.net.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import java.security.Principal;
import java.security.cert.Certificate;

public class ClientTest {
 private static Fake connection;
 @BeforeClass public static void transport() {
  URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol) ? new URLStreamHandler() {
   protected URLConnection openConnection(URL url) { connection = new Fake(url); return connection; }
  } : null);
 }
 private static String body;
 private static int status;
 @Before public void response() { body="{\"response\":\"{\\\"terms\\\":[\\\"power\\\",\\\"tools\\\",\\\"tools\\\",3]}\"}"; status=200; }
 @Test public void expandsDeduplicatesAndSendsOnlyQuery() throws Exception {
  assertArrayEquals(new String[]{"power","tools"},AiDiscoveryClient.expand("https://ai.example.com","model","power"));
  assertEquals("/api/generate",connection.getURL().getPath());
  assertFalse(connection.getInstanceFollowRedirects());
  assertTrue(connection.output.toString("UTF-8").contains("power"));
  assertTrue(connection.closed);
 }
 @Test public void rejectsRedirects() throws Exception {
  status=302;
  try { AiDiscoveryClient.expand("https://ai.example.com","model","power"); fail(); }
  catch(IOException expected) { assertTrue(connection.closed); }
 }
 @Test public void rejectsOversizedOutput() throws Exception {
  body="x".repeat(65537);
  try { AiDiscoveryClient.expand("https://ai.example.com","model","power"); fail(); }
  catch(IOException expected) { assertTrue(connection.closed); }
 }
 @Test public void rejectsHttpBeforeRequest() throws Exception {
  try { AiDiscoveryClient.expand("http://ai.example.com","model","power"); fail(); }
  catch(IllegalArgumentException expected) { }
 }
 @Test public void skipsInvalidAndPersianDuplicatesWithoutConsumingSlots() throws Exception {
  org.json.JSONArray suggestions=new org.json.JSONArray()
      .put(7).put("x").put("tools").put("سيني كابل")
      .put("سینی کابل").put("https://example.org/unsafe")
      .put("first").put("second").put("third");
  body=new org.json.JSONObject().put("response",
      new org.json.JSONObject().put("terms",suggestions).toString()).toString();
  assertArrayEquals(new String[]{"برق","tools","سيني كابل","first","second"},
      AiDiscoveryClient.expand("https://ai.example.com","model","برق"));
 }
 @Test public void queryIsKeptWhenSuggestionsAreInvalid() throws Exception {
  org.json.JSONArray suggestions=new org.json.JSONArray()
      .put("x").put("https://fake.example").put("برق")
      .put("برق\nغیرمعتبر");
  body=new org.json.JSONObject().put("response",
      new org.json.JSONObject().put("terms",suggestions).toString()).toString();
  assertArrayEquals(new String[]{"برق"},
      AiDiscoveryClient.expand("https://ai.example.com","model","برق"));
 }
 static class Fake extends HttpsURLConnection {
  final ByteArrayOutputStream output=new ByteArrayOutputStream(); boolean closed;
  Fake(URL url) { super(url); }
  public void disconnect() { closed=true; }
  public boolean usingProxy() { return false; }
  public void connect() { }
  public int getResponseCode() { return status; }
  public OutputStream getOutputStream() { return output; }
  public InputStream getInputStream() { return new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
  public String getCipherSuite() { return "test"; }
  public Certificate[] getLocalCertificates() { return null; }
  public Certificate[] getServerCertificates() { return null; }
  public Principal getPeerPrincipal() { return null; }
  public Principal getLocalPrincipal() { return null; }
 }
}
