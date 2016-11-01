package perf.stack;

import org.json.JSONObject;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wreicher
 */
public class Stack {


    private List<Integer> frames;
    private List<Integer> lineNumbers;
    private int uid;

    public Stack(int uid,List<Integer> stacks,List<Integer> lineNumbers){
        this.uid = uid;
        this.frames = stacks;
        this.lineNumbers = lineNumbers;
        if(stacks.size()!=lineNumbers.size()){
            throw new IllegalArgumentException("stack count and lineNumber count must match. frames:"+stacks.size()+" lineNumbers:"+lineNumbers.size());
        }
    }
    public int size(){return frames.size();}
    public int getFrame(int index){return frames.get(index);}
    public int getLineNumber(int index){return lineNumbers.get(index);}
    public List<Integer> getFrames(){return Collections.unmodifiableList(frames);}
    public List<Integer> getLineNumbers(){return Collections.unmodifiableList(lineNumbers);}
    public String getUniqueString(){
        StringBuilder rtrn = new StringBuilder();
        for(int i = 0; i< frames.size(); i++){
            if(i > 0){
                rtrn.append(", ");
            }
            rtrn.append(frames.get(i));
            rtrn.append(":");
            rtrn.append(lineNumbers.get(i));

        }
        return rtrn.toString();
    }
    public int getUid(){return uid;}
    public int hashCode(){return uid;}
    public boolean equals(Object o){
        if( o instanceof Stack){
            Stack that = (Stack)o;
            return this.getUniqueString().equals(that.getUniqueString());
        }
        return false;
    }

    public void writeJson(PrintStream out, int indent){
        String suffix = indent > 0 ? System.lineSeparator() : "";
        out.print("{"+suffix);
        pad(out,indent); out.print("\"uid\": "+this.getUid()+", "+suffix);
        pad(out,indent); out.print("\"frames\": "+this.getFrames().toString()+", "+suffix);
        pad(out,indent); out.print("\"lineNumbers\": "+this.getLineNumbers().toString()+"} ");
    }

    public static Stack fromJSON(JSONObject object){
        int uid = object.getInt("uid");
        List<Integer> frames = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();
        for(int i=0; i<object.getJSONArray("frames").length(); i++){
            frames.add(object.getJSONArray("frames").getInt(i));
            lineNumbers.add(object.getJSONArray("lineNumbers").getInt(i));
        }
        return new Stack(uid,frames,lineNumbers);
    }


    private void pad(PrintStream out,int indent){
        for(int i=0; i<indent; i++){
            out.print(" ");
        }
    }
}
