package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 * A lamba-friendly interface for reflecting on a Field and Class.
 */
public interface FieldClassActor extends Actor {
    String apply(Field field, Class knownClass);

    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (field != null && knownClass != null) {
            return apply(field, knownClass);
        } else {
            return null;
        }
    }
}
