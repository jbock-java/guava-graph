package com.google.common.graph;

import static java.util.Spliterators.spliteratorUnknownSize;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A simple multimap that remembers the insertion order.
 *
 * @param <K> key type
 * @param <V> value type
 */
final class Multimap<K, V> {

    // set of all values, in insertion order
    private final Set<V> set;
    private final Map<K, Set<V>> map;

    // visible for testing
    Multimap(Set<V> set, Map<K, Set<V>> map) {
        this.set = set;
        this.map = map;
    }

    static <K, V> Multimap<K, V> create() {
        return new Multimap<>(new LinkedHashSet<>(), new LinkedHashMap<>());
    }

    static <K, V> Multimap<K, V> copyOf(Map<K, Set<V>> map) {
        Multimap<K, V> result = create();
        map.forEach((k, values) ->
                values.forEach(v -> result.put(k, v)));
        return result;
    }

    void put(K key, V value) {
        map.compute(key, (k, v) -> {
            if (v == null) {
                v = new LinkedHashSet<>();
            }
            if (v.add(value)) {
                set.add(value);
            }
            return v;
        });
    }

    void putAll(Multimap<K, V> multimap) {
        multimap.stream().forEach(e -> put(e.getKey(), e.getValue()));
    }

    void putAll(K key, Collection<V> values) {
        for (V value : values) {
            put(key, value);
        }
    }

    /* Values in insertion order. */
    Set<V> flatValues() {
        return set;
    }

    Collection<Set<V>> values() {
        return map.values();
    }

    Set<V> get(K key) {
        return map.getOrDefault(key, Set.of());
    }

    /* Entries in no particular order. */
    Stream<Entry<K, V>> stream() {
        Iterator<Entry<K, Set<V>>> iterator = map.entrySet().iterator();
        if (!iterator.hasNext()) {
            return Stream.empty();
        }
        return StreamSupport.stream(spliteratorUnknownSize(new Iterator<>() {

            Entry<K, Set<V>> current = iterator.next();
            Iterator<V> setIterator = current.getValue().iterator();

            @Override
            public boolean hasNext() {
                if (setIterator.hasNext()) {
                    return true;
                }
                while (iterator.hasNext()) {
                    current = iterator.next();
                    setIterator = current.getValue().iterator();
                    if (setIterator.hasNext()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Entry<K, V> next() {
                return new SimpleImmutableEntry<>(current.getKey(), setIterator.next());
            }
        }, Spliterator.ORDERED), false);
    }

    boolean isEmpty() {
        return map.isEmpty();
    }

    Multimap<K, V> filterKeys(Predicate<? super K> keyPredicate) {
        Multimap<K, V> result = create();
        map.entrySet()
                .stream()
                .filter(e -> keyPredicate.test(e.getKey()))
                .forEach(e -> e.getValue().forEach(v -> result.put(e.getKey(), v)));
        return result;
    }

    boolean containsKey(K k) {
        return map.containsKey(k);
    }

    boolean containsValue(V v) {
        for (Collection<V> collection : map.values()) {
            if (collection.contains(v)) {
                return true;
            }
        }
        return false;
    }

    Map<K, Set<V>> asMap() {
        return new HashMap<>(map);
    }
}
