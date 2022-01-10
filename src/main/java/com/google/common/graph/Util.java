package com.google.common.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    static <E> Set<E> mutableUnion(Set<E> set1, Set<E> set2) {
        if (set1 instanceof HashSet) {
            set1.addAll(set2);
            return set1;
        }
        return union(set1, set2);
    }

    static <E> Set<E> union(Set<E> set1, Set<E> set2) {
        Set<E> result = new LinkedHashSet<>(Math.max(4, (int) (1.5 * (set1.size() + set2.size()))));
        result.addAll(set1);
        result.addAll(set2);
        return result;
    }

    static <K, V> Map<K, V> asMap(
            Set<K> set, Function<? super K, V> function) {
        LinkedHashMap<K, V> result = new LinkedHashMap<>((int) (set.size() * 1.5));
        for (K k : set) {
            result.put(k, function.apply(k));
        }
        return result;
    }
}