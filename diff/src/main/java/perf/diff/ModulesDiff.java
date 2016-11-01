package perf.diff;

import perf.util.file.FileUtility;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wreicher
 */
public class ModulesDiff {

    String aPath;
    String bPath;

    public ModulesDiff(){

    }
    public ModulesDiff(String aPath,String bPath){
        this.aPath = aPath;
        this.bPath = bPath;
    }

    public void setA(String path){
        aPath = path;
    }
    public void setB(String path){
        bPath = path;
    }
    public Map<String,Diff> getDiffs(){
        HashMap<String,Diff> rtrn = new HashMap<>();

        List<String> aModules = FileUtility.getFiles(aPath,"module.xml",true);
        List<String> bModules = FileUtility.getFiles(bPath,"module.xml",true);

        System.out.println(aModules);

        while(!aModules.isEmpty()){
            String next = aModules.remove(0);

            File f = new File(next);
            if(f.exists()){
                
            }else{
                //TODO log error for non-existing file
            }
        }

        return rtrn;
    }

}
