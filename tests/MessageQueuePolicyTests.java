package ir.sinicable.rubikaqueue;

public final class MessageQueuePolicyTests {
    private static int passed;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        passed++;
    }
    private static void reject(Runnable action, String message) {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { passed++; }
    }
    public static void main(String[] args) {
        check(MessageQueuePolicy.mayDispatch("pending"), "pending can dispatch");
        for (String status : new String[]{"sending", "uncertain", "sent", "accepted",
                "sent_manual", "skipped", "retry_wait", null}) {
            check(!MessageQueuePolicy.mayDispatch(status), status + " cannot dispatch");
        }
        check(MessageQueuePolicy.requiresReview("sending", false, ""), "in-flight after restart");
        check(MessageQueuePolicy.requiresReview("uncertain", false, ""), "uncertain result");
        check(MessageQueuePolicy.requiresReview("skipped", false, "نتیجه نامشخص"), "legacy ambiguous row");
        check(!MessageQueuePolicy.requiresReview("skipped", true, "نتیجه نامشخص"), "reviewed legacy row");
        check(!MessageQueuePolicy.requiresReview("sent", false, ""), "confirmed send");
        check("متن پیام".equals(MessageQueuePolicy.requireDraft("  متن پیام  ")), "trim draft");
        reject(() -> MessageQueuePolicy.requireDraft(null), "null draft");
        reject(() -> MessageQueuePolicy.requireDraft("  \n"), "blank draft");
        check(MessageQueuePolicy.requireRecipients(1) == 1, "non-empty recipients");
        reject(() -> MessageQueuePolicy.requireRecipients(0), "empty recipients");
        check(MessageQueuePolicy.requireInterval(30, 30, 3600) == 30, "min interval");
        check(MessageQueuePolicy.requireInterval(3600, 30, 3600) == 3600, "max interval");
        reject(() -> MessageQueuePolicy.requireInterval(29, 30, 3600), "below min interval");
        reject(() -> MessageQueuePolicy.requireInterval(3601, 30, 3600), "above max interval");
        System.out.println("PASS MessageQueuePolicyTests " + passed + " assertions");
    }
}
