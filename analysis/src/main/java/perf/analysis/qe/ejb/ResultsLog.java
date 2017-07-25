package perf.analysis.qe.ejb;

import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.JsonConsumer;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.Rule;
import perf.parse.Value;
import perf.parse.reader.TextLineReader;
import perf.util.json.Jsons;

/**
 * Created by wreicher
 */
public class ResultsLog implements JsonConsumer {

    public static void main(String[] args) {
        String filePath = "/home/wreicher/code/redhat/eap-tests-ejb/performance-test-eap7/results.log";
        filePath = "/home/wreicher/perfWork/eap-tests-ejb/client2.results.log";
        ResultsLog resultsLog = new ResultsLog();

        Parser p = new Parser();

        p.add(new Exp("timestamp","(?<timestamp>\\d{2}:\\d{2}:\\d{2},\\d{3})").eat(Eat.Match).set(Merge.NewStart)
            .add(new Exp("level", " (?<level>[A-Z]+)\\s+").eat(Eat.Match))
            .add(new Exp("logger", "\\[(?<logger>[^\\]]+)\\]\\s+").eat(Eat.Match)));
        p.add(new Exp("waitTime", "total time spent waiting=(?<waitTime>\\d+)ms").eat(Eat.Match).set("waitTime",Value.Number));
        p.add(new Exp("clients", "collecting results.. clients=(?<clients>\\d+);\\s+").eat(Eat.Match).set("clients",Value.Number)
            .add(new Exp("duration","average call duration=(?<duration>\\d+\\.\\d+)ms;\\s+").eat(Eat.Match).set("duration",Value.Number))
            .add(new Exp("success","successful calls=(?<success>\\d+);\\s+").eat(Eat.Match).set("success",Value.Number))
            .add(new Exp("perClient","calls per client=(?<perClient>\\d+\\.\\d+);\\s+").eat(Eat.Match).set("perClient",Value.Number))
            .add(new Exp("errors","total errors so far=(?<errors>\\d+)").eat(Eat.Line).set("errors",Value.Number)));
        p.add(new Exp("error", "Error encountered:\\s+")
            .group("error").eat(Eat.Match)
                .add(new Exp("count","errors so far=(?<count>\\d+),\\s+").eat(Eat.Match).set("count",Value.Number))
                .add(new Exp("threshold",", error threshold=(?<threshold>\\d+)").eat(Eat.Match).set("threshold",Value.Number))
                .add(new Exp("message","(?<message>.+)")));
        p.add(new Exp("frame","\\s+at (?<frame>[^\\(]+)").eat(Eat.Match)
            .group("stack").set(Merge.Entry)
                //.debug()
                .add(new Exp("nativeMethod", "\\((?<nativeMethod>Native Method)\\)").eat(Eat.Line).set("nativeMethod", Value.BooleanKey))
                .add(new Exp("lineNumber","\\((?<file>[^:]+):(?<line>[^\\)]+)\\)").eat(Eat.Line).set("line",Value.Number))
                .add(new Exp("unknownSource","\\((?<unknownSource>Unknown Source)\\)").eat(Eat.Line).set("unknownSource", Value.BooleanKey)));
        p.add(new Exp("causedBy","Caused by: (?<exception>[^:]+): (?<message>.+\n?)")
                //.group("stack")
                .group("causedBy")
                    .set(Rule.PushContext).eat(Eat.Line));
        p.add(new Exp("more","\\s+\\.\\.\\. (?<stackRemainder>\\d+) more")
                .set("stackRemainder",Value.Number)
                .eat(Eat.Line));
        p.add(new Exp("message", "(?<message>.*)").eat(Eat.Match));
        p.add(resultsLog);

        TextLineReader r = new TextLineReader();
        r.addParser(p);
        r.read(filePath);


    }

    @Override
    public void consume(Jsons object) {
        if(object.has("clients")){
            System.out.printf("%d,%d,%.3f,%.3f%n",
                    object.getLong("clients"),
                    object.getLong("success"),
                    object.getDouble("duration"),
                    object.getDouble("perClient"));

        }

    }


}
