package ir.sinicable.rubikaqueue;

/** Small, deterministic policy for opting into discovery at queue start. */
public final class QueueStartDiscoveryPolicy {
    public static final String PREFERENCE = "auto_on_queue_start";

    private QueueStartDiscoveryPolicy() {}

    public static boolean shouldRun(boolean enabled, boolean groups, boolean contacts,
                                    String[] terms) {
        return enabled && (groups || contacts) && terms != null && terms.length > 0;
    }
}
