package perf.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by wreicher
 */
public class Sets {

    public static <T> Set<T> getOverlap(Set<T> a, Set<T> b){
        Set<T> rtrn = new HashSet<T>(a);
        rtrn.retainAll(b);
        return rtrn;
    }

}
