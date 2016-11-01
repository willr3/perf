package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by wreicher
 * A lamba-friendly interface for reflecting on a Method and Class.
 */
public interface MethodClassActor extends Actor {


    String apply(Method method, Class knownClass);

    /**
     * @return apply(Method method,Class kownClass) if method != null && knownClass!=null, otherwise just return null
     */
    default public String apply(Class knownClass, Field field, Method method, Constructor constructor) {
        if (method != null && knownClass != null) {
            return apply(method, knownClass);
        } else {
            return null;
        }
    }
}
