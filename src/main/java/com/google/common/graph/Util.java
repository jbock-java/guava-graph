package com.google.common.graph;

import java.util.LinkedHashSet;
import java.util.Set;

class Util {

    private Util() {
    }

    static <E> Set<E> setOf(Iterable<? extends E> elements) {
        Set<E> result = new LinkedHashSet<>();
        for (E element : elements) {
            result.add(element);
        }
        return result;
    }
}
