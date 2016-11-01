package perf.qed.internal.executor;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wreicher
 */
public class DelayedService extends ThreadPoolExecutor {


    private LinkedBlockingDeque<Runnable> queue;
    public DelayedService(){
        super(1,1,1, TimeUnit.MINUTES,new LinkedBlockingDeque<Runnable>());
        queue = (LinkedBlockingDeque<Runnable>)super.getQueue();
    }

    public void addFirst(Runnable runnable){
        queue.addFirst(runnable);

    }
}
