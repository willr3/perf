package perf.jfr;

import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.FlightRecordingLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by wreicher
 */
public class JfrCollection {


    public static final JfrCollection COLLECTION = new JfrCollection();

    private HashMap<String,FlightRecording> jfrs;

    private JfrCollection(){
        jfrs = new HashMap<>();
    }

    public boolean onlyOneLoaded(){
        return jfrs.size()==1;
    }
    public String getDefaultName(){
        if(jfrs.size()==1) {
            return jfrs.keySet().iterator().next();
        }
        return null;
    }
    public FlightRecording getDefaultRecording(){
        if(this.size()==0)
            return null;
        else
            return jfrs.get(getDefaultName());
    }

    public int size(){
        return jfrs.size();
    }
    public boolean contains(String name){
        return jfrs.containsKey(name);
    }

    public Set<String> getNames(){
        return jfrs.keySet();
    }

    public void unload(String name){
        if(jfrs.containsKey(name)){
            jfrs.remove(name);
        }
    }
    public void loadJfr(File file){
        loadJfr(file,file.getName());
    }
    public void loadJfr(File file, String name){
        FlightRecording newRecording = FlightRecordingLoader.loadFile(file);
        jfrs.put(name,newRecording);
    }
    public FlightRecording get(String name){
        if(jfrs.containsKey(name)){
            return jfrs.get(name);
        }
        else {
            return null;
        }
    }
}


