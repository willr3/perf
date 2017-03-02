package perf.jfr;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class NestedMap<K,V> {

    private V value;
    private LinkedHashMap<K,NestedMap<K,V>> children;

    public NestedMap(){
        this(null);
    }
    public NestedMap(V value){
        this.value = value;
        this.children = new LinkedHashMap<>();
    }
    public V get(){
        return value;
    }
    public NestedMap<K,V> get(K key){
        if(!children.containsKey(key)){
            children.put(key,null);
        }
        return children.get(key);
    }


    public static void main(String[] args) {
        NestedMap<String,String> test = new NestedMap<>();
        test.put("1","one");
        test.put("2","two");
        test.put("3","three");
        test.put("4","four");
        test.put("5","five");
        test.put("6","six");
        test.put("7","seven");
        test.put("8","eight");
        test.put("9","nine");
        test.put("10","ten");
        test.put("11","eleven");

        System.out.println(test.isList());
    }

    public boolean isList(){
        return
            !children.isEmpty() &&
            children.keySet().stream()
                    .map( (K)->K.toString() ).
                    allMatch( (v)->Pattern.matches("\\d+",v) );
    }
    public boolean hasChildren(){
        return !children.isEmpty();
    }
    public NestedMap<K,V> put(K key, V value){
        if(!children.containsKey(key)){
            children.put(key,new NestedMap<>(value));
        }
        return children.get(key);
    }

    public Set<K> getKeys(){ return children.keySet(); }

    public boolean has(K key){
        return children.containsKey(key);
    }
    public String toString(){
        StringBuffer sb = new StringBuffer();
        append(sb,2);
        return sb.toString();
    }
    private void append(StringBuffer buffer,int indent){
        for(K key : children.keySet()){
            NestedMap<K,V> nested = children.get(key);
            for(int i=0; i<indent; i++){
                buffer.append(" ");
            }
            buffer.append(key);
            buffer.append(": ");
            buffer.append(nested.get());
            buffer.append("\n");
            nested.append(buffer,indent+2);
        }
    }
}
