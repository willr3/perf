package perf.wildfly.client;

import org.jboss.dmr.ModelNode;

import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.jboss.dmr.ModelType.*;

/**
 * Created by wreicher
 */
public class Tree {


    public static final URLDecoder decoder = new URLDecoder();

    public static final String WILDCARD = "*";
    public static final String DELIM = "/";
    public static final String ROOT = "__ROOT__";


    private boolean isPath;
    private Consumer<ModelNode> todo;
    private String value;
    private Map<String, Tree> children;

    public static Tree newRoot(){
        return new Tree();
    }

    private Tree(){
        this(ROOT,false);
    }
    private Tree(String value,boolean isPath){
        this.value = value;
        this.isPath = isPath;
        this.children = new ConcurrentHashMap<>();
    }
    public Tree(String value){
        this(value,false);
    }
    public void run(Consumer<ModelNode> action){
        this.todo = action;
    }
    public Tree as(String name){
        return add(name,true);
    }
    public Tree add(Tree child){
        if(!hasChild(child.value)){
            children.put(child.value,child);
        }
        return child;
    }
    public Tree add(String...childNames){
        Tree parent = this;
        for(int i=0; i<childNames.length; i++){
            parent = parent.add(childNames[i],false);
        }
        return parent;
    }
    private Tree add(String childName,boolean isPath){
        if(!hasChild(childName)){
            children.put(childName,new Tree(childName,isPath));
        }
        return children.get(childName);
    }
    public List<String> getChildrenNames(){
        return Arrays.asList((String[])children.keySet().toArray());
    }
    public boolean hasChild(String childName){
        return children.containsKey(childName);
    }
    public boolean hasChildren(){
        return !children.isEmpty();
    }
    public boolean isRoot(){
        return this.value.equals(ROOT);
    }
    public boolean isWildcard(){
        return this.value.startsWith(WILDCARD);
    }
    public boolean hasAction(){
        return this.todo!=null;
    }
    private void walkChildren(ModelNode node,String path,Map<String,Double> matches){
        for(Tree child : children.values()){
            child.walk(node,path,matches);
        }
    }
    public void walk(ModelNode node,String path,Map<String,Double> matches){
        if(hasChildren()){
            if(isRoot()){
                //root just pass to all the children
                if(hasAction()){
                    this.todo.accept(node);
                }
                walkChildren(node,path,matches);

            }
            else if(isWildcard()) {
                node.asPropertyList().forEach(p->{
                    walkChildren(p.getValue(),(path==null || path.isEmpty()) ? p.getName() : path+DELIM+p.getName(),matches);
                });
            }
            else {
                ModelNode n = null;

                if(node.getType().equals(OBJECT) && (n=node.get(this.value)).isDefined()){
                    if(isPath){
                        if(hasAction()){
                            this.todo.accept(node);
                        }
                        walkChildren(node, (path==null || path.isEmpty()) ? n.asString() : path+DELIM+n.asString() ,matches);
                    }else{
                        if(hasAction()){
                            this.todo.accept(n);
                        }
                        walkChildren(n,path,matches);
                    }

                }
            }
        }
        else {
            ModelNode n = null;
            //TODO if this.value = WILDCARD and we want to capture all values
            if(node.getType().equals(OBJECT) && (n=node.get(this.value)).isDefined()){
                if(hasAction()){
                    this.todo.accept(n);
                }
                switch(n.getType()){
                    case BIG_DECIMAL:
                    case DOUBLE:
                    case INT:
                    case LONG:
                    case BIG_INTEGER:
                        matches.put( URLDecoder.decode( (path==null || path.isEmpty()) ? this.value : path+DELIM+this.value ) , n.asDouble()  );
                        break;
                    default:
                        System.out.println("no children of "+this.value+" but have a match");
                }

            }
        }
    }
}
