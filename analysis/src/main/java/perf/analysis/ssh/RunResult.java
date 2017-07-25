package perf.analysis.ssh;

import perf.parse.JsonConsumer;
import perf.util.json.Json;
import perf.util.json.Jsons;

import java.io.File;
import java.util.*;

/**
 * Created by wreicher
 */
public class RunResult {

    class RunState {
        private Map<String,String> runState;
        private Map<String,Map<String,String>> hostState;
        private Map<String,Map<String,String>> scriptState;
        public RunState(){
            runState = new HashMap<>();
            hostState = new HashMap<>();
            scriptState = new HashMap<>();
        }
        public boolean hasRun(String key){
            return runState.containsKey(key);
        }
        public String getRun(String key){
            return runState.get(key);
        }
        public void putRun(String key,String value){
            runState.put(key,value);
        }
        public boolean hasHost(String hostName,String key){
            return ensureMap(hostName,hostState).containsKey(key);
        }
        public void getHost(String hostName,String key){
            ensureMap(hostName,hostState).get(key);
        }
        public void putHost(String hostName,String key,String value){
            ensureMap(hostName,hostState).put(key,value);
        }
        public boolean hasScript(String scriptName,String key){
            return ensureMap(scriptName,scriptState).containsKey(key);
        }
        public void getScript(String scriptName,String key){
            ensureMap(scriptName,scriptState).get(key);
        }
        public void putScript(String scriptName,String key,String value){
            ensureMap(scriptName,scriptState).put(key,value);
        }
        public List<String> runKeys(){
            return Collections.unmodifiableList(Arrays.asList(runState.keySet().toArray(new String[0])));
        }
        public List<String> hostNames(){
            return Collections.unmodifiableList(Arrays.asList(hostState.keySet().toArray(new String[0])));
        }
        public List<String> scriptKeys(){
            return Collections.unmodifiableList(Arrays.asList(scriptState.keySet().toArray(new String[0])));
        }



        private Map<String,String> ensureMap(String key,Map<String,Map<String,String>> map){
            if(!map.containsKey(key)){
                map.put(key,new HashMap<>());
            }
            return map.get(key);
        }
    }

    class RunLogConsumer implements JsonConsumer{

        RunState startState;
        RunState endState;
        String firstTimestamp;
        String lastTimestamp;
        Map<String,String> signalTimes;

        public RunLogConsumer(){
            startState = new RunState();
            endState = new RunState();
            firstTimestamp = null;
            lastTimestamp = null;
            signalTimes = new LinkedHashMap<>();
        }

        @Override
        public void consume(Jsons object) {
            if(object.has("timestamp")){
                if(firstTimestamp == null){
                    firstTimestamp = object.getString("timestamp");
                }
                lastTimestamp = object.getString("timestamp");
            }
            if(object.has("message") ) {
                if (object.getString("message").contains("starting run state")) {
                    for (String key : object.keySet()) {
                        if (!key.equals("timestamp") && !key.equals("message")) {
                            startState.putRun(key, object.getString(key));
                        }
                    }
                }else if (object.getString("message").contains(" host state:")){
                    String hostName = object.getString("message").substring(0,object.getString("message").indexOf(" host state:"));
                    for (String key : object.keySet()) {
                        if (!key.equals("timestamp") && !key.equals("message")) {
                            startState.putHost(hostName,key, object.getString(key));
                        }
                    }
                }
            }
        }
    }

    File baseFile;
    Map<String,String> runState;
    Map<String,Map<String,String>> hostState;
    Map<String,Map<String,String>> scriptState;

    public RunResult(File baseFile){
        this.baseFile = baseFile;
        File runLog = new File(baseFile,"run.log");
        if(runLog.exists()){

        }
    }

}
