package ir.sinicable.rubikaqueue;
/** Bounded rate-limit retries; delays never exceed fifteen minutes. */
public final class RetryController {
 public static final int MAX_ATTEMPTS = 8;
 public static long backoff(int attempt) {
  int exponent = Math.max(0, Math.min(8, attempt));
  long base = 5000L * (1L << exponent);
  return Math.min(900000L, base + (long)(Math.random() * 10000L));
 }
 public static boolean canRetry(int attempt) { return attempt >= 0 && attempt < MAX_ATTEMPTS; }
}
