package ir.sinicable.rubikaqueue;
import java.util.concurrent.*;
public class WorkerPool {
 private final ExecutorService pool=Executors.newFixedThreadPool(2);
 public void submit(Runnable r){pool.submit(r);}
 public void shutdown(){pool.shutdown();}
}
