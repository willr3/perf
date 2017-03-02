/**
 * Created by wreicher
 */

import oracle.jrockit.jfr.parser.ChunkParser;
import oracle.jrockit.jfr.parser.FLRValueInfo;
import oracle.jrockit.jfr.parser.Parser;
import perf.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RunJmc {

    public static void main(String[] args) {
        String jfrPath = "/home/wreicher/perfWork/byteBuffer/322U/flight_record_20170209_161252.jfr"; //405M

        //Loads it all into memory
        System.out.println("record "+jfrPath);
//        FlightRecording recording = FlightRecordingLoader.loadFile(new File(jfrPath));
//        IView view = recording.createView();
//
//        int count = 0;
//        for (IEvent event : view) {
//            count++;
//        }
//        System.out.println("Fount " + count + " events");


        NavigableMap<Long,String> map = new TreeMap<>();

        System.out.println("parser");

        File recordingFile = new File(jfrPath);
        Parser parser = null;
        try {

            long startTime = System.currentTimeMillis();
            parser = new Parser(recordingFile);
            System.out.println(" made the parser");
            AtomicInteger count = new AtomicInteger(0);

            Iterator<ChunkParser> chunkIter = parser.iterator();

//            Spliterator<ChunkParser> spliterator = parser.spliterator();
//            System.out.println("characteristics "+spliterator.characteristics());
//            System.out.println("estimateSize "+spliterator.estimateSize());
//            System.out.println("exactSize "+spliterator.getExactSizeIfKnown());
//
//
//            Spliterator<ChunkParser> otherSplit = spliterator.trySplit();
//
//            System.out.println("split ");
//            System.out.println("characteristics "+spliterator.characteristics());
//            System.out.println("estimateSize "+spliterator.estimateSize());
//            System.out.println("exactSize "+spliterator.getExactSizeIfKnown());
//            System.out.println("otherSplit");
//            System.out.println("characteristics "+otherSplit.characteristics());
//            System.out.println("estimateSize "+otherSplit.estimateSize());
//            System.out.println("exactSize "+otherSplit.getExactSizeIfKnown());



            HashSet<String> names = new HashSet<>();
            HashSet<String> paths = new HashSet<>();
            HashSet<String> classNames = new HashSet<>();

            chunkIter.forEachRemaining((chunkParser)->{
                chunkParser.forEach((flrEvent)->{
                    count.incrementAndGet();
                    String path = flrEvent.getPath();
                    long timestamp = flrEvent.getTimestamp();
                    flrEvent.getValues().forEach((v)->{classNames.add(v.getClass().getName());});
                    if(path.equals("vm/runtime/thread_dump")){
                      List<? extends FLRValueInfo> valueInfos = flrEvent.getValueInfos();
                        //Object zero = flrEvent.getValue(0);
                        //System.out.println(" zero "+zero.getClass().getName());
                        //System.out.println(timestamp);
                        //System.out.println(zero);
                        //System.out.println(valueInfos.size());
                        //valueInfos.forEach((info)-> System.out.println("  "+info.getClass().getName()));
                    }
                });
            });
            System.out.println("classNames "+classNames.size());
            classNames.forEach(System.out::println);

//            while (chunkIter.hasNext()) {
//                ChunkParser chunkParser = chunkIter.next();
//
//                for (FLREvent event : chunkParser) {
//                    paths.add(event.getPath());
//                    names.add(event.getName());
//                    if (event.getName().equals("Thread Dump")) {
//                        System.out.println(event.getClass());
//                        event.getValueInfos().stream().forEach((i) -> System.out.println(i.getClass().getName()));
//                        event.getValues().stream().forEach((e) -> System.out.println(e.getClass()));
//                    }
//                    if(event.getPath().equals("vm/runtime/thread_dump")){
//                        long timestamp = event.getTimestamp();
//                        event.g
//                    }
//                    count++;
//                    //System.out.println(event.toString());
//                }
//            }
            parser.close();
            System.out.println("Found " + count.get() + " events");
            System.out.println(names.size()+" names");
            names.stream().forEach((c)-> System.out.println("  "+c));
            System.out.println(paths.size()+" paths");
            paths.stream().forEach((c)-> System.out.println("  "+c));


            long stopTime = System.currentTimeMillis();

            System.out.println("elapsed "+ StringUtil.durationToString(stopTime - startTime));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}

