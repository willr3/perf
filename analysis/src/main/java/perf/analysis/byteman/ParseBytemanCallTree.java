package perf.analysis.byteman;

import org.json.JSONArray;
import org.json.JSONException;
import perf.parse.*;
import perf.parse.reader.TextLineReader;
import perf.stack.Frame;
import perf.util.AsciiArt;
import perf.util.HashedSets;
import perf.util.Indexer;
import perf.util.json.Jsons;
import perf.util.json.JsonArray;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class ParseBytemanCallTree {


    public static void main(String[] args) {
        String workFolder = "/home/wreicher/perfWork/byteBuffer/";
        String filePath = workFolder+"btm.stack.log";
        String indexPath = workFolder+"methodIndex.json";
        String bufferPath = workFolder+"bufferCallTree.json";
        String messagePath = workFolder+"bufferCallTree.json";

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
                    .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)")
                        .set("nativeMethod", Value.BooleanKey))
                    .add(new Exp("unknownSource","\\((?<unknownSource>Unknown Source)\\)")
                        .set("unknownSource", Value.BooleanKey))
                    .add(new Exp("lineNumber", "\\((?<file>[^:]+):(?<line>\\-?\\d+)\\)")
                        .set("line",Value.Number)
                ));

        TextLineReader r = new TextLineReader();
        r.addParser(parser);

        Indexer<String> frames = new Indexer<>();
        HashMap<String,Frame> bufferFrames = new HashMap<>();
        HashMap<String,Frame> messageFrames = new HashMap<>();
        AtomicInteger count = new AtomicInteger(0);
        //build the frames from each entry
        parser.add(json->{
            try {
                String hashCode = json.getString("hashCode");
                String className = json.getString("className");
                //Frame root = new Frame(-1);
                HashMap<String,Frame> targetFrames = className.startsWith("org.apache.activemq.artemis.core.buffers") ? bufferFrames : messageFrames;
                if (!targetFrames.containsKey(hashCode)) {
                    targetFrames.put(hashCode, new Frame(-1));
                }
                Frame root = targetFrames.get(hashCode);

                Frame currentFrame = root;
                List<Integer> lineNumbers = new LinkedList<Integer>();
                if (json.has("stack")) {
                    JsonArray stack = json.getJsonArray("stack").reverse();
                    for (int i = 0; i < stack.length(); i++) {
                        Jsons frame = stack.getJson(i);
                        int frameId = frames.add(frame.getString("frame"));
                        currentFrame = currentFrame.addChild(frameId);
                        if(frame.has("line")){
                            lineNumbers.add(0, (int)frame.getLong("line"));
                        }else{
                            lineNumbers.add(0, -10);
                        }

                    }
                    currentFrame.addValues("line", lineNumbers);
                    currentFrame.addValue("thread",json.getString("threadName"));
                }
                if (json.has("param")) {
                    Frame fRef = currentFrame;
                    json.getJsonArray("param").forEachJson(param -> {
                        fRef.addValue(""+param.getLong("idx"), param.getString("hashCode"));
                    });
                    //System.exit(0);

                }
                count.incrementAndGet();
            }catch(JSONException e){

                e.printStackTrace();
                System.out.println(json.toString(2));
                System.exit(0);
            }
        });

        System.out.println("start reading");
        long start = System.currentTimeMillis();
        System.out.println("Start "+(start=System.currentTimeMillis()));
        r.read(filePath);
        long stop = System.currentTimeMillis();
        System.out.println("processed "+count+" in "+(stop-start)+"ms");
        System.out.println("done reading");
        System.exit(-1);

        AsciiArt.TREE_OFFSET_SPACE     = " ";
        AsciiArt.TREE_OFFSET_SUB_CHILD = "│";
        AsciiArt.TREE_CHILD            = "├";
        AsciiArt.TREE_CHILD_LAST       = "└";

        HashedSets<Frame,String> bufferFramesToHashCodes = new HashedSets<>();
        HashedSets<Frame,String> messageFramesToHashCodes = new HashedSets<>();

        System.out.println("finding unique callTrees");

        bufferFrames.entrySet().forEach(entry->{
            bufferFramesToHashCodes.put(entry.getValue(),entry.getKey());
        });
        bufferFramesToHashCodes.keys().forEach(frame->{
            Set<String> hashes = bufferFramesToHashCodes.get(frame);
            frame.addValues("buffers",Arrays.asList(hashes.toArray(new String[]{})));
        });
        messageFrames.entrySet().forEach(entry->{
            messageFramesToHashCodes.put(entry.getValue(),entry.getKey());
        });
        messageFramesToHashCodes.keys().forEach(frame->{
            Set<String> hashes = messageFramesToHashCodes.get(frame);
            frame.addValues("messages",Arrays.asList(hashes.toArray(new String[]{})));
        });
        System.out.println(bufferFramesToHashCodes.size()+" uniq buffer callTrees");
        System.out.println(messageFramesToHashCodes.size()+" uniq message callTrees");


        System.out.println("writing index file");
        try {
            ObjectOutputStream indexOS = new ObjectOutputStream(new FileOutputStream(indexPath));
            indexOS.writeObject(new JSONArray(frames.getIndexedList()).toString(1));
            indexOS.flush();
            indexOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("writing buffer file");
        try {
            ObjectOutputStream bufferOS = new ObjectOutputStream(new FileOutputStream(bufferPath));
            bufferOS.writeObject(new JSONArray(bufferFramesToHashCodes.keys()).toString(1));
            bufferOS.flush();
            bufferOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("writing message file");
        try {
            ObjectOutputStream messageOS = new ObjectOutputStream(new FileOutputStream(messagePath));
            messageOS.writeObject(new JSONArray(messageFramesToHashCodes.keys()).toString(1));
            messageOS.flush();
            messageOS.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Function<Frame,String> toStringFunction = (frame)->{
            StringBuilder rtrn = new StringBuilder();
            if(frame.getId()<0){
                return "[ "+bufferFramesToHashCodes.get(frame).size()+": "+bufferFramesToHashCodes.get(frame).toString()+" ]";
            }
            return frames.get(frame.getId())+"[ "+frame.getValues().toString(0)+" ]";
        };
        Function<Frame,List<Frame>> getChildren = (b)->{
            return b.getChildren();
        };

//        for(Frame x : bufferFrames.values()){
//            System.out.print(AsciiArt.printTree(x,getChildren,toStringFunction));
//            System.out.println("\n");
//            System.exit(0);
//        }

    }
}
