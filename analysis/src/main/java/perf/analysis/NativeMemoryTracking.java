package perf.analysis;

import org.json.JSONObject;
import perf.parse.*;
import perf.parse.reader.TextLineReader;
import perf.util.json.Jsons;

import java.util.concurrent.atomic.LongAdder;

/**
 * Created by wreicher
 */
public class NativeMemoryTracking implements JsonConsumer{

    static TextLineReader r = new TextLineReader();
    static Parser p = new Parser();

    Jsons json = new Jsons(new JSONObject());

    static {
        p.add(new Exp("totals","Total: reserved=(?<reservedKb>\\d+)KB, committed=(?<committedKb>\\d+)KB")
            .eat(Eat.Line)
            .set(Merge.NewStart));
        p.add(new Exp("section","-\\s+(?<category>.+?) \\(reserved=(?<reservedKb>\\d+)KB, committed=(?<committedKb>\\d+)KB\\)")
                .eat(Eat.Line)
                .group("section")
                .enables("nmt_section")
                .set(Merge.Entry)
                .set(Rule.PushContext));
        p.add(new Exp("count","\\s+\\((?<unit>[^ #:=]+)\\s+#(?<count>\\d+)\\)")
                .eat(Eat.Line)
                .requires("nmt_section"));
        p.add(new Exp("malloc","\\s+\\(malloc=(?<mallocKb>\\d+)KB #(?<mallocCount>\\d+)\\)")
                .eat(Eat.Line)
                .requires("nmt_section"));
        p.add(new Exp("malloc","\\s+\\(malloc=(?<mallocKb>\\d+)KB\\)")
                .eat(Eat.Line)
                .requires("nmt_section"));
        p.add(new Exp("arena","\\s+\\(arena=(?<arenaKb>\\d+)KB #(?<arenaCount>\\d+)\\)")
                .eat(Eat.Line)
                .requires("nmt_section"));
        p.add(new Exp("mmap","-\\s+\\(mmap: reserved=(?<mmapReservedKb>\\d+)KB, committed=(?<mmapCommittedKb>\\d+)KB\\)")
                .eat(Eat.Line)
                .requires("nmt_section"));
        p.add(new Exp("BLANK","^\\s+$")
                .eat(Eat.Line)
                .requires("nmt_section")
                .disables("nmt_section")
                .set(Rule.PopContext));
        r.addParser(p);
    }

    public static NativeMemoryTracking readFile(String file){
        p.clearConsumers();

        NativeMemoryTracking rtrn = new NativeMemoryTracking();

        p.add(rtrn);
        r.read(file);

        p.clearConsumers();
        return rtrn;
    }

    @Override
    public void consume(Jsons object) {
        System.out.println(object.toString(2));
        this.json = object;

    }

    public static void main(String[] args) {
        NativeMemoryTracking nativeMemoryTracking = readFile("/home/wreicher/perfWork/openshift/local/oracle/DR17/ootb/VM.native_memory.detail.20446.log");
        LongAdder reserved = new LongAdder();
        LongAdder committed = new LongAdder();

        int categorySize = nativeMemoryTracking.json.getJsonArray("section").toList().stream().map((section)->section.getString("category").length()).max(Integer::compare).get();

        String pattern = "%"+categorySize+"s %10s %10s %n";

        System.out.printf(pattern,
                "category",
                "reserved",
                "committed");
        nativeMemoryTracking.json.getJsonArray("section").forEachJson((section)->{
            long reservedKb = section.getLong("reservedKb");
            long committedKb = section.getLong("committedKb");

            reserved.add(reservedKb);
            committed.add(committedKb);

            System.out.printf(pattern,
                    section.getString("category"),
                    reservedKb,
                    committedKb);
        });
        System.out.println("┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅┅");
        System.out.printf(pattern,
                "TOTAL",
                reserved.sum(),
                committed.sum());
        System.out.printf(pattern,
                "NMT",
                nativeMemoryTracking.json.getLong("reservedKb"),
                nativeMemoryTracking.json.getLong("committedKb"));
    }
}
