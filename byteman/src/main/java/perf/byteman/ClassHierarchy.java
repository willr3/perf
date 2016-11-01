package perf.byteman;

import perf.util.Indexer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class ClassHierarchy {
    private Indexer<String> indexer;
    private HashMap<Integer, ClassEntry> classes;

    public ClassHierarchy(Indexer<String> indexer) {
        this.indexer = indexer;
        this.classes = new HashMap<>();

    }

    public Iterator<ClassEntry> classEntryIterator(){return classes.values().iterator();}
    public Stream<ClassEntry> entryStream() {
        return classes.values().stream();
    }
    public Indexer<String> getClassIndexer(){return indexer;}
    public ClassEntry add(String className, Class entryClass, ClassEntry.ClassType classType) {
        int id = indexer.add(className);
        if (classes.containsKey(id)) {
            return classes.get(id);
        } else {
            synchronized (this) {
                if (classes.containsKey(id)) {
                    return classes.get(id);
                } else {
                    classes.put(id, new ClassEntry(id, entryClass, classType));
                }
            }
        }
        return classes.get(id);
    }

    public ClassEntry add(Class entryClass) {
        ClassEntry.ClassType classType = ClassEntry.toClassType(entryClass);
        return add(entryClass.getName(), entryClass, classType);
    }

    public int size() {
        return classes.size();
    }

    public Set<Integer> getIds() {
        return classes.keySet();
    }

    public ClassEntry get(int id) {
        return classes.get(id);
    }

    public ClassEntry get(String className) {
        int id = indexer.get(className);
        return classes.get(id);
    }
}
