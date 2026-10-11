package ir.sinicable.rubikaqueue;

/** Scheduler bridge for persistent queue processing. */
public class QueueScheduler {
    private final QueueManager queue;
    /** Creates a scheduler bridge using the supplied queue manager. */
    public QueueScheduler(QueueManager q){ this.queue=q; }
    /** Atomically claims the next due queue item, returning null if none is ready. */
    public QueueItem next(){ return queue.nextReady(); }
}
