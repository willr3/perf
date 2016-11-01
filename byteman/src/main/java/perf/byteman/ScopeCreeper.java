package perf.byteman;

import perf.util.HashedLists;
import perf.util.StringUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by wreicher
 * Start with a set A of interfaces/classes and walk the CLASSPATH looking for all usage of those classes.
 * Classes can be added to set A if they implement/extend an entry in A or if they accept an entry of A as a Constructor argument.
 *
 */
public class ScopeCreeper {

    private enum Reason {Initial,HasField, HasConstructorParam, ImplementedBy ,IsA , AncestorOf}

    public static class ScopeAction {
        private Reason reason;
        private ClassEntry existingScopeEntry;

        public ScopeAction(Reason reason,ClassEntry existingScopeEntry ){
            this.reason = reason;
            this.existingScopeEntry = existingScopeEntry;
        }

    }

    private HashMap<String,HashMap<String,Boolean>> testMe;

    private HashSet<ClassEntry> scanScope;
    private HashedLists<ClassEntry,ScopeAction> ruleScope;
    private ClassHierarchy classHierarchy;
    private ClassLoader classLoader;
    public ScopeCreeper(ClassHierarchy classHierarchy,ClassLoader classLoader){
        this.scanScope = new HashSet<>();
        this.ruleScope = new HashedLists<>();
        this.classHierarchy = classHierarchy;
        this.classLoader = classLoader;
    }

    public int buildScope(){
        Thread.currentThread().setContextClassLoader(classLoader);
        int loopCount=0;
        boolean run=true;
        while(run){
            loopCount++;
            run=false;
            for(Iterator<ClassEntry> hierarchyIter = classHierarchy.classEntryIterator(); hierarchyIter.hasNext();) {
                ClassEntry classEntry = hierarchyIter.next();

                if(scanScope.contains(classEntry)) {
                    continue;
                }
                for(Iterator<ClassEntry> scopeIter = scanScope.iterator(); scopeIter.hasNext();){
                    ClassEntry scopeEntry = scopeIter.next();
                    if(classEntry.isA(scopeEntry)){
                        run=true;
                        expandScope(classEntry,Reason.IsA,scopeEntry);
                        break;
                    }
                }
                if(classEntry.getEntryClass()==null){
                    //System.out.println("null Class for "+classHierarchy.getClassIndexer().get(classEntry.getId()));
                    continue;
                }
                try {
                    for (Field field : classEntry.getEntryClass().getFields()) {
                        Class fieldType = field.getType();
                        while (fieldType.isArray()) {
                            fieldType = fieldType.getComponentType();
                        }
                        ClassEntry fieldEntry = classHierarchy.get(fieldType.getName());

                        if (fieldEntry == null) {
                            continue;
                        }
                        if (scanScope.contains(fieldEntry)) {
                            run = true;
                            expandScope(classEntry,Reason.HasField,fieldEntry);
                            break;
                        }
                        //if the field is a type that the hierarchyIter hasn't reached but we know it will be a subclass / implementer of current scanScope
                        for (Iterator<ClassEntry> scopeIter = scanScope.iterator(); scopeIter.hasNext(); ) {
                            ClassEntry scopeEntry = scopeIter.next();
                            if (fieldEntry.isA(scopeEntry)) {
                                run = true;
                                expandScope(classEntry,Reason.HasField,scopeEntry);
                                break;
                            }
                        }
                    }
                }catch(NoClassDefFoundError e){
                    //TODO track the no NoClassDefFoundError class
                }
                try {
                    for (Constructor constructor : classEntry.getEntryClass().getDeclaredConstructors() ){
                        for(Parameter parameter : constructor.getParameters()){

                            if(run){
                                break;// we already marked the class so stop processing the constructor
                            }
                            Class parameterClass = parameter.getType();
                            while (parameterClass.isArray()) {
                                parameterClass = parameterClass.getComponentType();
                            }
                            ClassEntry parameterEntry = classHierarchy.get(parameterClass.getName());

                            if (parameterEntry == null) {
                                continue;
                            }
                            if (scanScope.contains(parameterEntry)) {
                                run = true;
                                expandScope(classEntry,Reason.HasConstructorParam,parameterEntry);
                                break;
                            }
                            //if the field is a type that the hierarchyIter hasn't reached but we know it will be a subclass / implementer of current scanScope
                            for (Iterator<ClassEntry> scopeIter = scanScope.iterator(); scopeIter.hasNext(); ) {
                                ClassEntry scopeEntry = scopeIter.next();
                                if (parameterEntry.isA(scopeEntry)) {
                                    run = true;
                                    expandScope(classEntry,Reason.HasConstructorParam,parameterEntry);
                                    break;
                                }
                            }
                        }

                    }
                }catch(NoClassDefFoundError e){
                    //TODO track the no NoClassDefFoundError class
                }
            }
        }
        return loopCount;
    }

