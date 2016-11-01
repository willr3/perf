package perf.diff;

/**
 * Created by wreicher
 */
public class Diff {


    public static enum Operation {ADD, MODIFY, DELETE, EQUAL}

    private final Operation op;
    private final String location;
    private final String left;
    private final String right;

    public Diff(Operation operation,String location, String left, String right){
        this.op = operation;
        this.location = location;
        this.left = left;
        this.right = right;
    }
    public Operation getOperation(){return op;}
    public String getLocation(){return location;}
    public String getLeft(){return left;}
    public String getRight(){return right;}


    public String toString(){
        return "@@ "+location+"\n"+op+"\n"+"left: "+left+"\n"+"right: "+right;
    }
}
