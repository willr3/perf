package perf.reflect;

import perf.reflect.visitor.MethodVisitor;
import perf.reflect.visitor.ParameterVisitor;
import perf.reflect.visitor.impl.CollectingTypeVisitor;
import perf.util.AsciiArt;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Created by wreicher
 */
public class ReflectRun {

    public static final String PREFIX="                                                                                                                                                                                                                             ";

    public static class Generic<T> {
        T value;
        public Generic(T value){}
    }
    public static class SubGeneric<V extends CharSequence> extends Generic<V>{
        List<V> listV;
        public SubGeneric(V value){
            super(value);
        }
    }
    public static class SubSubGeneric extends SubGeneric<String> implements Serializable{
        private List<String> stringValue;
        public SubSubGeneric(String value){
            super(value);
        }
    }
    public static class DoubleBoundType<T extends String & Serializable> {

        //onConstructor
        public <V extends String & Comparator<T>> DoubleBoundType(T parameterizedType, List<? extends CharSequence> upperBoundWildcardList, List<? super Integer> lowerBoundList,T[] typeArray){}

        //methods
        T getT(){return null;}
//        List<T[]> getListT(){return null;}
//        T[] getArrayT(){return null;}
//        void takeT(T value){}
//        void takeListT(List<T> value){}
//        void takeArrayT(T[] value){}
//        void takeListSuperT(List<? super T> value){}
//        void takeComplexClass(String foo,SubSubGeneric value){}
//        <V extends T> void takeVextendsT(V value){}

    }
    public static void onField(Field field){
        System.out.println(AsciiArt.ANSI_RED+field.toGenericString()+AsciiArt.ANSI_RESET);

        Type genericType = field.getGenericType();
        System.out.println(pad(2)+"genericType "+genericType);

        Class fieldClass = field.getType();
        System.out.println(pad(2)+"fieldClass "+fieldClass);

        onType(genericType,4);
    }
    public static void onMethod(Method method){
        System.out.println(AsciiArt.ANSI_RED+method.toGenericString()+AsciiArt.ANSI_RESET);
        Parameter parameters[] = method.getParameters();
        TypeVariable<Method> typeParameters[] = method.getTypeParameters();
        Class paramaterClasses[] = method.getParameterTypes();
        Type returnType = method.getGenericReturnType();
        Class returnClass = method.getReturnType();
        while(returnClass!=null && returnClass.isArray()){
            returnClass = returnClass.getComponentType();
        }
        System.out.println(pad(2)+"Parameters "+parameters.length);
        for(Parameter p : parameters){
            System.out.println("    "+p.toString());
            Type paramType = p.getParameterizedType();
            onType(paramType,6);
        }
        System.out.println(pad(2)+"ParameterClasses "+paramaterClasses.length);
        for(Class c : paramaterClasses){
            System.out.println("    "+c.toString());
        }
        System.out.println(pad(2)+"TypeParameters "+typeParameters.length);
        for(TypeVariable<Method> tV : typeParameters){
            System.out.println(pad(4)+tV.toString());
            onType(tV,6);
        }
        System.out.println(pad(2)+"return class "+returnClass.toString());
        System.out.println(pad(2)+"return onType "+returnType.toString());
        onType(returnType,4);


    }
    public static void onConstructor(Constructor constructor){
        System.out.println(AsciiArt.ANSI_RED+constructor.toGenericString()+AsciiArt.ANSI_RESET);
        Parameter parameters[] =constructor.getParameters();
        TypeVariable<Constructor> typeParameters[] = constructor.getTypeParameters();
        Class paramaterClasses[] = constructor.getParameterTypes();
        System.out.println(pad(2)+"Parameters "+parameters.length);
        for(Parameter p : parameters){
            System.out.println("    "+p.toString());
            Type paramType = p.getParameterizedType();
            onType(paramType,6);
        }
        System.out.println(pad(2)+"ParameterClasses "+paramaterClasses.length);
        for(Class c : paramaterClasses){
            System.out.println("    "+c.toString());
        }
        System.out.println(pad(2)+"TypeParameters "+typeParameters.length);
        for(TypeVariable<Constructor> tV : typeParameters){
            System.out.println(pad(4)+tV.toString());
            onType(tV,6);
        }
    }

