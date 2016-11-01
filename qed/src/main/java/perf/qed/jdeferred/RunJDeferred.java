package perf.qed.jdeferred;

import org.jdeferred.*;
import org.jdeferred.impl.DefaultDeferredManager;
import org.jdeferred.impl.DeferredObject;
import org.jdeferred.impl.PipedPromise;

/**
 * Created by wreicher
 */
public class RunJDeferred {

    public static void main(String[] args) {
        Deferred<String,String,String> d = new DeferredObject<>();

        DeferredManager dm = new DefaultDeferredManager();



        Promise second = d.promise().then((DonePipe) (result)->{
            DeferredObject toReturn = new DeferredObject();
            System.out.println("DonePipe.toReturn...");
            try{
                Thread.currentThread().sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("DonePipe.resolving");
            return toReturn.resolve("foo");
        });
        second.then((input)->{
            System.out.println("second.then "+input+" "+Thread.currentThread().getName());
        });
        second.then((input)->{
            System.out.println("second.second.then "+input+" "+Thread.currentThread().getName());
        });
        second.done(System.out::println);


        System.out.println("testing");
        d.resolve("hi");
        try{
            Thread.currentThread().sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
