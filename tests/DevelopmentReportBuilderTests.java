package ir.sinicable.rubikaqueue;

public final class DevelopmentReportBuilderTests {
    private static int count = 0;
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Failed: " + (++count));
        count++;
    }
    private static void mustReject(Runnable callback) {
        try { callback.run(); throw new AssertionError("Invalid report accepted"); }
        catch (IllegalArgumentException expected) { count++; }
    }

    public static void main(String[] args) {
        String value = DevelopmentReportBuilder.format(
                "ir.sinicable.rubikaqueue", "1.3.2-development-report", 15, 35,
                "گزارش فنی قابل ارسال");
        check(value.contains("https://github.com/sinicablekaveh-create/Rubika-Queu"));
        check(value.contains("versionCode 15"));
        check(value.contains("Android SDK دستگاه: 35"));
        check(value.contains("گزارش فنی قابل ارسال"));
        check(!value.contains("RUBIKA_TEST_KEYSTORE_PASSWORD"));
        check(!value.contains("EXTRA_TEXT"));
        mustReject(() -> DevelopmentReportBuilder.format("other.app", "1.3", 15, 35, "درخواست"));
        mustReject(() -> DevelopmentReportBuilder.format("ir.sinicable.rubikaqueue", "1.3.2", 0, 35, "درخواست"));
        mustReject(() -> DevelopmentReportBuilder.format("ir.sinicable.rubikaqueue", "1.3.2", 15, 35, " "));
        mustReject(() -> DevelopmentReportBuilder.format("ir.sinicable.rubikaqueue", "1.3.2", 15, 35, "a".repeat(2501)));
        mustReject(() -> DevelopmentReportBuilder.format("ir.sinicable.rubikaqueue", "1.3\npassword", 15, 35, "درخواست"));
        System.out.println("PASS DevelopmentReportBuilderTests " + count + " checks");
    }
}
