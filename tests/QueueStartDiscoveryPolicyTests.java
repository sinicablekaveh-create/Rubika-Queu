package ir.sinicable.rubikaqueue;

public final class QueueStartDiscoveryPolicyTests {
    private static int checks;

    private static void check(boolean expected, boolean actual) {
        if (expected != actual) throw new AssertionError("policy mismatch at check " + (checks + 1));
        checks++;
    }

    public static void main(String[] args) {
        String[] terms = {"برق صنعتی"};
        check(true, QueueStartDiscoveryPolicy.shouldRun(true, true, true, terms));
        check(true, QueueStartDiscoveryPolicy.shouldRun(true, true, false, terms));
        check(true, QueueStartDiscoveryPolicy.shouldRun(true, false, true, terms));
        check(false, QueueStartDiscoveryPolicy.shouldRun(false, true, true, terms));
        check(false, QueueStartDiscoveryPolicy.shouldRun(true, false, false, terms));
        check(false, QueueStartDiscoveryPolicy.shouldRun(true, true, true, new String[0]));
        check(false, QueueStartDiscoveryPolicy.shouldRun(true, true, true, null));
        System.out.println("PASS QueueStartDiscoveryPolicyTests " + checks + " checks");
    }
}
