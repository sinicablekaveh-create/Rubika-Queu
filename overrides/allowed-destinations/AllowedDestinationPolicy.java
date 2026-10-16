package ir.sinicable.rubikaqueue;

import java.util.Set;

/** Deny-by-default matching of manually approved, account-scoped destinations. */
public final class AllowedDestinationPolicy {
    private AllowedDestinationPolicy() {}

    public static String key(String kind, String canonicalId) {
        if (kind == null || canonicalId == null) {
            throw new IllegalArgumentException("نوع یا شناسه مقصد مشخص نیست");
        }
        if ("group".equals(kind) && canonicalId.matches("g0[A-Za-z0-9_-]{10,126}")) {
            return "group:" + canonicalId;
        }
        if ("phone".equals(kind) && canonicalId.matches("[0-9]{10,15}")) {
            return "phone:" + canonicalId;
        }
        throw new IllegalArgumentException("شناسه مقصد مجاز یا معتبر نیست");
    }

    public static boolean isApproved(Set<String> approved, String kind, String id) {
        if (approved == null || approved.isEmpty()) return false;
        try {
            return approved.contains(key(kind, id));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
