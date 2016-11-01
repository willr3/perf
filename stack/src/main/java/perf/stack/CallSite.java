package perf.stack;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.util.HashedList;
import perf.util.Indexer;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class CallSite {

    private int id;
    private long start;
    private long stop;
    private CallSite parent;
    private int lineNumbers[];
    private List<CallSite> children;
    private HashedList<Integer> threadIds;
    private JSONArray data;

    public CallSite(int id,long start,long stop,CallSite parent){
        this.id = id;
        this.start = start;
        this.stop = stop;
        this.parent = parent;
        this.lineNumbers = new int[]{};
        this.children = new ArrayList<>();
        this.data = new JSONArray();
        this.threadIds = new HashedList<>();

        data.put(new JSONObject());

    }
    public static CallSite fromJsonHelper(JSONObject json){
        CallSite root = new CallSite(-1,0,0,null);
        long timestamp = json.getLong("timestamp");
        root.start = json.getLong("timestamp");
        root.stop = json.getLong("timestamp");

        CallSite current = root;
        List<Integer> parsedLineNumbers = new ArrayList<>();
        JSONArray stack = json.getJSONArray("stack");
        Integer threadName = null;
        if(json.has("threadName")){
            threadName = Integer.parseInt(json.getString("threadName"));
            if ( threadName!=null ) {
                current.threadIds.add(threadName);
            }
        }
        for(int i=0; i<stack.length(); i++){
            JSONObject frame = stack.getJSONObject(i);
            CallSite toAdd = new CallSite(frame.getInt("frame"),timestamp,timestamp,current);

            parsedLineNumbers.add(frame.getInt("line"));
            current.addLast(toAdd);
            current = toAdd;
            if ( threadName!=null ) {
                current.threadIds.add(threadName);
            }
        }
        for(String key : json.keySet()){
            switch(key){
                case "stack":
                case "timestamp":
                case "id":
                case "className":
                    break;
                case "threadName":
                    //current.threadIds.add(Integer.parseInt(json.getString(key)));
                    current.data().put("threadName",Integer.parseInt(json.getString(key)));
                    break;
                case "hashCode":
                    current.data().put("this.hashCode",json.getString(key));
                    break;
                case "callerHashCode":
                    current.data().put("caller.hashCode",json.getString(key));
                    break;
                default:
                    current.data().put(key,json.get(key));
            }
            current.lineNumbers = new int[parsedLineNumbers.size()];
            for(int i=0; i<parsedLineNumbers.size();i++){
                current.lineNumbers[i]=parsedLineNumbers.get(i);
            }
        }
        return root;
    }



    public boolean hasLineNumbers(){return lineNumbers.length>0;}
    public int[] getLineNumbers(){return lineNumbers;}
    public JSONArray allData(){return data;}
    public List<Integer> getThreads(){return threadIds.toList();}
    public long getStart(){return start;}
    public long getStop(){return stop;}
    public int getId(){return id;}
    public List<CallSite> getChildren(){return Collections.unmodifiableList(children);}
    public static int compare(CallSite a,CallSite b){
        if(a==null){
            return 1;
        }
        if (a.start <= b.stop){
            if(a.stop >= b.start){
                if(a.stop == b.start && b.start==b.stop){//catch case where b is right after
                    return -1;
                }
                return 0;
            }
            return -1;
        }else {
            return 1;
        }
    }
    public String toDebug(){
        StringBuilder rtrn = new StringBuilder();
        toDebug(rtrn,2);
        return rtrn.toString();
    }
    public void pad(StringBuilder rtrn,int pad){
        for(int i=0; i<pad; i++){
            rtrn.append(" ");
        }
    }
    public void toDebug(StringBuilder rtrn,int pad){
        rtrn.append(this.getId());
        rtrn.append("(");
        rtrn.append(this.threadIds.getLast());
        rtrn.append(")");
        if(this.data.length()>1 || this.data.getJSONObject(0).length()>0){
            rtrn.append(".");
            rtrn.append(this.data.length());
        }
        if(this.children.isEmpty()){
            rtrn.append("{}");
        }else if (this.children.size()==1){
            rtrn.append("[");
            this.children.get(0).toDebug(rtrn,pad);
            rtrn.append("]");
        }else{
            rtrn.append("[\n");
            for(int i=0; i<this.children.size(); i++){
                if(i>0){
                    rtrn.append("\n");
                }
                pad(rtrn,pad);
                this.children.get(i).toDebug(rtrn,pad+2);

            }
            rtrn.append("]");
        }
    }
    public void mergeData(CallSite peer){
        if(peer.children.size()!=this.children.size()){
            throw new RuntimeException("mergeData called on CallSites with differing children count");
        }
        for(int i=0; i<peer.data.length(); i++){
            if(peer.data.getJSONObject(i).length()>0){
                this.data.put(peer.data.getJSONObject(i));
            }
        }
        for(int i=0; i<this.children.size();i++){
            this.children.get(i).mergeData(peer.children.get(i));
        }
    }
    public void mergeStack(CallSite singleStack){
        if(singleStack.getId()!=this.getId()){
            //error
            throw new RuntimeException("cannot add id="+singleStack.getId()+" to "+this.getId());
        }
        CallSite target = this;
        CallSite toAdd = singleStack;
        while(toAdd!=null){
            if(toAdd.children.size()>1){
                throw new RuntimeException("should not call mergeStack on a CallSite with more than 1 child");
            }
            if(target!=null && target.getId()==toAdd.getId()){//merge them and continue down the
                //merge timespan
                target.expandTimes(toAdd.getStart(),toAdd.getStop());
                if ( target.threadIds.getLast()!=toAdd.threadIds.getLast() ){
                    target.threadIds.add(toAdd.threadIds.getLast());
                }
                //merge data
                if(toAdd.data().length()>0){
                    if(target.data().length()==0){
                        target.data.remove(0);
                    }
                    target.data.put(toAdd.data());//shouldn't have more than 1 given the use case
                }
                //line numbers
                if(toAdd.lineNumbers.length>0 && target.lineNumbers.length==0){
                    target.lineNumbers=toAdd.lineNumbers;
                }
                if(toAdd.children.size()>0){//if we have to merge the next CallSite
                    CallSite lastTargetChild = target.children.size()>0 ? target.children.get(target.children.size()-1) : null;
                    toAdd = toAdd.children.size()>0 ? toAdd.children.get(0): null;

                    if(toAdd!=null && lastTargetChild!=null && toAdd.getId()==lastTargetChild.getId()){
                        target = lastTargetChild;
                    }else{
                        if(toAdd!=null){
                            target.addLast(toAdd);
                            toAdd = null;
                        }
                    }
                }else{
                    toAdd = null;
                }
            }else {
                //just add it to the end and be done
                toAdd = null;
            }
        }
    }
    private void addLast(CallSite toAdd){
        this.children.add(toAdd);
        toAdd.parent = this;
    }
    public static CallSite merge(CallSite a,CallSite b){
        if(a.id!=b.id){
            throw new RuntimeException("cannot merge "+a.id+" with "+b.id);
        }
        CallSite rtrn = a;

        //a.expandTimes(b.getStart(),b.getStop());
        ListIterator<CallSite> aIter = a.children.listIterator();
        ListIterator<CallSite> bIter = b.children.listIterator();

        if(b.data().length()>0){
            if(a.data().length()==0){
                a.data.remove(0);
            }
            for(int i=0; i<b.data.length();i++){
                a.data.put(b.data.get(i));
            }
        }
        CallSite aCur = (aIter.hasNext()?aIter.next():null);//advance to the next a (if it exists)
        //for each child in b
        while(bIter.hasNext()){
            CallSite bCur = bIter.next();
            int comp = CallSite.compare(aCur,bCur);
            if(aCur==null){
                aIter.add(bCur);
                bCur.parent=a;
            }else if(comp==0){//overlap
                if(aCur.getId()==bCur.getId()){
                    merge(aCur,bCur);
                }else{//overlap but not the same id :(
                    System.out.println("aCur."+aCur.getId()+" "+aCur.start+" - "+aCur.stop);
                    CallSite aTop = aCur;
                    while(aTop.parent!=null){aTop=aTop.parent;}
                    System.out.println(aTop.toString());
                    System.out.println("bCur."+bCur.getId()+" "+bCur.start+" - "+bCur.stop);
                    CallSite bTop = bCur;
                    while(bTop.parent!=null){bTop=bTop.parent;}
                    System.out.println(bTop.toString());
                    //System.out.println(bCur.toJson().toString(1));
                    throw new RuntimeException("cannot split ");
                }
            }else if ( comp < 0 ){//a is before b
                if(aCur.getId()==bCur.getId()){//merge them because nothing was fond between the two
                    merge(aCur,bCur);
                }else{
                    bIter.previous();//rewind so we can compare the same element from b
                    aCur = (aIter.hasNext()?aIter.next():null);//advance to the next a (if it exists)
                    //let the next iteration of while loop compare bCur to the next A
                }
            } else {//a is after b
                if(aCur.getId()==bCur.getId()){
                    merge(aCur,bCur);
                }else {
                    aIter.previous();//rewind to put b before a
                    aIter.add(bCur);//add b before a
                    bCur.parent=a;
                    aIter.next();//move aIter back to looking at a
                }
            }
        }
        return rtrn;
    }
    public void expandTimes(long start,long stop){
        boolean expanded = false;
        if(this.start > start){
            this.start = start;
            expanded = true;
        }
        if(this.stop < stop){
            this.stop = stop;
            expanded = true;
        }
        if(expanded && this.parent!=null){
            this.parent.expandTimes(start,stop);
        }
    }
    public JSONObject data(){
        return this.data.getJSONObject(0);
    }
    public JSONObject toJson(){
        JSONObject rtrn = new JSONObject();
        rtrn.put("id",id);
        if(lineNumbers.length>0){
            rtrn.put("lines",new JSONArray(lineNumbers));
        }
        if(threadIds.size()>0){
            rtrn.put("threads",new JSONArray(threadIds.toList().toArray()));
        }
        if(data.length()>1 || data().length()>0){
            rtrn.put("data",data);
        }
        for(CallSite child : children){
            rtrn.append("children",child.toJson());
        }
        return rtrn;
    }
    public void walk(Consumer<CallSite> walker){
        walker.accept(this);
        for(CallSite child : children){
            child.walk(walker);
        }

    }
    public int hashCode(){
        StringBuilder sb = new StringBuilder();
        appendIDs(sb);
        return sb.toString().hashCode();
    }
    public boolean equals(Object o){
        if(o instanceof CallSite){
            CallSite that = (CallSite)o;
            return this.hashCode()==that.hashCode();
        }
        return false;
    }
    public String toString(){
        StringBuilder sb = new StringBuilder();
        appendIDs(sb);
        return sb.toString();
    }
    public void appendIDs(StringBuilder sb){
        sb.append(this.id);
        sb.append("[");
        for(CallSite child : children){
            child.appendIDs(sb);
        }
        sb.append("]");
    }
}

