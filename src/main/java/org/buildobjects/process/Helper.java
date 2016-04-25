package org.buildobjects.process;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class Helper {

    public static <T> Set<T> asSet(T... elements) {
        HashSet<T> set = new HashSet<T>();
        set.addAll(asList(elements));
        return set;
    }

    public static Set<Integer> asSet(int[] elements) {
        HashSet<Integer> set = new HashSet<Integer>();
        for (int element : elements) {
            set.add(element);
        }
        return set;
    }
}
