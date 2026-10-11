package ir.sinicable.rubikaqueue;

public final class BotSendOptionsTests {
    private static int passed;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        passed++;
    }
    private static void reject(Runnable job, String message) {
        try { job.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { passed++; }
    }
    public static void main(String[] args) {
        check(BotSendOptions.validateReplyTo(null) == null, "null");
        check(BotSendOptions.validateReplyTo("") == null, "empty");
        check(BotSendOptions.validateReplyTo("  ") == null, "blank");
        check("123".equals(BotSendOptions.validateReplyTo(" 123 ")), "trim");
        check("a-B_c:9".equals(BotSendOptions.validateReplyTo("a-B_c:9")), "opaque");
        check(BotSendOptions.validateReplyTo("X".repeat(180)).length()==180, "max");
        reject(() -> BotSendOptions.validateReplyTo("X".repeat(181)), "too long");
        reject(() -> BotSendOptions.validateReplyTo("hi\nthere"), "newline");
        reject(() -> BotSendOptions.validateReplyTo("hi\rthere"), "carriage");
        reject(() -> BotSendOptions.validateReplyTo("hi\tthere"), "tab");
        System.out.println("PASS BotSendOptionsTests " + passed + " assertions");
    }
}
