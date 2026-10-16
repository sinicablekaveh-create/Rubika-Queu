package ir.sinicable.rubikaqueue;

import java.util.HashSet;
import java.util.Set;

public final class AllowedDestinationPolicyTests {
    private static int count;
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError("assertion " + count);
        count++;
    }
    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("must reject"); }
        catch (IllegalArgumentException expected) { count++; }
    }
    public static void main(String[] ignored) {
        String g = "g0abcdefghijklmnop";
        String phone = "989121234567";
        Set<String> allowed = new HashSet<>();
        check(!AllowedDestinationPolicy.isApproved(allowed, "group", g));
        check(!AllowedDestinationPolicy.isApproved(allowed, "phone", phone));
        check("group:".concat(g).equals(AllowedDestinationPolicy.key("group", g)));
        check("phone:".concat(phone).equals(AllowedDestinationPolicy.key("phone", phone)));
        allowed.add(AllowedDestinationPolicy.key("group", g));
        check(AllowedDestinationPolicy.isApproved(allowed, "group", g));
        check(!AllowedDestinationPolicy.isApproved(allowed, "group", "g0differentabcdef"));
        check(!AllowedDestinationPolicy.isApproved(allowed, "phone", phone));
        allowed.add(AllowedDestinationPolicy.key("phone", phone));
        check(AllowedDestinationPolicy.isApproved(allowed, "phone", phone));
        check(!AllowedDestinationPolicy.isApproved(allowed, "phone", "989121234568"));
        allowed.remove(AllowedDestinationPolicy.key("group", g));
        check(!AllowedDestinationPolicy.isApproved(allowed, "group", g));
        check(!AllowedDestinationPolicy.isApproved(null, "group", g));
        check(!AllowedDestinationPolicy.isApproved(allowed, "group", "INVALID"));
        reject(() -> AllowedDestinationPolicy.key("group", "u0abcdef"));
        reject(() -> AllowedDestinationPolicy.key("phone", "abc"));
        reject(() -> AllowedDestinationPolicy.key("phone", ""));
        reject(() -> AllowedDestinationPolicy.key("unknown", phone));
        reject(() -> AllowedDestinationPolicy.key(null, phone));
        reject(() -> AllowedDestinationPolicy.key("phone", null));
        System.out.println("PASS AllowedDestinationPolicyTests " + count + " assertions");
    }
}
