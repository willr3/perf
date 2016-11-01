package perf.util.json;

/**
 * Created by wreicher
 */
public interface Jsonable<T> {

    String toJsonString();
    T fromJsonString(String json);
}
