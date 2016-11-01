package perf.qed.script;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class Command {

    private String name;
    private List<String> arguments;

    public Command(String name){
        this(name,new String[]{});
    }
    public Command(String name,String...arguments){
        this.name = name;
        this.arguments = new ArrayList<>();
        for(String a : arguments){
            this.arguments.add(a);
        }
    }

    public String getName(){
        return name;
    }
    public int argumentCount(){
        return arguments.size();
    }
    public String argument(int index){
        if(index>=arguments.size() || index < 0)
            return "";
        return arguments.get(index);
    }
    public void forEachArgument(Consumer<String> consumer){

    }
}
