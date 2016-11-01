package perf.qed.internal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by wreicher
 */
public class BlockingSupplier<T> extends SimpleSupplier<T> {


    private ReentrantLock lock;
    private Condition condition;
    private boolean isSet;

    public BlockingSupplier(){
        this(null);
    }
    public BlockingSupplier(T value){
        super(value);
        this.isSet = false;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    public boolean reset(){
        boolean rtrn = isSet;

        isSet = false;

        return rtrn;
    }
    public boolean isSet(){return isSet;}

    @Override
    public void set(T value){
        if(isSet) {
            return;
        }
        lock.lock();
        try{
            if(!isSet()){
                super.set(value);
                isSet = true;
                condition.signalAll();

            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T get() {
        if(isSet())
            return super.get();

        while(!isSet()){
            lock.lock();
            try{
                condition.await();
                return super.get();
            } catch (InterruptedException e){
                Thread.interrupted();
            } finally {
                lock.unlock();
            }
        }
        return super.get();
    }

    public T get(long timeout, TimeUnit unit){
        if(isSet())
            return super.get();
        boolean resolved = true;

        while(resolved && !isSet()){
            //lock.lock();
            try{
                resolved = condition.await(timeout,unit);
            } catch (InterruptedException e){
                Thread.interrupted();
            }finally{
                //lock.unlock();
            }
        }

        return super.get();
    }


    public static void main(String[] args) {
        BlockingSupplier<String> bs = new BlockingSupplier<>();

        Runnable r1 = ()->{
            System.out.println("start "+Thread.currentThread().getName());
            String value = null;
            do{
                value = bs.get();
                System.out.println(Thread.currentThread().getName()+" received "+value);
            }while(value!="stop" && value!=Thread.currentThread().getName());
            System.out.println("stop "+Thread.currentThread().getName());
        };
        Thread t1 = new Thread(r1,"T_1");
        Thread t2 = new Thread(r1,"T_2");
        Thread t3 = new Thread(r1,"T_3");

        t1.start();
        t2.start();
        t3.start();

        try {
            System.out.println("sleeping");

            Thread.sleep(2000);
            bs.set("foo");
            Thread.sleep(2000);
            bs.reset();
            bs.set("T_1");
            Thread.sleep(2000);
            bs.reset();
            bs.set("stop");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
