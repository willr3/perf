package perf.analysis.serverlog;

import org.json.JSONArray;
import org.json.JSONObject;
import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.parse.factory.ServerLogFactory;
import perf.parse.reader.TextLineReader;
import perf.stack.Frame;
import perf.util.AsciiArt;
import perf.util.Counters;
import perf.util.Indexer;
import perf.util.file.FileUtility;
import perf.util.json.Jsons;
import perf.util.json.JsonArray;

import java.util.List;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class Exceptions implements JsonConsumer{

    private Indexer<String> frames;
    private Counters<Frame> frameFrequency;


    public Exceptions(){
        this(new Indexer<>());
    }
    public Exceptions(Indexer<String> frames){
        this.frames = frames;
        this.frameFrequency = new Counters<>();
    }

    @Override
    public void consume(Jsons object) {
        if(object.has("stack") && object.has("message")){

            Jsons target = object;
            Frame root = new Frame(-1);
            Frame currentFrame = root;
            while(target!=null && target.has("stack")){
                JsonArray stack = target.getJsonArray("stack");
                for(int i=stack.length()-1; i>=0; i--){
                    Jsons o = stack.getJson(i);

                    boolean isNewFrame = frames.get(o.getString("frame")) < 0;
                    if(isNewFrame){

                    }
                    int frameId = frames.add(o.getString("frame"));
                    currentFrame = currentFrame.addChild(frameId);

                }
                if(target.has("causedBy")){

                    //TODO handle the exception in target
                    target = target.getJson("causedBy");
                    //System.out.println(target.getString("exception"));

                }else{
                    target = null;
                }

            }
            frameFrequency.add(root);
        }
    }

    public static void main(String[] args) {


        String filePath = null;
        filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/log/server.log";
        filePath = "/home/wreicher/specWork/server.246Y.log";
        filePath = "/home/wreicher/runtime/wildfly-10.0.0.Final-pool/standalone/log/server.log";
        filePath = "/home/wreicher/perfWork/byteBuffer/10E-MR/server.log";
        filePath = "/home/wreicher/perfWork/insurance/server.log";
        //String frameIndexPath = "/home/wreicher/perfWork/byteBuffer/frameIndex.json";

        JSONArray jsonArray = null;
        Indexer<String> frameIndex = new Indexer<>();
        jsonArray = new JSONArray();//FileUtility.readJsonArrayFile(frameIndexPath);
        System.out.println("  frames : " + AsciiArt.ANSI_CYAN + jsonArray.length() + AsciiArt.ANSI_RESET);
        for (int i = 0; i < jsonArray.length(); i++) {
            frameIndex.add(jsonArray.getString(i));
        }
        jsonArray = null;

//        JSONObject allData = FileUtility.readJsonObjectFile("/home/wreicher/perfWork/byteBuffer/9O-AMQBuffer/allData.json");
//        System.out.println("allData");
//        System.out.println(allData.keySet());

        Exceptions c = new Exceptions(frameIndex);

        ServerLogFactory slf = new ServerLogFactory();
        Parser p = slf.newLogEntryParser();
        p.add(c);
        TextLineReader r = new TextLineReader();
        r.addParser(p);
        r.read(filePath);

        System.out.printf("FrameFrequency: %d %n",c.frameFrequency.size());

        Function<Frame,String> toStringFunction = (a)->{
            if(a.getId()<0){

            }
            return frameIndex.get(a.getId());
        };
        Function<Frame,List<Frame>> getChildren = (b)-> b.getChildren();

        AsciiArt.TREE_OFFSET_SPACE     = " ";
        AsciiArt.TREE_OFFSET_SUB_CHILD = "│";
        AsciiArt.TREE_CHILD            = "├";
        AsciiArt.TREE_CHILD_LAST       = "└";

        AsciiArt.TREE_OFFSET_SPACE     = "";
        AsciiArt.TREE_OFFSET_SUB_CHILD = "";
        AsciiArt.TREE_CHILD            = "";
        AsciiArt.TREE_CHILD_LAST       = "";

        c.frameFrequency.entries().forEach(f->{
            System.out.println(c.frameFrequency.count(f)+" -> "+f.toString());

            String tree = AsciiArt.printTree(f,getChildren,toStringFunction);
            System.out.println(tree);

        });



    }
}
