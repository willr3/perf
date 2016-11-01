package perf.qed.jdeferred;

import com.sun.xml.internal.ws.util.CompletedFuture;

import java.util.concurrent.CompletableFuture;

/**
 * Created by wreicher
 */
public class RunCompFuture {


    public static void main(String[] args) {
        CompletableFuture<String> start = new CompletableFuture<String>();
        
        CompletableFuture<String> second = new CompletableFuture<>();

        CompletableFuture<String> callMe = new CompletableFuture<>();
        callMe.thenAccept(System.out::println);



        CompletableFuture then = start.thenApply((input)->{
            System.out.println("then "+Thread.currentThread().getName());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return input;
        });
        start.thenApply((input)->{
            System.out.println("start.accept => "+input+" "+Thread.currentThread().getName());
            return input;
        });

        then.thenApply((input)->{
            System.out.println("then.1 (Apply) => "+input+" "+Thread.currentThread().getName());
            return input;
        }).thenCompose((input)->{
            System.out.println("then.Compose ("+input+")");
            return second;
        }).thenAccept((input)->{
            System.out.println("then.compose.1 => "+input+" "+Thread.currentThread().getName());
        });

        System.out.println("Starting");
        start.complete("value");

        try {
            Thread.sleep(10000);
            System.out.println("completing second future");
            second.complete("second");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("closing");

    }
}
