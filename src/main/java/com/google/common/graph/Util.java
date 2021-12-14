package com.google.common.graph;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Util {

    private Util() {
    }

    static <E> Set<E> setOf(Iterable<? extends E> elements) {
        LinkedHashSet<E> result = new LinkedHashSet<>();
        for (E element : elements) {
            result.add(element);
        }
        return Collections.unmodifiableSet(result);
    }

    static <E> Set<E> difference(Set<E> set1, Set<E> set2) {
        LinkedHashSet<E> result = set1.stream()
                .filter(e -> !set2.contains(e))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(result);
    }

    static <E> Set<E> union(Set<E> set1, Set<E> set2) {
        LinkedHashSet<E> result = Stream.concat(
                set1.stream(), set2.stream().filter((E e) -> !set1.contains(e)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(result);
    }
}