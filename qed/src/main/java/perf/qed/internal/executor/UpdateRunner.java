package perf.qed.internal.executor;

import perf.qed.internal.action.Action;

import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wreicher
 */
public class UpdateRunner implements Runnable {

    private List<Action> watchers;
    private BlockingDeque<String> updates;
    private AtomicBoolean isRunning;
    private AtomicBoolean shutdown;

    public UpdateRunner(){
        watchers = null;
        updates = new LinkedBlockingDeque<>();
        isRunning = new AtomicBoolean(false);
        shutdown = new AtomicBoolean(false);
    }

    public void setWatchers(List<Action> watchers){
        this.watchers = watchers;
    }
    public void update(String message){
        updates.add(message);
    }
    public boolean isEmpty(){return updates.isEmpty();}
    public void clearWatchers(){this.watchers = null;}
    public void shutdown(){
        shutdown.set(true);
        Thread.currentThread().interrupt();
    }
    public void run(){
        while(!shutdown.get()) {
            try {
                String next = updates.takeFirst();
                if (watchers != null) {
                    isRunning.set(true);
                    for (Action a : watchers) {
                        try {
                            a.apply(next);
                        } catch (Exception e){
                            a.error(e.getMessage());
                        }
                    }
                }
                isRunning.set(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.interrupted();
            }
        }
    }
}
