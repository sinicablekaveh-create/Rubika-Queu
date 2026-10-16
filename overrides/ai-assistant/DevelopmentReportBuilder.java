package ir.sinicable.rubikaqueue;

/**
 * A deliberately minimal development brief.
 * Does not inspect contacts, queued messages, logs, sessions or tokens.
 * The note is entered by the owner and reviewed before any external share.
 */
public final class DevelopmentReportBuilder {
    private static final int MAX_NOTE_LENGTH = 2500;
    private DevelopmentReportBuilder() {}

    public static String format(String packageName, String versionName, long versionCode,
                                int androidSdk, String requestedChange) {
        if (!"ir.sinicable.rubikaqueue".equals(packageName)) {
            throw new IllegalArgumentException("Unexpected package");
        }
        if (versionName == null || versionName.isEmpty() || versionName.length() > 100
                || versionName.indexOf('\n') >= 0 || versionName.indexOf('\r') >= 0
                || versionCode <= 0 || androidSdk < 1) {
            throw new IllegalArgumentException("Invalid application metadata");
        }
        if (requestedChange == null || requestedChange.trim().isEmpty()
                || requestedChange.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Describe one change in 1–2500 characters");
        }
        return "درخواست توسعه برنامه صف پیام روبیکا (Rubika-Queu)\n"
                + "مخزن سورس: https://github.com/sinicablekaveh-create/Rubika-Queu\n"
                + "شناسه برنامه: " + packageName + "\n"
                + "نسخه: " + versionName + " (versionCode " + versionCode + ")\n"
                + "Android SDK دستگاه: " + androidSdk + "\n\n"
                + "درخواست من برای توسعه:\n" + requestedChange.trim() + "\n\n"
                + "لطفاً سورس موجود مخزن را بررسی کن، راهکار قابل تست پیشنهاد بده و "
                + "در صورت داشتن دسترسی مجاز، تغییرات را در شاخه جداگانه و PR آماده کن. "
                + "ارسال پیام روبیکا، ادغام خودکار یا انتشار APK بدون تأیید من انجام نشود. "
                + "برای آپدیت، packageName و گواهی امضای قبلی حفظ شود و versionCode افزایش یابد.\n"
                + "این گزارش عمداً فاقد اطلاعات مخاطبان، متن پیام‌ها، صف، نشست و توکن‌هاست.\n";
    }
}
