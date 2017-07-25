package perf.ast.test;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by wreicher
 */
public class Example implements Comparable<Example> {


    public static final int ONE = 1;
    public static final int TWO = 2;

    private final static Map<Integer,String> staticFinalMap = new HashMap<>();
    private static List<Comparator<String>> staticItialized;

    static {
        staticItialized = new ArrayList<>();
        staticFinalMap.put(1,"One");
    }

    private int privateInt = 2, secondInt;
    protected String name;
    protected int[] singleDimInt;
    private int[][] multiDimInt;
    private Example parent;
    private SubClass subClass;

    public Example(){
        this("name");
    }
    public Example(String name){
        this.name = name.toLowerCase();
        this.parent = null;
    }

    @Open(name = "openName")
    public void noop(){}
    public void varArgsThrows(int count,String...args) throws IOException{
        if(args == null){
            throw new IOException();
        }
    }

    public void tryCatchFinally(){
        try{
            File f = new File("");
            String source = FileUtils.readFileToString(f);
            f.exists();
        }catch(IOException e){
            this.loops();
        }finally{
            this.logic(1);
        }
    }

    public void setParent(Example parent){
        if(this.parent!=null){
            return;
        }
        this.parent = parent;
    }

    public void untypted(List<?> untyped){

    }
    public void extenders(List<? extends Example> exteners){

    }

    public <V extends Serializable> V implementers(List<V> serials){
        return null;
    }

    public void lowerBounds(List<? super Example> descendent){

    }

    public boolean methodCall(List<Example> exampleList){
        loops();
        this.loops();
        return exampleList!=null && exampleList.isEmpty();
    }

    @Override
    public String toString(){
        String rtrn =  super.toString();
        return rtrn.toUpperCase();
    }

    public void loops(){
        for(int i=0; i<10; i++){}
        int i=0;
        do{
            i++;
            staticFinalMap.put(i,""+i);
        }while(i<10);
        while(i>0 && staticFinalMap!=null){
            i--;
            if (i == 20) {
                continue;
            }
        }
    }
    public void logic(int arg){
        switch (arg){
            case ONE:
                System.out.println("ONE");
                break;
            case TWO:
                System.out.println("TWO");
                break;
        }
        if (arg == 0) {
            System.out.println("ZERO");
        } else if (arg == 1 && ONE == 1){
            System.out.println("ONE");
        } else {
            loops();
        }
    }

    @Override
    public int compareTo(Example example) {
        return 0;
    }


    public static class SubClass extends ArrayList<Example>{}
    public enum Opt {On,Off}
    public @interface Open{
        String name();
    }
}
