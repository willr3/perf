package perf.reflect;

import java.util.*;

/**
 * Created by wreicher
 */
public class DescendantGraph {


    private HashMap<Class,HashSet<Class>> descendants;

    public DescendantGraph(){
        descendants = new HashMap<>();
    }

    public int size(){return descendants.size();}
    public Set<Class> getClasses(){return descendants.keySet();}

    public Set<Class> getExtenders(Class baseClass){
        if(!descendants.containsKey(baseClass)){
            return Collections.emptySet();
        }
        return descendants.get(baseClass);
    }

    public boolean hasAncestor(Class current){
        if (current.isInterface()){
            //not sure the interface could be null but being defensive
            return current.getInterfaces().length>0 && current.getInterfaces()[0]!=null;
        }else{
            return current.getSuperclass()!=null;
        }
    }
    public Set<Class> getAncestors(Class currentClass){
        Set<Class> rtrn = new HashSet<>();
        if(currentClass.getSuperclass()!=null){
            rtrn.add(currentClass.getSuperclass());
        }
        if(currentClass.getInterfaces().length>0){
            rtrn.addAll(Arrays.asList(currentClass.getInterfaces()));
        }
        return rtrn;
    }
    public void add(Class addClass){
        if(!descendants.containsKey(addClass)){
            descendants.put(addClass,new HashSet<>());
        }

        Class superClass = addClass.getSuperclass();
        if(superClass!=null){
            if(!descendants.containsKey(superClass)){
                descendants.put(superClass,new HashSet<>());
            }
            descendants.get(superClass).add(addClass);
        }

        if(addClass.getInterfaces().length>0){
            for(Class implemented : addClass.getInterfaces()){
                if(!descendants.containsKey(implemented)){
                    descendants.put(implemented,new HashSet<>());
                }
                descendants.get(implemented).add(addClass);
            }
        }
    }
    public Set<Class> getDirectDescendants(Class ancestor){
        if(!descendants.containsKey(ancestor)){
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(descendants.get(ancestor));
    }
    public Set<Class> getAllDescendants(Class ancestor){
        if(!descendants.containsKey(ancestor)){
            return Collections.emptySet();
        }
        HashSet<Class> rtrn = new HashSet<>();
        Queue<Class> toScan = new LinkedList<>();

        toScan.addAll(descendants.get(ancestor));
        while(!toScan.isEmpty()){
            Class next = toScan.poll();
            rtrn.add(next);
            if(descendants.containsKey(next)){
                toScan.addAll(descendants.get(next));
            }
        }
        return Collections.unmodifiableSet(rtrn);
    }


}
