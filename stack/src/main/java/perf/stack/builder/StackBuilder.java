package perf.stack.builder;

import perf.stack.Stack;
import perf.util.Indexer;

import java.util.List;

/**
 * Created by wreicher
 */
public class StackBuilder {

    private Indexer<String> uidGenerator;

    public StackBuilder(){
        uidGenerator = new Indexer<>();
    }

    public Stack getStack(List<Integer> frames,List<Integer> lineNumbers){
        int uid = uidGenerator.add(frames.toString()+" : "+lineNumbers.toString());
        Stack rtrn = new Stack(uid,frames,lineNumbers);
        return rtrn;
    }
}
