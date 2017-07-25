package perf.analysis.jstack;

import perf.parse.Parser;
import perf.parse.factory.JStackFactory;
import perf.parse.reader.TextLineReader;
import perf.util.AsciiArt;
import perf.util.Counters;
import perf.util.StringUtil;
import perf.util.file.FileUtility;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class ThreadGroups {

    static class HashHashList {
        HashMap<String,Map<String,Integer>> data;

        HashHashList(){
            data = new HashMap<>();
        }


        public void add(String key1,String key2,Integer value){
            if(!data.containsKey(key1)){
                data.put(key1,new HashMap<>());
            }
            data.get(key1).put(key2,value);
        }
        public Set<String> getKeys(){return data.keySet();}
        public Set<String> getKeys(String key1){
            if(!data.containsKey(key1)){
                return Collections.emptySet();
            }else{
                return data.get(key1).keySet();
            }
        }
        public int get(String key1,String key2){
            if(data.containsKey(key1) && data.get(key1).containsKey(key2)){
                return data.get(key1).get(key2);
            }else{
                return 0;
            }
        }
    }

    public static void main(String[] args) {

        List<Pattern> patternList = Arrays.asList(
                Pattern.compile("(?<name>RMI TCP Connection)\\(\\d+\\)-.*"),
                Pattern.compile("(?<name>GC task thread)#\\d+ .*"),
                Pattern.compile("(?<name>Timer)-\\d+"),
                Pattern.compile("(?<name>XNIO-\\d+ task)-\\d+"),
                Pattern.compile("(?<name>XNIO-\\d+ I/O)-\\d+"),
                Pattern.compile("(?<name>SMAgent)\\d+"),
                Pattern.compile("(?<name>C\\d+ CompilerThread)\\d+"),
                Pattern.compile("(?<name>SM_[^_]+)_\\d+_EHID_\\d+"),
                Pattern.compile("Thread-\\d+ \\((?<name>ActiveMQ-client-global-threads)\\)"),
                Pattern.compile("Thread-\\d+ \\((?<name>ActiveMQ-client-global-scheduled-threads)\\)"),
                Pattern.compile("Thread-\\d+ \\((?<name>ActiveMQ-client-netty-threads)\\)"),
                Pattern.compile("Thread-\\d+ \\((?<name>ActiveMQ-client-factory-pinger-threads)-\\d+\\)"),
                Pattern.compile("Thread-\\d+ \\((?<name>ActiveMQ-client-factory-threads)-\\d+\\)")

        );


        String path = "/home/wreicher/perfWork/amq/jdbc/save/run-50-1500652935888/benchclient1";
        path = "/home/wreicher/perfWork/amq/jdbc/run-50-1500578282584/benchclient1";
        path = "/home/wreicher/perfWork/amq/jdbc/";

        List<String> jstacks = FileUtility.getFiles(path,"jstack",true);
        jstacks.sort(String::compareTo);
        TextLineReader reader = new TextLineReader();

        JStackFactory jStackFactory = new JStackFactory();
        Parser fileStartParser = jStackFactory.newFileStartParser();
        Parser threadParser = jStackFactory.newThreadParser();



        Counters<String> pools = new Counters<>();

        HashHashList poolCounts = new HashHashList();

        threadParser.add((json)->{
            if(json.has("name")){
                String name = json.getString("name");

                Optional<Matcher> matcher = patternList.stream().map((p)-> p.matcher(name)).filter((m)->m.matches()).findFirst();

                if(matcher.isPresent()){
                    String matcherName = matcher.get().group("name");
                    pools.add(matcherName);
                }else{
                    pools.add(name);
                }

            }

        });

        reader.addParser(fileStartParser);
        reader.addParser(threadParser);



        for(String jstack : jstacks){
            long start = System.currentTimeMillis();
            reader.read(jstack);
            long stop = System.currentTimeMillis();
            System.out.println(jstack+" in "+ StringUtil.durationToString((stop-start))+" size="+pools.size());

            //names.stream().forEach(System.out::println);
            List<String> poolList = pools.entries();
            poolList.sort(String::compareTo);
            poolList.forEach(pool->poolCounts.add(pool,jstack,pools.count(pool)));
            pools.clear();
        }

        Set<String> poolNames = poolCounts.getKeys();
        int nameSize = poolNames.stream().mapToInt((name)->name.length()).max().getAsInt();
        for(String poolName : poolNames){
            System.out.print(String.format("%"+(nameSize+1)+"s",poolName));
            for(String jstack : jstacks){
                int amount = poolCounts.get(poolName,jstack);
                System.out.printf("%5d",amount);
            }
            System.out.printf("%n");
        }

    }

}
