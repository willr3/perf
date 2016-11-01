package perf.reflect.visitor;

import java.lang.reflect.*;

/**
 * Created by wreicher on 6/14/16.
 */
public interface TypeVisitor {

    public void onParameterizedType(ParameterizedType type);
    public void onWildcardType(WildcardType type);
    public void onGenericArrayType(GenericArrayType type);
    public void onTypeVariable(TypeVariable type);
    public void onClass(Class type);

    public default void visit(Type type){
        if(type instanceof ParameterizedType) {
            onParameterizedType((ParameterizedType) type);
        }
        if(type instanceof WildcardType){
            onWildcardType((WildcardType)type);
        }
        if(type instanceof GenericArrayType){
            onGenericArrayType((GenericArrayType)type);
        }
        if(type instanceof TypeVariable){
            onTypeVariable((TypeVariable)type);
        }
        if(type instanceof Class){
            onClass((Class)type);
        }
    }
}
