package ir.sinicable.rubikaqueue;
public class RateLimiter {
 private long next;
 public synchronized boolean allowed(){return System.currentTimeMillis()>=next;}
 public synchronized void delay(long ms){next=System.currentTimeMillis()+ms;}
}
