package perf.parse.consumers;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.parse.JsonConsumer;
import perf.util.json.Jsons;

/**
 *
 */
public class JsonKeyMapConsumer implements JsonConsumer {

    private JSONObject keymap;

    public JsonKeyMapConsumer(){
        keymap = new JSONObject();
    }

    @Override public void start() {  }

    @Override public void consume(Jsons object) {
        mergeKeys(keymap,object);
    }

    @Override public void close() {  }

    public JSONObject getMap(){return keymap;}

    public void mergeKeys(JSONObject target,JSONArray source){
        for(int i=0; i<source.length(); i++){
            Object val = source.get(i);
            if(val instanceof JSONArray){
                if(!target.has("#")){
                    target.put("#",new JSONObject());
                }
                mergeKeys(target.getJSONObject("#"),(JSONArray)val);
            }else if ( val instanceof JSONObject){
                mergeKeys(target,new Jsons((JSONObject)val));
            }
        }
    }
    public void mergeKeys(JSONObject target,Jsons source){
        for(String key : source.keySet()){
            Object val = source.get(key);
            if(target.has(key)){

                if(val instanceof JSONArray){
                    if(!target.getJSONObject(key).has("#")){
                        target.getJSONObject(key).put("#",new JSONObject());
                    }
                    mergeKeys(target.getJSONObject(key).getJSONObject("#"),(JSONArray)val);
                }else if (val instanceof JSONObject){
                    mergeKeys(target.getJSONObject(key), new Jsons((JSONObject) val));
                }
            }else{
                if(val instanceof JSONArray){
                    JSONObject ary = new JSONObject();
                    ary.put("#",new JSONObject());
                    target.put( key, ary );
                    mergeKeys(ary.getJSONObject("#"), (JSONArray) val);
                }else {
                    target.put(key,new JSONObject());
                    if (val instanceof JSONObject){
                        mergeKeys(target.getJSONObject(key),new Jsons((JSONObject)val));
                    }
                }
            }
        }
    }
}
