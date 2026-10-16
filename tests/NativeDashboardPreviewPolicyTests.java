package ir.sinicable.rubikaqueue;

/** Dependency-free tests for offline, non-network discovery previews. */
public final class NativeDashboardPreviewPolicyTests {
    private static void check(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }
    public static void main(String[] args) {
        String none = NativeDashboardPreviewPolicy.describe("برق صنعتی", false, false);
        check(none.contains("هیچ نوع منبعی"), "no categories should be rejected");
        String missing = NativeDashboardPreviewPolicy.describe("   ، ,  ", true, true);
        check(missing.contains("موضوع"), "blank topic should be rejected");
        String group = NativeDashboardPreviewPolicy.describe("، سینی کابل، لاله‌زار", true, false);
        check(group.contains("سینی کابل"), "selected topic should appear");
        check(group.contains("گروه عمومی نمونه"), "group selection must appear");
        check(!group.contains("کسب‌وکار عمومی نمونه"), "disabled category should be absent");
        String contact = NativeDashboardPreviewPolicy.describe("برق صنعتی", false, true);
        check(contact.contains("کسب‌وکار عمومی نمونه"), "business selection must appear");
        check(!contact.contains("گروه عمومی نمونه"), "disabled group category must be absent");
        check(contact.contains("بدون جست‌وجوی شبکه‌ای"), "must disclose offline sample");
        System.out.println("NativeDashboardPreviewPolicyTests: 8 assertions passed");
    }
}
