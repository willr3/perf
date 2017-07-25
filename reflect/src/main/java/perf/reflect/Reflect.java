package perf.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by wreicher
 */
public class Reflect {

    private String foo;

    Reflect(){}

    public static class Foo{

        public static class Bar{

        }
    }

    public boolean and(boolean a,boolean b){
        boolean rttrn = a || b;
        return rttrn;
    }
    public boolean isMulti(String...args){
        return false;
    }
    public Runnable getRunnable(){
        return ()->{
            System.out.print("hi");
        };
    }

    public static boolean isStatic(Method method,boolean flag) {
        return isStatic(method) && flag;
    }

    public static boolean isStatic(Method method){
        int modifiers = method.getModifiers();
        method.toString();
        String str = new String("hi mom");
        System.out.println("foo");
        int a=1,b=2,c=4;



        return Modifier.isStatic(modifiers);
    }
}
