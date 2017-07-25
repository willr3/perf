package perf.reflect;

import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Textifier;

/**
 * Created by wreicher
 */
public class Asmify {

    public static void main(String[] args) {
        try {
            //ASMifier.main(new String[]{"perf.reflect.test.Example"});
            Textifier.main(new String[]{"perf.reflect.test.Example"});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
