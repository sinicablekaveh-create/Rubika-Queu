package ir.sinicable.rubikaqueue;
import org.junit.Test;
import static org.junit.Assert.*;
public class QueueSafetyTest {
 @Test public void onlyPendingIsDispatchable(){
  assertTrue(MessageQueuePolicy.mayDispatch("pending"));
  for(String s:new String[]{"sending","uncertain","retry_wait","sent","accepted","skipped"})
   assertFalse("Must not replay "+s,MessageQueuePolicy.mayDispatch(s));
 }
 @Test public void uncertainMessagesRequireReview(){
  assertTrue(MessageQueuePolicy.requiresReview("uncertain",false,""));
  assertTrue(MessageQueuePolicy.requiresReview("sending",false,""));
  assertFalse(MessageQueuePolicy.requiresReview("sent",false,""));
 }
 @Test public void draftAndIntervalValidation(){
  assertEquals("سلام",MessageQueuePolicy.requireDraft("  سلام  "));
  assertEquals(30,MessageQueuePolicy.requireInterval(30,30,3600));
  try{MessageQueuePolicy.requireDraft(" ");fail("Empty draft accepted");}
  catch(IllegalArgumentException expected){}
  try{MessageQueuePolicy.requireInterval(29,30,3600);fail("Fast retry accepted");}
  catch(IllegalArgumentException expected){}
 }
 @Test public void duplicateGuardHonorsInFlight(){
  assertTrue(QueueDuplicateGuard.blocksRepeat("sending"));
  assertTrue(QueueDuplicateGuard.blocksRepeat("uncertain"));
  assertTrue(QueueDuplicateGuard.blocksRepeat("accepted"));
  assertFalse(QueueDuplicateGuard.blocksRepeat("pending"));
 }
}
