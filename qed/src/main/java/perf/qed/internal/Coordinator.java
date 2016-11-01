package perf.qed.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class Coordinator {

    private Map<String,CountDownLatch> countdowns;

    public Coordinator(){
        countdowns = new HashMap<>();
    }

    public void initialize(String name,int count){
        if(countdowns.containsKey(name)){
            System.err.println("duplicate initialize for "+name);
            System.exit(-1);
        }
        CountDownLatch latch = new CountDownLatch(count);
        countdowns.put(name,latch);
    }
    public void signal(String name){
        if(!countdowns.containsKey(name)){
            System.err.println("cannot signal, missing "+name);
            System.exit(-1);
        }
        countdowns.get(name).countDown();
    }
    public void waitFor(String name){
        if(!countdowns.containsKey(name)){
            System.err.println("cannot waitFor, missing "+name);
            System.exit(-1);
        }
        try{
            countdowns.get(name).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public boolean waitFor(String name, long timeout, TimeUnit unit){
        boolean acquired = false;
        if(!countdowns.containsKey(name)){
            System.err.println("cannot waitFor, missing "+name);
            System.exit(-1);
        }
        try{
            acquired = countdowns.get(name).await(timeout,unit);
            if(!acquired){
                //TODO what todo when not acquired
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return acquired;
    }
}
