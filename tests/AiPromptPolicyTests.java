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
        System.out.println("PASS AiPromptPolicyTests " + count + " assertions");
    }
}
