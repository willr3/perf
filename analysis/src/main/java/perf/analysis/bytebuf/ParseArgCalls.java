package perf.analysis.bytebuf;


import org.json.JSONArray;
import org.json.JSONObject;

import perf.stack.*;
import perf.stack.Stack;
import perf.stack.builder.StackBuilder;
import perf.stack.builder.StackSetBuilder;
import perf.util.AsciiArt;

import perf.util.HashedLists;
import perf.util.HashedSets;
import perf.util.Indexer;
import perf.util.file.FileUtility;


import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by wreicher
 */
public class ParseArgCalls {

    public static final String OBJECT_ID_KEY = "oId";

    public static void main(String[] args) {

        StackBuilder stackBuilder = new StackBuilder();

        //Inputs
        String perfWork = "/home/wreicher/perfWork/byteBuffer/";
        String workFolder = Paths.get(perfWork,"9O-AMQBuffer").toString();//"/home/wreicher/perfWork/byteBuffer/9O-AMQBuffer/";
        String btmPath = Paths.get(workFolder,"argCalls.log").toString();

        String frameIndexPath = Paths.get(workFolder,"frameIndex.json").toString();
        String threadNamesOutput = Paths.get(workFolder,"threadNames.json").toString();

        String allDataOutput = Paths.get(workFolder,"allData.cn2.tst.json").toString();

        Indexer<String> frameIndex = new Indexer<>();
        Indexer<String> threadNameIndex = new Indexer<>();
        Indexer<String> classNameIndexer = new Indexer<>();

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

        Matcher paramMather = Pattern.compile("param\\.(?<idx>\\d+)\\.(?<paramName>\\S+)").matcher("");
        Matcher intMatcher = Pattern.compile("\\d+").matcher("");

        Function<StackSetInvocation,Set<String>> getObjects = (stackSetInvocation)->{
            Set<String> rtrn = new HashSet<>();
            stackSetInvocation.getInvocations().forEach((stackInvocation)->{
                Queue<JSONObject> todo = new LinkedList<JSONObject>();

                todo.add(stackInvocation.getData());

                while(!todo.isEmpty()){
                    JSONObject next = todo.remove();
                    next.keySet().forEach((key)->{
                        Object value = next.get(key);
                        if(key.contains("hashCode")){
                            rtrn.add(value.toString());
                        }else if (value instanceof JSONObject){
                            todo.add((JSONObject)value);
                        }
                    });
                }
            });
            return rtrn;
        };

        try ( BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(btmPath))) ) {

            Map<String,List<StackInvocation>> bufferInvocations = new HashMap<>();
            HashMap<Integer,Stack> uniqueStacks = new HashMap<>();

            long start = System.currentTimeMillis();
            reader.lines().limit(1).forEach((line)-> {
                JSONObject json = new JSONObject(line);
                System.out.println(json.toString(2));
                //convert param.#.*:value to param : { # : {*:value, ... }, ... }
                List<String> keys = Arrays.asList(json.keySet().toArray(new String[]{}));
                System.out.println(keys);

                List<Integer> frames = new ArrayList<Integer>();
                List<Integer> lines = new ArrayList<Integer>();

                JSONObject data = new JSONObject();

                for (String key : keys) {
                    Object value = json.get(key);

                    if(key.indexOf("className") > -1){
                        int classNameIndex = classNameIndexer.add(value.toString());
                        value = new Integer(classNameIndex);
                    }
                    if(value instanceof String){
                        if(intMatcher.reset(((String)value)).matches()){
                            value = Integer.valueOf(((String)value));
                        }
                    }
                    //turn param.x.y into param : { x : { y : ? } }
                    if (paramMather.reset(key).matches()) {

                        String idx = paramMather.group("idx");
                        String idxName = paramMather.group("paramName");

                        //put it into data too
                        if (!data.has("param")) {
                            data.put("param", new JSONObject());
                        }
                        if (!data.getJSONObject("param").has(idx)) {
                            data.getJSONObject("param").put(idx, new JSONObject());
                        }
                        data.getJSONObject("param").getJSONObject(idx).put(idxName, value);

                    } else if (key.equals("stack")) { //
                        JSONArray arry = json.getJSONArray(key);
                        for (int i = 0; i < arry.length(); i++) {
                            frames.add(arry.getJSONObject(i).getInt("frame"));
                            lines.add(arry.getJSONObject(i).getInt("line"));
                        }
                    } else {
                        data.put(key,value);
                    }
                }

                Stack stack = stackBuilder.getStack(frames,lines);
                if(!uniqueStacks.containsKey(stack.getUid())){
                    uniqueStacks.put(stack.getUid(),stack);
                }

                StackInvocation stackInvocation = new StackInvocation(stack.getUid(),data);
                for (String key : keys) {
                    if(key.indexOf("hashCode")>-1){
                        String value = classNameIndexer.get(json.getString("className"))+"."+json.getInt(key);
                        if(!bufferInvocations.containsKey(value)){
                            bufferInvocations.put(value,new ArrayList<>());
                        }
                        bufferInvocations.get(value).add(stackInvocation);
                    }
                }
            });

            HashSet<StackSet> uniqueStackSets = new HashSet<>();
            HashedLists<Integer,StackSetInvocation> stackSetInvocations = new HashedLists<>();
            HashedSets<Integer,Integer> objectReferences = new HashedSets<>();

            StackSetBuilder stackSetBuilder = new StackSetBuilder();

            bufferInvocations.forEach((hashCode,stackInvocationList)->{
                List<JSONObject> stackData = stackInvocationList.stream().map(StackInvocation::getData).collect(Collectors.toList());
                StackSet stackSet = stackSetBuilder.getStackSet(
                        stackInvocationList.stream().map((stackInvocation)-> uniqueStacks.get(stackInvocation.getStackUid())).collect(Collectors.toList()));

                uniqueStackSets.add(stackSet);

                StackSetInvocation stackSetInvocation = new StackSetInvocation(stackSet.uid,stackInvocationList);

                stackSetInvocations.put(stackSet.getUid(),stackSetInvocation);

                Set<String> objects =  getObjects.apply(stackSetInvocation);
                objects.forEach((objectHash)->{
                    objectReferences.put(Integer.parseInt(objectHash),stackSet.getUid());
                });
            });

            System.out.println("uniqueStacks  = "+AsciiArt.ANSI_CYAN+uniqueStacks.size()+AsciiArt.ANSI_RESET);
            System.out.println("uniqueBuffers = "+AsciiArt.ANSI_CYAN+bufferInvocations.size()+AsciiArt.ANSI_RESET);
            System.out.println("uniqueStackSets = "+AsciiArt.ANSI_CYAN+uniqueStackSets.size()+AsciiArt.ANSI_RESET);
            System.out.println("stackSetInvocations = "+ AsciiArt.ANSI_CYAN+stackSetInvocations.size()+AsciiArt.ANSI_RESET);
            System.out.println("objectReferences " + AsciiArt.ANSI_CYAN + objectReferences.size()+AsciiArt.ANSI_RESET);

            List<Map.Entry<Integer,List<StackSetInvocation>>> sortedStackSetInvocations =
                stackSetInvocations.stream()
                    .sorted((a,b)-> -(a.getValue().size() - b.getValue().size()))
                    .collect(Collectors.toList());

            //write everything to the file :)
            try (PrintStream writer = new PrintStream(new FileOutputStream(allDataOutput))){
                writer.println("{");
                writer.println("  \"frames\": [");
                List<String> frames = frameIndex.getIndexedList();
                for(int i=0; i<frames.size(); i++){
                    if(i>0){
                        writer.print(", ");
                    }
                    writer.print("\"");
                    writer.print(frames.get(i));
                    writer.print("\"");
                }
                writer.println("  ],");
                writer.println("  \"classNames\": [");
                List<String> classNames = classNameIndexer.getIndexedList();
                for(int i=0; i<classNames.size(); i++){
                    if(i>0){
                        writer.print(", ");
                    }
                    writer.print("\"");
                    writer.print(classNames.get(i));
                    writer.print("\"");
                }
                writer.println("  ],");
                writer.println("  \"threadNames\": [");
                List<String> threadNames = threadNameIndex.getIndexedList();
                for(int i=0; i<threadNameIndex.size(); i++){
                    if(i>0){
                        writer.print(", ");
                    }
                    writer.print("\"");
                    writer.print(threadNames.get(i));
                    writer.print("\"");
                }
                writer.println("  ],");
                writer.println("  \"stacks\": {");
                for(Iterator<Map.Entry<Integer,Stack>> iter = uniqueStacks.entrySet().iterator(); iter.hasNext();){
                    Map.Entry<Integer,Stack> next = iter.next();
                    writer.print("    \""+next.getKey()+"\": ");
                    next.getValue().writeJson(writer,6);
                    if(iter.hasNext()){
                        writer.println(",");
                    }
                }
                writer.println("  },");
                writer.println("  \"stackSets\": {");
                for(Iterator<StackSet> iter = uniqueStackSets.iterator(); iter.hasNext();){
                    StackSet next = iter.next();
                    writer.print("    \""+next.getUid()+"\": ");
                    next.writeJson(writer,6);
                    if(iter.hasNext()){
                        writer.println(",");
                    }
                }
                writer.println("  },");

                writer.println("  \"stackSetInvocations\": {");
                for(Iterator<Map.Entry<Integer,List<StackSetInvocation>>> iter = sortedStackSetInvocations.iterator(); iter.hasNext();){
                    Map.Entry<Integer,List<StackSetInvocation>> next = iter.next();
                    writer.print("    \""+next.getKey()+"\": [");
                    for(Iterator<StackSetInvocation> setIter = next.getValue().iterator(); setIter.hasNext();){
                        StackSetInvocation setInvocation = setIter.next();
                        setInvocation.writeJson(writer,6);
                        if(setIter.hasNext()){
                            writer.print(",");
                        }
                    }

                    if(iter.hasNext()){
                        writer.print("    ],");
                    }else{
                        writer.print("    ]");
                    }
                }
                writer.println("  },");
                writer.println("  \"objectsToSets\": {");
                for(Iterator<Map.Entry<Integer,HashSet<Integer>>> iter = objectReferences.iterator(); iter.hasNext();){
                    Map.Entry<Integer,HashSet<Integer>> next = iter.next();
                    writer.print("    \""+next.getKey()+"\": "+next.getValue().toString());
                    if(iter.hasNext()){
                        writer.println(",");
                    }
                }
                writer.println("  }");
                writer.println("}");
                writer.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }
}
