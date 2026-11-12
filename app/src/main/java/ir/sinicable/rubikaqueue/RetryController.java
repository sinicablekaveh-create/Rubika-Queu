package ir.sinicable.rubikaqueue;
public class RetryController {
 public static long backoff(int attempt){long base=5000L*(1L<<Math.min(5,attempt)); return Math.min(900000L,base+(long)(Math.random()*10000L));}
 public static boolean canRetry(int attempt){return attempt<8;}
}
