package perf.qed.internal.action;

import perf.qed.ActionPromise;
import perf.qed.Line;
import perf.qed.internal.Result;
import perf.qed.internal.ResultListener;
import perf.qed.internal.SimpleSupplier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public abstract class Action implements ActionPromise, Result, Runnable{

    private static final AtomicInteger HC = new AtomicInteger(0);

    private final int hc;
    private final String name;

    private ActionState state;

    private Action parent;

    private SimpleSupplier<String> input;
    private SimpleSupplier<String> output;

    List<ResultListener> listeners;

    List<Action> thens;
    List<Action> fails;
    List<Action> watchers;
    List<Action> dones;

    public Action(String name){
        this.name = name;
        this.input = SimpleSupplier.EMPTY_STRING;
        this.output = new SimpleSupplier<>();
        this.hc = HC.getAndIncrement();
        this.state = ActionState.PENDING;

        this.parent = null;
        this.thens = new LinkedList<>();
        this.fails = new LinkedList<>();
        this.watchers = new LinkedList<>();
        this.dones = new LinkedList<>();

        this.listeners = new LinkedList<>();

    }

    abstract public void apply(String input);

    public void run(){
        this.state = ActionState.ACTIVE;
        String message = input.get();
        apply(message);
    }


    public void addListener(ResultListener listener){
        this.listeners.add(listener);
    }
    public void removeListener(ResultListener listener){
        this.listeners.remove(listener);
    }
    public void setParent(Action action){
        this.parent = action;
        this.input = this.parent.getOutput();
    }
    public Action getParent(){return this.parent;}
    public boolean hasParent(){return this.parent!=null;}
    public boolean isPending(){return this.state == ActionState.PENDING;}
    public boolean isActive(){return this.state == ActionState.ACTIVE;}
    public boolean isDone(){return this.state != ActionState.PENDING && this.state != state.ACTIVE;}
    public boolean failed(){return this.state == ActionState.ERROR;}
    public boolean worked(){return this.state == ActionState.OK;}

    public SimpleSupplier<String> getInput(){return input;}
    public void setInput(SimpleSupplier<String> input){
        this.input = input;
    }

    public SimpleSupplier<String> getOutput(){return output;}
    public void setOutput(SimpleSupplier<String> output){
        this.output = output;
    }

    public void ok(String message){
        if(!isDone()) {
            this.state = ActionState.OK;
            this.output.set(message);
        }
    }
    public void error(String message){
        if(!isDone()) {
            this.state = ActionState.ERROR;
            this.output.set(message);
        }
    }
    public void update(String message){
        if(!listeners.isEmpty()){
            for (ResultListener l : listeners){
                l.onUpdate(message);
            }
        }
    }

    public boolean isTruthy(String input){
        return input!=null && !input.equals("fail") && !input.equals("FAIL");
    }

    public boolean hasThens(){return !thens.isEmpty();}
    public List<Action> getThens(){
        return Collections.unmodifiableList(thens);
    }

    public boolean hasFails(){return !fails.isEmpty();}
    public List<Action> getFails(){
        return Collections.unmodifiableList(fails);
    }

    public boolean hasDones(){return !dones.isEmpty();}
    public List<Action> getDones(){
        return Collections.unmodifiableList(dones);
    }

    public boolean hasWatchers(){return !watchers.isEmpty();}
    public List<Action> getWatchers(){
        return Collections.unmodifiableList(watchers);
    }

    public String getName(){return name;}


    public final ActionPromise then(Line action){
        Action a = new LineAction(action,action.toString());
        return then(a);
    }
    public final ActionPromise then(ActionPromise action){
        return then(action,true);

    }
    protected final ActionPromise then(ActionPromise action,boolean setParent){

        if(action instanceof Action){
            Action a = (Action)action;
            if(setParent) {
                a.setInput(this.getOutput());
                a.setParent(this);
            }
            thens.add(a);
        }
        return action;
    }
    public final ActionPromise watch(Line action){
        Action a = new LineAction(action,action.toString());
        a.setParent(this);
        watchers.add(a);
        return a;
    }
    public final ActionPromise done(Line action){
        Action a = new LineAction(action,action.toString());
        return done(a);
    }
    public final ActionPromise done(ActionPromise action){
        if(action instanceof Action){
            Action a = (Action)action;
            a.setParent(this);
            dones.add(a);
        }
        return action;
    }
    public final ActionPromise fail(Line action){
        Action a = new LineAction(action,action.toString());
        return fail(a);
    }
    public final ActionPromise fail(ActionPromise action){
        if(action instanceof Action) {
            Action a = (Action)action;
            a.setInput(this.getOutput());
            a.setParent(this);
            fails.add(a);
        }
        return action;
    }
    @Override
    public int hashCode(){
        return hc;
    }
    @Override
    public boolean equals(Object object){
        if(object instanceof Action){
            return hashCode()==object.hashCode();
        }
        return false;
    }

    private String pad(int amount){
        return "                                                                ".substring(0,amount);
    }
    public void print(int shift){
        Consumer<Action> cc = (action)->{action.print(shift+2);};
        Consumer<Action> ca = (action)->{action.print(shift);};
        System.out.println(pad(shift)+this.toString());
        if(hasWatchers()){
            System.out.println(pad(shift+1)+"watch:");
            getWatchers().forEach(cc);
        }
        if(hasFails()){
            System.out.println(pad(shift+1)+"fail:");
            getFails().forEach(cc);
        }
        if(hasThens()){
            getThens().forEach(ca);
        }
    }
}
