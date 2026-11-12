package ir.sinicable.rubikaqueue;

/** Delay after an explicit rate rejection; never used for ambiguous send results. */
public final class RetryBackoff {
 public static final long MAX_DELAY_MS=900000L;
 private RetryBackoff(){}
 public static long delayMs(long retryCount,long jitterMs){
  long exponent=Math.max(1L,Math.min(8L,retryCount));
  long base=Math.min(MAX_DELAY_MS,5000L*(1L<<(int)exponent));
  long jitter=Math.max(0L,Math.min(9999L,jitterMs));
  return Math.min(MAX_DELAY_MS,base+jitter);
 }
}
