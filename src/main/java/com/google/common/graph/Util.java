package com.google.common.graph;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class Util {

    private Util() {
    }

    static <E> Set<E> setOf(Iterable<? extends E> elements) {
        return StreamSupport.stream(elements.spliterator(), false)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static <E> Set<E> difference(Set<E> set1, Set<E> set2) {
        return set1.stream()
                .filter(e -> !set2.contains(e))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static <E> Set<E> union(Set<E> set1, Set<E> set2) {
        var result = new LinkedHashSet<E>(Math.max(4, (int) (1.5 * (set1.size() + set2.size()))));
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    static <K, V> Map<K, V> asMap(
            Set<K> set, Function<? super K, V> function) {
        var result = new LinkedHashMap<K, V>(Math.max(4, (int) (1.5 * set.size())));
        for (K k : set) {
            result.put(k, function.apply(k));
        }
        return result;
    }
}