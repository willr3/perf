package perf.analysis.serverlog;

import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.parse.factory.ServerLogFactory;
import perf.parse.reader.TextLineReader;
import perf.stack.Frame;
import perf.util.AsciiArt;
import perf.util.Counters;
import perf.util.Indexer;
import perf.util.json.Jsons;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class NettyLeak implements JsonConsumer{


    private Indexer<String> frameIndexer;
    private Counters<Frame> frameFrequency;

    public NettyLeak(){
        this(new Indexer<>(),new Counters<>());
    }
    public NettyLeak(Indexer<String> frames,Counters<Frame> frameFrequency){
        this.frameIndexer = frames;
        this.frameFrequency = frameFrequency;
    }


    public Indexer<String> getFrameIndexer(){return frameIndexer;}
    public Counters<Frame> getFrameFrequency(){return frameFrequency;}

    public int count = 0;

    @Override
    public void consume(Jsons object) {
        if(object.has("message") && object.getString("message").contains("LEAK: ByteBuf.release() was not called")){// netty leak detection
            count++;
            Frame root = new Frame(-1);
            Frame currentFrame = root;
            String stack[] = object.getString("message").split(System.lineSeparator());

            for(int i=stack.length-1; i>=0; i--){
                String frameString = stack[i];
                if(frameString.startsWith("\t")){//is a frameStack
                    frameString = frameString.trim();
                    //trim the (Class.java:lineNumber)
                    int parenIdx=-1;
                    if( (parenIdx=frameString.indexOf("("))>-1){
                        frameString = frameString.substring(0,parenIdx);
                    }
                    int frameId = frameIndexer.add(frameString);
                    currentFrame = currentFrame.addChild(frameId);

                }else{ // not a stack frame so marks the end of a stack
                    currentFrame = root;
                }
            }

            frameFrequency.add(root);


            Function<Frame,String> toStringFunction = (a)->{
                if(a.getId()<0){
                    String uniqueString = a.toString();
                    return "[ "+frameFrequency.count(a)+" ]";
                }
                return frameIndexer.get(a.getId())+" : "+a.getCount();
            };
            Function<Frame,List<Frame>> getChildren = (b)->{
                return b.getChildren();
            };


            //System.out.println(AsciiArt.printTree(root,getChildren,toStringFunction));
        }
    }

    public static void main(String[] args) {
        NettyLeak nl = new NettyLeak();

        String filePath = null;
//        filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/log/server.log";
//        filePath = "/home/wreicher/specWork/server.246Y.log";
//        filePath = "/home/wreicher/runtime/wildfly-10.0.0.Final-invm/standalone/log/server.log";
//        filePath = "/home/wreicher/specWork/6C/server.log";
        filePath = "/home/wreicher/runtime/wildfly-10.0.0.Final-pool/standalone/log/server.log";
        String outPath = "/home/wreicher/runtime/wildfly-10.0.0.Final-pool/standalone/log/server.tree.log";
        ServerLogFactory slf = new ServerLogFactory();
        Parser p = slf.newLogEntryParser();
        p.add(nl);
        TextLineReader r = new TextLineReader();
        r.addParser(p);
        r.read(filePath);

        Indexer<String> frames = nl.getFrameIndexer();
        Counters<Frame> frameFrequency = nl.getFrameFrequency();
        List<Frame> uniqueFrameList = frameFrequency.entries();

        Function<Frame,String> toStringFunction = (a)->{
            if(a.getId()<0){
                return "[ "+frameFrequency.count(a)+" ]";
            }
            return frames.get(a.getId())+" : "+a.getCount();
        };
        Function<Frame,List<Frame>> getChildren = (b)-> b.getChildren();


        Collections.sort( uniqueFrameList,(a, b)-> frameFrequency.count(a)-frameFrequency.count(b) );
        Collections.reverse(uniqueFrameList);

        AsciiArt.TREE_OFFSET_SPACE     = " ";
        AsciiArt.TREE_OFFSET_SUB_CHILD = "│";
        AsciiArt.TREE_CHILD            = "├";
        AsciiArt.TREE_CHILD_LAST       = "└";

        try (Writer writer = new FileWriter(outPath)){
            for(Frame x : uniqueFrameList){
                //System.out.println(frameFrequency.count(x)+":"+x.getChildren().get(0).getCount());
                String tree = AsciiArt.printTree(x,getChildren,toStringFunction);
                System.out.print(tree);
                writer.write(tree);
                System.out.println("\n");
                writer.write("\n");
                writer.flush();
                //System.out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}

