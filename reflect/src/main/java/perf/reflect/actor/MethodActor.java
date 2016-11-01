package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 * A lamba-friendly interface for reflecting on a Method.
 */
public interface MethodActor extends Actor {
    String apply(Method method);

    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (method != null) {
            return apply(method);
        } else {
            return null;
        }
    }
}
