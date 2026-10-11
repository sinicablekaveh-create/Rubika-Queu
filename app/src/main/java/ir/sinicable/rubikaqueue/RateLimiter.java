package ir.sinicable.rubikaqueue;
public class RateLimiter {
 private long next;
 /** Returns whether the wall-clock delay deadline has passed. */
 public synchronized boolean allowed(){return System.currentTimeMillis()>=next;}
 /** Replaces the delay deadline with the current wall time plus the supplied milliseconds. */
 public synchronized void delay(long ms){next=System.currentTimeMillis()+ms;}
}
