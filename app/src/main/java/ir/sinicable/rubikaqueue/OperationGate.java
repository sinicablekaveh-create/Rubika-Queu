package ir.sinicable.rubikaqueue;
public final class OperationGate {
 private static String owner="";
 /** Claims the process-wide gate for a key if free; callers must supply a nonempty key. */
 public static synchronized boolean acquire(String key){if(!owner.isEmpty())return false;owner=key;return true;}
 /** Releases the gate only when the supplied key matches the current owner. */
 public static synchronized void release(String key){if(owner.equals(key))owner="";}
 /** Returns whether an operation currently holds the gate. */
 public static synchronized boolean occupied(){return !owner.isEmpty();}
}
