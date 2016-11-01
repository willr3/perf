package perf.qed.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wreicher
 */
public class SimpleFuture<T> implements Future<T> {

    private T value;
    private boolean isCancelled;
    private boolean isResolved;

    private ReentrantLock lock;
    private Condition condition;

    public SimpleFuture(){
        value = null;
        this.isCancelled = false;
        this.isResolved = false;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if(isResolved || isCancelled){
            return false;
        }
        this.isCancelled = true;
        condition.notifyAll();
        return true;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return isResolved;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if(isDone())
            return value;
        if(isCancelled())
            return null;
        while(!isDone() && !isCancelled()){
            lock.lock();
            try{

                condition.await();
                return value;
            } catch(InterruptedException e){
                Thread.interrupted();
            } finally {
                lock.unlock();
            }
        }
        //We should never really get here
        return null;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if(isDone())
            return value;
        if(isCancelled())
            return null;

        boolean resolved = true;
        while(resolved && !isCancelled() && isDone()){
            lock.lock();
            try{
                resolved = condition.await(timeout,unit);
            } catch (InterruptedException e){
                Thread.interrupted();
            }finally{
                lock.unlock();
            }
        }
        return value;
    }

    /**
     * Sets the value only if the value was not already set
     * @param value
     * @return true if the value we set, false if it was already set or cancelled
     */
    public boolean set(T value){
        boolean rtrn = false;
        if(isCancelled() || isDone()){
            return rtrn;
        }
        lock.lock();
        try{

            if(!isDone() && !isCancelled()){
                this.value = value;
                isResolved = true;
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
        return rtrn;
    }
}
