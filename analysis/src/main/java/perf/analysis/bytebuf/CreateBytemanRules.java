package perf.analysis.bytebuf;

import perf.byteman.BytemanUtil;
import perf.byteman.RulePattern;
import perf.reflect.EnumeratingClassLoader;
import perf.reflect.HierarchyWalker;
import perf.reflect.Reflect;
import perf.reflect.actor.ConstructorClassActor;
import perf.reflect.actor.MethodClassActor;
import perf.reflect.visitor.impl.CollectingTypeVisitor;
import perf.util.AsciiArt;
import perf.util.StringUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/**
 * Created by wreicher
 */
public class CreateBytemanRules {

    public static void main(String[] args) {
        long start,stop = 0;
        String targetRuntime = "/home/wreicher/runtime/wildfly-10.0.0.Final-pool/";
        EnumeratingClassLoader classLoader = new EnumeratingClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        classLoader.addModules(targetRuntime+"modules/");
        classLoader.addJarPath(targetRuntime+"jboss-modules.jar");

        start = System.currentTimeMillis();
        classLoader.findClasses();
        stop = System.currentTimeMillis();

        System.out.println(
            "scan " +
            "classes:" + AsciiArt.ANSI_BLUE + String.format("%6d",classLoader.getJarClassCount()) + AsciiArt.ANSI_RESET + " " +
            "jars:" + AsciiArt.ANSI_BLUE + String.format("%4d",classLoader.getJarCount()) + AsciiArt.ANSI_RESET + " " +
            "in " + AsciiArt.ANSI_YELLOW + StringUtil.durationToString((stop-start)) + AsciiArt.ANSI_RESET);

        start = System.currentTimeMillis();
        classLoader.loadClasses();
        stop = System.currentTimeMillis();

        System.out.println(
            "load " +
            "classes:" + AsciiArt.ANSI_BLUE + String.format("%6d",classLoader.getLoadedClassCount()) + AsciiArt.ANSI_RESET + " " +
            "fail:" + AsciiArt.ANSI_RED + String.format("%4d",classLoader.getFailedLoadCount()) + AsciiArt.ANSI_RESET + " " +
            "missing:" + AsciiArt.ANSI_RED + String.format("%4d",classLoader.getMissingDependenciesCount()) + AsciiArt.ANSI_RESET + " " +
            "in " + AsciiArt.ANSI_YELLOW + StringUtil.durationToString((stop-start)) + AsciiArt.ANSI_RESET);

        RulePattern jsonHelperStart = RulePattern.onStart("JsonHelperStart",
            "HELPER perf.byteman.JsonHelper",
            "AT ENTRY",
            "IF TRUE",
            "DO",
            "setTriggering(false);",
            "loadFrameIndexFile(\"{{frameIndex}}\");",
            "loadThreadNameIndexFile(\"{{threadIndex}}\");",
            "setTriggering(true);");
        RulePattern jsonHelperStop = RulePattern.onStop("JsonHelperStop",
            "HELPER perf.byteman.JsonHelper",
            "AT ENTRY",
            "IF TRUE",
            "DO",
            "setTriggering(false);",
            "writeFrameIndexFile(\"{{frameIndex}}\");",
            "writeThreadNameIndexFile(\"{{threadIndex}}\");",
            "setTriggering(true);");

        //Tracks this, return, and any params that are
        RulePattern hierarchyPattern = new RulePattern("callTrace",
            "RULE {{name}}_{{uid}}",
            "{{classType}} ^{{currentClassName}}",
            "METHOD {{{method}}}",
            "HELPER perf.byteman.JsonHelper",
            "AT {{tracePoint}}",
            "{{#nonStatic}}" +
                "IF $this.getClass().getName().equals(\"{{targetClassName}}\")" +
            "{{/nonStatic}}" +
            "{{^nonStatic}}" +
                "IF TRUE" +
            "{{/nonStatic}}",
            "DO",
            "setTriggering(false);",
            "openTrace(\"{{name}}\",\"{{logFile}}\");",
            "traceJsonStack(\"{{name}}\", new String[]{ " +
                "\"rule\", "+
                "\"{{name}}_{{uid}}\"," +
                "{{#nonStatic}}" +
                    "\"className\",$this.getClass().getName()," +
                    "\"hashCode\",\"\"+System.identityHashCode($this)" +
                "{{/nonStatic}}" +
                "{{^nonStatic}}" +
                    "\"className\",\"{{currentClassName}}\"" +
                "{{/nonStatic}}" +
                "{{#hasReturn}} , " +
                    "\"rtrn.className\", \"\"+$!.getClass().getName() , " +
                    "\"rtrn.hashCode\", \"\"+System.identityHashCode($!) " +
                "{{/hasReturn}}" +
                "{{#params}} , " +
                    "\"param.{{.}}.className\", \"\"+${{.}}.getClass().getName() , " +
                    "\"param.{{.}}.hashCode\", \"\"+System.identityHashCode(${{.}}) " +
                "{{/params}} });",
            "setTriggering(true);",
            "ENDRULE");
        RulePattern fieldPattern = new RulePattern("fieldWrapper",
                "RULE {{name}}_{{uid}}",
                "{{classType}} ^{{currentClassName}}",
                "METHOD {{{method}}}",
                "HELPER perf.byteman.JsonHelper",
                "AT {{tracePoint}}",
                "IF {{#hasReturn}}TRUE{{/hasReturn}}{{^hasReturn}}FALSE{{/hasReturn}} {{#fields}} || (this.{{.}} != null && this.{{.}}.getClass().getName().equals(\"{{targetClassName}}\") ) {{/fields}}",
                "DO",
                "setTriggering(false);",
                "openTrace(\"{{name}}\",\"{{logFile}}\");",
                "traceJsonStack(\"{{name}}\", new String[]{ " +
                        "{{#nonStatic}}" +
                            "\"className\", $this.getClass().getName()," +
                            "\"callerHashCode\", \"\"+System.identityHashCode($this) " +
                        "{{/nonStatic}}" +
                        "{{^nonStatic}}" +
                            "\"className\", \"{{currentClassName}}\"" + //need this here to prevent formatting problems with the param loop starting with a ,
                        "{{/nonStatic}}" +
                        "{{#hasReturn}} , " +
                            "\"rtrn.className\", \"\"+$!.getClass().getName() , " +
                            "\"rtrn.hashCode\", \"\"+System.identityHashCode($!) " +
                        "{{/hasReturn}}" +
                        "{{#fields}} , " +
                            "\"field.{{.}}.className\", \"\"+$this.{{.}}.getClass().getName() , "+
                            "\"field.{{.}}.hashCode\", \"\"+System.identityHashCode($this.{{.}}) "+
                        "{{/fields}} });",
                "setTriggering(true);",
                "ENDRULE");
        RulePattern argPattern = new RulePattern("argTrace",
            "RULE {{name}}_{{uid}}",
            "{{classType}} ^{{currentClassName}}",
            "METHOD {{{method}}}",
            "HELPER perf.byteman.JsonHelper",
            "AT {{tracePoint}}",
            "IF {{#hasReturn}}TRUE{{/hasReturn}}{{^hasReturn}}FALSE{{/hasReturn}} {{#params}} || ${{.}}.getClass().getName().equals(\"{{targetClassName}}\") {{/params}}",
            "DO",
            "setTriggering(false);",
            "openTrace(\"{{name}}\",\"{{logFile}}\");",
            "traceJsonStack(\"{{name}}\", new String[]{ " +
                    "\"rule\", "+
                    "\"{{name}}_{{uid}}\"," +
                "{{#nonStatic}}" +
                    "\"className\", $this.getClass().getName()," +
                    "\"callerHashCode\", \"\"+System.identityHashCode($this) " +
                "{{/nonStatic}}" +
                "{{^nonStatic}}" +
                    "\"className\", \"{{currentClassName}}\"" + //need this here to prevent formatting problems with the param loop starting with a ,
                "{{/nonStatic}}" +
                "{{#hasReturn}} , " +
                    "\"rtrn.className\", \"\"+$!.getClass().getName() , " +
                    "\"rtrn.hashCode\", \"\"+System.identityHashCode($!) " +
                "{{/hasReturn}}" +
                "{{#params}} , " +
                    "\"param.{{.}}.className\", \"\"+${{.}}.getClass().getName() , " +
                    "\"param.{{.}}.hashCode\", \"\"+System.identityHashCode(${{.}}) " +
                "{{/params}} });",
            "setTriggering(true);",
            "ENDRULE");

        final HashMap<String,Object> ruleValues = new HashMap<>();

        HashSet<String> ruleTargets = new HashSet<>();
        Predicate<String> targetIsNew = (ruleId)->{
            boolean added = ruleTargets.add(ruleId);
            if(!added){
                System.out.println(AsciiArt.ANSI_RED+" ALREADY HAVE "+AsciiArt.ANSI_RESET+" "+ruleId);
            }
            return added;
        };
        List<RulePattern> rules = Arrays.asList(jsonHelperStart,jsonHelperStop,hierarchyPattern,argPattern);

        final CollectingTypeVisitor collector = new CollectingTypeVisitor(false);
        final BiPredicate<Class,Class> isTypeAssignable = (type, classTarget)->{
            boolean rtrn = type.isAssignableFrom(classTarget) && !type.equals(Object.class);
            return rtrn;
        };
        final Function<Class,Predicate<Class>> getTargetMatcher = (classTarget)-> (type)-> isTypeAssignable.test(type,classTarget);
        final BiPredicate<Parameter,Class> isAssignable = (parameter, classTarget)->{
            collector.clear();
            collector.visit(parameter.getParameterizedType());
            return collector.getTypes().stream().anyMatch(getTargetMatcher.apply(classTarget));
        };
        final BiPredicate<Field,Class> isFieldAssignable = (field, classTarget)->{
            collector.clear();
            collector.visit(field.getGenericType());
            //should make the predicate a parameter not the classTarget
            return collector.getTypes().stream().anyMatch(getTargetMatcher.apply(classTarget));
        };
        final BiFunction<Parameter[],Class,Set<Integer>> paramIndexMatcher = (parameters, classTarget)->{
            LinkedHashSet<Integer> rtrn = new LinkedHashSet<>();
            for(int i=0; i<parameters.length; i++){
                Parameter p = parameters[i];
                if(isAssignable.test(p,classTarget)){
                    rtrn.add(1+i);//1 offset because byteman starts params at 1 not 0
                }
            }
            return rtrn;
        };
        final BiFunction<Field[],Class,Set<String>> fieldMatcher = (fields,classTarget)->{
            LinkedHashSet<String> rtrn = new LinkedHashSet<>();
            for(int i=0; i<fields.length; i++){
                Field f = fields[i];
                if(isFieldAssignable.test(f,classTarget)){
                    rtrn.add(f.getName());
                }
            }
            return rtrn;
        };

        List<String> targetClassNames = Arrays.asList("org.apache.activemq.artemis.api.core.ActiveMQBuffers$AMQBuffer","org.apache.activemq.artemis.core.server.impl.MessageReferenceImpl");

        try (Writer ruleWriter = new FileWriter("/home/wreicher/script/byteman/MessageReference.Arg.btm")){

            Map<String,Object> helperMap = new HashMap<>();
            helperMap.put("frameIndex","/home/wreicher/perfWork/byteBuffer/frameIndex.json");
            helperMap.put("threadIndex","/home/wreicher/perfWork/byteBuffer/threadNames.json");

            jsonHelperStart.apply(ruleWriter,helperMap);
            jsonHelperStop.apply(ruleWriter,helperMap);

            for(String targetClassName : targetClassNames) {
                System.out.println("Starting scan for "+targetClassName);
                Class targetClass = classLoader.loadClass(targetClassName);

                ruleValues.put("targetClassName", targetClassName);
                //ruleValues.put("logFile","/home/wreicher/perfWork/byteBuffer/hierarchyCalls.log");
                //changing logic to put everything in 1 file
                ruleValues.put("logFile", "/home/wreicher/perfWork/byteBuffer/argCalls.log");

                final MethodClassActor methodParamChecker = (method, currentClass) -> {
                    String signature = BytemanUtil.getMethodSignature(method);
                    ruleValues.put("classType", currentClass.isInterface() ? "INTERFACE" : "CLASS");
                    ruleValues.put("currentClassName", currentClass.getName());
                    ruleValues.put("method", signature);
                    ruleValues.remove("params");
                    Parameter parameters[] = method.getParameters();
                    Set<Integer> paramSet = paramIndexMatcher.apply(parameters, targetClass);
                    if (!paramSet.isEmpty()) {//we have a parameter we need to track
                        ruleValues.put("params", paramSet);
                    }
                    if (Reflect.isStatic(method)) {
                        ruleValues.remove("nonStatic");
                    } else {
                        ruleValues.put("nonStatic", true);
                    }
                    collector.clear();
                    collector.visit(method.getGenericReturnType());
                    boolean returnTypeMatch = collector.getTypes().stream().anyMatch(getTargetMatcher.apply(targetClass));
                    if (returnTypeMatch) {
                        ruleValues.put("tracePoint", "EXIT");
                        ruleValues.put("hasReturn", true);
                    } else {
                        ruleValues.put("tracePoint", "ENTRY");
                        ruleValues.remove("hasReturn");
                    }
//                System.out.println(
//                        AsciiArt.ANSI_PURPLE + currentClass.getName().toString() + AsciiArt.ANSI_RESET + " " +
//                                vals.get("params") + " " +
//                                AsciiArt.ANSI_CYAN + signature + AsciiArt.ANSI_RESET);
                    //hierarchyPattern.apply(ruleWriter,vals);
                    if (targetIsNew.test(currentClass.getName() + "." + signature)) {
                        hierarchyPattern.apply(ruleWriter, ruleValues);
                    } else {
                        System.out.println("hierarchyPattern skipping " + currentClass.getName() + "." + signature);
                    }
                    return "";
                };
                final ConstructorClassActor constructorParamChecker = (constructor, currentClass) -> {
                    String signature = BytemanUtil.getConstructorSignature(constructor);
                    ruleValues.put("classType", currentClass.isInterface() ? "INTERFACE" : "CLASS");
                    ruleValues.put("currentClassName", currentClass.getName());
                    ruleValues.put("method", signature);

                    ruleValues.remove("params");
                    Parameter parameters[] = constructor.getParameters();
                    Set<Integer> paramSet = paramIndexMatcher.apply(parameters, targetClass);
                    if (!paramSet.isEmpty()) {//we have a parameter we need to track
                        ruleValues.put("params", paramSet);
                    }
                    ruleValues.put("nonStatic", true);
                    ruleValues.put("tracePoint", "ENTRY");
                    ruleValues.remove("hasReturn");
//                System.out.println(
//                        AsciiArt.ANSI_PURPLE + currentClass.getName().toString() + AsciiArt.ANSI_RESET + " " +
//                                vals.get("params") + " " +
//                                AsciiArt.ANSI_CYAN + signature + AsciiArt.ANSI_RESET);

                    if (targetIsNew.test(currentClass.getName() + "." + signature)) {
                        hierarchyPattern.apply(ruleWriter, ruleValues);
                    } else {
                        System.out.println("hierarchyPattern skipping " + currentClass.getName() + "." + signature);
                    }
                    return "";
                };
                HierarchyWalker walker = new HierarchyWalker();
                walker.addMethodClassActor("method", methodParamChecker);
                walker.addConstructorClassActor("constructor", constructorParamChecker);

                walker.walk(targetClass);
                System.out.println("Finished walking hierarchy for: " + targetClassName);
                System.out.println("Scanning classpath for potential usage");

                ruleValues.put("logFile", "/home/wreicher/perfWork/byteBuffer/argCalls.log");

                classLoader.getLoadedClasses().forEach((knownClass) -> {
                    ruleValues.put("currentClassName", knownClass.getName());
                    ruleValues.put("classType", knownClass.isInterface() ? "INTERFACE" : "CLASS");
                    try {
                        ruleValues.remove("fields");
                        Field fields[] = knownClass.getDeclaredFields();
                        Set<String> fieldSet = fieldMatcher.apply(fields, targetClass);
                        if (!fieldSet.isEmpty()) {
                            ruleValues.put("fields", fieldSet);
                        }

                        for (Constructor c : knownClass.getDeclaredConstructors()) {
                            ruleValues.remove("params");
                            collector.clear();
                            Parameter parameters[] = c.getParameters();
                            Set<Integer> paramSet = paramIndexMatcher.apply(parameters, targetClass);
                            if (!paramSet.isEmpty()) {
                                ruleValues.put("params", paramSet);
                            } else {
                                ruleValues.remove("params");
                            }
                            if (!paramSet.isEmpty() || !fieldSet.isEmpty()) {//we have a parameter we need to track
                                String methodSignature = BytemanUtil.getConstructorSignature(c);
                                ruleValues.put("method", methodSignature);
                                ruleValues.put("tracePoint", "ENTRY");
                                ruleValues.remove("hasReturn");
//                            System.out.println(
//                                    AsciiArt.ANSI_PURPLE + knownClass.getName().toString() + AsciiArt.ANSI_RESET + " " +
//                                            vals.get("params") + " " +
//                                            AsciiArt.ANSI_CYAN + methodSignature + AsciiArt.ANSI_RESET);

                                if (targetIsNew.test(knownClass.getName() + "." + methodSignature)) {
                                    argPattern.apply(ruleWriter, ruleValues);
                                } else {
                                    System.out.println("argPattern skipping " + knownClass.getName() + "." + methodSignature);
                                }
                            }
                        }

                        for (Method m : knownClass.getDeclaredMethods()) {
                            ruleValues.remove("params");
                            Parameter parameters[] = m.getParameters();
                            Set<Integer> paramSet = paramIndexMatcher.apply(parameters, targetClass);
                            collector.clear();
                            collector.visit(m.getGenericReturnType());
                            boolean returnTypeMatch = collector.getTypes().stream().anyMatch(getTargetMatcher.apply(targetClass));
                            if (returnTypeMatch) {
                                ruleValues.put("tracePoint", "EXIT");
                                ruleValues.put("hasReturn", true);
                            } else {
                                ruleValues.put("tracePoint", "ENTRY");
                                ruleValues.remove("hasReturn");
                            }
                            if (Reflect.isStatic(m)) {
                                ruleValues.remove("nonStatic");
                            } else {
                                ruleValues.put("nonStatic", true);
                            }
                            if (!paramSet.isEmpty()) {
                                ruleValues.put("params", paramSet);
                            } else {
                                ruleValues.remove("params");
                            }
                            String methodSignature = BytemanUtil.getMethodSignature(m);
                            ruleValues.put("method", methodSignature);
                            if (!paramSet.isEmpty() || returnTypeMatch || !fieldSet.isEmpty()) {//we have a parameter we need to track
//                            System.out.println(
//                                AsciiArt.ANSI_PURPLE + knownClass.getName().toString() + AsciiArt.ANSI_RESET + " " +
//                                vals.get("params") + " " +
//                                AsciiArt.ANSI_CYAN + methodSignature + AsciiArt.ANSI_RESET);

                                if (targetIsNew.test(knownClass.getName() + "." + methodSignature)) {
                                    argPattern.apply(ruleWriter, ruleValues);
                                } else {
                                    System.out.println("argPattern skipping " + knownClass.getName() + "." + methodSignature);
                                }
                            }
                        }
                    } catch (NoClassDefFoundError | TypeNotPresentException e) {
                    }
                });
                ruleWriter.flush();
                System.out.println(targetClassName);
                for(RulePattern rule : rules){
                    System.out.println(
                            rule.getName() +
                                    " count " +
                                    AsciiArt.ANSI_CYAN +
                                    rule.getUid() +
                                    AsciiArt.ANSI_RESET);
                }
            }
        } catch (ClassNotFoundException | TypeNotPresentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(RulePattern rule : rules){
            System.out.println(
                rule.getName() +
                " count " +
                AsciiArt.ANSI_CYAN +
                rule.getUid() +
                AsciiArt.ANSI_RESET);
        }


    }
}
