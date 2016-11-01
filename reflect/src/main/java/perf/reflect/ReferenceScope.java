package perf.reflect;

import perf.reflect.actor.Actor;
import perf.reflect.actor.ClassActor;
import perf.reflect.actor.ConstructorActor;
import perf.reflect.actor.FieldActor;
import perf.reflect.actor.MethodActor;
import perf.reflect.visitor.impl.CollectingTypeVisitor;
import perf.util.AsciiArt;
import perf.util.HashedSets;
import perf.util.Sets;
import perf.util.StringUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by wreicher
 */
public class ReferenceScope extends HasActor {

    private HashedSets<Class,String> scopeClasses;
    private Set<Class> knownClasses;
    private Set<String> failedLoad;
    private DescendantGraph graph;

    private boolean saveTruthyResponse(Class knownClass, Field field, Method method, Constructor constructor,boolean first){
        boolean rtrn = false;
        for(String name : actors.keySet()){
            Actor expander = actors.get(name);
            String response = expander.apply(knownClass,field,method,constructor);
            if(isTruthy(response)){
                rtrn = true;
                saveScope(knownClass,response);
                if(first) {
                    return true;
                }
            }
        }
        return rtrn;
    }

    public ReferenceScope(DescendantGraph graph,Set<Class> knownClasses,Set<String> failedLoad){
        this.scopeClasses = new HashedSets<>();
        this.graph = graph;
        this.knownClasses = knownClasses;
        this.failedLoad = failedLoad;
        this.actors = new LinkedHashMap<>();
    }

    public void addTarget(Class referent){
        saveScope(referent,"initial target");
    }

    private void saveScope(Class toAdd, String reason){
        //System.out.println(AsciiArt.ANSI_CYAN+toAdd.getName()+AsciiArt.ANSI_RESET+" "+reason);
        this.scopeClasses.put(toAdd,reason);
    }
    private boolean isTruthy(String input){
        return
            input!=null &&
            !input.toString().trim().isEmpty() &&
            !"false".equalsIgnoreCase(input.toString()) &&
            !"fail".equalsIgnoreCase(input.toString());
    }

    public boolean addScope(Class knownClass){
        //removed to try and get all reasons
//        if(scopeClasses.has(knownClass)){
//            return false;
//        }
        try {
            boolean saved;
            //for the ClassActors
            boolean onlyFirst=false;
            saved = saveTruthyResponse(knownClass,null,null,null,onlyFirst);
            if(saved){
                return true;
            }
            for(Field f : knownClass.getDeclaredFields()){
                if(f.getName().equals("this$0")) {//NOTE hack to get around member class reference to containing class instance
                    continue;
                }
                saved = saveTruthyResponse(knownClass,f,null,null,onlyFirst);
                if(saved){
                    return true;
                }
            }
            for(Constructor c : knownClass.getDeclaredConstructors()){
                saved = saveTruthyResponse(knownClass,null,null,c,onlyFirst);
                if(saved){
                    return true;
                }
            }
            for(Method m : knownClass.getDeclaredMethods()){
                saved = saveTruthyResponse(knownClass,null,m,null,onlyFirst);
                if(saved){
                    return true;
                }
            }
        }catch(NoClassDefFoundError|TypeNotPresentException e){
            if(!this.failedLoad.contains(knownClass.getName())){
                //System.out.println(AsciiArt.ANSI_RED+knownClass.getName()+AsciiArt.ANSI_RESET+" missing "+e.getMessage());
            }

        }
        return false;
    }

    /**
     * Calculate the set of classes that *could* retain a reference to a target class through their reference chain.
     */
    public int buildScope(){
        AtomicInteger counter = new AtomicInteger();
        int size = scopeClasses.size();
        boolean modified;
        do {
            modified=false;
            counter.incrementAndGet();
            for(Iterator<Class> knownIterator = knownClasses.iterator(); knownIterator.hasNext();){
                Class knownClass = knownIterator.next();
                boolean checkResult = addScope(knownClass);
                modified = modified || checkResult;
            }
            System.out.println(
                    "loop:" + AsciiArt.ANSI_BLUE + String.format("%3d",counter.get()) + AsciiArt.ANSI_RESET + " " +
                    "size: " + AsciiArt.ANSI_BLUE + String.format("%4d",size) + AsciiArt.ANSI_RESET + " " +
                    "scope: " + AsciiArt.ANSI_BLUE + String.format("%4d",scopeClasses.size()) + AsciiArt.ANSI_RESET);
        }while(size < scopeClasses.size() && (size=scopeClasses.size())>0);
        return counter.get();
    }
    public int getScopeCount(){return scopeClasses.size();}
    public Set<Class> getScopeClasses(){return Collections.unmodifiableSet(scopeClasses.keys());}

