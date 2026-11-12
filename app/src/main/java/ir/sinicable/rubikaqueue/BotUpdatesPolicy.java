package ir.sinicable.rubikaqueue;

/** Manual Bot API read limits; unrelated to personal-account messaging. */
public final class BotUpdatesPolicy {
    private BotUpdatesPolicy() {}

    /** Limit an explicit one-shot getUpdates preview to 1..10 records. */
    public static int validateLimit(int limit) {
        if (limit < 1 || limit > 10) throw new IllegalArgumentException("limit must be 1..10");
        return limit;
    }

    /** Cursor is opaque. Reject control characters and excessively long values. */
    public static String validateOffset(String value) {
        if (value == null || value.isEmpty()) return null;
        if (value.length() > 512) throw new IllegalArgumentException("offset_id too long");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c)) throw new IllegalArgumentException("invalid offset_id");
        }
        return value;
    }
}
