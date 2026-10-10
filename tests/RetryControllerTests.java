import ir.sinicable.rubikaqueue.RetryController;
public final class RetryControllerTests {
 public static void main(String[] args) {
  for(int attempt:new int[]{-1,0,1,7,8,100,Integer.MAX_VALUE}) {
   for(int i=0;i<1000;i++) {
    long delay=RetryController.backoff(attempt);
    if(delay<5000 || delay>900000)throw new AssertionError("delay bounds");
   }
  }
  if(!RetryController.canRetry(7)||RetryController.canRetry(8)||RetryController.canRetry(-1))throw new AssertionError("retry budget");
  System.out.println("RetryControllerTests: PASS");
 }
}
