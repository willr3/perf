package perf.analysis.bytebuf;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.stack.CallSite;
import perf.util.AsciiArt;
import perf.util.Indexer;
import perf.util.file.FileUtility;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class ParseBytemanJsonLog {

    public static final String OBJECT_ID_KEY = "oId";

    public static void main(String[] args) {

        //Inputs
        String workFolder = "/home/wreicher/perfWork/byteBuffer/9L-AMQBuffer/";
        String btmPath = workFolder+"argCalls.log";

        String frameIndexPath = workFolder+"frameIndex.json";
        String callsiteOutput = workFolder+"callsites.json";
        String threadNamesOutput = workFolder+"threadNames.json";

        Indexer<String> frameIndex = new Indexer<>();
        Indexer<String> threadNameIndex = new Indexer<>();

        System.out.println(AsciiArt.ANSI_GREEN + "workFolder : "+workFolder+AsciiArt.ANSI_RESET);

        //load the Indexer<Strings> for this dataset
        JSONArray jsonArray = null;

        jsonArray = FileUtility.readJsonArrayFile(frameIndexPath);
        System.out.println("  frames : " + AsciiArt.ANSI_CYAN + jsonArray.length() + AsciiArt.ANSI_RESET);
        for (int i = 0; i < jsonArray.length(); i++) {
            frameIndex.add(jsonArray.getString(i));
        }
        jsonArray = FileUtility.readJsonArrayFile(threadNamesOutput);
        System.out.println("  threads : " + AsciiArt.ANSI_CYAN + jsonArray.length() + AsciiArt.ANSI_RESET);
        for (int i = 0; i < jsonArray.length(); i++) {
            threadNameIndex.add(jsonArray.getString(i));
        }

        Matcher m = Pattern.compile("param\\.(?<idx>\\d+)\\.(?<paramName>\\S+)").matcher("");

        HashMap<String,CallSite> targetMap = new HashMap<>();

        AsciiArt.TREE_OFFSET_SPACE     = " ";
        AsciiArt.TREE_OFFSET_SUB_CHILD = "│";
        AsciiArt.TREE_CHILD            = "├";
        AsciiArt.TREE_CHILD_LAST       = "└";

        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(btmPath))) ) {
            long start = System.currentTimeMillis();
            reader.lines().limit(10).forEach((line)->{
                JSONObject json = new JSONObject(line);
                //convert param.#.*:value to param : { # : {*:value, ... }, ... }
                List<String> keys = Arrays.asList(json.keySet().toArray(new String[]{}));
                for(String key : keys){
                    if(m.reset(key).matches()){
                        String value = json.getString(key);
                        String idx = m.group("idx");
                        String idxName = m.group("paramName");
                        json.remove(key);
                        //json.append("param",param);
                        if(!json.has("param")){
                            json.put("param",new JSONObject());
                        }
                        if(!json.getJSONObject("param").has(idx)){
                            json.getJSONObject("param").put(idx,new JSONObject());
                        }
                        json.getJSONObject("param").getJSONObject(idx).put(idxName,value);
                        //json.getJSONObject("param").getJSONObject(idx).put("idx",idx);
                    }
                }

                //build the callsites
                String key = null;
                if(json.has("hashCode")){ // heirarchy of AMQBuffer
                    key = json.getString("hashCode");
                    CallSite newCallSite = CallSite.fromJsonHelper(json);
                    if(!targetMap.containsKey(key)){
                        //System.out.println("NEWSTACK "+key);
                        targetMap.put(key,newCallSite);
                    }else {
                        targetMap.get(key).mergeStack(newCallSite);
                    }
                }
                if (json.has("param")){ // methodCall with HierarchyBuffer as argument
                    JSONObject param = json.getJSONObject("param");
                    for(String paramIdx : param.keySet()){
                        JSONObject paramIdxObj = param.getJSONObject(paramIdx);
                        key = paramIdxObj.getString("hashCode");
                        CallSite newCallSite = CallSite.fromJsonHelper(json);
                        if(!targetMap.containsKey(key)){
                            targetMap.put(key,newCallSite);
                        }else {
                            targetMap.get(key).mergeStack(newCallSite);
                        }
                    }
                }
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            try (PrintWriter writer = new PrintWriter(new FileWriter(threadNamesOutput))){
                writer.println("[");
                List<String> threadNames = threadNameIndex.getIndexedList();
                for(int i=0; i<threadNames.size(); i++){
                    if(i>0){
                        writer.append(",\n");
                    }
                    writer.append("\"");
                    writer.append(threadNames.get(i));
                    writer.append("\"");
                }
                writer.append("]");
                writer.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            Comparator<CallSite> callSiteComparator = (a,b)->{
                if(a.data().has(OBJECT_ID_KEY)){
                    if(b.data().has(OBJECT_ID_KEY)){
                        return Integer.compare(a.data().getJSONArray(OBJECT_ID_KEY).length(),b.data().getJSONArray(OBJECT_ID_KEY).length());
                    }else{
                        return -1;
                    }
                }else{
                    if(b.data().has(OBJECT_ID_KEY)){
                        return 1;
                    }else{
                        return -1;
                    }
                }
            };

            HashMap<Integer,CallSite> uniqueCallsites = new HashMap<>();

            String prefixes[] = new String[]{AsciiArt.ANSI_CYAN,AsciiArt.ANSI_RED,AsciiArt.ANSI_MAGENTA,AsciiArt.ANSI_YELLOW,AsciiArt.ANSI_BLUE,AsciiArt.ANSI_GREEN};
            for(String key : targetMap.keySet()){
                CallSite callSite = targetMap.get(key);
                int hashCode = callSite.hashCode();
                if(!uniqueCallsites.containsKey(hashCode)){
                    uniqueCallsites.put(hashCode,callSite);
                    if(callSite.getThreads().size()>2){
                        Indexer<Integer> threadIndexer = new Indexer<>();
                        Function<CallSite,String> toStringFunction= (cs)->{
                            if(cs.getId()<0){
                                return "ROOT-"+cs.getThreads().size();
                            }else if (cs.getThreads().size()==1) {
                                int index = threadIndexer.add(cs.getThreads().get(cs.getThreads().size()-1));
                                return prefixes[index%prefixes.length]+frameIndex.get(cs.getId())+AsciiArt.ANSI_RESET + " : " + (cs.hasLineNumbers() ? cs.getLineNumbers()[cs.getLineNumbers().length-1] : "" );
                            }else{
                                return frameIndex.get(cs.getId()) + " : " + (cs.hasLineNumbers() ? cs.getLineNumbers()[cs.getLineNumbers().length-1] : "" );
                            }
                        };
                        System.out.println(AsciiArt.printTree(targetMap.get(key),CallSite::getChildren,toStringFunction));
                    }
                }else{
                    uniqueCallsites.get(hashCode).mergeData(callSite);
                }
                uniqueCallsites.get(hashCode).data().append(OBJECT_ID_KEY,key);
            }

            System.out.println("unique threadNames = " + AsciiArt.ANSI_CYAN + threadNameIndex.size() + AsciiArt.ANSI_RESET);
            System.out.println("unique buffers = " + AsciiArt.ANSI_CYAN + targetMap.size() + AsciiArt.ANSI_RESET);
            System.out.println("unique callsites: " + AsciiArt.ANSI_CYAN + uniqueCallsites.size() + AsciiArt.ANSI_RESET);

            try (PrintWriter writer = new PrintWriter(new FileWriter(callsiteOutput))){
                writer.print("[");
                List<CallSite> callSites = new ArrayList<CallSite>(uniqueCallsites.values());
                callSites.sort(callSiteComparator);
                Collections.reverse(callSites);
                for(Iterator<CallSite> iter = callSites.iterator(); iter.hasNext();){
                    CallSite toWrite = iter.next();
                    writer.print(toWrite.toJson().toString(0));
                    if(iter.hasNext()) {
                        writer.println(",");
                    }
                }
                writer.write("]");
                writer.flush();
            } catch (IOException e){
                e.printStackTrace();
            }

        } finally {

        }
    }
}
