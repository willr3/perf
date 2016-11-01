package perf.reflect;

import perf.reflect.actor.Actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

/**
  * Walks through all the {@code field | method | constructor} given class and its superclasses / interfaces and passes the {@code field | method | constructor} to any attached {@link Actor}.
 *
 */
public class HierarchyWalker extends HasActor implements Actor{

    public HierarchyWalker(){}

    public void walk(Class targetClass){
        LinkedList<Class> toScan = new LinkedList<Class>();
        toScan.add(targetClass);
        Set<Class> walked = new LinkedHashSet<>();
        while(!toScan.isEmpty()){
            Class knownClass = toScan.pop();
            walked.add(knownClass);
            try{
                apply(knownClass,null,null,null);

                for(Field f : knownClass.getDeclaredFields()){
                    if(f.getName().equals("this$0")) {//NOTE hack to get around member class reference to containing class instance
                        continue;
                    }
                    apply(knownClass,f,null,null);
                }
                for(Constructor c : knownClass.getDeclaredConstructors()){
                    apply(knownClass,null,null,c);
                }
                for(Method m : knownClass.getDeclaredMethods()){
                    apply(knownClass,null,m,null);
                }

                if(knownClass.getInterfaces().length>0){
                    for(Class implemented : knownClass.getInterfaces()){
                        if(!walked.contains(implemented)) {
                            toScan.add(implemented);
                        }
                    }
                }

                if(knownClass.getSuperclass()!=null && !walked.contains(knownClass.getSuperclass()) && ! knownClass.getSuperclass().equals(Object.class) ){
                    toScan.add(knownClass.getSuperclass());
                }


            }catch(NoClassDefFoundError | TypeNotPresentException e){}
        }
    }

    @Override
    public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        for(Actor actor : actors.values()){
            actor.apply(knownClass,field,method,constructor);
        }
        return "";
    }
}
