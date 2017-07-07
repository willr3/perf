package perf.util.json;

import org.json.JSONArray;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class JsonArray {

    private class ConsumerWrapper implements BiConsumer<Jsons,Integer> {

        private Consumer<Jsons> consumer;

        public ConsumerWrapper(Consumer<Jsons> c){
            this.consumer = c;
        }
        @Override
        public void accept(Jsons json, Integer integer) {
            consumer.accept(json);
        }
    }

    private final JSONArray obj;

    public JsonArray(JSONArray array){
        this.obj = array;
    }

    public JsonArray reverse(){
        JSONArray rtrn = new JSONArray();
        for(int i=obj.length()-1; i>=0; i--){
            rtrn.put(obj.get(i));
        }
        return new JsonArray(rtrn);
    }

    public int length(){
        return obj.length();
    }
    public Object get(int index){
        return obj.get(index);
    }
    public Jsons getJson(int index){
        return new Jsons(obj.getJSONObject(index));
    }
    public void forEachJson(Consumer<Jsons> action){
        forEachJson(new ConsumerWrapper(action));
    }
    public void forEachJson(BiConsumer<Jsons,Integer> action){
        for(int i=0; i<obj.length(); i++){
            Jsons j = new Jsons(obj.getJSONObject(i));
            action.accept(j,i);
        }
    }
    public List<Jsons> toList(){
        LinkedList<Jsons> rtrn = new LinkedList<>();
        for(int i=0; i<obj.length();i++){
            Jsons j = new Jsons(obj.getJSONObject(i));
            rtrn.add(j);
        }
        return rtrn;
    }

}
