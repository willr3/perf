package perf.util;

import java.util.*;

/**
 * Created by wreicher
 * A list that only supports items being added once (really? this doesn't exit? I'm probably just being too lazy to search).
 * Also has getFirst and getLast like a Dequeue
 */
public class HashedList<V> {

    private HashSet<V> seen;
    private LinkedList<V> data;

    public HashedList(){
        this.seen = new HashSet<V>();
        this.data = new LinkedList<V>();
    }
    public boolean add(V value){
        if(seen.contains(value)){
            return false;
        }
        synchronized (this){
            if(seen.contains(value)){
                return false;
            }
            seen.add(value);
            data.add(value);
            return true;
        }
    }
    public boolean contains(V value){
        return seen.contains(value);
    }
    public int size(){
        return data.size();
    }
    public V getLast(){
        return data.getLast();
    }
    public V getFirst(){
        return data.getFirst();
    }
    public Iterator<V> iterator(){
        return data.iterator();
    }
    public List<V> toList(){return Collections.unmodifiableList(data);}
}
