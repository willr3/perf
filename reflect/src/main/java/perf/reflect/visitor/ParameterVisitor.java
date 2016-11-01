package perf.reflect.visitor;

import java.lang.reflect.Parameter;

/**
 * Created by wreicher
 */
public interface ParameterVisitor {

    void onParameter(Parameter parameter);
}
