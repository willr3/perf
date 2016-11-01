package perf.parse;

import perf.util.json.Jsons;

/**
 *
 */
public interface JsonConsumer {

    default public void start(){}
    default public void close(){}
    public void consume(Jsons object);

}
