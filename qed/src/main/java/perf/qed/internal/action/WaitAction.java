package perf.qed.internal.action;

import perf.qed.internal.Coordinator;

/**
 * Created by wreicher
 */
public class WaitAction extends Action {

    private Coordinator coordinator;
    private String monitor;
    public WaitAction(Coordinator coordinator, String monitor){
        super("waitFor "+monitor);
        this.coordinator = coordinator;
        this.monitor = monitor;
    }

    @Override
    public void apply(String input) {
        coordinator.waitFor(monitor);
        ok(input);
    }


}
