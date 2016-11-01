package perf.reflect.visitor;

import java.lang.reflect.Method;

/**
 * Created by wreicher
 */
public interface MethodVisitor {

    public void onMethod(Method method);
}
