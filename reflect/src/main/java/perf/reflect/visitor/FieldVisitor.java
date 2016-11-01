package perf.reflect.visitor;

import java.lang.reflect.Field;

/**
 * Created by wreicher
 */
public interface FieldVisitor {

    void onField(Field field);
}
