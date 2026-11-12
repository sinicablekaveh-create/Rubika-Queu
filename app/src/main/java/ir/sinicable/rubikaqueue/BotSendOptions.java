package ir.sinicable.rubikaqueue;

/** Validation for optional user-confirmed Rubika Bot API sendMessage fields. */
public final class BotSendOptions {
    private BotSendOptions() {}

    /** Optional opaque message ID; never used as a recipient or URL. */
    public static String validateReplyTo(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String value = id.trim();
        if (value.length() > 180) throw new IllegalArgumentException("reply_to_message_id too long");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException("invalid reply_to_message_id");
            }
        }
        return value;
    }
}
