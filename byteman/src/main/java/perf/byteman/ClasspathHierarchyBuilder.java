package perf.byteman;

import perf.util.Indexer;
import perf.util.StringUtil;
import perf.util.file.FileUtility;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by wreicher
 */
public class ClasspathHierarchyBuilder {

    public ClassEntry loadClass(Class loadedClass,ClassHierarchy classHeirarchy){
        Thread.currentThread().setContextClassLoader(classLoader);
        Class superClass = loadedClass.getSuperclass();
        ClassEntry loadedEntry = classHeirarchy.add(loadedClass);

        //build the parent hierarchy
        ClassEntry targetEntry = loadedEntry;
        ClassEntry parentEntry;
        if(superClass!=null && !superClass.equals(Object.class)) {//don't do Object superclass
            do {
                parentEntry = classHeirarchy.add(superClass);
                parentEntry.addExtender(targetEntry);
                targetEntry = parentEntry;
            } while ((superClass = superClass.getSuperclass())!=null && !superClass.equals(Object.class));
        }
        for(Class interfaces : loadedClass.getInterfaces()){
            ClassEntry interfaceEntry = classHeirarchy.add(interfaces);
            loadedEntry.addInterface(interfaceEntry);
            interfaceEntry.addImplementer(loadedEntry);
        }

        try {
            Method methods[] = loadedClass.getMethods();
            for (Method method : methods) {
                MethodEntry methodEntry = new MethodEntry(methodIndexer.add(method.getName()/*getMethodSignature(method)*/), MethodEntry.toMethodType(method), loadedEntry);
                Parameter parameters[] = method.getParameters();
                for (Parameter parameter : parameters) {
                    ClassEntry argEntry = classHeirarchy.add(parameter.getType());
                    methodEntry.addArg(argEntry);
                }
                loadedEntry.addMethod(methodEntry);
            }
        } catch (NoClassDefFoundError e){
            loadedEntry.setMethodErrors(true);
            Throwable c = e;
            while( (c=c.getCause())!=null){
                if(c instanceof ClassNotFoundException){
                    missingMethodClasses.add(c.getMessage());

                }
            }
        }
        try {
            Constructor constructors[] = loadedClass.getConstructors();
            for (Constructor constructor : constructors) {
                MethodEntry methodEntry = new MethodEntry(methodIndexer.add(BytemanUtil.getConstructorSignature(constructor)), MethodEntry.MethodType.Init, loadedEntry);
                Parameter parameters[] = constructor.getParameters();
                for (Parameter parameter : parameters) {
                    ClassEntry argEntry = classHeirarchy.add(parameter.getType());
                    methodEntry.addArg(argEntry);
                }
                loadedEntry.addMethod(methodEntry);
            }
        } catch (NoClassDefFoundError e){
            loadedEntry.setMethodErrors(true);
            Throwable c = e;
            while( (c=c.getCause())!=null){
                if(c instanceof ClassNotFoundException){
                    missingMethodClasses.add(c.getMessage());
                }
            }
        }
        return loadedEntry;
    }

    private Indexer<String> classNameIndexer;
    private Indexer<String> methodIndexer;
    private List<String> searchDirectories;
    private HashSet<String> erroredClasses;
    private HashSet<String> missingClasses;
    private HashSet<String> missingMethodClasses;
    private Map<String,List<String>> jarClasses;
    private ClassHierarchy classHierarchy;

    private ClassLoader classLoader;

    public ClasspathHierarchyBuilder(){
        classNameIndexer = new Indexer<>();
        methodIndexer = new Indexer<>();
        searchDirectories = new LinkedList<>();
        erroredClasses = new HashSet<>();
        missingClasses = new HashSet<>();
        missingMethodClasses = new HashSet<>();

        jarClasses = new HashMap<>();
        classHierarchy = new ClassHierarchy(classNameIndexer);

        classLoader = null;
    }

    public ClassLoader getClassLoader(){return classLoader;}
    public void addJarPath(String path){
        searchDirectories.add(path);
    }

