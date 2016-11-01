package perf.stack.builder;

import perf.stack.Stack;
import perf.stack.StackSet;
import perf.util.Indexer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by wreicher
 */
public class StackSetBuilder {

    private Indexer<String> uidGenerator;

    public StackSetBuilder(){
        uidGenerator = new Indexer<>();
    }

//    public StackSet getStackSet(List<Integer> stacks){
//        int uid = uidGenerator.add(stacks.toString());
//        StackSet rtrn = new StackSet(uid,stacks);
//        return rtrn;
//    }
    public StackSet getStackSet(List<Stack> stacks){
        List<Integer> stackUids = stacks.stream().map(Stack::getUid).collect(Collectors.toList());
        int uid = uidGenerator.add(stackUids.toString());
        StackSet rtrn = new StackSet(uid,stackUids);
        return rtrn;
    }
}