    public static Set<Class> getAssignable(Set<Class> from,Set<Class> potentialParents){
        Set<Class> rtrn = new LinkedHashSet<>();
        for(Class fromClass : from){
            rtrn.addAll(
                potentialParents.stream()
                    .filter(parentClass -> {
                        boolean includeMe = !parentClass.equals(Object.class) && parentClass.isAssignableFrom(fromClass);
                        if(includeMe){
                            System.out.println(parentClass.getName()+".isAssignableFrom("+fromClass.getName()+")");
                        }
                        return includeMe;
                    }).collect(Collectors.toList())
            );
        }
        if(!rtrn.isEmpty()){
            System.out.println("getAssignable");
            System.out.println("  from [" + from.stream().map((s) -> s.getName()).collect(Collectors.reducing("",(a, b) -> a+" "+b)) + "]");
            System.out.println("  to [" + potentialParents.stream().map((s) -> s.getName()).collect(Collectors.reducing("",(a, b) -> a+" "+b)) + "]");
            System.out.println("  -> [" + rtrn.stream().map((s) -> s.getName()).collect(Collectors.reducing("",(a, b) -> a + " " + b)) + "]");
        }


        return rtrn;
    }
    public Set<String> getReasons(Class scopeClass) {
        if(!scopeClasses.has(scopeClass)){
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(scopeClasses.get(scopeClass));
    }
    public static void main(String[] args) {
        long start,stop = 0;
        EnumeratingClassLoader classLoader = new EnumeratingClassLoader(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        classLoader.addModules("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/modules/");
        classLoader.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/jboss-modules.jar");

        start = System.currentTimeMillis();
        classLoader.findClasses();
        stop = System.currentTimeMillis();
        System.out.println(
            "scans " +
            "classes:" + AsciiArt.ANSI_BLUE + String.format("%6d",classLoader.getJarClassCount()) + AsciiArt.ANSI_RESET + " " +
            "jars:" + AsciiArt.ANSI_BLUE + String.format("%4d",classLoader.getJarCount()) + AsciiArt.ANSI_RESET + " " +
            "in " + AsciiArt.ANSI_YELLOW + StringUtil.durationToString((stop-start)) + AsciiArt.ANSI_RESET);

        start = System.currentTimeMillis();
        classLoader.loadClasses();
        stop = System.currentTimeMillis();

        System.out.println(
            "loads " +
            "classes:" + AsciiArt.ANSI_BLUE + String.format("%6d",classLoader.getLoadedClassCount()) + AsciiArt.ANSI_RESET + " " +
            "failed:" + AsciiArt.ANSI_RED + String.format("%4d",classLoader.getFailedLoadCount()) + AsciiArt.ANSI_RESET + " " +
            "missing:" + AsciiArt.ANSI_RED + String.format("%4d",classLoader.getMissingDependenciesCount()) + AsciiArt.ANSI_RESET + " " +
            "in " + AsciiArt.ANSI_YELLOW + StringUtil.durationToString((stop-start)) + AsciiArt.ANSI_RESET);


        DescendantGraph graph = new DescendantGraph();
        classLoader.getLoadedClasses().forEach(graph::add);
        System.out.println(
            "graph " +
            "classes:" + AsciiArt.ANSI_BLUE + String.format("%6d",graph.getClasses().size()) + AsciiArt.ANSI_RESET

        );
        System.out.println("Descendant Graph ready");
        ReferenceScope scope = new ReferenceScope(graph,classLoader.getLoadedClassesSet(),classLoader.getFailedLoad());

        final CollectingTypeVisitor collector = new CollectingTypeVisitor(false);
        //if the class extends a class in scope
        ClassActor scopeIsAssignableFrom = (c)->{
            Set<Class> set = Sets.getOverlap(
                scope.getScopeClasses().stream()
                .filter(scopeClass-> scopeClass.isAssignableFrom(c) && !c.equals(scopeClass) )
                .collect(Collectors.toSet()),scope.getScopeClasses());
            if(!set.isEmpty()){
                return "Scope.isAssignableFrom ["+set.stream().map((s) -> s.getName()).collect(Collectors.reducing((a, b) -> a+" "+b)).get()+"]";
            } else {
                return null;
            }
        };
        FieldActor fieldType = (field)->{
            collector.clear();
            collector.visit(field.getGenericType());
            Set<Class> set = Sets.getOverlap(collector.getTypes(),scope.getScopeClasses());
            if(!set.isEmpty()){
                return "Field "+field.getType().getName()+" "+field.getName()+" "+set;
            } else {
//                set = getAssignable(scope.getScopeClasses(),collector.getTypes());
//                if(!set.isEmpty()){
//                    String toRtrn = "Field ["+field.getGenericType().toString()+"] "+field.getName()+" assignable "+set;
//                    System.out.println(AsciiArt.ANSI_GREEN+field.getDeclaringClass().getName()+AsciiArt.ANSI_PURPLE+" "+field.getName()+AsciiArt.ANSI_RESET+" "+toRtrn);
//                    return toRtrn;
//                }
                return null;
            }
        };
        ClassActor isAssignableFromScope = (c)->{
            Set<Class> set = Sets.getOverlap(scope.getScopeClasses().stream()
                    .filter(scopeClass-> {
                        boolean ok = !c.getName().equals("java.lang.Object") && c.isAssignableFrom(scopeClass) && !c.equals(scopeClass);
                        if(ok) {
                            System.out.println(AsciiArt.ANSI_GREEN +
                                    c.getName() +
                                    AsciiArt.ANSI_RESET +
                                    " isAssignableFrom " +
                                    AsciiArt.ANSI_GREEN +
                                    scopeClass.getName() +
                                    AsciiArt.ANSI_RESET);
                        }
                        return ok;
                    })
                    .collect(Collectors.toSet()),scope.getScopeClasses());
            if(!set.isEmpty()){
                return "isAssignableFrom ["+set.stream().map((s) -> s.getName()).collect(Collectors.reducing((a, b) -> a+" "+b)).get()+"]";
            } else {
                return null;
            }
        };
        MethodActor returnType = (method)->{
            collector.clear();
            collector.visit(method.getGenericReturnType());
            Set<Class> set = Sets.getOverlap(collector.getTypes(),scope.getScopeClasses());
            if(!set.isEmpty()){
                return "returnType "+method.getName()+" ["+set.stream().map((c) -> c.getName()).collect(Collectors.reducing((a, b) -> a+" "+b)).get()+"]";
            } else {
                return null;
            }
        };
        ConstructorActor constructorParam = (constructor)->{
            collector.clear();
            for(Parameter p : constructor.getParameters()){
              collector.visit(p.getParameterizedType());
            }
            Set<Class> set = Sets.getOverlap(collector.getTypes(),scope.getScopeClasses());
            if(!set.isEmpty()){
                return "constructorParam "+constructor.toString()+" ["+set.stream().map((c) -> c.getName()).collect(Collectors.reducing((a, b) -> a+" "+b)).get()+"]";
            } else {
                return null;
            }
        };
//        scope.addClassActor("Scope.isAssignableFrom",scopeIsAssignableFrom);
//        scope.addFieldActor("fieldType",filedType);
//        scope.addClassActor("isAssignableFromScope",isAssignableFromScope);
//        scope.addMethodActor("returnType",returnType);
//        scope.addConstructorActor("constructorParam",constructorParam);
        try {
            scope.addTarget(classLoader.loadClass("org.apache.activemq.artemis.api.core.ActiveMQBuffer"));

            //scope.addClassActor("Scope.isAssignableFrom",scopeIsAssignableFrom);
            scope.addFieldActor("fieldType",fieldType);
            //scope.addMethodActor("returnType",returnType);
            //scope.addClassActor("Class.isAssignableFrom(Scope)",isAssignableFromScope);

            start = System.currentTimeMillis();
            int loops = scope.buildScope();
            stop = System.currentTimeMillis();

            System.out.println(
                "loops:" + AsciiArt.ANSI_BLUE + String.format("%3d",loops) +  AsciiArt.ANSI_RESET + " " +
                "scope: " + AsciiArt.ANSI_BLUE + String.format("%6d",scope.getScopeCount()) + AsciiArt.ANSI_RESET + " " +
                "in " + AsciiArt.ANSI_BLUE + StringUtil.durationToString((stop-start)));

            scope.clearActors();

            scope.getScopeClasses().stream().forEach((scopeClass)->{
                Set<String> reasons = scope.getReasons(scopeClass);
                System.out.println(
                        AsciiArt.ANSI_CYAN +
                        (scopeClass.isInterface() ? "I:" : "" ) +
                        scopeClass.getName() +
                        AsciiArt.ANSI_RESET);
                reasons.forEach((reason)->{
                    System.out.println("  "+reason);
                });
            });

            //write the jars and classes as json
            try (PrintWriter out = new PrintWriter(new FileWriter("/home/wreicher/perfWork/byteBuffer/jarClasses.json"))){
                out.println(classLoader.toJson());
            } catch (IOException e) {
                e.printStackTrace();
            }

            //write the classes that appear in multiple jars
            try (PrintWriter out = new PrintWriter(new FileWriter("/home/wreicher/perfWork/byteBuffer/duplicateClasses.log"))){
                Map<String,Set<String>> classJars = classLoader.classtoJars();
                for(String className : classJars.keySet()){
                    Set<String> jars = classJars.get(className);
                    if(jars.size()>1){
                        out.println(className);
                        for(String jar : jars){
                            out.println("  "+jar);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //write the scope with reason
            try (PrintWriter out = new PrintWriter(new FileWriter("/home/wreicher/perfWork/byteBuffer/scope.log"))){
                scope.getScopeClasses().forEach(
                    (scopeClass)->{
                        out.println(scopeClass.getName());
                        Set<String> reasons = scope.getReasons(scopeClass);
                        reasons.forEach((r)->{out.println("  "+r);});
                    }
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
