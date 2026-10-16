package ir.sinicable.rubikaqueue;

/** Offline, deterministic sample builder; never accesses network, accounts or contacts. */
public final class NativeDashboardPreviewPolicy {
    private NativeDashboardPreviewPolicy() {}
    public static String describe(String topics, boolean groups, boolean businesses) {
        if (!groups && !businesses) return "هیچ نوع منبعی برای پیش‌نمایش انتخاب نشده است.";
        String first = "";
        if (topics != null) {
            for (String value : topics.split("[,،\\n\\r]")) {
                if (!value.trim().isEmpty()) { first = value.trim(); break; }
            }
        }
        if (first.isEmpty()) return "برای پیش‌نمایش، دست‌کم یک موضوع وارد کنید.";
        if (first.length() > 70) first = first.substring(0, 70) + "…";
        StringBuilder sample = new StringBuilder("پیش‌نمایش ساختگی برای «").append(first).append("»:\n");
        if (groups) sample.append("• گروه عمومی نمونه درباره ").append(first).append("\n");
        if (businesses) sample.append("• کسب‌وکار عمومی نمونه درباره ").append(first).append("\n");
        sample.append("فقط نمایش محلی، بدون جست‌وجوی شبکه‌ای یا تغییر تنظیمات واقعی منابع.");
        return sample.toString();
    }
}
