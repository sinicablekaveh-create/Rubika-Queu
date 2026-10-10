package ir.sinicable.rubikaqueue;

/** Scheduler bridge for persistent queue processing. */
public class QueueScheduler {
    private final QueueManager queue;
    public QueueScheduler(QueueManager q){ this.queue=q; }
    public QueueItem next(){ return queue.nextReady(); }
}
