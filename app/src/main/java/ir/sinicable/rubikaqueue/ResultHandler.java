package ir.sinicable.rubikaqueue;

/** Updates queue state after worker result. */
public class ResultHandler {
    /** Marks the supplied in-memory queue item as SENT without persisting it. */
    public void success(QueueItem item){ item.status="SENT"; }
    /** Marks the in-memory item RETRY_WAIT and records its error and wall-clock retry deadline from a millisecond delay. */
    public void retry(QueueItem item,String error,long delay){
        item.status="RETRY_WAIT"; item.error=error; item.nextRetryTime=System.currentTimeMillis()+delay;
    }
    /** Marks the supplied in-memory queue item FAILED and records its error without persisting it. */
    public void failed(QueueItem item,String error){ item.status="FAILED"; item.error=error; }
}
