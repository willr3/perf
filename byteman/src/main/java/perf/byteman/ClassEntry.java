package perf.byteman;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by wreicher
 */
public class ClassEntry {

    public static enum ClassType {Abstract,Anonymous,Annotation,Array,Enum,Final,Interface,Local,Pojo,Synthetic,Error}

    public static ClassType toClassType(Class loadedClass){
        try {
            if (loadedClass.isInterface()) {
                return ClassType.Interface;
            }
            if (Modifier.isAbstract(loadedClass.getModifiers())) {
                return ClassType.Abstract;
            }
            if (loadedClass.isAnnotation()) {
                return ClassType.Annotation;
            }
            if( loadedClass.getName().matches("[^\\$]+\\$\\d+")){//to avoid the ILAE
                return ClassType.Anonymous;
            }
            if (loadedClass.isEnum()) {
                return ClassType.Enum;
            }
            return ClassType.Pojo;
        }catch(IllegalAccessError e){
            System.out.println("ILAE "+loadedClass.getName());
            e.printStackTrace();
            System.exit(0);
        }catch(IncompatibleClassChangeError e){
            System.out.println("ICCE "+loadedClass.getName());
            e.printStackTrace();
            System.exit(0);
        }
        return ClassType.Error;
    }

    private int id;
    private ClassEntry parent;
    private ClassType classType;

    private Class entryClass;

    private List<ClassEntry> interfaces;
    private List<ClassEntry> extenders;
    private List<ClassEntry> implementers;

    private boolean methodErrors = false;
    private List<MethodEntry> methods;

    public ClassEntry(int id, Class entryClass, ClassType classType) {
        this.id = id;
        this.entryClass = entryClass;
        this.classType = classType;
        this.interfaces = new ArrayList<>();
        this.extenders = new ArrayList<>();
        this.implementers = new ArrayList<>();
        this.methods = new ArrayList<>();
    }
    public ClassType getType(){return classType;}
    public int getId(){return id;}
    public boolean hasParent(){return parent!=null;}
    public ClassEntry getParent(){return parent;}
    public Class getEntryClass(){return entryClass;}

    public void setMethodErrors(boolean methodErrors){
        this.methodErrors = methodErrors;
    }
    public boolean hasMethodErrors(){return methodErrors;}

    public void addMethod(MethodEntry entry) {
        methods.add(entry);
    }
    public List<MethodEntry> getMethods(){return Collections.unmodifiableList(methods);}

    public String toJson() {
        StringBuilder rtrn = new StringBuilder();
        rtrn.append("{\"id\":");
        rtrn.append(this.id);
        if (this.parent != null) {
            rtrn.append(",\"parentId\":");
            rtrn.append(this.parent.id);
        }
        rtrn.append(",\"T\":");
        rtrn.append("\"" + this.classType + "\"");
        rtrn.append(",\"interfaces\":");
        idList(interfaces, rtrn);
        rtrn.append(",\"extenders\":");
        idList(extenders, rtrn);
        rtrn.append(",\"implementers\":");
        idList(implementers, rtrn);
        if (methodErrors) {
            rtrn.append(",\"methodErrors\":");
            rtrn.append(methodErrors);
        }
//            rtrn.append(",\"methods\":");
//            if(methods.isEmpty()){
//                rtrn.append("[]");
//            }else{
//                rtrn.append("[");
//                for(int i=0; i<methods.size();i++){
//                    if(i>0){
//                        rtrn.append(",");
//                    }
//                    rtrn.append(methods.get(i).toJson());
//                }
//                rtrn.append("]");
//            }
        rtrn.append("}");
        return rtrn.toString();
    }

    private void idList(List<ClassEntry> list, StringBuilder rtrn) {
        if (list.isEmpty()) {
            rtrn.append("[]");
            return;
        }
        rtrn.append("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                rtrn.append(",");
            }
            rtrn.append(list.get(i).id);
        }
        rtrn.append("]");
    }

    public void addExtender(ClassEntry classHeirarchy) {
        if (!this.extenders.contains(classHeirarchy.id)) {
            this.extenders.add(classHeirarchy);
            classHeirarchy.parent = this;
        }
    }

    public List<ClassEntry> getExtenders() {
        return Collections.unmodifiableList(extenders);
    }

    public void addInterface(ClassEntry classHeirarchy) {
        if (!this.interfaces.contains(classHeirarchy.id)) {
            this.interfaces.add(classHeirarchy);
            classHeirarchy.addImplementer(this);
        }
    }

    public List<ClassEntry> getInterfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    public void addImplementer(ClassEntry classHeirarchy) {
        if (!this.implementers.contains(classHeirarchy.id)) {
            this.implementers.add(classHeirarchy);
        }
    }

    public boolean isA(ClassEntry entry) {
        if (entry == null) {
            return false;
        } else if (this.id == entry.id) {
            return true;
        } else if (parent != null && parent.isA(entry)) {
            return true;
        } else {
            for (ClassEntry implemented : interfaces) {
                if (implemented.isA(entry)) {
                    return true;
                }
            }
        }

        return false;
    }


    public static void main(String[] args) {
        List<String> list = new ArrayList<>();

//        Class listClass = list.getClass();
//        System.out.println(listClass.getComponentType());
//        Type type = listClass.getGenericSuperclass();
//        if(type instanceof  ParameterizedType){
//            ParameterizedType parameterizedType = (ParameterizedType)type;
//
//            Type typeArguments[] = parameterizedType.getActualTypeArguments();
//            for(Type t: typeArguments){
//                System.out.println(t);
//            }
//        }

        try {
            Class toInspect = ScopeCreeper.class;

            System.out.println(Arrays.toString(toInspect.getDeclaredFields()));
            Field f = toInspect.getDeclaredField("testMe");
            Type t = f.getGenericType();
            System.out.println(t.getTypeName());
            if (t instanceof ParameterizedType){
                ParameterizedType pt = (ParameterizedType)t;
                System.out.println(Arrays.toString(pt.getActualTypeArguments()));
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }
}
