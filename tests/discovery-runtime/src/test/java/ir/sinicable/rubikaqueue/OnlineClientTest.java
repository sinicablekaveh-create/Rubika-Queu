package ir.sinicable.rubikaqueue;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OnlineClientTest {
    private void rejects(Throwing action) throws Exception {
        try { action.run(); fail("Expected invalid input"); }
        catch (IllegalArgumentException expected) { /* rejected */ }
    }
    private interface Throwing { void run() throws Exception; }

    @Test public void gatewayRequiresHttpsAndRejectsDirectProvider() throws Exception {
        assertEquals("https://ai.example.com/v1/chat/completions",
                AiOnlineClient.gatewayUrl("https://ai.example.com/"));
        rejects(() -> AiOnlineClient.gatewayUrl("http://ai.example.com"));
        rejects(() -> AiOnlineClient.gatewayUrl("https://api.openai.com"));
        rejects(() -> AiOnlineClient.gatewayUrl("https://someone:password@ai.example.com"));
    }

    @Test public void gatewayRequiresOwnTokenNotProviderKey() throws Exception {
        assertEquals("gateway-session-0123", AiOnlineClient.gatewayToken(" gateway-session-0123 "));
        rejects(() -> AiOnlineClient.gatewayToken(""));
        rejects(() -> AiOnlineClient.gatewayToken("secret\nInjected: yes"));
        rejects(() -> AiOnlineClient.gatewayToken("sk-proj-looks-like-provider-key"));
    }

    @Test public void requestContainsOnlyExplicitTextAndNoTokens() throws Exception {
        JSONObject data = AiOnlineClient.request("gpt-4.1-mini", "پیش نویس پیام");
        assertEquals(false, data.getBoolean("stream"));
        assertEquals(false, data.getBoolean("store"));
        assertEquals(2, data.getJSONArray("messages").length());
        assertEquals("پیش نویس پیام", data.getJSONArray("messages").getJSONObject(1).getString("content"));
        assertFalse(data.toString().contains("Authorization"));
        rejects(() -> AiOnlineClient.request("gpt bad", "متن"));
        rejects(() -> AiOnlineClient.request("gpt-4.1-mini", ""));
    }

    @Test public void resultRejectsMalformedAndTruncatedContent() throws Exception {
        assertEquals("متن اصلاح شده", AiOnlineClient.extract(
                "{\"choices\":[{\"message\":{\"content\":\" متن اصلاح شده \"},\"finish_reason\":\"stop\"}]}"));
        rejects(() -> AiOnlineClient.extract(
                "{\"choices\":[{\"message\":{\"content\":\" \"}}]}"));
        try {
            AiOnlineClient.extract("{\"choices\":[{\"message\":{\"content\":\"ناتمام\"},\"finish_reason\":\"length\"}]}");
            fail("Truncated response should fail");
        } catch (java.io.IOException expected) { /* good */ }
    }
}
