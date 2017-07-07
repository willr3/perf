package perf.util;

import java.util.*;

/**
 * Created by wreicher
 */
public class Sets {

    public static <T> Set<T> getOverlap(Set<T> a, Set<T> b){
        Set<T> rtrn = new HashSet<T>(a);
        rtrn.retainAll(b);
        return rtrn;
    }
    public static <T> Set<T> of(T...t){
        LinkedHashSet<T> rtrn = new LinkedHashSet<>();
        if(t!=null && t.length > 0){
            rtrn.addAll(Arrays.asList(t));
        }
        return rtrn;
    }

}
