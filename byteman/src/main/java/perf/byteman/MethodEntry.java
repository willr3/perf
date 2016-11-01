package perf.byteman;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wreicher
 */
public class MethodEntry {

    public static enum MethodType {Abstract,Final,Impl,Init}

    private int id;
    private MethodType type;
    private ClassEntry parent;
    private List<ClassEntry> args;

    public MethodEntry(int id, MethodType type, ClassEntry parent) {
        this.id = id;
        this.type = type;
        this.parent = parent;
        this.args = new ArrayList<>();
    }

    public int getId(){return id;}
    public MethodType getType(){return type;}
    public ClassEntry getParent(){return parent;}
    public String toJson() {
        StringBuilder rtrn = new StringBuilder();
        rtrn.append("{");
        rtrn.append("\"id\":");
        rtrn.append(id);
        rtrn.append(",\"T\":");
        rtrn.append("\"" + type + "\"");
        if (this.parent != null) {
            rtrn.append(",\"parentId\":");
            rtrn.append(this.parent.getId());
        }
        rtrn.append(",\"overrides\":");

        rtrn.append(",\"args\":");
        if (args.isEmpty()) {
            rtrn.append("[]");
        } else {
            rtrn.append("[");
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    rtrn.append(",");
                }
                rtrn.append(args.get(i).getId());
            }
            rtrn.append("]");
        }
        return rtrn.toString();
    }

    public void addArg(ClassEntry argClass) {
        args.add(argClass);
    }
    public List<ClassEntry> getArgs(){return Collections.unmodifiableList(args);}

    public static MethodEntry.MethodType toMethodType(Method m){
        if(Modifier.isAbstract(m.getModifiers())){
            return MethodType.Abstract;
        }
        if(Modifier.isFinal(m.getModifiers())){
            return MethodType.Final;
        }
        return MethodType.Impl;
    }
}
