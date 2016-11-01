package perf.reflect.visitor.impl;

import perf.reflect.visitor.TypeVisitor;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by wreicher
 */
public class CollectingTypeVisitor implements TypeVisitor {

    private HashSet<Class> types;
    private HashSet<Type> seen;
    private boolean includeExtensions;

    public CollectingTypeVisitor(boolean includeExtensions){
        this.seen = new HashSet<>();
        this.types = new HashSet<>();
        this.includeExtensions = includeExtensions;
    }
    public Set<Class> getTypes(){return Collections.unmodifiableSet(types);}
    public int size(){return types.size();}
    public void clear(){types.clear();seen.clear();}

    @Override
    public void onParameterizedType(ParameterizedType type) {
        //System.out.println("PT:"+type+" ["+Arrays.asList(type.getActualTypeArguments())+"]");
        this.visit(type.getRawType());
        this.visit(type.getOwnerType());
        for(Type arg : type.getActualTypeArguments()){
            this.visit(arg);
        }
    }

    @Override
    public void onWildcardType(WildcardType type) {
        for(Type lowerBound : type.getLowerBounds()){
            this.visit(lowerBound);
        }
        for(Type upperBound : type.getUpperBounds()){
            this.visit(upperBound);
        }
    }

    @Override
    public void onGenericArrayType(GenericArrayType type) {
        this.visit(type.getGenericComponentType());
    }

    @Override
    public void onTypeVariable(TypeVariable type) {
        //System.out.println("TV:"+type+" ["+ Arrays.asList(type.getBounds())+"]");
        for(Type bound : type.getBounds()){
            this.visit(bound);
        }
    }

    @Override
    public void visit(Type type){
        if(!seen.contains(type)){
            seen.add(type);
            TypeVisitor.super.visit(type);
        }
    }

    @Override
    public void onClass(Class type) {
        if(!types.contains(type) && !type.equals(Void.TYPE)){
            types.add(type);
            for(Type t : type.getTypeParameters()){
                this.visit(t);
            }
            if(includeExtensions){
                this.visit(type.getGenericSuperclass());
                for(Type t : type.getGenericInterfaces()){
                    this.visit(t);
                }
            }


        }
    }
}
