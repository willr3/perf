package perf.stack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by wreicher
 */
public class Data {

    private Map<Object,Object> data;
    private boolean isArray;

    public Data(){
        this.data = new LinkedHashMap<>();
        this.isArray = true;
    }

    public void put(Object key,Object value){
        if(key instanceof Integer){
            key = ((Number)key).intValue();
        }else{
            isArray = false;
        }
        this.data.put(key,value);
    }
    public int size(){
        return data.size();
    }

    public boolean has(Object key){
        return data.containsKey(key);
    }
    public Set<Object> keys(){return data.keySet();}
    public Object get(Object key){
        return data.get(key);
    }
    public String getString(Object key){
        return has(key) ? data.get(key).toString() : null;
    }
    public Data getData(Object key){
        return has(key) ? (Data)data.get(key) : null;
    }
    public long getLong(Object key){
        return has(key) ? (Long)data.get(key) : null;
    }

}
