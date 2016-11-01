package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 * A lamba-friendly interface for reflecting on the Constructor and Class
 */
public interface ConstructorClassActor extends Actor {
    String apply(Constructor constructor, Class knownClass);

    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (constructor != null & knownClass != null) {
            return apply(constructor, knownClass);
        } else {
            return null;
        }

    }
}
