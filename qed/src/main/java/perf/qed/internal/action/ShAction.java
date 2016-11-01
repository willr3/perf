package perf.qed.internal.action;


import perf.qed.Actor;
import perf.qed.internal.ResultListener;
import perf.qed.internal.Shell;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class ShAction extends Action implements ResultListener {

    private final Matcher envRef = Pattern.compile("\\$\\{\\{([^\\}]+)\\}\\}").matcher("");

    private Shell shell;
    private String command;
    private Actor actor;
    public ShAction(Shell shell, Actor actor, String command){
        super(command);
        this.command = command;
        this.shell = shell;
        this.actor = actor;
    }

    @Override
    public void apply(String input) {
        //result.addListener(this);

        //shell.setResult(this); TODO removed

        if(envRef.reset(command).find()){
            StringBuilder sb = new StringBuilder();
            int prev = 0;
            do {
                String refName = envRef.group(1);
                String value = resolveVariable(refName);
                sb.append(command.substring(prev,envRef.start()));
                sb.append(value);
                prev = envRef.end();
            } while ( envRef.find() );
            sb.append(command.substring(prev));
            command = sb.toString();
        }


        shell.sh(command);
    }

    public String toString(){
        return command;
    }

    protected String resolveVariable(String name){
        String def = null;
        int defSplit=-1;
        if((defSplit=name.indexOf(":"))>-1){
            def = name.substring(defSplit+1);
            name = name.substring(0,defSplit);
        }
        String value = actor.getHostEnv(name);
        if(value==null){
            value = actor.getRunEnv(name);
        }
        if(value==null) {
            value = def;
        }
        if(value==null){
            actor.abort("Run aborting because no value bound for "+name+" on the host or run level");
        }
        return value;
    }

    @Override
    public void onOk(String message) {
        //shell.clearResult(); TODO remove
    }

    @Override
    public void onError(String message) {
        //shell.clearResult(); TODO remove
    }

    @Override
    public void onUpdate(String message) {}
}
