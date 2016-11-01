package perf.reflect.visitor;

import java.lang.reflect.Constructor;

/**
 * Created by wreicher
 */
public interface ConstructorVisitor {

    public void onConstructor(Constructor constructor);
}
