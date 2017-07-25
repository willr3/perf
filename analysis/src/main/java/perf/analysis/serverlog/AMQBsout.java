package perf.analysis.serverlog;

import perf.parse.JsonConsumer;
import perf.parse.Parser;
import perf.parse.factory.ServerLogFactory;
import perf.parse.reader.TextLineReader;
import perf.util.AsciiArt;
import perf.util.Counters;
import perf.util.HashedLists;
import perf.util.json.Jsons;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class AMQBsout implements JsonConsumer {


    private Matcher incM = Pattern.compile("ServerMessageimpl.incrementRefCount (\\d+).*").matcher("");
    private Matcher decM = Pattern.compile("ServerMessageimpl.decrementRefCount (\\d+).*").matcher("");
    private Counters<Integer> inc = new Counters<>();
    private Counters<Integer> dec = new Counters<>();


    @Override
    public void consume(Jsons object) {
        if(object.has("message") && object.getString("message").contains("AMQB.re")){
            if(incM.reset(object.getString("message")).matches()){
                int hash = Integer.parseInt(incM.group(1));
                inc.add(hash);
            }
            else if(decM.reset(object.getString("message")).matches()){
                int hash = Integer.parseInt(decM.group(1));
                dec.add(hash);
            }
        }
    }


    public static void main(String[] args) {
        AMQBsout amqbs = new AMQBsout();

        String filePath = "/home/wreicher/perfWork/byteBuffer/11N-incrementDecrementTrace/server.log";

        ServerLogFactory slf = new ServerLogFactory();
        Parser p = slf.newLogEntryParser();
        p.add(amqbs);
        TextLineReader r = new TextLineReader();
        r.addParser(p);
        r.read(filePath);

        final AtomicInteger leaks = new AtomicInteger(0);
        amqbs.inc.entries().forEach((hash)->{
            int i = amqbs.inc.count(hash);
            int d = amqbs.dec.count(hash);
            if( i != d ){
                System.out.println(AsciiArt.ANSI_RED+hash+" inc="+i+" dec="+d+AsciiArt.ANSI_RESET);
            }
        });
        System.out.println(leaks.get());
    }
}
