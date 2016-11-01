package perf.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by wreicher
 */
public class Reflect {



    public static boolean isStatic(Method method){
        int modifiers = method.getModifiers();
        return Modifier.isStatic(modifiers);
    }
}
