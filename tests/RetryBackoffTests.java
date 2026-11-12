package ir.sinicable.rubikaqueue;
public final class RetryBackoffTests {
 private static int checks;
 private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
 public static void main(String[] args){
  check(RetryBackoff.delayMs(1,0)==10000L,"first rejection");
  check(RetryBackoff.delayMs(5,0)==160000L,"fifth rejection");
  check(RetryBackoff.delayMs(6,0)==320000L,"sixth rejection must increase delay");
  check(RetryBackoff.delayMs(7,0)==640000L,"seventh rejection");
  check(RetryBackoff.delayMs(8,9999)==900000L,"ceiling includes jitter");
  check(RetryBackoff.delayMs(Long.MAX_VALUE,9999)==900000L,"large persisted count");
  check(RetryBackoff.delayMs(-1,-1)==10000L,"invalid values normalized");
  check(RetryBackoff.delayMs(1,Long.MAX_VALUE)==19999L,"jitter bounded");
  long previous=0;
  for(int i=1;i<=100;i++){
   long delay=RetryBackoff.delayMs(i,0);
   check(delay>=previous&&delay<=900000L,"monotonic bounded delay");previous=delay;
  }
  System.out.println("RetryBackoffTests: PASS ("+checks+" checks)");
 }
}
