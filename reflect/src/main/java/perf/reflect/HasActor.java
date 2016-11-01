package perf.reflect;

import perf.reflect.actor.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides adder methods that make it easier to write lambda's for the various {@link Actor} sub-interfaces and retain them in the same {@code Actor} collection.
 */
public class HasActor {

    protected Map<String,Actor> actors;

    public HasActor(){
        this.actors = new LinkedHashMap<>();
    }

    private void addActor(String name, Actor expander){
        actors.put(name,expander);
    }
    public boolean haveActor(String name){
        return actors.containsKey(name);
    }

    public void removeActor(String name){
        actors.remove(name);
    }
    public void clearActors(){
        actors.clear();}

    public void addFieldActor(String name, FieldActor expander){
        addActor(name,expander);
    }
    public void addFieldClassActor(String name, FieldClassActor expander){
        addActor(name,expander);
    }
    public void addClassActor(String name, ClassActor expander){
        addActor(name,expander);
    }
    public void addMethodActor(String name, MethodActor expander){
        addActor(name,expander);
    }
    public void addMethodClassActor(String name, MethodClassActor expander){
        addActor(name,expander);
    }
    public void addConstructorActor(String name, ConstructorActor expander){
        addActor(name,expander);
    }
    public void addConstructorClassActor(String name, ConstructorClassActor expander){
        addActor(name,expander);
    }
}
