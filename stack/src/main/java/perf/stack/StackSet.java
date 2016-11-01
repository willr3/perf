package perf.stack;

import org.json.JSONObject;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wreicher
 */
public class StackSet {

    public int uid;
    public List<Integer> stacks;

    public StackSet(int uid,List<Integer> stacks){
        this.uid = uid;
        this.stacks = stacks;
    }

    public int getUid(){return uid;}
    public List<Integer> getStackUids(){return Collections.unmodifiableList(stacks);}
    public int getStackCount(){return stacks.size();}

    public String getUniqueString(){return stacks.toString();}

    public int hashCode(){return uid;}
    public boolean equals(Object o){
        if(o instanceof StackSet){
            StackSet that = (StackSet)o;
            return this.getUid() == that.getUid();
        }
        return false;
    }

    public void writeJson(PrintStream out, int indent){
        String suffix = indent > 0 ? System.lineSeparator() : "";
        out.print("{"+suffix);
        pad(out,indent); out.print("\"uid\": "+this.getUid()+", "+suffix);
        pad(out,indent); out.print("\"stacks\": "+this.getStackUids().toString()+"}  ");
    }

    public static StackSet fromJSON(JSONObject object){
        int uid = object.getInt("uid");
        List<Integer> stacks = new ArrayList<>();

        for(int i=0; i<object.getJSONArray("stacks").length(); i++){
            stacks.add(object.getJSONArray("stacks").getInt(i));
        }
        return new StackSet(uid,stacks);
    }

    private void pad(PrintStream out, int indent){
        for(int i=0; i<indent; i++){
            out.print(" ");
        }
    }
}
