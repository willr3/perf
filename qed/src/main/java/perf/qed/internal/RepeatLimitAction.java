package perf.qed.internal;

import perf.qed.Line;

/**
 * Created by wreicher
 */
public class RepeatLimitAction implements Line {

    private long delay;
    private int limit;
    private int tries = 0;

    public RepeatLimitAction(int limit,long delay){
        this.limit = limit;
        this.delay = delay;
    }

    @Override
    public String apply(String input) {
        if(input == null || input.isEmpty()){
            tries++;
            try{
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            if(tries >= limit){
                return null;
            }else{
                return "retry";
            }
        }else{
            return null;
        }
    }
}
