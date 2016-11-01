package perf.qed.internal.action;

/**
 * Created by wreicher
 */
public class CheckAction extends Action {

    private Action check;
    private Action pass;
    private Action fail;
    public CheckAction(Action check,Action pass,Action fail){
        super("check");
        this.check = check;
        this.pass = pass;
        this.fail = fail;

        check.setInput(this.getInput());
        check.then(pass);
        check.fail(fail);

    }



    @Override
    public void apply(String input) {
        ok(input); //pass the input to the next action for after the check
    }

    @Override
    public void run(){
        super.run();
        check.run();
    }

}
