package perf.util.json;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by wreicher
 * A javascript object representation that can be either an Array or Object.
 */
public class Json {

    private Map<Object,Object> data;
    private boolean isArray;

    public Json(){
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
    private void checkKeyType(Object key){
        if(! (key instanceof Integer) ){
            isArray = false;
        }
    }
    public int size(){
        return data.size();
    }
    public boolean has(Object key){
        return data.containsKey(key);
    }

    public void set(Object key,Object value){
        checkKeyType(key);
        data.put(key,value);
    }
    public void add(Object value){
        data.put(data.size(),value);
    }
    public void add(Object key,Object value){
        checkKeyType(key);
        if(has(key)){
            Object existing = get(key);
            if(existing instanceof Json && ( (Json)existing).isArray()){
                Json existingJson = (Json)existing;
                existingJson.add(value);
            }else{
                Json newArray = new Json();
                newArray.add(existing);
                newArray.add(value);
                data.put(key,newArray);
            }
        } else {
            data.put(key,value);
        }
    }


    public boolean isArray(){return isArray;}

    public Object get(Object key){
        return data.get(key);
    }
    public String getString(Object key){
        return has(key) ? data.get(key).toString() : null;
    }
    public Json getJson(Object key){
        return has(key) ? (Json)data.get(key) : null;
    }
    public long getLong(Object key){
        return has(key) ? (Long)data.get(key) : null;
    }

    public Set<Object> keys(){return data.keySet();}

    public void forEach(Consumer<Object> consumer){
        data.values().forEach(consumer);
    }
    public void forEach(BiConsumer<Object,Object> consumer){
        data.entrySet().forEach((entry)->consumer.accept(entry.getKey(),entry.getValue()));
    }
}
