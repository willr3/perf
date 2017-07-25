package perf.analysis;

import perf.parse.*;
import perf.parse.reader.TextLineReader;
import perf.util.AsciiArt;
import perf.util.Counters;
import perf.util.HashedList;
import perf.util.json.Jsons;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wreicher
 */
public class Pmap implements JsonConsumer {

    static TextLineReader r = new TextLineReader();
    static Parser p = new Parser();

    static {
        p.add(new Exp("address","(?<address>[0-9a-f]{16})").eat(Eat.Match).set(Merge.NewStart)
                .add(new Exp("Kbytes", "\\s+(?<Kbytes>\\d+)").eat(Eat.Match).set("Kbytes", Value.Number))
                .add(new Exp("RSS", "\\s+(?<RSS>\\d+)").eat(Eat.Match).set("RSS", Value.Number))
                .add(new Exp("Dirty", "\\s+(?<Dirty>\\d+)").eat(Eat.Match).set("Dirty", Value.Number))
                .add(new Exp("Mode", "\\s+(?<Mode>[\\-rwxs]{5})").eat(Eat.Match))
                .add(new Exp("Mapping", "\\s+(?<Mapping>.*)").eat(Eat.Match))
        );

        r.addParser(p);
    }

    public static Pmap readFile(String file){
        p.clearConsumers();

        Pmap rtrn = new Pmap();

        p.add(rtrn);
        r.read(file);

        p.clearConsumers();
        return rtrn;
    }

    Counters<String> mappingKBytes = new Counters<>();
    Counters<String> mappingRss = new Counters<>();
    Counters<String> mappingCount = new Counters<>();
    Counters<String> anonKBytesCount = new Counters<>();
    Counters<String> anonRss = new Counters<>();

    BigInteger minAddress = null;
    BigInteger maxAddress = null;
    BigInteger runnerUpAddress = null;

    public static void main(String[] args) {

        Parser p = new Parser();


        Pmap orig =  readFile("/home/wreicher/perfWork/openshift/openjdk/pmapx.MALLOC_ARENA_MAX.insurance.7.1.0.DR17.log");
        Pmap after = readFile("/home/wreicher/perfWork/openshift/openjdk/pmapx.MALLOC.insurance.postRun.postGc.log");


        System.out.println("orig: "+orig.minAddress.toString(10)+" - "+orig.runnerUpAddress.toString(10)+" = "+orig.runnerUpAddress.subtract(orig.minAddress));
        System.out.println("post: "+after.minAddress.toString(10)+" - "+after.runnerUpAddress.toString(10)+" = "+after.runnerUpAddress.subtract(after.minAddress));
        System.out.println(Long.MAX_VALUE);
        System.exit(0);

        List<String> entries = orig.mappingKBytes.entries();
        entries.sort((a,b)-> orig.mappingKBytes.count(a)-orig.mappingKBytes.count(b));

        HashedList<String> allEntries = new HashedList<>();
        entries.stream().forEach(a->allEntries.add(a));
        after.mappingKBytes.entries().stream().forEach(a->allEntries.add(a));

        int size = allEntries.toList().stream().map(s->s.length()).max(Integer::compare).get();

        for(String mapping : allEntries.toList()){
            long origKBytes = orig.mappingKBytes.count(mapping);
            long origRss = orig.mappingRss.count(mapping);
            long postKBytes = after.mappingKBytes.count(mapping);
            long postRss = after.mappingRss.count(mapping);
            long diff = postRss - origRss;
            System.out.printf("%"+size+"s │%10d :%10d │%10d :%10d │ [%s] %n",
                    mapping,
                    origKBytes,
                    origRss,
                    postKBytes,
                    postRss,
                    diff > 0 ?
                        AsciiArt.ANSI_RED + diff + AsciiArt.ANSI_RESET :
                        diff == 0 ?
                            "0" :
                            AsciiArt.ANSI_GREEN + diff + AsciiArt.ANSI_RESET);
        }
        System.out.printf("%"+size+"s │%10d :%10d │%10d :%10d │ [%s] %n",
                "",
                orig.mappingKBytes.sum(),
                orig.mappingRss.sum(),
                after.mappingKBytes.sum(),
                after.mappingRss.sum(),
                "");

        HashedList<String> anonAllEntries = new HashedList<>();
        orig.anonKBytesCount.entries().stream().forEach(e->anonAllEntries.add(e));
        after.anonKBytesCount.entries().stream().forEach(e->anonAllEntries.add(e));

        List<String> anonEntries = new ArrayList();
        anonEntries.addAll(anonAllEntries.toList());
        anonEntries.sort((a,b)->Integer.parseInt(a) - Integer.parseInt(b));

        String pattern = "%10s [%4s %8s] [%4s %8s] %s %n";

        for(String anonSize : anonEntries){
            long anonSizeLong = Long.parseLong(anonSize);
            long origSizes = orig.anonKBytesCount.count(anonSize);
            long origRss = orig.anonRss.count(anonSize);

            long postSizes = after.anonKBytesCount.count(anonSize);
            long postRss = after.anonRss.count(anonSize);

            long rssDiff = postRss - origRss;

            System.out.printf(pattern,
                    anonSize,
                    origSizes,
                    AsciiArt.printKMG(1024*origRss),
                    postSizes,
                    AsciiArt.printKMG(1024*postRss),
                    rssDiff > 0 ?
                        AsciiArt.ANSI_RED + AsciiArt.printKMG(1024*rssDiff) + AsciiArt.ANSI_RESET :
                        rssDiff == 0 ?
                            "0" :
                        AsciiArt.ANSI_GREEN + "-"+AsciiArt.printKMG(1024*Math.abs(rssDiff)) + AsciiArt.ANSI_RESET);
        }
        System.out.printf("TOTALS %n");
        System.out.printf(pattern,
                anonEntries.size(),
                orig.anonKBytesCount.sum(),
                AsciiArt.printKMG(1024*orig.anonRss.sum()),
                after.anonKBytesCount.sum(),
                AsciiArt.printKMG(1024*after.anonRss.sum()),
                AsciiArt.printKMG(1024*(after.anonRss.sum()-orig.anonRss.sum())));

    }

    @Override
    public void consume(Jsons json) {
        String mapping = json.getString("Mapping");
        //long addr = Long.parseLong(json.getString("address"),16);
        BigInteger address = new BigInteger(json.getString("address"),16);

        if(minAddress == null){
            minAddress = address;
        }
        if(maxAddress == null){
            maxAddress = address;
        }
        if(runnerUpAddress == null){
            runnerUpAddress = address;
        }
        if(minAddress.compareTo(address) > 0){
            minAddress = address;
        }
        if(maxAddress.compareTo(address) < 0){
            runnerUpAddress = maxAddress;
            maxAddress = address;
        }


        long kBytes = json.getLong("Kbytes");
        long Rss = json.getLong("RSS");
        if(mapping.equals("[ anon ]")){
            anonKBytesCount.add(""+kBytes);
            anonRss.add(""+kBytes,(int)Rss);
        }
        mappingKBytes.add(mapping,(int)kBytes);
        mappingRss.add(mapping,(int)Rss);
        mappingCount.add(mapping);

    }
}
