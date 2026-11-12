package ir.sinicable.rubikaqueue;
public final class QueueSafetyChecks {
 public static void main(String[] args){
  if(!MessageQueuePolicy.mayDispatch("pending"))throw new AssertionError("pending blocked");
  for(String state:new String[]{"sending","uncertain","retry_wait","sent","accepted","skipped"})
   if(MessageQueuePolicy.mayDispatch(state))throw new AssertionError("replay "+state);
  if(!MessageQueuePolicy.requiresReview("uncertain",false,""))throw new AssertionError("uncertain missed");
  if(!QueueDuplicateGuard.blocksRepeat("sending"))throw new AssertionError("duplicate in-flight");
  if(QueueDuplicateGuard.blocksRepeat("pending"))throw new AssertionError("pending incorrect block");
  System.out.println("PASS: 10 queue safety assertions");
 }
}
