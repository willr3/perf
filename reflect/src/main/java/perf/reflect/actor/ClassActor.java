package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 * A lamba-friendly interface for reflecting on just the Class.
 */
public interface ClassActor extends Actor {
    public abstract String apply(Class knownClass);

    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (knownClass != null && field == null && method == null && constructor == null) {
            return apply(knownClass);
        } else {
            return null;
        }
    }
}
