package ir.sinicable.rubikaqueue;

public final class AiPromptPolicyTests {
    private static int count;
    private static void check(boolean result) {
        if (!result) throw new AssertionError("Failed check " + (count + 1));
        count++;
    }
    private static void rejects(Runnable r) {
        try { r.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException valid) { count++; }
    }
    public static void main(String[] args) {
        check("متن نمونه".equals(AiPromptPolicy.prompt(" متن نمونه ")));
        rejects(() -> AiPromptPolicy.prompt(""));
        rejects(() -> AiPromptPolicy.prompt("  "));
        rejects(() -> AiPromptPolicy.prompt("x".repeat(3501)));
        check("x".repeat(3500).equals(AiPromptPolicy.prompt("x".repeat(3500))));
        check("qwen2.5:1.5b".equals(AiPromptPolicy.model("qwen2.5:1.5b")));
        rejects(() -> AiPromptPolicy.model("bad model"));
        check("https://ai.example.com".equals(AiPromptPolicy.endpoint("https://ai.example.com/")));
        check("https://ai.example.com:8443".equals(AiPromptPolicy.endpoint("https://ai.example.com:8443")));
        rejects(() -> AiPromptPolicy.endpoint("http://192.168.1.5:11434"));
        rejects(() -> AiPromptPolicy.endpoint("https://user:pass@host.example"));
        rejects(() -> AiPromptPolicy.endpoint("https://host.example/path"));
        rejects(() -> AiPromptPolicy.endpoint("https://host.example/?secret=1"));
        rejects(() -> AiPromptPolicy.endpoint("https://host.example/#section"));
        rejects(() -> AiPromptPolicy.endpoint(null));
        check("پاسخ آماده".equals(AiPromptPolicy.suggestion("  پاسخ آماده  ")));
        rejects(() -> AiPromptPolicy.suggestion(null));
        rejects(() -> AiPromptPolicy.suggestion(""));
        rejects(() -> AiPromptPolicy.suggestion("   "));
        check(AiPromptPolicy.suggestion("x".repeat(4200)).length() == 4200);
        rejects(() -> AiPromptPolicy.suggestion("x".repeat(4201)));
        check("https://api.example.com/v1/chat/completions".equals(AiPromptPolicy.chatCompletionsUrl("https://api.example.com")));
        check("https://api.example.com/v1/chat/completions".equals(AiPromptPolicy.chatCompletionsUrl("https://api.example.com/v1/")));
        check("https://api.groq.com/openai/v1/chat/completions".equals(AiPromptPolicy.chatCompletionsUrl("https://api.groq.com/openai/v1")));
        rejects(() -> AiPromptPolicy.chatCompletionsUrl("http://api.example.com"));
        rejects(() -> AiPromptPolicy.chatCompletionsUrl("https://user:pass@api.example.com/v1"));
        rejects(() -> AiPromptPolicy.chatCompletionsUrl("https://api.example.com/a/../v1"));
        rejects(() -> AiPromptPolicy.chatCompletionsUrl("https://api.example.com/v1?secret=abc"));
        rejects(() -> AiPromptPolicy.chatCompletionsUrl("https://api.example.com/%2e%2e"));
        rejects(() -> AiPromptPolicy.chatCompletionsUrl(null));
        check("".equals(AiPromptPolicy.apiKey(null)));
        check("sample-key".equals(AiPromptPolicy.apiKey(" sample-key ")));
        rejects(() -> AiPromptPolicy.apiKey("abc\r\nAnother:header"));
        rejects(() -> AiPromptPolicy.apiKey("abc def"));
        rejects(() -> AiPromptPolicy.apiKey("x".repeat(513)));
        System.out.println("PASS AiPromptPolicyTests " + count + " assertions");
    }
}
