package ir.sinicable.rubikaqueue;

/** Pure policies shared by the message queue and its preflight UI. */
public final class MessageQueuePolicy {
    private MessageQueuePolicy() {}

    /** A queue may dispatch only rows explicitly marked pending. */
    public static boolean mayDispatch(String status) {
        return "pending".equals(status);
    }

    /** Persisted in-flight/ambiguous rows must be resolved by the user first. */
    public static boolean requiresReview(String status, boolean reviewed, String error) {
        if ("sending".equals(status) || "uncertain".equals(status)) return true;
        return "skipped".equals(status) && !reviewed
                && error != null && error.contains("نتیجه نامشخص");
    }

    public static String requireDraft(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("متن پیام را بنویسید");
        }
        return text.trim();
    }

    public static int requireRecipients(int count) {
        if (count <= 0) throw new IllegalArgumentException("فهرست گیرندگان خالی است");
        return count;
    }

    public static int requireInterval(int seconds, int min, int max) {
        if (min <= 0 || max < min || seconds < min || seconds > max) {
            throw new IllegalArgumentException("فاصله ارسال خارج از محدوده مجاز است");
        }
        return seconds;
    }
}
