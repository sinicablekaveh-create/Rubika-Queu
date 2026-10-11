package ir.sinicable.rubikaqueue;

public final class BotUpdatesPolicyTests {
    private static int count = 0;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        count++;
    }
    private static void rejects(Runnable action, String message) {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { count++; }
    }
    public static void main(String[] args) {
        check(BotUpdatesPolicy.validateLimit(1) == 1, "minimum");
        check(BotUpdatesPolicy.validateLimit(10) == 10, "maximum");
        rejects(() -> BotUpdatesPolicy.validateLimit(0), "reject zero");
        rejects(() -> BotUpdatesPolicy.validateLimit(11), "reject large");
        check(BotUpdatesPolicy.validateOffset(null) == null, "null cursor");
        check(BotUpdatesPolicy.validateOffset("") == null, "empty cursor");
        check("opaque+/=a".equals(BotUpdatesPolicy.validateOffset("opaque+/=a")), "opaque cursor");
        rejects(() -> BotUpdatesPolicy.validateOffset("hi\nsecret"), "reject control");
        rejects(() -> BotUpdatesPolicy.validateOffset("x".repeat(513)), "reject huge");
        System.out.println("PASS BotUpdatesPolicyTests " + count + " assertions");
    }
}
