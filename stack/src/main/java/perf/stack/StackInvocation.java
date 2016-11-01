package perf.stack;

import org.json.JSONObject;

import java.io.PrintStream;

/**
 * Created by wreicher
 */
public class StackInvocation {

    private int stackUid;

    private JSONObject data;

    public StackInvocation(int stackId,JSONObject data){
        this.stackUid = stackId;
        this.data = data;
    }

    public int getStackUid(){return stackUid;}
    public JSONObject getData(){return  data;}

    public void writeJson(PrintStream out, int indent){
        String suffix = indent > 0 ? System.lineSeparator() : "";
        out.print("{"+suffix);
        pad(out,indent); out.print("\"stackUid\": "+this.getStackUid()+", "+suffix);
        pad(out,indent); out.print("\"data\": "+this.getData().toString()+"} ");

    }

    public static StackInvocation fromJSON(JSONObject object){
        int stackUid = object.getInt("stackUid");
        return new StackInvocation(stackUid,object.getJSONObject("data"));
    }

    private void pad(PrintStream out,int indent){
        for(int i=0; i<indent; i++){
            out.print(" ");
        }
    }
}
