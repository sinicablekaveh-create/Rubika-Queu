package ir.sinicable.rubikaqueue;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class DeveloperClientTest {
    private interface Checked { void run() throws Exception; }
    private void invalid(Checked fn) throws Exception {
        try { fn.run(); fail("Expected invalid input"); }
        catch (IllegalArgumentException expected) { }
    }
    @Test public void onlyValidatedGatewayOriginsAreAllowed() throws Exception {
        assertEquals("https://ai.example.com/v1/dev/chat",
                AiDeveloperClient.path("https://ai.example.com", "/v1/dev/chat"));
        invalid(() -> AiDeveloperClient.path("http://ai.example.com", "/v1/dev/chat"));
        invalid(() -> AiDeveloperClient.path("https://chatgpt.com", "/v1/dev/chat"));
        invalid(() -> AiDeveloperClient.path("https://api.openai.com", "/v1/dev/chat"));
    }
    @Test public void separatesResearchAndDevelopmentTaskBounds() throws Exception {
        assertEquals("بحث برنامه", AiDeveloperClient.prompt(" بحث برنامه "));
        assertEquals("بهبود الگوریتم کشف گروه", AiDeveloperClient.task(" بهبود الگوریتم کشف گروه "));
        invalid(() -> AiDeveloperClient.prompt(" "));
        invalid(() -> AiDeveloperClient.task("کوتاه"));
        invalid(() -> AiDeveloperClient.task("a".repeat(1201)));
        invalid(() -> AiDeveloperClient.prompt("a".repeat(3501)));
        JSONObject query = AiDeveloperClient.chatRequest("توسعه جاوا", true);
        assertTrue(query.getBoolean("use_web"));
        assertEquals("توسعه جاوا",query.getString("prompt"));
        assertEquals(1,AiDeveloperClient.proposalRequest("توسعه موتور جستجو")
                .length());
    }
    @Test public void acceptsOnlyWellFormedResponses() throws Exception {
        assertEquals("پاسخ آماده", AiDeveloperClient.chatAnswer("{\"answer\":\" پاسخ آماده \"}"));
        assertTrue(AiDeveloperClient.proposalAnswer(
                "{\"status\":\"accepted\",\"workflow_url\":\"https://github.com/example\"}")
                .contains("GitHub"));
        assertTrue(AiDeveloperClient.statusAnswer("{\"status\":\"not_started\"}")
                .contains("هنوز"));
        assertTrue(AiDeveloperClient.statusAnswer("{\"status\":\"completed\",\"conclusion\":\"success\"}")
                .contains("success"));
        for(String body:new String[]{"{}", "{\"answer\":\"\"}", "{\"answer\":42}"}) {
            try { AiDeveloperClient.chatAnswer(body); fail("Must reject bad chat reply"); }
            catch(java.io.IOException expected) { }
        }
        try {
            AiDeveloperClient.proposalAnswer("{\"status\":\"pending\"}");
            fail("Should reject unconfirmed dispatch");
        } catch(java.io.IOException expected) {}
        try {
            AiDeveloperClient.statusAnswer("{\"status\":\"unknown\"}");
            fail("Unknown run status");
        } catch(java.io.IOException expected) {}
    }
    @Test public void desktopResultsRequireTextAndAllowedTool() throws Exception {
        assertEquals("device result", AiDeveloperClient.desktopAnswer(
                "{\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"device result\"}]}}"));
        try {
            AiDeveloperClient.desktopAnswer("{\"result\":{\"isError\":true,\"content\":[{\"type\":\"text\",\"text\":\"failed\"}]}}");
            fail("Tool error must fail");
        } catch (java.io.IOException expected) {}
        invalid(() -> AiDeveloperClient.desktop("https://ai.example.com", "token",
                "kill_process", new JSONObject()));
    }
}
