package ir.sinicable.rubikaqueue;

import org.json.JSONObject;

/** Per-job minimum gap between attempted sends. Never bypasses server rate limits. */
public final class SendInterval {
    public static final int MIN_SECONDS = 30;
    public static final int MAX_SECONDS = 3600;
    private SendInterval() {}

    public static int requireValid(String raw) {
        final int seconds;
        try { seconds = Integer.parseInt(raw.trim()); }
        catch (RuntimeException e) { throw new IllegalArgumentException("فاصله ارسال باید برحسب ثانیه و عدد صحیح باشد"); }
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS)
            throw new IllegalArgumentException("فاصله ارسال باید بین ۳۰ تا ۳۶۰۰ ثانیه باشد");
        return seconds;
    }

    public static int fromJob(JSONObject job) {
        if (job == null) return MIN_SECONDS;
        int s = job.optInt("interval_seconds", MIN_SECONDS);
        return Math.min(MAX_SECONDS, Math.max(MIN_SECONDS, s));
    }

    public static long remainingMs(long previousAttemptWallTimeMs, long nowWallTimeMs, int seconds) {
        if (previousAttemptWallTimeMs <= 0) return 0;
        long elapsed = nowWallTimeMs - previousAttemptWallTimeMs;
        // A backwards wall-clock adjustment must never accelerate a pending send.
        return Math.max(0, seconds * 1000L - Math.max(0L, elapsed));
    }
}
