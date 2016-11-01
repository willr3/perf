package perf.util;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class HashedLists<K,V> implements Serializable{
    private HashMap<K,List<V>> sets;
    private transient List<V> empty;
    public HashedLists(){
        sets = new HashMap<>();
        empty = Collections.unmodifiableList( new ArrayList<>() );
    }
    public void put(K name,V value){
        if(!sets.containsKey(name)){
            sets.put(name,new ArrayList<>());
        }
        sets.get(name).add(value);
    }
    public void putAll(K name,Collection<V> values){
        if(!sets.containsKey(name)){
            sets.put(name,new ArrayList<V>());
        }
        sets.get(name).addAll(values);
    }
    public List<V> get(K name){
        if(sets.containsKey(name)){
            return Collections.unmodifiableList(sets.get(name));
        } else {
            return empty;
        }
    }
    public int size(){return sets.size();}
    public Set<K> keys(){
        return Collections.unmodifiableSet(sets.keySet());
    }

    public void forEach(BiConsumer<K,List<V>> consumer){
        sets.forEach(consumer);
    }

    public Stream<Map.Entry<K, List<V>>> stream(){return sets.entrySet().stream();}
}
