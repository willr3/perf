package perf.qed.internal.action;

import perf.qed.internal.Coordinator;

/**
 * Created by wreicher
 */
public class SignalAction extends Action {

    private Coordinator coordinator;
    private String monitor;
    public SignalAction(Coordinator coordinator, String monitor){
        super("signal "+monitor);
        this.coordinator = coordinator;
        this.monitor = monitor;
    }

    @Override
    public void apply(String input) {
        coordinator.signal(monitor);
        ok(input);
    }
}
