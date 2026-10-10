package ir.sinicable.rubikaqueue;
public final class OperationGate {
 private static String owner="";
 public static synchronized boolean acquire(String key){if(!owner.isEmpty())return false;owner=key;return true;}
 public static synchronized void release(String key){if(owner.equals(key))owner="";}
 public static synchronized boolean occupied(){return !owner.isEmpty();}
}
