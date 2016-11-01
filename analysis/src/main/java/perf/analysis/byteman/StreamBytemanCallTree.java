package perf.analysis.byteman;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import perf.parse.*;
import perf.stack.CallSite;
import perf.stack.Frame;
import perf.util.Indexer;
import perf.util.json.Jsons;
import perf.util.json.JsonArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class StreamBytemanCallTree {

    public static List<String> indexFrames = new ArrayList<String>();

    public static void main(String[] args) {
        String workFolder = "/home/wreicher/perfWork/byteBuffer/";
        String btmPath = workFolder+"btm.log";
        String indexPath = workFolder+"frameIndex.json";

        Indexer<String> frames = new Indexer<>();

        try {
            JSONArray frameArray = new JSONArray(new String(Files.readAllBytes(Paths.get(indexPath))));
            for(int i=0; i<frameArray.length();i++){
                indexFrames.add(frameArray.getString(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        indexFrames = Collections.unmodifiableList(indexFrames);

        final String eof = "<Stream.EOF "+Math.random()+">";

        final Parser parser = new Parser();
        parser.add(new Exp("thread","Thread=(?<threadName>.+)")
                .set(Merge.NewStart)
                .eat(Eat.Line));
        parser.add(new Exp("className","Class=(?<className>.+)")
                .eat(Eat.Line));
        parser.add(new Exp("hashCode","Hashcode=(?<hashCode>\\d+)")
                .eat(Eat.Line));
        parser.add(new Exp("param", "Param\\[(?<idx>\\d+)\\]\\.ActiveMQBuffer\\.hashCode=(?<hashCode>\\d+)")
                .group("param")
                .set(Merge.Entry)
                .set("idx",Value.Number)
                .eat(Eat.Line));
        parser.add(new Exp("stack", "(?<frame>\\S[^\\(]+)")
                .group("stack")
                .set(Merge.Entry)
                .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)")//doesn't actually happen?
                        .set("nativeMethod", Value.BooleanKey))
                .add(new Exp("unknownSource","\\((?<unknownSource>Unknown Source)\\)")
                        .set("unknownSource", Value.BooleanKey))
                .add(new Exp("lineNumber", "\\((?<file>[^:]+):(?<line>\\-?\\d+)\\)")
                        .set("line",Value.Number)
                ));
        parser.add(new Exp("EOF",eof).set(Merge.NewStart));

        Function<String,Stream<Jsons>> stringToJson = new Function<String, Stream<Jsons>>() {
            @Override
            public Stream<Jsons> apply(String s) {
                JSONObject emit = parser.onLine(s);
                if(emit==null){
                    return null;
                }
                return Stream.of(new Jsons(emit));
            }
        };
        Function<Jsons,Frame> jsonToFrame = (json)->{

            Frame root = new Frame(-1);
            Frame currentFrame = root;
            try {
                List<Integer> lineNumbers = new LinkedList<Integer>();
                if (json.has("stack")) {
                    JsonArray stack = json.getJsonArray("stack").reverse();
                    for (int i = 0; i < stack.length(); i++) {
                        Jsons frame = stack.getJson(i);
                        int frameId = frames.add(frame.getString("frame"));
                        currentFrame = currentFrame.addChild(frameId);
                        if (frame.has("line")) {
                            lineNumbers.add(0, (int) frame.getLong("line"));
                        } else {
                            lineNumbers.add(0, -10);
                        }

                    }
                    currentFrame.addValues("line", lineNumbers);
                    currentFrame.addValue("thread", json.getString("threadName"));
                }
                if (json.has("param")) {
                    Frame fRef = currentFrame;
                    json.getJsonArray("param").forEachJson(param -> {
                        fRef.addValue("" + param.getLong("idx"), param.getString("hashCode"));
                    });
                }
            }catch (JSONException e){
                e.printStackTrace();
                System.out.println(json.toString(2));
                System.exit(0);
            }
            return root;
        };

        try {
            long start = System.currentTimeMillis();
            System.out.println("Start " + (start = System.currentTimeMillis()));
            //Stream<String> stream = Files.lines(Paths.get(btmPath));
            //Stream<Jsons> jsonStream = StreamSupport.stream(new JsonSpliterator(btmPath,parser),false);
//            long count = stream
//
//                    .flatMap(stringToJson);

            HashMap<Integer, CallSite> callSites = new HashMap<>();
            AtomicLong stackCount = new AtomicLong(0);
//            HashMap<String,CallSite> hashedCallSites=
////                    Stream.concat(stream,Stream.of(eof))//leaves an invalid json emit in the parser but gets around missing the last emit :)
////                    .flatMap(stringToJson)
////                    jsonStream
//                    Files.lines(Paths.get("/home/wreicher/perfWork/byteBuffer/btm.json.log"))
//                    .map((line)->{
//                        try {
//                            JSONObject jso = new JSONObject(line);
//                            List<String> keys = Arrays.asList(jso.keySet().toArray(new String[]{}));
//                            //convert param_# : hash to param : [{ idx: #, hashCode: hash}...]
//                            for(String key : keys){
//                                if(key.startsWith("param_")){
//                                    String value = jso.getString(key);
//                                    jso.remove(key);
//
//                                    JSONObject param = new JSONObject();
//                                    param.put("idx",Integer.valueOf(key.substring("param_".length())));
//                                    param.put("hashCode",value);
//                                    jso.append("param",param);
//
//                                }
//                            }
//                            //replace timestamp
////                            if(jso.has("timestamp")){
////                                long timestamp = Long.parseLong(jso.getString("timestamp"));
////                                jso.put("timestamp",timestamp);
////                            }
//                            //replace frames with id
////                            for(int i=0; i<jso.getJSONArray("stack").length();i++){
////                                JSONObject frame = jso.getJSONArray("stack").getJSONObject(i);
////                                int frameIndex = frames.add(frame.getString("frame"));
////                                frame.put("frame",frameIndex);
////                            }
//                            return jso;
//                        }catch(JSONException e){
//                            e.printStackTrace();
//                            System.out.println(line);
//                            System.exit(0);
//                        }
//                        System.out.println("Failed to generate rtrn ");
//                        System.exit(0);
//                        return null;
//                    })
////                    .limit(10)
//                            .peek((json)->{stackCount.incrementAndGet();})
//                    .collect(
//                        Collectors.toMap(
//                            (json)->{return json.getString("className")+json.getString("hashCode");}, //key include className in case re-used hashCodes between classes
//                            (json)->{
//                                CallSite rtrn = CallSite.fromJsonHelper(json);
//                                return rtrn;
//                            }, //value
//                            CallSite::merge,
//                            ()-> new HashMap<String, CallSite>()
//                        )
//                    );

//            List<CallSite> callSiteList = Arrays.asList(hashedCallSites.values().toArray(new CallSite[]{}));
//            hashedCallSites.clear();
//            System.out.println(callSiteList.size());


/*                    .values().stream()//callTree
                    .peek((callSite)->{
                        if(callSites.containsKey(callSite.hashCode())){
                            CallSite mergeTo = callSites.get(callSite.hashCode());
                            mergeTo.copyData(callSite);
                        }else{
                            callSites.put(callSite.hashCode(),callSite);
                            System.out.println(callSites.size());
                        }
                    }).count();*/


//                    .map((json)->{
//
//                    })
//                    //.map(jsonToFrame)
//                    .count();
            long stop = System.currentTimeMillis();
            //System.out.println("processed "+stackCount.get()+" stacks in "+(stop-start)+"ms found "+count+" unique hashes and "+callSites.size()+" unique callTrees");
            //stream.close();

        }finally{

        }

    }
}
