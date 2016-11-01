package perf.byteman;

import perf.util.file.FileUtility;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by wreicher
 */
public class ClassMethodUsage {

    private PrintStream out;
    private static final AtomicInteger uid= new AtomicInteger(0);

    private List<String> classPaths;
    private String targetClass;
    HashSet<String> methods;

    public ClassMethodUsage(){
        out = System.out;
        classPaths = new LinkedList<>();
        targetClass = "";
        methods = new HashSet<>();
    }

    private void printRule(String className,String instanceClass,String method,String meToo){
        out.println("RULE "+className+"_"+uid.getAndIncrement());
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
    public void addClassPaths(List<String> classPaths){
        classPaths.addAll(classPaths);
    }
    public void addClassPath(String classPath){
        classPaths.add(classPath);
    }
    public void setOutput(String filePath){
        FileOutputStream btmFile = null;
        try {
            btmFile = new FileOutputStream(filePath);
            out = new PrintStream(btmFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static String getConstructorSignature(Constructor c){
        StringBuilder rtrn = new StringBuilder();
        rtrn.append("<init>(");
        Parameter params[] = c.getParameters();
        for(int i=0; i<params.length; i++){
            Parameter p = params[i];
            if(i>0){
                rtrn.append(", ");
            }
            rtrn.append(p.getType().getName());
        }
        rtrn.append(")");
        return rtrn.toString();
    }
    public static String getMethodSignature(Method m){
        StringBuilder rtrn = new StringBuilder();
        rtrn.append(m.getReturnType().getName());
        rtrn.append(" ");
        rtrn.append(m.getName());
        rtrn.append("(");
        Parameter params[] = m.getParameters();
        for(int i=0; i<params.length;i++){
            Parameter p = params[i];
            if(i>0){
                rtrn.append(", ");
            }
            rtrn.append(p.getType().getName());
        }
        rtrn.append(")");
        return rtrn.toString();
    }
    public static void main(String...args){
        ClassMethodUsage chw = new ClassMethodUsage();
        chw.addClassPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/modules/");
        List<URL> urls = new LinkedList<>();
        for(String classPath : chw.classPaths){
            List<String> filePaths = FileUtility.getFiles(classPath,".jar",true);
            Map<String,List<String>> jarClasses = new HashMap<>();
            for(String filePath: filePaths){

                try {
                    List<String> classNames = new ArrayList<>();
                    jarClasses.put(filePath,classNames);
                    ZipInputStream zip = new ZipInputStream(new FileInputStream(filePath));
                    for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                            // This ZipEntry represents a class. Now, what class does it represent?
                            String className = entry.getName().replace('/', '.'); // including ".class"
                            classNames.add(className.substring(0, className.length() - ".class".length()));
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    urls.add( (new File(filePath)).toURL() );
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            ClassLoader cl = new URLClassLoader(urls.toArray(new URL[1]),Thread.currentThread().getContextClassLoader());
            Class cs[] = new Class[]{cl.loadClass("org.apache.activemq.artemis.core.buffers.impl.ChannelBufferWrapper"),
                    cl.loadClass("org.apache.activemq.artemis.core.message.impl.MessageImpl")};

            FileOutputStream btmFile = new FileOutputStream("/home/wreicher/script/byteman/ChannelBufferWrapper_MessageImpl.json2.test.btm");
            chw.out = new PrintStream(btmFile);


            for(Class c : cs) {
                Class o = c;
                HashSet<String> methods = new HashSet<String>();
                //printRule(c.getName(),"<init>");
                chw.printRule(c.getName(), c.getName(), "<init>", "");
                do {
                    Method m[] = c.getDeclaredMethods();
                    for (int i = 0; i < m.length; i++) {

                        Method currentMethod = m[i];
                        Parameter[] p = currentMethod.getParameters();
                        StringBuilder sb = new StringBuilder();
                        for (int pi = 0; pi < p.length; pi++) {
                            Parameter param = p[pi];
                            if (param.getType().getName().equals("org.apache.activemq.artemis.api.core.ActiveMQBuffer")) {
                                sb.append(", \"param_" + pi + "\", ");
                                sb.append("\"\"+System.identityHashCode($" + pi + ")");
                            }
                        }
                        String methodSignature = getMethodSignature(currentMethod);
                        if (!methods.contains(methodSignature)) {
                            methods.add(methodSignature);
                            //printRule(c.getName(),m[i].getName());
                            chw.printRule(c.getName(), o.getName(), methodSignature, sb.toString());
                        }
                    }
                } while (!(c = c.getSuperclass()).equals(Object.class));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
