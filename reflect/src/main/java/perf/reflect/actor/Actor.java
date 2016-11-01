package perf.reflect.actor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Represents all the possible input parameters for reflecting on a class.
 * Note, most of the fields will be {@code null} when called.
 * e.g. when looping over a Class's fields the method and constructor will be {@code null}
 * e.g. when first inspecting the Class the field, method, and constructor will be {@code null}
 */

public interface Actor {
    String apply(Class knownClass, Field field, Method method, Constructor constructor);
}
