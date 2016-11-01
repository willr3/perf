package perf.qed.internal.executor;

import perf.qed.internal.ResultListener;
import perf.qed.internal.action.Action;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wreicher
 */
public class ActionRunner implements Runnable, ResultListener{

    private Deque<Action> actions;
    private Action currentAction;
    private AtomicBoolean shutdown;
    private Semaphore lock;

    private UpdateRunner updateRunner;
    private Thread updateThread;

    Action first;

    public ActionRunner(){
        actions = new LinkedList<>();
        currentAction = null;
        shutdown = new AtomicBoolean(false);
        lock = new Semaphore(1);
        updateRunner = new UpdateRunner();
        updateThread = new Thread(updateRunner);

        first = new Action("START") {
            @Override
            public void apply(String input) {
                this.ok(input);
            }
        };
        actions.addFirst(first);
    }
    public Action tail(){
        return actions.peekLast();
    }
    public Action peek(){
        return actions.peek();
    }
    public void addFirst(Action action){
        actions.addFirst(action);
    }
    public void addLast(Action action){
        actions.addLast(action);
    }
    public void remove(Action action){
        actions.remove(action);
    }
    public void add(List<Action> toAdd){
        for(int i=toAdd.size()-1;i>=0; i--){
            actions.addFirst(toAdd.get(i));
        }
    }
    public Action getNext(){
        try{
            lock.acquire();
            currentAction = actions.pop();
        } catch(InterruptedException e){
            e.printStackTrace();
            Thread.interrupted();
        } finally {

        }
        return null;
    }
    public boolean isEmpty(){
        return actions.isEmpty();
    }

    @Override
    public void run() {
        updateThread.start();

        Action action = null;
        while(!shutdown.get() && !isEmpty()){
            try{
                action = getNext();
                if(action==null){
                    //TODO how do we handle no next?
                    return;
                }
                if(action.hasWatchers()){

                }
                action.run();

            } catch(Exception e){
                e.printStackTrace();
                if(action!=null){
                    action.error(e.getMessage());
                }
            } finally { }
        }
        updateRunner.shutdown();
        updateThread.interrupt();
    }

    @Override
    public void onOk(String message) {
        if(currentAction!=null){
            if(currentAction.hasDones()){
                add(currentAction.getDones());
            }
            if(currentAction.hasThens()){
                add(currentAction.getThens());
            }
        }
    }

    @Override
    public void onError(String message) {
        if(currentAction.hasDones()){
            add(currentAction.getDones());
        }
        if(currentAction.hasFails()){
            add(currentAction.getFails());
        }
    }

    @Override
    public void onUpdate(String message) {
        if(currentAction!=null && currentAction.hasWatchers()){
            updateRunner.update(message);
        }
    }
}
