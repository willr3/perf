package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 * A lamba-friendly interface for reflecting on just the Constructor.
 */
public interface ConstructorActor extends Actor {
    String apply(Constructor constructor);

    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (constructor != null) {
            return apply(constructor);
        } else {
            return null;
        }
    }
}
