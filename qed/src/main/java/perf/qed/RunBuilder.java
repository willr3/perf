package perf.qed;

import perf.qed.internal.Host;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wreicher
 */
public class RunBuilder {


    private File workingDirectory;
    private Run run;

    private Map<String,Host> hosts;
    private Map<String,Script> scripts;
    private Map<String,Properties> properties;

    public RunBuilder(){
        hosts = new HashMap<>();
        scripts = new HashMap<>();
        properties = new HashMap<>();

        run = null;
    }

    public void init(String name){
        if(run != null){
            System.out.println("WARNING discarding current run plan");
        }
        run = new Run(name);
    }
    public void setLocalDirectory(String workingDirectory){
        this.workingDirectory = new File(workingDirectory);
        if(!this.workingDirectory.exists()){
            System.out.println("WARNING working directory does not exist");
        }
    }
    public void loadPropertiesFile(String groupName,String filePath){
        if(!properties.containsKey(groupName)){
            properties.put(groupName,new Properties());
        }
        try {
            File f = new File(filePath);
            if(!f.exists() && workingDirectory!=null){
                //check if the link is relative
                f = new File(workingDirectory.getPath()+File.separator+filePath);
            }
            properties.get(groupName).load(new FileInputStream(f));
        } catch (FileNotFoundException e){
            System.out.println("Failed to load "+filePath+" could not find in workingDirecotry or on file system");
        } catch (IOException e) {
            System.out.println("Failed to load "+filePath+"");
        }
    }
    public void loadXmlFile(String groupName,String filePath){

    }

    public String getProperty(String groupName,String propertyName){
        return getProperty(groupName,propertyName,"");
    }
    public String getProperty(String groupName,String propertyName,String defaultValue){
        if(properties.containsKey(groupName)){
            return properties.get(groupName).getProperty(propertyName,defaultValue);
        }
        return defaultValue;
    }
    public String getXmlValue(String groupName,String xPath){
        return "";
    }

    public void addScript(String name,Script script){
        scripts.put(name,script);
        //TODO actually return an actor
        //return new ActorImpl(null,null,null,null,null,null);
    }

    public String getRunEnv(String name){
        return run.getEnv(name);
    }
    public void setRunEnv(String name,String value){
        run.setEnv(name,value);
    }

    public void addHost(String name){
        addHost(name,22);
    }
    public void addHost(String name,int port){
        hosts.put(name,new Host(name,22));
    }
    public void addHost(String name,String userName){
        addHost(name,userName,22);
    }
    public void addHost(String name,String userName,int port){
        Host newHost = new Host(name,userName,port);
        hosts.put(name,newHost);
    }
    public Host getHost(String name){
        return hosts.get(name);
    }
    public void mapScriptToHost(String scriptName,String hostName){
        run.mapScriptToHost(scriptName,hostName);
    }
    public void initWaitPoint(String name,int count){
        run.getCoordinator().initialize(name,count);
    }

    public void submitRun(){

    }
}
