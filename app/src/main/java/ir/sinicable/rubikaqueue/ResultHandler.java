package ir.sinicable.rubikaqueue;

/** Updates queue state after worker result. */
public class ResultHandler {
    public void success(QueueItem item){ item.status="SENT"; }
    public void retry(QueueItem item,String error,long delay){
        item.status="RETRY_WAIT"; item.error=error; item.nextRetryTime=System.currentTimeMillis()+delay;
    }
    public void failed(QueueItem item,String error){ item.status="FAILED"; item.error=error; }
}
