package perf.byteman;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Created by wreicher
 */
public class BytemanUtil {

    /**
     * Return a String suitable to uniquely identify Constructor c in a Byteman METHOD line
     * @param c
     * @return
     */
    public static String getConstructorSignature(Constructor c){
        StringBuilder rtrn = new StringBuilder();
        rtrn.append("<init>(");
        Parameter params[] = c.getParameters();
        for(int i=0; i<params.length; i++){
            Parameter p = params[i];
            if(i>0){
                rtrn.append(", ");
            }
            rtrn.append(paramClassToString(p.getType()));
        }
        rtrn.append(")");
        return rtrn.toString();
    }

    /**
     * Return a String suitable to uniquely identify Method m in a Byteman METHOD line
     * @param m
     * @return
     */
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
            rtrn.append(paramClassToString(p.getType()));
        }
        rtrn.append(")");
        return rtrn.toString();
    }

    /**
     * Return a String representation of a Parameter class for use as a Byteman METHOD line parameter
     * @param param
     * @return
     */
    public static String paramClassToString(Class param){
        StringBuilder suffix = new StringBuilder();
        if(param.isArray()){
            while(param.isArray()) {
                suffix.append("[]");
                param = param.getComponentType();
            }
        }
        return param.getName()+suffix.toString();
    }
}
