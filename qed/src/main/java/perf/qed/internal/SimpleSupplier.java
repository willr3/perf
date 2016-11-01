package perf.qed.internal;

import java.util.function.Supplier;

/**
 * Created by wreicher
 */
public class SimpleSupplier<T> implements Supplier<T> {

    public static final SimpleSupplier<String> EMPTY_STRING = new SimpleSupplier<String>(){
        @Override
        public String get(){return "";}
    };

    private T value;

    public SimpleSupplier(){
        this(null);
    }
    public SimpleSupplier(T def){
        value = def;
    }
    public void set(T value){
        this.value = value;
    }
    @Override
    public T get() {
        return value;
    }
}
