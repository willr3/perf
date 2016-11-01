package perf.stack;

import org.json.JSONObject;
import perf.util.json.Jsons;
import perf.util.json.JsonArray;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by wreicher
 */
public class Frame implements Serializable{
    private int id;
    private int count;
    private JSONObject value;
    private ArrayList<Frame> children;

    public Frame(int id){
        this.id=id;
        this.count=0;
        this.children = new ArrayList<Frame>();
        this.value = new JSONObject();
    }

    public Jsons toJson(){
      JSONObject rtrn = new JSONObject();
        if(childCount()>0){
            for(Frame f : children){
                rtrn.accumulate("children",f.toJson().asJSON());
            }
        }
        rtrn.put("id",id);
        rtrn.put("count",count);
        rtrn.put("data",value);
        return new Jsons(rtrn);
    }

    public Jsons getValues(){
        return new Jsons(value);
    }
    public String getValue(String key){
        return value.getString(key);

    }
    public void setValue(String key,String value){
        this.value.put(key,value);
    }
    public void addValue(String key,String value){
        //values.put(key,value);
        this.value.accumulate(key,value);
    }
    public void addValue(String key,long value){
        this.value.accumulate(key,value);
    }
    public void addValue(String key,double value){
        this.value.accumulate(key,value);
    }
    public void addValue(String key,Jsons value){
        this.value.accumulate(key,value.asJSON());
    }
    public void addValues(String key,List<? extends Object> values){
        if(!values.isEmpty()){
            for(Object v : values){
                value.append(key,v);
            }
        }
    }
    public JsonArray getValues(String key){
        return new JsonArray(value.getJSONArray(key));
    }
    public Set<String> valueKeys(){return value.keySet();}
    public int hashCode(){
        return toString().hashCode();
    }
    public boolean equals(Object o){
        return toString().equals(o.toString());
    }

    public void increment(){this.count+=1;}
    public int getCount(){return count;}
    public int getId(){return id;}
    public int childCount(){return children.size();}
    public Frame addChild(int frameId){
        Frame f = null;
        if(children.isEmpty() || children.get(children.size()-1).getId()!=frameId){
            f = new Frame(frameId);
            children.add(f);
        }else {
            f = children.get(children.size()-1);
        }
        f.increment();
        return f;
    }
    public List<Frame> getChildren(){
        return Collections.unmodifiableList(children);
    }

    public String toString(){
        StringBuilder rtrn = new StringBuilder();
        appendString(rtrn);
        return rtrn.toString();
    }
    public void appendString(StringBuilder sb){
        sb.append(this.id);
        if(childCount()>0){
            sb.append(" [ ");
            for(Frame child : children){
                child.appendString(sb);
                sb.append(" ");
            }
            sb.append("]");
        }
    }


    public void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeInt(id);
        stream.writeInt(count);
        stream.writeUTF(value.toString(0));
        stream.writeObject(children);
    }
    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        this.id = stream.readInt();
        this.count = stream.readInt();
        String value = stream.readUTF();
        this.value = new JSONObject(value);
        this.children = (ArrayList<Frame>) stream.readObject();
    }
    private void readObjectNoData()
            throws ObjectStreamException {

    }

}
