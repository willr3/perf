package perf.parse.factory;

import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.JsonConsumer;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Rule;
import perf.parse.Value;
import perf.parse.reader.TextLineReader;
import perf.util.AsciiArt;
import perf.util.json.Jsons;

/**
 *
 */
public class OpenJdkGcFactory {

    public Exp newTimestampPattern(){
        return new Exp("timestamp","^(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}-\\d{4}): ").set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newElapsedPattern(){
        return new Exp("elapsed","^(?<elapsed>\\d+\\.\\d{3}): ")
            .set("elapsed", Value.Number).set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newStopTimePattern() {
        return new Exp("stopTime","Total time for which application threads were stopped: (?<threadpause>\\d+\\.\\d+) seconds")
                .eat(Eat.Line)
                .add(new Exp("stoppingThread","Stopping threads took: (?<theadStopping>\\d+\\.\\d+) seconds"));
    }
    public Exp newUserSysRealPattern(){
        return new Exp("usersysreal","\\[Times: user=(?<user>\\d+\\.\\d{2}) sys=(?<sys>\\d+\\.\\d{2}), real=(?<real>\\d+\\.\\d{2}) secs\\]").group("times").eat(Eat.Match);
    }
    public Exp newRegionPattern(){
        return new Exp("region","\\[(?<name>\\w+): (?<pregc>\\d+[KMG]?)->(?<postgc>\\d+[KMG]?)\\((?<size>\\d+[KMG]?)\\)\\][ ,]")
            .group("region")
            .key("name") // key name means we do not need to Merge as new Entry
            //.set(Exp.Merge.Entry)
            .set(Rule.Repeat)
            .eat(Eat.Match)
            .set("pregc", Value.KMG)
            .set("postgc", Value.KMG)
            .set("size", Value.KMG);
    }
    public Exp newSurvivorThresholdPattern(){
        return new Exp("survivorthreshold","Desired survivor size (?<survivorsize>\\d+) bytes, new threshold (?<threshold>\\d+) \\(max (?<maxThreshold>\\d+)\\)");
    }
    public Exp newGCReasonPattern(){
        return new Exp("gcreason","\\((?<gcreason>[\\w ]+)\\) ")
            .eat(Eat.Match);
    }
    public Exp newGCTimePattern(){
        return new Exp("gctime",", (?<gctime>\\d+\\.\\d+) secs\\] ")
                .set("gctime",Value.Number);
    }
    public Exp newHeapSizePattern(){
        return new Exp("heapsize","(?<pregc>\\d+[KMG]?)->(?<postgc>\\d+[KMG]?)\\((?<size>\\d+[KMG]?)\\)")
            .group("heap")
            .eat(Eat.Match)
            .set("pregc", Value.KMG)
            .set("postgc", Value.KMG)
            .set("size", Value.KMG);
    }
    public Exp newFullGCPattern(){
        return new Exp("FullGC","^\\[(?<gctype>Full GC) ").set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newGCPattern(){
        return new Exp("GC","^\\[(?<gctype>GC) ").set(Merge.NewStart).eat(Eat.Match);
    }
    public Exp newPolicyPattern(){
        return new Exp("policy","^(?<key>\\w+)::(?<value>[^:]+):").set("key","value").group("policy").set(Merge.Entry).eat(Eat.Match)
            .add(new Exp("K,Vnumber", "  (?<key>\\w+): (?<value>\\d+\\.?\\d*)").set("key", "value").set(Rule.Repeat));
    }
    public Parser newGcParser(){
        Parser p = new Parser();
        p.add(newTimestampPattern());
        p.add(newElapsedPattern());
        p.add(newGCPattern());
        p.add(newFullGCPattern());
        p.add(newStopTimePattern());
        p.add(newUserSysRealPattern());
        p.add(newRegionPattern());
        p.add(newSurvivorThresholdPattern());
        p.add(newGCReasonPattern());
        p.add(newGCTimePattern());
        p.add(newHeapSizePattern());
        p.add(newPolicyPattern());
        return p;
    }

    public static void main(String[] args) {
        String filePath="";
        //String filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/client1.specjms.verbose-gc-sm.gclog";
        //String filePath = "/home/wreicher/specWork/reentrant/reentrant-aio-196/server_20160114_221048.gclog";
        //String filePath = "/tmp/server_2lc_setup.gclog";
        //String filePath = "/home/wreicher/specWork/server_20140902_122635.152K.gclog";
        filePath = "/home/wreicher/specWork/server.255Z.gclog";
        filePath = "/home/wreicher/specWork/server.256A.gclog";
        filePath = "/home/wreicher/specWork/server.256B.gclog";
        filePath = "/home/wreicher/specWork/server.256B.gclog";
        filePath = "/home/wreicher/specWork/server.256I.gclog";
        TextLineReader r = new TextLineReader();
        OpenJdkGcFactory f = new OpenJdkGcFactory();
        Parser p = f.newGcParser();


        long gargage = 0;

        //fullGC
        StringBuilder fullGc = new StringBuilder();
        p.add(new JsonConsumer(){

            @Override
            public void consume(Jsons object) {
                if(object.has("gctype")){
                    if(object.getString("gctype").contains("Full GC")){
                        fullGc.append("■");
                    }else{
                        fullGc.append(" ");
                    }
                }
            }
        });

        //gcOverhead
        StringBuilder gcOverhead = new StringBuilder();
        p.add(new JsonConsumer() {
            private double elapsed = 0.0;
            @Override
            public void consume(Jsons object) {
                if(object.has("elapsed") && object.has("gctime")){
                    double newElapsed = object.getDouble("elapsed");
                    double gcTime = object.getDouble("gctime");
                    double overHead = gcTime/(newElapsed-elapsed);

                    gcOverhead.append( AsciiArt.horiz( overHead , 1 ) );
                    elapsed = newElapsed;
                }
            }
        });
        //gcbars
        long totalSize=12l*1024l*1024l*1024l; // 1GB
        System.out.println("toalSize="+totalSize);
        int maxWidth = 20;
        double ratio = 1.0*totalSize/maxWidth;
        p.add(new JsonConsumer() {
            @Override
            public void consume(Jsons object) {
                if(object.has("region")){
                    Jsons region = object.getJson("region");
                    String youngGen="";
                    if(region.has("PSYoungGen")){
                        long size = region.getJson("PSYoungGen").getLong("size");
                        long pregc = region.getJson("PSYoungGen").getLong("pregc");
                        long postgc = region.getJson("PSYoungGen").getLong("postgc");
                        youngGen = AsciiArt.vert(postgc,totalSize,maxWidth,true);

                    }
                    String oldGen = " ";
                    if(region.has("ParOldGen")){

                        long size = region.getJson("ParOldGen").getLong("size");
                        long pregc = region.getJson("ParOldGen").getLong("pregc");
                        long postgc = region.getJson("ParOldGen").getLong("postgc");
                        oldGen = AsciiArt.vert(postgc,totalSize,maxWidth,true);
                    }
                    String heap = " ";
                    if(object.has("heap")){
                        long size = object.getJson("heap").getLong("size");
                        long pregc = object.getJson("heap").getLong("pregc");
                        long postgc = object.getJson("heap").getLong("postgc");

                        heap = AsciiArt.vert(postgc,totalSize,maxWidth,true);
                    }

                    //System.out.println("maxWidth="+maxWidth+" youngGen.length="+youngGen.length());

                    System.out.printf("|%"+maxWidth+"s|%"+maxWidth+"s|%"+maxWidth+"s|\n", youngGen, oldGen, heap);

                }
            }
        });

        //postgc heap usage v size
        StringBuilder pgc = new StringBuilder();
        p.add(new JsonConsumer() {
            @Override
            public void start() {}

            @Override
            public void consume(Jsons object) {
                if(object.has("heap") && object.getJson("heap").has("postgc")){
                    long postgc = object.getJson("heap").getLong("postgc");
                    long size = object.getJson("heap").getLong("size");
                    pgc.append(AsciiArt.horiz(postgc,size));
                }
            }

            @Override
            public void close() {}
        });
        r.addParser(p);

        //p.onLine(new CheatChars("2015-11-13T11:16:55.950-0600: 25.617: [GC (Allocation Failure) [PSYoungGen: 269312K->11928K(309248K)] 316281K->58905K(658944K), 0.0059238 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]\n"));
        //p.close();
        //p.onLine(new CheatChars("2015-11-13T11:16:37.515-0600: 7.182: [Full GC (Metadata GC Threshold) [PSYoungGen: 30352K->0K(306688K)] [ParOldGen: 17229K->46969K(349696K)] 47582K->46969K(656384K), [Metaspace: 21093K->21093K(1069056K)], 0.2619827 secs] [Times: user=0.89 sys=0.02, real=0.27 secs]\n"));
        //p.close();
        //p.onLine(new CheatChars("2015-11-13T11:16:55.950-0600: 25.617: [GC (Allocation Failure) [PSYoungGen: 269312K->11928K(309248K)] 316281K->58905K(658944K), 0.0059238 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]\n"));

        r.read(filePath);
        System.out.println("heap.postgc │"+pgc.toString());
        System.out.println("gcOverhead  │"+gcOverhead.toString());
        System.out.println("full GCs    │"+fullGc.toString());


    }
}
