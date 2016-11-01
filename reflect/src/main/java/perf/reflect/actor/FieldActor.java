package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 *  A lamba-friendly interface for reflecting on a Field.
 */
public interface FieldActor extends Actor {

    String apply(Field field);

    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (field != null) {
            return apply(field);
        } else {
            return null;
        }
    }
}
