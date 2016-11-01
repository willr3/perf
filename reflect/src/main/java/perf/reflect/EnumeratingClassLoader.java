package perf.reflect;

import perf.util.StringUtil;
import perf.util.file.FileUtility;
import perf.util.xml.XmlLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by wreicher
 */
public class EnumeratingClassLoader extends ClassLoader {

    private URLClassLoader classLoader;
    private List<String> jarPaths;
    private Map<String,List<String>> jarClasses;
    private Map<String,Class> loadedClasses;
    private Set<String> failedLoad;
    private Set<String> missingDependencies;
    private ClassLoader parentLoader;

    public void addJarPath(String path){
        jarPaths.add(path);
    }
    public void addModules(String modulePath){
        XmlLoader loader = new XmlLoader();
        FileUtility.getFiles(modulePath,"module.xml",true).forEach(
            (moduleXml)->{
                File moduleFile = new File(moduleXml);
                loader.getValues(moduleXml+FileUtility.SEARCH_KEY+"./resources/resource-root[@path]/@path").forEach(
                    (resourcePath)->{
                        if(resourcePath.endsWith(".jar")){
                            String toAdd = moduleFile.getParent()+File.separator+resourcePath;
                            jarPaths.add(toAdd);
                        }
                    }
                );
            }
        );
    }
    public Map<String,Set<String>> classtoJars(){
        Map<String,Set<String>> rtrn = new HashMap<>();

        for(String jarName : jarClasses.keySet()){
            List<String> classNames = jarClasses.get(jarName);
            for(String className : classNames){
                if(!rtrn.containsKey(className)){
                    rtrn.put(className,new HashSet<>());
                }
                rtrn.get(className).add(jarName);
            }
        }
        return rtrn;
    }
    public EnumeratingClassLoader(){
        this(null);
    }
    public EnumeratingClassLoader(ClassLoader parentLoader){
        jarPaths = new ArrayList<>();
        jarClasses = new HashMap<>();

        loadedClasses = new HashMap<>();

        failedLoad = new HashSet<>();
        missingDependencies = new HashSet<>();
        this.parentLoader = parentLoader;
    }

    public String toJson(){
        return toJson(true,true);
    }
    private void printMap(StringBuilder sb,Map<String,? extends Collection<String>> map){
        String NL = System.lineSeparator();
        sb.append("{");
        for(Iterator<String> keyIter = map.keySet().iterator(); keyIter.hasNext();){
            String keyName = keyIter.next();
            sb.append("\""+keyName+"\": [");
            for(Iterator<String> colIter = map.get(keyName).iterator(); colIter.hasNext();){
                String colString = colIter.next();
                sb.append("\""+colString+"\"");
                if(colIter.hasNext()){
                    sb.append(", "+NL);
                }
            }
            if(keyIter.hasNext()){
                sb.append("], "+NL);
            }else{
                sb.append("]"+NL);
            }
        }
        sb.append("}");
    }
    public String toJson(boolean includeFail,boolean includeDependencies){
        StringBuilder sb = new StringBuilder();
        String NL = System.lineSeparator();
        sb.append("{");
        sb.append("\"jars\": "+NL);
        printMap(sb,jarClasses);
        sb.append(NL);
        if(includeFail){
            sb.append(", fails: [");
            for(Iterator<String> failIter = failedLoad.iterator(); failIter.hasNext();){
                String failName = failIter.next();
                sb.append("\""+failName+"\"");
                if(failIter.hasNext()){
                    sb.append(", "+NL);
                }
            }
            sb.append("]"+NL);
        }
        if(includeDependencies){
            sb.append(", dependencies: [");
            for(Iterator<String> depIter = missingDependencies.iterator(); depIter.hasNext();){
                String depName = depIter.next();
                sb.append("\""+depName+"\"");
                if(depIter.hasNext()){
                    sb.append(", "+NL);
                }
            }
            sb.append("]"+NL);
        }
        return sb.toString();
    }


