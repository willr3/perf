package perf.analysis.bytebuf;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.stack.Stack;
import perf.stack.StackSet;
import perf.stack.StackSetInvocation;
import perf.util.AsciiArt;
import perf.util.HashedList;
import perf.util.HashedLists;
import perf.util.Indexer;
import perf.util.file.FileUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static perf.parse.Merge.Entry;


/**
 * Created by wreicher
 */
public class ReadAllData {

    public static void main(String[] args) {
        String workFolder = "/home/wreicher/perfWork/byteBuffer/9O-AMQBuffer/";
        String allDataFile = workFolder+"allData.json";

        System.out.println("Reading "+ AsciiArt.ANSI_CYAN+allDataFile+AsciiArt.ANSI_RESET+" @ "+System.currentTimeMillis());
        JSONObject allData = FileUtility.readJsonObjectFile(allDataFile);

        System.out.println(allData.keySet());
        for(String key : allData.keySet()){
            Object value = allData.get(key);
            if(value instanceof JSONArray){
                JSONArray array = (JSONArray)value;
                System.out.println(key+" : "+array.length());
            }else if (value instanceof JSONObject){
                JSONObject object = (JSONObject)value;
                System.out.println(key+" : "+object.length());
            }
        }



        Indexer<String> frameIndexer = Indexer.fromJSONArray(allData.getJSONArray("frames"));
        Indexer<String> classNameIndexer = Indexer.fromJSONArray(allData.getJSONArray("classNames"));
        HashMap<Integer,Stack> stacks = new HashMap<>();
        HashMap<Integer, StackSet> stackSets = new HashMap<>();
        HashedLists<Integer,StackSetInvocation> stackSetInvocations = new HashedLists<>();

        for(int i=0; i<allData.getJSONObject("stacks").length(); i++){
            JSONObject stackJson = allData.getJSONObject("stacks").getJSONObject(""+i);
            Stack stack = Stack.fromJSON(stackJson);
            stacks.put(stack.getUid(),stack);
        }
        for(int i=0; i<allData.getJSONObject("stackSets").length(); i++){
            JSONObject stackSetJson = allData.getJSONObject("stackSets").getJSONObject(""+i);
            StackSet stackSet = StackSet.fromJSON(stackSetJson);
            stackSets.put(stackSet.getUid(),stackSet);
        }

        JSONObject stackSetInvocationsJson = allData.getJSONObject("stackSetInvocations");
        for(String key : stackSetInvocationsJson.keySet()){
            JSONArray invocationListJson = stackSetInvocationsJson.getJSONArray(key);
            for(int i=0; i<invocationListJson.length(); i++){
                JSONObject invocationJson = invocationListJson.getJSONObject(i);
                StackSetInvocation invocation = StackSetInvocation.fromJSON(invocationJson);
                stackSetInvocations.put(Integer.parseInt(key),invocation);
            }
        }
        //at this point data is loaded

        int max = stackSetInvocations.stream().map((entry)->{
            Integer key = entry.getKey();
            List<StackSetInvocation> list = entry.getValue();
            int m = list.stream().map((invocation)->{
                HashSet<Integer> threadNames = new HashSet<>();
                threadNames.addAll(invocation.getInvocations().stream().map(i->i.getData().getInt("threadName")).collect(Collectors.toList()));
                return threadNames.size();
            }).max(Integer::compare).get();
            if(m == 4){
                System.out.println("4 == "+key);
            }
            return m;
        }).max(Integer::compare).get();

        System.out.println("max = "+max);
//        ArrayList<Integer> foundFrames = new ArrayList<>();
//        frameIndexer.forEach((index,frameString)->{
//            if(frameString.indexOf("<init>")>0){
//                foundFrames.add(index);
//            }
//        });
//        System.out.println("found frames = "+foundFrames.toString());
//        foundFrames.forEach((frameUid)->{
//            System.out.println("  "+frameUid+" "+frameIndexer.get(frameUid));
//        });
//
//        ArrayList<Integer> foundStacks = new ArrayList<>();
//        stacks.forEach((index,stack)->{
//            ArrayList<Integer> overlap = ((ArrayList<Integer>)foundFrames.clone());
//            overlap.retainAll(stack.getFrames());
//            if(!overlap.isEmpty()){
//                foundStacks.add(stack.getUid());
//            }
//        });
//        System.out.println("found stacks = "+foundStacks.size()+" / "+stacks.size());
//
//        ArrayList<Integer> foundStackSets = new ArrayList<>();
//        stackSets.forEach((index,stackSet)->{
//            ArrayList<Integer> overlap = ((ArrayList<Integer>)foundStacks.clone());
//            overlap.retainAll(stackSet.getStackUids());
//            if(!overlap.isEmpty()){
//                foundStackSets.add(stackSet.getUid());
//            }
//        });
//
//        List<Integer> notFoundSets = stackSets.values().stream().map(StackSet::getUid).collect(Collectors.toList());
//        notFoundSets.removeAll(foundFrames);
//
//        System.out.println("found StackSets = "+foundStackSets.size()+" / "+stackSets.size());
//        notFoundSets.forEach((stackSetUid)->{
//            StackSet stackSet = stackSets.get(stackSetUid);
//            System.out.println(stackSet.getStackUids().toString());
//        });


    }

}
