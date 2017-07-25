package perf.qed.internal;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.jboss.logging.Logger;
import perf.qed.ActionPromise;
import perf.qed.Actor;
import perf.qed.Line;
import perf.qed.Run;
import perf.qed.internal.action.Action;
import perf.qed.internal.action.ExecAction;
import perf.qed.internal.action.LineAction;
import perf.qed.internal.action.PatternLine;
import perf.qed.internal.action.ShAction;
import perf.qed.internal.executor.ActionRunner;

/**
 * Created by wreicher
 */
public class ActorImpl implements Actor {


    private Shell shell;
    private Session session;
    private Coordinator coordinator;
    private Run run;
    private String scriptName;
    private Host host;

    private final Action startAction;
    private Action currentAction;
    private Logger logger;

    private ActionRunner runner;

    public ActorImpl(Shell shell, Session session, Coordinator coordinator, Run run, String scriptName, Host host) {
        this.shell = shell;
        this.session = session;
        this.runner = new ActionRunner();
        this.coordinator = coordinator;
        this.run = run;
        this.scriptName = scriptName;
        this.host = host;
        this.startAction = new Action(host.getHostName()+"-"+scriptName) {
            @Override
            public void apply(String input) {
                this.getOutput().set(input);
            }
        };
        logger = Logger.getLogger(scriptName, host.getHostName());

    }

    @Override
    public ActionPromise sh(String command) {
        Action tail = runner.tail();
        ShAction rtrn = new ShAction(shell, this,command);
        rtrn.setParent(tail);
        runner.addLast(rtrn);
        return rtrn;
    }

    @Override
    public ActionPromise exec(String command) {
        Action tail = runner.tail();
        ExecAction rtrn = null;
        try {
            rtrn = new ExecAction((ChannelExec) session.openChannel("exec"), command);
            rtrn.setParent(tail);
        } catch (JSchException e) {
            e.printStackTrace();
        }
        if(rtrn!=null){
            runner.addLast(rtrn);
        }else{
            //TODO handle error creating ExecAction due to ChannelExec execptions
        }

        return rtrn;
    }

    @Override
    public ActionPromise then(Line action) {
        Action tail = runner.tail();
        LineAction rtrn = new LineAction(action,"then "+action.toString());
        rtrn.setParent(tail);
        return then(rtrn);
    }

    @Override
    public ActionPromise then(ActionPromise action) {
        Action tail = runner.tail();
        if(action instanceof Action){
            Action a = (Action)action;
            a.setParent(tail);
            runner.addLast(a);
        }
        return action;
    }

    @Override
    public ActionPromise thenAll(Line... action) {
        Action tail = runner.tail();
        Action rtrn = new LineAction(Line.ECHO,"thenAll");
        rtrn.setParent(tail);
        for (Line a : action) {
            if (a != null) {
                rtrn.then(a);
            }
        }
        runner.addLast(rtrn);
        return rtrn;
    }

    @Override
    public ActionPromise thenAll(ActionPromise... action) {
        Action tail = runner.tail();
        Action rtrn = new LineAction(Line.ECHO,"thenAll");
        rtrn.setParent(tail);
        for (ActionPromise a : action) {
            if (a != null && a instanceof Action) {
                Action act = (Action) a;
                runner.remove(act);
                act.setParent(rtrn);
                rtrn.then(a);
            }
        }
        runner.addLast(rtrn);
        return rtrn;
    }

    @Override
    public ActionPromise check(Line condition, Line pass) {
        return check(condition, pass, Line.NULL);
    }

    @Override
    public ActionPromise check(Line condition, ActionPromise pass) {
        Action c = new LineAction(condition,"check "+condition.toString());
        Action f = new LineAction(Line.NULL," null fail");
        return check(c, pass, f);
    }

    @Override
    public ActionPromise check(Line condition, Line pass, Line fail) {
        Action c = new LineAction(condition,"check "+condition.toString());
        Action p = new LineAction(pass,"pass "+pass.toString());
        Action f = new LineAction(fail,"fail "+fail.toString());

        return check(c, p, f);
    }

    @Override
    public ActionPromise check(Line condition, ActionPromise pass, ActionPromise fail) {
        Action c = new LineAction(condition,"check "+condition.toString());
        return check(c, pass, fail);
    }

    @Override
    public ActionPromise check(ActionPromise condition, Line pass) {
        Action p = new LineAction(pass,"pass "+pass.toString());
        Action f = new LineAction(Line.NULL," null fail");
        return check(condition, p, f);
    }

    @Override
    public ActionPromise check(ActionPromise condition, Line pass, Line fail) {
        Action p = new LineAction(pass,"pass "+pass.toString());
        Action f = new LineAction(fail,"fail "+fail.toString());
        return check(condition, p, f);
    }

    @Override
    public ActionPromise check(ActionPromise condition, ActionPromise pass, ActionPromise fail) {
        if( condition instanceof Action && pass instanceof Action && fail instanceof Action){
            Action c = (Action) condition;
            Action p = (Action) pass;
            Action f = (Action) fail;

            c.then(p);
            c.fail(f);
        }
        return condition;
    }

    @Override
    public ActionPromise doWhile(ActionPromise action, Line check) {
        Action rtrn = new LineAction(Line.ECHO,"doWhile");
        Action c = new LineAction(check,"while "+check.toString());

        rtrn.then(action);
        action.done(c);
        c.then(action);
        return rtrn;
    }

    @Override
    public void signal(final String name) {
        Action rtrn = new LineAction((String input)-> {
                coordinator.signal(name);
                return name;
            },"signal "+name
        );
    }

    @Override
    public void waitFor(final String name) {
        Action rtrn = new LineAction((input)-> {
            coordinator.waitFor(name);
            return input;
        },"waitFor "+name);
    }

    @Override
    public void addArtifact(String name) {
        Action rtrn = new LineAction((input)->{
            run.addArtifact(host,scriptName,name);
            return input;
        },"addArtifact "+name);
    }

    @Override
    public void abort(String message) {
        Action rtrn = new LineAction((input)->{
            run.abort(message);
            return message;
        },"Abort: "+message);
    }

    @Override
    public void log(String message) {
        logger.info(message);
    }

    @Override
    public void log(String message, Exception e) {
        logger.error(message, e);
    }

    @Override
    public String getHostName() {
        return host.getHostName();
    }

    @Override
    public String getRunId() {
        return run.getName();
    }

    @Override
    public String getRunEnv(String name) {
        return run.getEnv(name);
    }

    @Override
    public void setRunEnv(String name, String value) {
        run.setEnv(name, value);
    }

    @Override
    public String getHostEnv(String name) {
        return host.getEnv(name);
    }

    @Override
    public void setHostEnv(String name, String value) {
        host.setEnv(name, value);
    }

    @Override
    public Line patternMatch(String regex) {
        return new PatternLine(regex);
    }

    @Override
    public Line delayCounter(int limit, long delay) {
        return new RepeatLimitAction(limit, delay);
    }

}