    public static Set<Class> methodDependencies(Method method){
        CollectingTypeVisitor v = new CollectingTypeVisitor(false);
        for(Parameter p : method.getParameters()){
            v.visit(p.getParameterizedType());
        }
        for(TypeVariable<Method> typeVariable : method.getTypeParameters()){
            v.visit(typeVariable);
        }
        v.visit(method.getGenericReturnType());
        return v.getTypes();

    }
    public static void onType(Type type, int prefix){
        if(type==null) {
            return;
        }
        System.out.println(pad(prefix)+"instanceof");
        if(type instanceof ParameterizedType){
            System.out.println(pad(prefix+2)+"parameterizedType");
            ParameterizedType parameterizedType = (ParameterizedType)type;

            Type rawType = parameterizedType.getRawType();
            System.out.println(pad(prefix+4)+"rawType "+rawType);
            onType(rawType,prefix+6);

            Type ownerType = parameterizedType.getOwnerType();
            System.out.println(pad(prefix+4)+"ownerType "+ownerType);
            onType(ownerType,prefix+6);

            Type typeArgs[] = parameterizedType.getActualTypeArguments();
            System.out.println(pad(prefix+4)+"typeArgs");
            for(Type arg : typeArgs){
                System.out.println(pad(prefix+6)+"arg "+arg.getTypeName());
                onType(arg,prefix+8);
            }
        }
        if(type instanceof  WildcardType ){
            System.out.println(pad(prefix+2)+"wildcardType");
            WildcardType wildcardType = (WildcardType)type;
            Type lowerBounds[] = wildcardType.getLowerBounds();
            System.out.println(PREFIX.substring(0,prefix+4)+" lowerBounds "+lowerBounds.length);
            for(Type lowerBound : lowerBounds){
                System.out.println(pad(prefix+6)+lowerBound);
                onType(lowerBound,prefix+8);
            }
            Type upperBounds[] = wildcardType.getUpperBounds();
            System.out.println(pad(prefix+4)+" upperBounds "+upperBounds.length);
            for(Type upperBound : upperBounds){
                System.out.println(pad(prefix+6)+upperBound);
                onType(upperBound,prefix+8);
            }
        }
        if(type instanceof GenericArrayType){
            System.out.println(pad(prefix+2)+"genericArrayType");
            GenericArrayType genericArrayType = (GenericArrayType)type;
            System.out.println(pad(prefix+4)+"name "+genericArrayType.getTypeName());
            Type typeComponent = genericArrayType.getGenericComponentType();
            System.out.println(pad(prefix+6)+"genericComponentType="+typeComponent);
            onType(typeComponent,prefix+8);

        }
        if(type instanceof  TypeVariable ){
            System.out.println(pad(prefix+2)+"typeVariable");
            TypeVariable typeVariable = (TypeVariable)type;

            Type bounds[] = typeVariable.getBounds();
            if(bounds!=null){
                System.out.println(pad(prefix+4)+"bounds");
                for(Type bound : bounds){
                    System.out.println(pad(prefix+6)+"bound "+bound.getTypeName());
                    onType(bound,prefix+8);
                }
            }
        }
        if(type instanceof Class){
            System.out.println(pad(prefix+2)+"class");
            Class typeClass = (Class)type;
            onClass(typeClass,prefix+4);
        }

    }
    private static void onClass(Class clazz,int prefix){

        //TODO stop if we have already seen / visited clazz

        System.out.println(pad(prefix)+"onClass "+clazz.getName());
        while(clazz.isArray()){//TODO isArray for componentClass
            System.out.println(pad(prefix+4)+" isArray "+clazz);
            clazz = clazz.getComponentType();
        }
        System.out.println(pad(prefix+2)+"typeClass "+clazz.getName());

        Class superClass = clazz.getSuperclass();
        if(superClass!=null) {
            while (superClass.isArray()) {
                System.out.println(pad(prefix + 4) + " isArray " + superClass);
                superClass = superClass.getComponentType();
            }
            System.out.println(pad(prefix + 2) + "superClass " + superClass.getName());
        }

        Type genericSuperclass = clazz.getGenericSuperclass();
        System.out.println(pad(prefix+2)+"genericSuperClass "+genericSuperclass);
        onType(genericSuperclass,prefix+4);

        //Duplicate use case of getGenericInterfaces?
        Class interfaces[] = clazz.getInterfaces();
        System.out.println(pad(prefix+2)+"interfaces "+interfaces.length+" "+ Arrays.asList(interfaces));
//        for(Class iface : interfaces){
//            onClass(iface,prefix+4);
//        }
        //does generic interfaces always provide the same # as getInterfaces?()
        Type genericInterfaces[] = clazz.getGenericInterfaces();
        System.out.println(pad(prefix+2)+"genericInterfaces "+genericInterfaces.length+" "+ Arrays.asList(genericInterfaces));
//        for(Type genericInterface : genericInterfaces){
//            onType(genericInterface,prefix+4);
//        }
        Class declaredClasses[] = clazz.getDeclaredClasses();
        for(Class declaredClass : declaredClasses){
            onClass(declaredClass,prefix+2);
        }
    }
    private static String pad(int amount){
        return PREFIX.substring(0,amount);
    }
    public static void main(String[] args) {

        EnumeratingClassLoader classLoader = new EnumeratingClassLoader();
        classLoader.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/modules/");
        classLoader.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/jboss-modules.jar");
        classLoader.loadClasses();
        System.out.println("jarCount    "+classLoader.getJarCount());
        System.out.println("classCount  "+classLoader.getJarClassCount());
        System.out.println("loadedCount "+classLoader.getLoadedClassCount());
        System.out.println("failedCount "+classLoader.getFailedLoadCount());
        System.out.println("missingDeps "+classLoader.getMissingDependenciesCount());


        HashSet<Class> traceSet = new HashSet<>();
        HashSet<Class> scanSet = new HashSet<>();
        HashSet<Class> targetSet = new HashSet<>();

        Predicate<Set<Class>> hasScanEntry = (set)->{
            for(Class setClass : set){
                if(scanSet.contains(setClass)){
                    return true;
                }
            }
            //separate loops because expect first loop to catch most cases
            for(Class setClass : set){
                for(Class scanClass : scanSet){
                    if(scanClass.isAssignableFrom(setClass)){
                        return true;
                    }
                }
            }
            return false;
        };

        Thread.currentThread().setContextClassLoader(classLoader);

        boolean run=true;
        int loopCount=0;
        List<Class> loadedClasses = classLoader.getLoadedClasses();


        Set<Class> targetClasses = new HashSet<>();
        Class amqClass = null;
        System.out.println("AMQClass test");
        System.out.println("AMQBuffer isLoaded "+classLoader.isLoaded("org.apache.activemq.artemis.api.core.ActiveMQBuffers$AMQBuffer"));
        try {
            amqClass = classLoader.loadClass("org.apache.activemq.artemis.api.core.ActiveMQBuffers$AMQBuffer");

            targetClasses.add(amqClass);
            amqClass = classLoader.loadClass("org.apache.activemq.artemis.api.core.ActiveMQBuffer");
            System.out.println("AMQClass loaded");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println(amqClass.getName());
        System.out.println("Extenders:");
        System.exit(0);

        while(run){
            run=false;
            loopCount++;
            loadedClasses.forEach(loadedClass->{




            });
        }


        System.exit(0);
        classLoader.getClassNames().forEach(className->{
            try {
                Class loadedClass = classLoader.loadClass(className);
                Method methods[] = loadedClass.getDeclaredMethods();
                for(Method m : methods){

                }
                Field fields[] = loadedClass.getDeclaredFields();
                for(Field f : fields){

                }
                Constructor constructors[] = loadedClass.getDeclaredConstructors();
                for(Constructor c : constructors){

                }


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        });

        Class reflectMe = null;
        try {
            reflectMe = classLoader.loadClass("org.apache.activemq.artemis.api.core.ActiveMQBuffers$AMQBuffer");
            do {
                System.out.println(AsciiArt.ANSI_RED+reflectMe+AsciiArt.ANSI_RESET);
                Method method = reflectMe.getMethod("notify");
                System.out.println(AsciiArt.ANSI_BLUE+method+AsciiArt.ANSI_RESET);
                Set<Class> dependencies = methodDependencies(method);
                System.out.println(dependencies);
//                for (Constructor constructor : reflectMe.getDeclaredConstructors()) {
//                    onConstructor(constructor);
//                }
//                for (Method method : reflectMe.getDeclaredMethods()) {
//                    System.out.println(AsciiArt.ANSI_BLUE+method+AsciiArt.ANSI_RESET);
//                    //onMethod(method);
//                    Set<Class> dependencies = methodDependencies(method);
//                    System.out.println(dependencies);
//                }
//
//                for (Field field : reflectMe.getDeclaredFields()) {
//                    onField(field);
//                }
            }while( (reflectMe=reflectMe.getSuperclass())!=null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }
}


