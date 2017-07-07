package perf.diff;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class PropDiff {


    private Properties left;
    private Properties right;

    public PropDiff(){
        left = new Properties();
        right = new Properties();
    }

    public void setLeft(String key, String value){
        left.setProperty(key,value);

    }
    public void setRight(String key, String value){
        right.setProperty(key,value);
    }
    public void loadLeft(Path path){
        load(left,path);
    }
    public void loadRight(Path path){
        load(right,path);
    }
    private void load(Properties p,Path path){
        File f = path.toFile();
        try {
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            p.load(bis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Map<String,Diff> getDiff(){
        Map<String,Diff> rtrn = new LinkedHashMap<String, Diff>();
        Set<String> as = left.stringPropertyNames();
        Set<String> bs = right.stringPropertyNames();

        Set<String> names = new HashSet<String>();
        names.addAll(as);
        names.addAll(bs);


        for(String name : as){
            if(left.containsKey(name)){
                if(right.containsKey(name)){
                    if( !compare(left.getProperty(name), right.getProperty(name)) ){
                        // modification
                        rtrn.put(name, new Diff(Diff.Operation.MODIFY, name,left.getProperty(name), right.getProperty(name)));
                    }else{
                        // do nothing, they are the same
                        //TODO option to add a Diff for no change
                    }
                }else {
                    //delete
                    rtrn.put(name, new Diff(Diff.Operation.DELETE, name,left.getProperty(name), ""));
                }
            }else {
                // add
                rtrn.put(name, new Diff(Diff.Operation.ADD, name, "", right.getProperty(name)));
            }
        }
        return Collections.unmodifiableMap(rtrn);
    }

    public void dump(){
        System.out.println("left : "+ left.size());
        for(String name : left.stringPropertyNames()){
            System.out.println(name+" = "+ left.get(name));
        }
        System.out.println("right : "+ left.size());
        for(String name : right.stringPropertyNames()){
            System.out.println(name + " = " + right.get(name));
        }
    }
    public boolean compare(String a, String b){
        return a.equals(b);
    }


    public static void main(String[] args) {
        Path aPath = new File("/home/wreicher/code/spec/specjms2007/config/sample-vendor.properties").toPath();

        Path bPath = new File("/home/wreicher/code/redhat/userContent/eap7/specjms2007/config/sample-vendor.properties").toPath();

        final PropDiff diff = new PropDiff();
        diff.loadLeft(aPath);
        diff.loadRight(bPath);
        //diff.dump();
        Map<String,Diff> difffs = diff.getDiff();
        for(String key : difffs.keySet()){
            System.out.println(key);
            Diff d = difffs.get(key);
            System.out.println("  "+d.getLeft());
            System.out.println("  "+d.getRight());
        }

//        try {
//            Files.walkFileTree(aPath,new SimpleFileVisitor<Path>(){
//                @Override
//                public FileVisitResult visitFile(Path file,BasicFileAttributes attrs) throws IOException {
//                    if( (file.toString().endsWith("properties") || file.toString().endsWith("env") ) && !(file.toString().contains("freeform") || file.toString().contains("vertical") || file.toString().contains("agent") ) ){
//
//                        System.out.println("++ = "+file.toString());
//                        diff.loadFrom(file);
//                    }else{
//                        System.out.println("-- = "+file.toString());
//                    }
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//            System.out.println("\n\n");
//            Files.walkFileTree(bPath, new SimpleFileVisitor<Path>() {
//                @Override
//                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                    if( (file.toString().endsWith("properties") || file.toString().endsWith("env") ) && !(file.toString().contains("freeform") || file.toString().contains("vertical") || file.toString().contains("agent") ) ){
//                        System.out.println("++ = " + file.toString());
//                        diff.loadTo(file);
//                    } else {
//                        System.out.println("-- = " + file.toString());
//                    }
//                    return FileVisitResult.CONTINUE;
//                }
//            });
//
////            diff.dump();
//            Map<String,Diff> diffs = diff.getDiff();
//            System.out.println("diffs : "+diffs.size());
//            for(String d : diffs.keySet()){
//                System.out.println(d+" ");
//                System.out.println(" left ["+diffs.get(d).getLeft()+"]");
//                System.out.println(" right ["+diffs.get(d).getRight()+"]");
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