    public List<String> getClassNames(){return jarClasses.values().stream().flatMap(list-> list.stream()).collect(Collectors.toList());}
    public long getJarClassCount(){return jarClasses.values().stream().flatMap(list-> list.stream()).collect(Collectors.counting());}
    public int getJarCount(){return jarClasses.size();}
    public int getJarClassCount(String jarPath){
        if(!jarClasses.containsKey(jarPath)){
            return 0;
        }
        return jarClasses.get(jarPath).size();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        //System.out.println("loadClass "+name);
        if(loadedClasses.containsKey(name)){
            return loadedClasses.get(name);
        }
        if(classLoader==null){
            findClasses();
        }
        try {
            Class rtrn = classLoader.loadClass(name);


            //called to make sure the class satisfies all dependencies
            rtrn.getDeclaredClasses();
            rtrn.getDeclaredConstructors();
            rtrn.getDeclaredFields();
            rtrn.getDeclaredMethods();
            rtrn.getDeclaredAnnotations();
            if(name.equals("org.apache.activemq.artemis.cli.commands.Configurable")){
                System.out.println("loaded org.apache.activemq.artemis.cli.commands.Configurable");
            }

            loadedClasses.put(name, rtrn);
            return rtrn;
        }catch(ClassNotFoundException e){
            failedLoad.add(name);
            throw e;
        }catch(NoClassDefFoundError e){
            failedLoad.add(name);
            missingDependencies.add(e.getMessage());
            throw e;
        }
    }
    public void loadClasses(){
        if(classLoader==null){
            findClasses();
        }
        getClassNames().stream().forEach(className->{
            try {
                Class loadedClass = this.loadClass(className);

            } catch (ClassNotFoundException e) {
                failedLoad.add(className);
            } catch (NoClassDefFoundError e){
                failedLoad.add(className);
                missingDependencies.add(e.getMessage());
            }
        });
    }
    public int getLoadedClassCount(){return loadedClasses.size();}
    public List<Class> getLoadedClasses(){return Collections.unmodifiableList(new ArrayList<>(loadedClasses.values()));}
    public Set<Class> getLoadedClassesSet(){return new HashSet<>(loadedClasses.values());}
    public int getFailedLoadCount(){return failedLoad.size();}
    public Set<String> getFailedLoad(){return Collections.unmodifiableSet(failedLoad);}
    public int getMissingDependenciesCount(){return missingDependencies.size();}
    public Set<String> getMissingDependencies(){return Collections.unmodifiableSet(missingDependencies);}

    public boolean isLoaded(String className){return loadedClasses.containsKey(className);}

    public void findClasses(){
        List<URL> jarUrls = new ArrayList<>();
        for(String path : jarPaths){
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
        if(parentLoader==null){
            classLoader = new URLClassLoader(jarUrls.toArray(new URL[1]));//,Thread.currentThread().getContextClassLoader());
        }else{
            classLoader = new URLClassLoader(jarUrls.toArray(new URL[1]),parentLoader);
        }

    }

    public static void main(String[] args) {
        long start,stop = 0;
        EnumeratingClassLoader classLoader = new EnumeratingClassLoader(Thread.currentThread().getContextClassLoader());
        classLoader.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/modules/");
        classLoader.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/jboss-modules.jar");
        start = System.currentTimeMillis();
        classLoader.findClasses();
        stop = System.currentTimeMillis();
        System.out.println("Finished findClasses() in "+ StringUtil.durationToString((stop-start)));
        System.out.println("  jarCount   "+classLoader.getJarCount());
        System.out.println("  classCount "+classLoader.getJarClassCount());
//        Set<String> duplcates = classLoader.classNames().stream()
//            .filter(className->Collections.frequency(classLoader.classNames(),className) > 1)
//            .collect(Collectors.toSet());
//        System.out.println("  duplicates "+duplcates.size());
        start = System.currentTimeMillis();
        classLoader.loadClasses();
        stop = System.currentTimeMillis();
        System.out.println("Finished loadClasses() in "+ StringUtil.durationToString((stop-start)));
        System.out.println("  loadedClasses     "+classLoader.getLoadedClassCount());
        System.out.println("  failedLoad        "+classLoader.getFailedLoadCount());
        System.out.println("  missingDependcies "+classLoader.getMissingDependenciesCount());

        try {
            Class amqBuffer = classLoader.loadClass("org.apache.activemq.artemis.api.core.ActiveMQBuffer");
            System.out.println(amqBuffer.getSuperclass());
            System.out.println(Arrays.asList(amqBuffer.getInterfaces()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }
}
