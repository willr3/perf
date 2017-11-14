package perf.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class HashedListTest {

    @Test
    public void addAllWithSomeOverlap(){
        HashedList<String> list = new HashedList<>();
        list.add("foo");
        list.add("bar");
        list.addAll(Arrays.asList("foo","bravo"));

        System.out.println(list.toList());
        Assert.assertEquals("List contains bravo",true,list.contains("bravo"));
    }

}



