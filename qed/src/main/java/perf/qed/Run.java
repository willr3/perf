package perf.qed;


import org.jboss.logging.Logger;
import perf.qed.internal.Coordinator;
import perf.qed.internal.Host;
import perf.util.HashedSets;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.LogManager;

/**
 *
 */
public class Run {

    public static final Logger LOGGER = Logger.getLogger("Qed");

    public enum RunStatus {Queued,Running,Done}
    public enum RunResult {Pending,Aborted,Complete,Error,Fail,Pass}
    private String name;
    private long startTime;
    private long stopTime;

    private Map<String,Script> scripts;
    private Map<String,String> env;
    private Map<String,Set<Host>> hosts;

    private HashedSets<String,String> hostArtifacts;
    private HashedSets<String,String> hostScripts;
    private HashedSets<String,String> scriptHosts;
    private Coordinator coordinator;

    public Run(){
        this("Run-"+System.currentTimeMillis());
    }
    public Run(String name){
        this.name = name;
        scripts = new HashMap<>();
        env= new HashMap<>();
        hosts = new HashMap<>();
        this.coordinator = new Coordinator();

        hostArtifacts = new HashedSets();
    }

    public String getName(){return name;}

    public Coordinator getCoordinator(){return coordinator;}

    public void setStartTime(long time){startTime = time;}
    public long getStartTime(){return startTime;}
    public void setStopTime(long time){stopTime = time;}
    public long getStopTime(){return stopTime;}

    public void addScript(String name,Script script){
        scripts.put(name,script);
    }
    public void addHosts(String scriptName,Host...host){
        if(!hosts.containsKey(scriptName)){
            hosts.put(scriptName,new HashSet<Host>());
        }
        for(int i=0; i<host.length; i++){
            hosts.get(scriptName).add(host[i]);
        }
    }
    public void mapScriptToHost(String scriptName,String hostName){
        hostScripts.put(hostName,scriptName);
        scriptHosts.put(scriptName,hostName);
    }

    public void addArtifact(Host host,String scriptName,String fileName){
        hostArtifacts.put(host.getHostName(),fileName);
    }
    public void setEnv(String name,String value){
        env.put(name,value);
    }
    public String getEnv(String name){
        return env.get(name);
    }

    public void run(){
        int count = 0;
        for(String name : hosts.keySet()){
            count+=hosts.get(name).size();
        }
    }

    public void abort(String message){

    }
    public void log(Host h,String scriptName,String message){
        LOGGER.infof("%-24s %-12s %s",h.getHostName(),scriptName,message);
    }
    public void log(Host h,String scriptName,String message,Exception e){

    }

    public static void main(String[] args) {
        System.out.println(LogManager.getLogManager().getClass().getName());
        Properties p = System.getProperties();

        Run r = new Run();
        r.log(new Host("localhost"),"bar","message");
    }
}
