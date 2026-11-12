package ir.sinicable.rubikaqueue;
public class QueueItem {
 public long id; public String target; public String message; public String type;
 public String status; public int retryCount; public long nextRetryTime; public String error;
 public QueueItem(){status="PENDING";}
}
