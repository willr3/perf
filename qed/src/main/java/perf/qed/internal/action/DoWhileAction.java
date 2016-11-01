package perf.qed.internal.action;

import perf.qed.internal.ResultListener;

/**
 * Created by wreicher
 */
public class DoWhileAction extends Action implements ResultListener{

    private Action action;
    private Action check;
    public DoWhileAction(Action action,Action check){
        super("doWhile");

        action.setInput(this.getInput());

        action.done(check);

        check.setParent(action);
        check.then(action);
        check.addListener(this);
    }

    @Override
    public void run(){
        super.run();
        action.run(); //start the doWhile loop
    }

    @Override
    public void apply(String input) {}

    @Override
    public void onOk(String message) {
        //this will re-run the loop
        this.update(message);//re running the loop is considered an update
    }

    @Override
    public void onError(String message) {
        //the loop is over (check returned false)
        //resolve as ok with last output from action
        ok(action.getOutput().get());
    }

    @Override
    public void onUpdate(String message) {

    }
}
