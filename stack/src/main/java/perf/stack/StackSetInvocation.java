package perf.stack;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;


/**
 * Created by wreicher
 */
public class StackSetInvocation {

    private int uid;
    private List<StackInvocation> invocations;

    public StackSetInvocation(int uid,List<StackInvocation> invocations){
        this.uid = uid;
        this.invocations = invocations;
    }

    public int getStackSetUid(){return uid;}
    public int getInvocationCount(){return invocations.size();}
    public List<StackInvocation> getInvocations(){return Collections.unmodifiableList(invocations);}

    public void writeJson(PrintStream out, int indent){
        String suffix = indent > 0 ? System.lineSeparator() : "";
        out.print("{"+suffix);
        pad(out,indent); out.print("\"setUid\": "+this.getStackSetUid()+", "+suffix);
        pad(out,indent); out.print("\"stackInvocations\": [ ");
        for(Iterator<StackInvocation> iter = this.getInvocations().iterator(); iter.hasNext();){
            StackInvocation stackInvocation = iter.next();
            stackInvocation.writeJson(out,indent > 0 ? indent + 2 : 0);
            if(iter.hasNext()){
                out.print(",");
            }
        }
        out.print("]} ");
    }
    public static StackSetInvocation fromJSON(JSONObject object){
        int uid = object.getInt("setUid");
        JSONArray invocationsJson = object.getJSONArray("stackInvocations");
        List<StackInvocation> stackInvocations= new ArrayList<>();
        for(int i=0; i<invocationsJson.length(); i++){
            JSONObject invocationJson = invocationsJson.getJSONObject(i);
            stackInvocations.add(StackInvocation.fromJSON(invocationJson));
        }
        return new StackSetInvocation(uid,stackInvocations);
    }

    private void pad(PrintStream out, int indent){
        for(int i=0; i<indent; i++){
            out.print(" ");
        }
    }

}
