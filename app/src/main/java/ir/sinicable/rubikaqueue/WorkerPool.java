package ir.sinicable.rubikaqueue;
import java.util.concurrent.*;
public class WorkerPool {
 private final ExecutorService pool=Executors.newFixedThreadPool(2);
 /** Submits work to the shared two-thread executor. */
 public void submit(Runnable r){pool.submit(r);}
 /** Stops accepting new work while allowing already submitted tasks to complete. */
 public void shutdown(){pool.shutdown();}
}
