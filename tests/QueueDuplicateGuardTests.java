package ir.sinicable.rubikaqueue;

public final class QueueDuplicateGuardTests {
 static void check(boolean result,String reason){if(!result)throw new AssertionError(reason);}
 public static void main(String[] args){
  String local=QueueDuplicateGuard.destinationKey("contact","09195041560","");
  check(!local.isEmpty(),"phone key missing");
  check(local.equals(QueueDuplicateGuard.destinationKey("contact","+98 919 504 1560","")),"international variation");
  check(local.equals(QueueDuplicateGuard.destinationKey("contact","0098-919-504-1560","")),"00 prefix");
  check(local.equals(QueueDuplicateGuard.destinationKey("contact","۰۹۱۹۵۰۴۱۵۶۰","")),"Persian digits");
  check(!local.equals(QueueDuplicateGuard.destinationKey("contact","09195041561","")),"different phone");
  check(!local.equals(QueueDuplicateGuard.destinationKey("group","","g0123")),"phone and group isolation");
  check("group:g0123".equals(QueueDuplicateGuard.destinationKey("group",""," g0123 ")),"same group");
  check(!QueueDuplicateGuard.destinationKey("group","","g0123").equals(
        QueueDuplicateGuard.destinationKey("group","","g0124")),"different group");
  check("".equals(QueueDuplicateGuard.destinationKey("contact",null,null)),"empty phone");
  check("".equals(QueueDuplicateGuard.destinationKey("group","",null)),"empty group");
  for(String state:new String[]{"sent","accepted","sending","uncertain","retry_wait"}){
   check(QueueDuplicateGuard.blocksRepeat(state),"duplicate send must be blocked after "+state);
  }
  for(String state:new String[]{"pending","skipped","failed",""}){
   check(!QueueDuplicateGuard.blocksRepeat(state),"never assume delivery for "+state);
  }
  System.out.println("Offline duplicate destination checks passed; zero Rubika calls");
 }
}
