package ir.sinicable.rubikaqueue;

/** Identity-only comparison; no network, state, recipients, or message content stored. */
public final class QueueDuplicateGuard {
 private QueueDuplicateGuard(){}

 public static String destinationKey(String kind,String phone,String guid){
  if("group".equals(kind)){
   String id=guid==null?"":guid.trim();
   return id.isEmpty()?"":"group:"+id;
  }
  String number=digits(phone);
  if(number.isEmpty())return "";
  // The queue's Excel reader already normalizes addresses; recognize common
  // local/international spellings of the SAME Iranian mobile number.
  if(number.startsWith("0098")&&number.length()==14)number=number.substring(2);
  if(number.startsWith("98")&&number.length()==12&&number.charAt(2)=='9'){
   number="0"+number.substring(2);
  }else if(number.length()==10&&number.charAt(0)=='9'){
   number="0"+number;
  }
  return "contact:"+number;
 }

 private static String digits(String input){
  if(input==null)return "";
  StringBuilder out=new StringBuilder();
  for(int i=0;i<input.length();i++){
   int n=Character.digit(input.charAt(i),10);
   if(n>=0)out.append((char)('0'+n));
  }
  return out.toString();
 }

 public static boolean blocksRepeat(String earlierStatus){
  return "sent".equals(earlierStatus)||"accepted".equals(earlierStatus)
    ||"sending".equals(earlierStatus)||"uncertain".equals(earlierStatus)
    ||"retry_wait".equals(earlierStatus);
 }
}
