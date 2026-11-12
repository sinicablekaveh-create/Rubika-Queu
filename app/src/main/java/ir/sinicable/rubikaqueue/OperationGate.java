package ir.sinicable.rubikaqueue;
import java.util.HashSet;
import java.util.Set;

public final class OperationGate {
 private static final Set<String> active=new HashSet<>();
 private OperationGate(){}
 private static boolean queueKey(String key){return key!=null&&key.startsWith("queue");}
 public static synchronized boolean acquire(String key){
  if(key==null||key.isEmpty()||active.contains(key))return false;
  if(queueKey(key)){
   for(String x:active)if(!queueKey(x))return false;
   active.add(key);return true;
  }
  if(!active.isEmpty())return false;
  active.add(key);return true;
 }
 public static synchronized void release(String key){active.remove(key);}
 public static synchronized boolean occupied(){return !active.isEmpty();}
 public static synchronized boolean occupied(String key){return active.contains(key);}
 public static synchronized boolean anyQueue(){for(String x:active)if(queueKey(x))return true;return false;}
 public static synchronized boolean exclusiveOccupied(){for(String x:active)if(!queueKey(x))return true;return false;}
}