    public void findClasses(){
        List<URL> jarUrls = new ArrayList<>();
        for(String path : searchDirectories){
            List<String> filePaths = FileUtility.getFiles(path,".jar",true);
            for(String filePath : filePaths){
                try{
                    jarUrls.add((new File(filePath)).toURI().toURL());
                    List<String> classNames = new ArrayList<>();
                    jarClasses.put(filePath,classNames);
                    ZipInputStream zip = new ZipInputStream(new FileInputStream(filePath));
                    for(ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()){
                        if(!entry.isDirectory() && entry.getName().endsWith(".class")){
                            String className = entry.getName().replace('/', '.'); // including ".class"
                            classNames.add(className.substring(0, className.length() - ".class".length()));
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        classLoader = new URLClassLoader(jarUrls.toArray(new URL[1]));//,Thread.currentThread().getContextClassLoader());
    }
    public void processClasses(){
        if(classLoader==null){
            findClasses();
        }
        for(List<String> classList : jarClasses.values()){
            for(String className : classList){
                if(className.equals("java.lang.Object")){
                    continue;
                }
                Class loadedClass = null;
                ClassEntry loadedEntry = null;
                try {
                    loadedClass = classLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    missingClasses.add(className);
                } catch (NoClassDefFoundError e){
                    Throwable c = e;
                    while( (c=c.getCause())!=null){
                        if(c instanceof ClassNotFoundException){
                            missingClasses.add(c.getMessage());
                        }
                    }
                }
                if(loadedClass!=null){
                    loadedEntry = loadClass(loadedClass,classHierarchy);
                }else{
                    loadedEntry = classHierarchy.add(className,null, ClassEntry.ClassType.Error);
                }
            }
        }
    }
    public Set<String> getErroredClasses(){return Collections.unmodifiableSet(erroredClasses);}
    public Set<String> getMissingClasses(){return Collections.unmodifiableSet(missingClasses);}
    public Set<String> getMissingMethodClasses(){return Collections.unmodifiableSet(missingMethodClasses);}
    public int getClassCount(){
        return jarClasses.values().stream().mapToInt(List::size).sum();
    }
    public int getJarCount(){
        return jarClasses.size();
    }
    public List<String> getJarNames(){
        return jarClasses.keySet().stream().collect(Collectors.toList());
    }
    public List<String> getJarClasses(String jarName){
        return Collections.unmodifiableList(jarClasses.get(jarName));
    }
    public Indexer<String> getClassNameIndexer(){return classNameIndexer;}
    public Indexer<String> getMethodIndexer(){return methodIndexer;}
    public ClassHierarchy getClassHierarchy(){return classHierarchy;}
    public void writeClassHierarchy(String outputPath){
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))){
            List<String> classNameIndex = classNameIndexer.getIndexedList();
            writer.println("{");
            writer.println("{");
            writer.println("\"classHierarchy\":[");
            for(Iterator<Integer> iter = classHierarchy.getIds().iterator(); iter.hasNext();){
                ClassEntry e = classHierarchy.get(iter.next());
                writer.println(e.toJson()+(iter.hasNext()?",":""));
            }
            writer.println("]");
            writer.println(",");
            writer.println("\"classIndex\":[");
            for(int i=0; i<classNameIndex.size(); i++){
                if(i>0){
                    writer.print(",");
                }
                writer.println("\""+classNameIndex.get(i)+"\"");
            }
            writer.println("]");
//            writer.println("\"methodIndex\":[");
//            List<String> methodIndex = methodIndexer.getIndexedList();
//            for(int i=0; i<methodIndex.size(); i++){
//                if(i>0){
//                    writer.print(",");
//                }
//                writer.println("\""+methodIndex.get(i)+"\"");
//            }
//            writer.println("]");
            writer.println(",\"missingClasses\":[");
            for(Iterator<String> iter = missingClasses.iterator(); iter.hasNext();){
                writer.println("\""+iter.next()+"\"");
                if(iter.hasNext()){
                    writer.print(",");
                }
            }
            writer.print("]");

            writer.println(",\"missingMethodClasses\":[");
            for(Iterator<String> iter = missingMethodClasses.iterator(); iter.hasNext();){
                writer.println("\""+iter.next()+"\"");
                if(iter.hasNext()){
                    writer.print(",");
                }
            }
            writer.print("]");

            writer.println("}");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        ClasspathHierarchyBuilder builder = new ClasspathHierarchyBuilder();
        builder.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final/modules/");
        builder.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final/jboss-modules.jar");
        builder.findClasses();
        System.out.println("Found "+builder.getJarCount()+" jars with "+builder.getClassCount()+" classes");
        long start = System.currentTimeMillis();
        builder.processClasses();
        long stop = System.currentTimeMillis();
        System.out.println("Finished in "+ StringUtil.durationToString(stop-start));
        System.out.println("  "+builder.getClassHierarchy().size()+" loaded classes");
        System.out.println("  "+builder.getMethodIndexer().size()+" unique methods");
        System.out.println("  "+builder.getClassHierarchy().entryStream().mapToInt((classEntry -> classEntry.getMethods().size())).sum()+" total methods");
        System.out.println("  "+builder.erroredClasses.size()+" unloadable classes");
        System.out.println("  "+builder.missingClasses.size()+" missing class dependendcies");
        System.out.println("  "+builder.missingMethodClasses.size()+" missing method dependendcies");


        HashSet<String> alreadyAdded = new HashSet<>();

        ClassEntry byteBuffer = builder.classHierarchy.get("org.apache.activemq.artemis.api.core.ActiveMQBuffer");
        ClassEntry message = builder.classHierarchy.get("org.apache.activemq.artemis.api.core.Message");

        ClassEntry messageRef = builder.classHierarchy.get("org.apache.activemq.artemis.core.server.MessageReference");

        Thread.currentThread().setContextClassLoader(builder.classLoader);

        try (PrintWriter out = new PrintWriter(new FileWriter("/home/wreicher/script/byteman/AllUsage.btm"))){
            AtomicInteger uid = new AtomicInteger(0);
            AtomicInteger counter = new AtomicInteger(0);
            boolean doItAgain = true;
            while(doItAgain){
                doItAgain=false;
                builder.classHierarchy.entryStream().forEach((classEntry -> {
                    counter.incrementAndGet();
                    if(classEntry.getType()!= ClassEntry.ClassType.Error && classEntry.getEntryClass()!=null && !classEntry.hasMethodErrors()){
                        try {
                            for(Constructor c : classEntry.getEntryClass().getConstructors()){
                                String constructorSignature = BytemanUtil.getConstructorSignature(c);
                                boolean writeConstructor = false;
                                StringBuilder param = new StringBuilder();
                                if(classEntry.isA(byteBuffer) || classEntry.isA(message)){
                                    writeConstructor=true;
                                }
                                Parameter parameters[] = c.getParameters();
                                for(int i=0; i< parameters.length; i++){
                                    Parameter p = parameters[i];
                                    Class paramType = p.getType();
                                    ClassEntry paramEntry = builder.classHierarchy.get(paramType.getName());
                                    if(paramEntry==null){
                                        //System.out.println("No ClassEntry for "+paramType.getName()+" of "+classEntry.entryClass.getName()+" "+methodSignature);
                                        builder.missingMethodClasses.add(paramType.getName());
                                    }else if(paramEntry.isA(byteBuffer) || paramEntry.isA(message)){
                                        param.append(", \"param_" + i + "\", ");
                                        param.append("\"\"+System.identityHashCode($" + i + ")");
                                        writeConstructor=true;
                                    }
                                }
                                if(writeConstructor && !alreadyAdded.contains(classEntry.getId()+"."+writeConstructor)){
                                    alreadyAdded.add(classEntry.getId()+"."+writeConstructor);
                                    writeRule(classEntry.getEntryClass().getName(),constructorSignature,uid.getAndIncrement()+"",param.toString(),out);
                                }
                            }

                            for(Method m : classEntry.getEntryClass().getMethods()){
                                String methodSignature = BytemanUtil.getMethodSignature(m);
                                boolean writeMethod = false;
                                StringBuilder param = new StringBuilder();
                                if(classEntry.isA(byteBuffer) || classEntry.isA(message)){
                                    writeMethod=true;
                                }
                                Parameter parameters[] = m.getParameters();
                                for(int i=0; i< parameters.length; i++){
                                    Parameter p = parameters[i];
                                    Class paramType = p.getType();
                                    ClassEntry paramEntry = builder.classHierarchy.get(paramType.getName());
                                    if(paramEntry==null){
                                        //System.out.println("No ClassEntry for "+paramType.getName()+" of "+classEntry.entryClass.getName()+" "+methodSignature);
                                        builder.missingMethodClasses.add(paramType.getName());
                                    }else if(paramEntry.isA(byteBuffer) || paramEntry.isA(message)){
                                        param.append(", \"param_" + i + "\", ");
                                        param.append("\"\"+System.identityHashCode($" + i + ")");
                                        writeMethod=true;
                                    }
                                }
                                if(writeMethod && !alreadyAdded.contains(classEntry.getId()+"."+methodSignature)){
                                    alreadyAdded.add(classEntry.getId()+"."+methodSignature);
                                    writeRule(classEntry.getEntryClass().getName(),methodSignature,uid.getAndIncrement()+"",param.toString(),out);
                                }
                            }
                        }catch (NoClassDefFoundError e){ }
                    }
                }));
            }
            System.out.println("COUNTER="+counter.get());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("post rule building");
        System.out.println("  "+builder.missingMethodClasses.size()+" missing method dependendcies");
        builder.writeClassHierarchy("/home/wreicher/perfWork/classHierarchy/classHierarchy.2.json");
        System.out.println("rule count = "+ruleCount.get());
    }

    public static AtomicInteger ruleCount=new AtomicInteger(0);
    private static void writeRule(String className,String method,String uid,String meToo,PrintWriter out){
        ruleCount.incrementAndGet();
        out.println("RULE "+className+"_"+uid);
        out.println("CLASS ^"+className);
        out.println("METHOD "+method);
        out.println("HELPER perf.byteman.JsonHelper");
        out.println("AT ENTRY");
        //out.println("IF $this.getClass().getName().equals(\""+instanceClass+"\")");
        out.println("IF TRUE");
        out.println("DO");
        out.println("setTriggering(false);");
        out.println("openTrace(\"hc\",\"/home/wreicher/perfWork/byteBuffer/btm.json.log\");");
        out.println("traceJsonStack(\"hc\", new String[]{"+
                "\"className\",$this.getClass().getName(), "+
                "\"hashCode\",\"\"+System.identityHashCode($this)"+
                meToo+" }"
                +");");
        out.println("setTriggering(true);");
        out.println("ENDRULE");
        out.println("");
    }
}
