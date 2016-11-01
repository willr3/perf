package perf.parse.consumers;

import perf.parse.JsonConsumer;
import perf.util.json.Jsons;

import java.util.HashSet;
import java.util.function.Function;

/**
 * Created by wreicher
 */
public class FilterConsumer implements JsonConsumer {


    private HashSet<JsonConsumer> consumers;
    private Function<Jsons,Boolean> filter;

    public FilterConsumer(Function<Jsons,Boolean> filter){
        this.filter = filter;
        this.consumers = new HashSet<>();
    }

    public void addConsumer(JsonConsumer consumer){
        this.consumers.add(consumer);
    }
    @Override
    public void consume(Jsons object) {
        if( filter.apply(object) ){
            for(JsonConsumer consumer : consumers){
                consumer.consume(object);
            }
        }
    }
}