    public void addInitialScope(String className){
        ClassEntry entry = classHierarchy.get(className);
        if(entry==null){
            //TODO Panic!
        }
        expandScope(entry,Reason.Initial,entry);
    }
    public void expandScope(ClassEntry entry,Reason reason,ClassEntry scopeEntry){
        //TODO problem with this is we only need to capture parent class method invocation if it is an instance of entry
        //This information won't be passed to the scanScope set so it will result in logs of unnecessary data being collected
        //Not to mention the amount of data on Object :(
        ClassEntry expand = scopeEntry;
        ClassEntry target = entry;
        scanScope.add(entry);
        do {
            ruleScope.put(target,new ScopeAction(reason,expand));
            if(target.getEntryClass()!=null){
                for(Class iface : target.getEntryClass().getInterfaces()){
                    ClassEntry ifaceEntry = classHierarchy.get(iface.getName());
                    if(ifaceEntry!=null){
                        ruleScope.put(ifaceEntry,new ScopeAction(Reason.ImplementedBy,entry));
                    }
                }
            }
            reason = Reason.AncestorOf;
            expand = entry;
        }while( (target=target.getParent())!=null);
    }

    public static void main(String[] args) {
        ClasspathHierarchyBuilder builder = new ClasspathHierarchyBuilder();
        builder.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/modules/");
        builder.addJarPath("/home/wreicher/runtime/wildfly-10.0.0.Final-pool/jboss-modules.jar");
        builder.findClasses();

        System.out.println("Found "+builder.getJarCount()+" jars with "+builder.getClassCount()+" classes");

        long start = System.currentTimeMillis();
        builder.processClasses();
        long stop = System.currentTimeMillis();
        System.out.println("Finished processing classes in "+ StringUtil.durationToString(stop-start));

        ScopeCreeper creeper = new ScopeCreeper(builder.getClassHierarchy(),builder.getClassLoader());

//        creeper.addInitialScope("org.apache.activemq.artemis.api.core.ActiveMQBuffer");
//        creeper.addInitialScope("org.apache.activemq.artemis.api.core.Message");
        creeper.addInitialScope("org.apache.activemq.artemis.api.core.ActiveMQBuffers$AMQBuffer");

        //creeper.addInitialScope("io.netty.buffer.ByteBuf");
        start = System.currentTimeMillis();
        int loopCount = creeper.buildScope();
        stop = System.currentTimeMillis();
        System.out.println("Finsihed "+loopCount+" loops in "+StringUtil.durationToString((stop-start)));
        System.out.println("loopCount = "+loopCount);
        System.out.println("  scanScope = "+creeper.scanScope.size());
        System.out.println("  ruleScope = "+creeper.ruleScope.size());
        System.out.println("ruleScope:");
        creeper.ruleScope.keys().forEach(classEntry->{
            List<ScopeAction> actions = creeper.ruleScope.get(classEntry);
            System.out.println(creeper.classHierarchy.getClassIndexer().get(classEntry.getId()));
            actions.forEach(scopeAction->{
                System.out.println("  "+scopeAction.reason+" "+
                        (scopeAction.existingScopeEntry==null?
                                "":
                                creeper.classHierarchy.getClassIndexer().get(scopeAction.existingScopeEntry.getId()))
                );
            });
        });
        creeper.ruleScope.keys().forEach(classEntry->{
            Class entryClass = classEntry.getEntryClass();
            if(entryClass==null){
                return;
            }
            try {
                for (Constructor constructor : entryClass.getDeclaredConstructors()) {

                }
            }catch(NoClassDefFoundError ncdfe){
                System.out.println(ncdfe.getMessage()+" "+ncdfe.getCause().getMessage());
            }


        });
        System.out.println("scanScope:");
        creeper.scanScope.forEach(classEntry->{
            System.out.println(creeper.classHierarchy.getClassIndexer().get(classEntry.getId()));
        });

    }
}
