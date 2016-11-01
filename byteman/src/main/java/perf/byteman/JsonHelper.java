package perf.byteman;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;
import org.json.JSONArray;
import perf.util.Indexer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by wreicher
 */
public class JsonHelper extends Helper {


    private static Indexer<String> frameIndexer = new Indexer<>();
    private static Indexer<String> threadNameIndexer = new Indexer<>();

    protected JsonHelper(Rule rule) {
        super(rule);
    }
    public void writeThreadNameIndexFile(String filePath){
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))){
            File f = new File(filePath);
            if(!f.exists()){
                f.getParentFile().mkdirs();
            }
            String toWrite = getThreadNameIndex();
            w.write(toWrite);
            w.flush();
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadThreadNameIndexFile(String filePath){
        synchronized (threadNameIndexer) { // prevent adds while loading data
            if(threadNameIndexer.size()==0) {
                try {
                    JSONArray frameArray = new JSONArray(new String(Files.readAllBytes(Paths.get(filePath))));
                    for (int i = 0; i < frameArray.length(); i++) {
                        threadNameIndexer.add(frameArray.getString(i));
                    }
                } catch (IOException e) {
                    System.out.println("failed to load threadNameIndexer from: " + filePath);
                }
            }
        }
    }
    public void writeFrameIndexFile(String filePath){
        try (BufferedWriter w = new BufferedWriter(new FileWriter(filePath))){
            File f = new File(filePath);
            if(!f.exists()){
                f.getParentFile().mkdirs();
            }
            String toWrite = getFrameIndex();
            w.write(toWrite);
            w.flush();
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void loadFrameIndexFile(String filePath){
        synchronized (frameIndexer) { // prevent adds while loading data
            if(frameIndexer.size()==0) {
                try {
                    JSONArray frameArray = new JSONArray(new String(Files.readAllBytes(Paths.get(filePath))));
                    for (int i = 0; i < frameArray.length(); i++) {
                        frameIndexer.add(frameArray.getString(i));
                    }
                } catch (IOException e) {
                    System.out.println("failed to load frameIndexer from: " + filePath);
                }
            }
        }
    }
    public String getFrameIndex(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        List<String> frames = frameIndexer.getIndexedList();
        for(int i=0; i<frames.size(); i++){
            if(i>0){
                sb.append(",\n");
            }
            sb.append("\"");
            sb.append(frames.get(i));
            sb.append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    public String getThreadNameIndex(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        List<String> frames = threadNameIndexer.getIndexedList();
        for(int i=0; i<frames.size(); i++){
            if(i>0){
                sb.append(",\n");
            }
            sb.append("\"");
            sb.append(frames.get(i));
            sb.append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    public void traceJsonStack(Object key, String keyValues[]){
        StackTraceElement stack[] = getStack();
        Thread thread = Thread.currentThread();
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(int i=0; i<keyValues.length;i++){
            sb.append(keyValues[i]);
            sb.append(": \"");
            i++;
            sb.append(keyValues[i]);
            sb.append("\"");
            sb.append(", ");
        }
        sb.append("timestamp: ");
        sb.append(System.currentTimeMillis());
        //sb.append(System.nanoTime());
        sb.append(",threadName: \"");
        int threadNameIndex = threadNameIndexer.add(thread.getName());
        sb.append(threadNameIndex);
        sb.append("\",");


        sb.append("stack: [");
        //TODO use triggerIndex like appendStack in Helper
        int stop = triggerIndex(stack);
        if(stop < 0){
            //return; //TODO should we raise some issue?
            throw new RuntimeException("perf.byteman.JsonHelper triggerIndex < 0");
        }
        boolean first=true;
        for(int i = stack.length-1 ; i>=stop; i--){
            if(!first){
                sb.append(", ");
            }else{
                first=false;
            }
            StackTraceElement frame = stack[i];

            //trying without this blocking call to frameIndexer
            int frameIndex = frameIndexer.add(frame.getClassName()+"."+frame.getMethodName());
            sb.append("{frame: ");
            //sb.append("\""+frame.getClassName()+"."+frame.getMethodName()+"\"");
            sb.append(frameIndex);
            sb.append(", ");
            if(frame.getFileName()!=null){
                sb.append("file: \"");
                sb.append(frame.getFileName());
                sb.append("\", ");
                sb.append("line: ");
                sb.append(frame.getLineNumber());
            }else{
                sb.append("unknownSource: true, ");
                sb.append("line: ");
                sb.append(frame.getLineNumber());
            }
            sb.append("}");//closes frame entry
        }
        sb.append("]");//closes stack array
        sb.append("}");//closes traceJson


        traceln(key,sb.toString());
    }
}
