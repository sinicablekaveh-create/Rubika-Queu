package ir.sinicable.rubikaqueue;
import android.app.Service;

public final class AccountSlots {
 private AccountSlots(){}
 public static int valid(int slot){if(slot<1||slot>3)throw new IllegalArgumentException("slot");return slot;}
 @SuppressWarnings("unchecked")
 public static Class<? extends Service> serviceClass(int slot){
  switch(valid(slot)){
   case 1:return QueueService.class;
   case 2:return QueueService2.class;
   default:return QueueService3.class;
  }
 }
 public static boolean running(int slot){
  switch(valid(slot)){case 1:return QueueService.running;case 2:return QueueService2.running;default:return QueueService3.running;}
 }
 public static boolean waitingForAuth(int slot){
  switch(valid(slot)){case 1:return QueueService.waitingForAuth;case 2:return QueueService2.waitingForAuth;default:return QueueService3.waitingForAuth;}
 }
 public static void setWaitingForAuth(int slot,boolean value){
  switch(valid(slot)){case 1:QueueService.waitingForAuth=value;break;case 2:QueueService2.waitingForAuth=value;break;default:QueueService3.waitingForAuth=value;}
 }
 public static String recoveryPrefs(int slot){return slot==1?"queue_recovery":"queue_recovery_"+valid(slot);}
 public static int noticeId(int slot){return 170+valid(slot);}
 public static String queueKey(int slot){return "queue"+valid(slot);}
}
