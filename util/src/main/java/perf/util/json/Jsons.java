package perf.util.json;

import org.json.JSONObject;

import java.util.Set;

/**
 * Created by wreicher
 */
public class Jsons {

    private static final String KEY_DELIM = ".";
    private final JSONObject obj;
    private volatile boolean readOnly;
    public Jsons(JSONObject obj){
        this(obj,true);
    }
    public Jsons(JSONObject obj, boolean readOnly){
        this.obj = obj;
        this.readOnly = readOnly;
    }

    public boolean has(String key){return obj.has(key);}
    public Jsons getJson(String key){
        return new Jsons(obj.getJSONObject(key));
    }

    public JsonArray getJsonArray(String key){
        return new JsonArray(obj.getJSONArray(key));
    }

    public double getDouble(String key){return obj.getDouble(key);}
    public Double optDouble(String key){return obj.optDouble(key);}

    public long getLong(String key){return obj.getLong(key);}
    public Long optLong(String key){return obj.optLong(key);}

    public String getString(String key){return obj.getString(key);}
    public String optString(String key){return obj.optString(key);}

    public Object get(String key){return obj.get(key);}
    public Set<String> keySet(){return obj.keySet();}

    public String toString(int indent){
        return obj.toString(indent);
    }

    public JSONObject asJSON(){return obj;}
}
