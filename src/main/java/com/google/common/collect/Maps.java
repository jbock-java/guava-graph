/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.primitives.Ints;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

/**
 * Static utility methods pertaining to {@link Map} instances (including instances of {@link
 * SortedMap}, {@link BiMap}, etc.). Also see this class's counterparts {@link Lists} and {@link Sets}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#maps"> {@code Maps}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Isaac Shum
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Maps {
    private Maps() {
    }

    static <K, V> Iterator<K> keyIterator(
            Iterator<Entry<K, V>> entryIterator) {
        return new TransformedIterator<Entry<K, V>, K>(entryIterator) {
            @Override
            K transform(Entry<K, V> entry) {
                return entry.getKey();
            }
        };
    }

    static <K, V> Iterator<V> valueIterator(
            Iterator<Entry<K, V>> entryIterator) {
        return new TransformedIterator<Entry<K, V>, V>(entryIterator) {
            @Override
            V transform(Entry<K, V> entry) {
                return entry.getValue();
            }
        };
    }


    /**
     * Creates a {@code HashMap} instance, with a high enough "initial capacity" that it <i>should</i>
     * hold {@code expectedSize} elements without growth. This behavior cannot be broadly guaranteed,
     * but it is observed to be true for OpenJDK 1.7. It also can't be guaranteed that the method
     * isn't inadvertently <i>oversizing</i> the returned map.
     *
     * @param expectedSize the number of entries you expect to add to the returned map
     * @return a new, empty {@code HashMap} with enough capacity to hold {@code expectedSize} entries
     *     without resizing
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     */
    public static <K, V>
    HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
        return new HashMap<>(capacity(expectedSize));
    }

    /**
     * Returns a capacity that is sufficient to keep the map from being resized as long as it grows no
     * larger than expectedSize and the load factor is ≥ its default (0.75).
     */
    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            checkNonnegative(expectedSize, "expectedSize");
            return expectedSize + 1;
        }
        if (expectedSize < Ints.MAX_POWER_OF_TWO) {
            // This is the calculation used in JDK8 to resize when a putAll
            // happens; it seems to be the most conservative calculation we
            // can make.  0.75 is the default load factor.
            return (int) ((float) expectedSize / 0.75F + 1.0F);
        }
        return Integer.MAX_VALUE; // any large value
    }

    /**
     * Creates a {@code LinkedHashMap} instance, with a high enough "initial capacity" that it
     * <i>should</i> hold {@code expectedSize} elements without growth. This behavior cannot be
     * broadly guaranteed, but it is observed to be true for OpenJDK 1.7. It also can't be guaranteed
     * that the method isn't inadvertently <i>oversizing</i> the returned map.
     *
     * @param expectedSize the number of entries you expect to add to the returned map
     * @return a new, empty {@code LinkedHashMap} with enough capacity to hold {@code expectedSize}
     *     entries without resizing
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     * @since 19.0
     */
    public static <K, V>
    LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
        return new LinkedHashMap<>(capacity(expectedSize));
    }

    /**
     * Returns a live {@link Map} view whose keys are the contents of {@code set} and whose values are
     * computed on demand using {@code function}.
     *
     * <p>Specifically, for each {@code k} in the backing set, the returned map has an entry mapping
     * {@code k} to {@code function.apply(k)}. The {@code keySet}, {@code values}, and {@code
     * entrySet} views of the returned map iterate in the same order as the backing set.
     *
     * <p>Modifications to the backing set are read through to the returned map. The returned map
     * supports removal operations if the backing set does. Removal operations write through to the
     * backing set. The returned map does not support put operations.
     *
     * <p><b>Warning:</b> If the function rejects {@code null}, caution is required to make sure the
     * set does not contain {@code null}, because the view cannot stop {@code null} from being added
     * to the set.
     *
     * <p><b>Warning:</b> This method assumes that for any instance {@code k} of key type {@code K},
     * {@code k.equals(k2)} implies that {@code k2} is also of type {@code K}. Using a key type for
     * which this may not hold, such as {@code ArrayList}, may risk a {@code ClassCastException} when
     * calling methods on the resulting map view.
     *
     * @since 14.0
     */
    public static <K, V> Map<K, V> asMap(
            Set<K> set, Function<? super K, V> function) {
        return new AsMapView<>(set, function);
    }

    private static class AsMapView<K, V>
            extends ViewCachingAbstractMap<K, V> {

        private final Set<K> set;
        final Function<? super K, V> function;

        Set<K> backingSet() {
            return set;
        }

        AsMapView(Set<K> set, Function<? super K, V> function) {
            this.set = checkNotNull(set);
            this.function = checkNotNull(function);
        }

        @Override
        public Set<K> createKeySet() {
            return removeOnlySet(backingSet());
        }

        @Override
        Collection<V> createValues() {
            return Collections2.transform(set, function);
        }

        @Override
        public int size() {
            return backingSet().size();
        }

        @Override
        public boolean containsKey(Object key) {
            return backingSet().contains(key);
        }

        @Override
        public V get(Object key) {
            return getOrDefault(key, null);
        }

        @Override
        public V getOrDefault(Object key, V defaultValue) {
            if (Collections2.safeContains(backingSet(), key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                K k = (K) key;
                return function.apply(k);
            } else {
                return defaultValue;
            }
        }

        @Override
        public V remove(Object key) {
            if (backingSet().remove(key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                K k = (K) key;
                return function.apply(k);
            } else {
                return null;
            }
        }

        @Override
        public void clear() {
            backingSet().clear();
        }

        @Override
        protected Set<Entry<K, V>> createEntrySet() {
            class EntrySetImpl extends EntrySet<K, V> {
                @Override
                Map<K, V> map() {
                    return AsMapView.this;
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return asMapEntryIterator(backingSet(), function);
                }
            }
            return new EntrySetImpl();
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            checkNotNull(action);
            // avoids allocation of entries
            backingSet().forEach(k -> action.accept(k, function.apply(k)));
        }
    }

    static <K, V>
    Iterator<Entry<K, V>> asMapEntryIterator(Set<K> set, final Function<? super K, V> function) {
        return new TransformedIterator<K, Entry<K, V>>(set.iterator()) {
            @Override
            Entry<K, V> transform(final K key) {
                return immutableEntry(key, function.apply(key));
            }
        };
    }

    private static <E> Set<E> removeOnlySet(final Set<E> set) {
        return new ForwardingSet<E>() {
            @Override
            protected Set<E> delegate() {
                return set;
            }

            @Override
            public boolean add(E element) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends E> es) {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns an immutable map entry with the specified key and value. The {@link Entry#setValue}
     * operation throws an {@link UnsupportedOperationException}.
     *
     * <p>The returned entry is serializable.
     *
     * <p><b>Java 9 users:</b> consider using {@code java.util.Map.entry(key, value)} if the key and
     * value are non-null and the entry does not need to be serializable.
     *
     * @param key the key to be associated with the returned entry
     * @param value the value to be associated with the returned entry
     */
    static <K, V> Entry<K, V> immutableEntry(
            K key, V value) {
        return new ImmutableEntry<>(key, value);
    }

    /**
     * {@code AbstractMap} extension that makes it easy to cache customized keySet, values, and
     * entrySet views.
     */
    abstract static class ViewCachingAbstractMap<
            K, V>
            extends AbstractMap<K, V> {
        /**
         * Creates the entry set to be returned by {@link #entrySet()}. This method is invoked at most
         * once on a given map, at the time when {@code entrySet} is first called.
         */
        abstract Set<Entry<K, V>> createEntrySet();

        private transient Set<Entry<K, V>> entrySet;

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = entrySet;
            return (result == null) ? entrySet = createEntrySet() : result;
        }

        private transient Set<K> keySet;

        @Override
        public Set<K> keySet() {
            Set<K> result = keySet;
            return (result == null) ? keySet = createKeySet() : result;
        }

        Set<K> createKeySet() {
            return new KeySet<>(this);
        }

        private transient Collection<V> values;

        @Override
        public Collection<V> values() {
            Collection<V> result = values;
            return (result == null) ? values = createValues() : result;
        }

        Collection<V> createValues() {
            return new Values<>(this);
        }
    }

    /**
     * Delegates to {@link Map#get}. Returns {@code null} on {@code ClassCastException} and {@code
     * NullPointerException}.
     */
    static <V> V safeGet(Map<?, V> map, Object key) {
        checkNotNull(map);
        try {
            return map.get(key);
        } catch (ClassCastException | NullPointerException e) {
            return null;
        }
    }

    /** An implementation of {@link Map#equals}. */
    static boolean equalsImpl(Map<?, ?> map, Object object) {
        if (map == object) {
            return true;
        } else if (object instanceof Map) {
            Map<?, ?> o = (Map<?, ?>) object;
            return map.entrySet().equals(o.entrySet());
        }
        return false;
    }

    /** An implementation of {@link Map#toString}. */
    static String toStringImpl(Map<?, ?> map) {
        StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
        boolean first = true;
        for (Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.append('}').toString();
    }

    /** An implementation of {@link Map#putAll}. */
    static <K, V> void putAllImpl(
            Map<K, V> self, Map<? extends K, ? extends V> map) {
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            self.put(entry.getKey(), entry.getValue());
        }
    }

    static class KeySet<K, V>
            extends Sets.ImprovedAbstractSet<K> {
        final Map<K, V> map;

        KeySet(Map<K, V> map) {
            this.map = checkNotNull(map);
        }

        Map<K, V> map() {
            return map;
        }

        @Override
        public Iterator<K> iterator() {
            return keyIterator(map().entrySet().iterator());
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            checkNotNull(action);
            // avoids entry allocation for those maps that allocate entries on iteration
            map.forEach((k, v) -> action.accept(k));
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map().containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            if (contains(o)) {
                map().remove(o);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    static <K> K keyOrNull(Entry<K, ?> entry) {
        return (entry == null) ? null : entry.getKey();
    }

    static class SortedKeySet<K, V>
            extends KeySet<K, V> implements SortedSet<K> {
        SortedKeySet(SortedMap<K, V> map) {
            super(map);
        }

        @Override
        SortedMap<K, V> map() {
            return (SortedMap<K, V>) super.map();
        }

        @Override
        public Comparator<? super K> comparator() {
            return map().comparator();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return new SortedKeySet<>(map().subMap(fromElement, toElement));
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return new SortedKeySet<>(map().headMap(toElement));
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return new SortedKeySet<>(map().tailMap(fromElement));
        }

        @Override
        public K first() {
            return map().firstKey();
        }

        @Override
        public K last() {
            return map().lastKey();
        }
    }

    @GwtIncompatible // NavigableMap
    static class NavigableKeySet<K, V>
            extends SortedKeySet<K, V> implements NavigableSet<K> {
        NavigableKeySet(NavigableMap<K, V> map) {
            super(map);
        }

        @Override
        NavigableMap<K, V> map() {
            return (NavigableMap<K, V>) map;
        }

        @Override
        public K lower(K e) {
            return map().lowerKey(e);
        }

        @Override
        public K floor(K e) {
            return map().floorKey(e);
        }

        @Override
        public K ceiling(K e) {
            return map().ceilingKey(e);
        }

        @Override
        public K higher(K e) {
            return map().higherKey(e);
        }

        @Override
        public K pollFirst() {
            return keyOrNull(map().pollFirstEntry());
        }

        @Override
        public K pollLast() {
            return keyOrNull(map().pollLastEntry());
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return map().descendingKeySet();
        }

        @Override
        public Iterator<K> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public NavigableSet<K> subSet(
                K fromElement,
                boolean fromInclusive,
                K toElement,
                boolean toInclusive) {
            return map().subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            return map().headMap(toElement, inclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            return map().tailMap(fromElement, inclusive).navigableKeySet();
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }
    }

    static class Values<K, V>
            extends AbstractCollection<V> {
        final Map<K, V> map;

        Values(Map<K, V> map) {
            this.map = checkNotNull(map);
        }

        final Map<K, V> map() {
            return map;
        }

        @Override
        public Iterator<V> iterator() {
            return valueIterator(map().entrySet().iterator());
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            checkNotNull(action);
            // avoids allocation of entries for those maps that generate fresh entries on iteration
            map.forEach((k, v) -> action.accept(v));
        }

        @Override
        public boolean remove(Object o) {
            try {
                return super.remove(o);
            } catch (UnsupportedOperationException e) {
                for (Entry<K, V> entry : map().entrySet()) {
                    if (Objects.equal(o, entry.getValue())) {
                        map().remove(entry.getKey());
                        return true;
                    }
                }
                return false;
            }
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<K> toRemove = new HashSet<>();
                for (Entry<K, V> entry : map().entrySet()) {
                    if (c.contains(entry.getValue())) {
                        toRemove.add(entry.getKey());
                    }
                }
                return map().keySet().removeAll(toRemove);
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                Set<K> toRetain = new HashSet<>();
                for (Entry<K, V> entry : map().entrySet()) {
                    if (c.contains(entry.getValue())) {
                        toRetain.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(toRetain);
            }
        }

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map().containsValue(o);
        }

        @Override
        public void clear() {
            map().clear();
        }
    }

    abstract static class EntrySet<K, V>
            extends Sets.ImprovedAbstractSet<Entry<K, V>> {
        abstract Map<K, V> map();

        @Override
        public int size() {
            return map().size();
        }

        @Override
        public void clear() {
            map().clear();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                Object key = entry.getKey();
                V value = Maps.safeGet(map(), key);
                return Objects.equal(value, entry.getValue()) && (value != null || map().containsKey(key));
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return map().isEmpty();
        }

        @Override
        public boolean remove(Object o) {
            /*
             * `o instanceof Entry` is guaranteed by `contains`, but we check it here to satisfy our
             * nullness checker.
             */
            if (contains(o) && o instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                return map().keySet().remove(entry.getKey());
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            try {
                return super.removeAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                // if the iterators don't support remove
                return Sets.removeAllImpl(this, c.iterator());
            }
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            try {
                return super.retainAll(checkNotNull(c));
            } catch (UnsupportedOperationException e) {
                // if the iterators don't support remove
                Set<Object> keys = Sets.newHashSetWithExpectedSize(c.size());
                for (Object o : c) {
                    /*
                     * `o instanceof Entry` is guaranteed by `contains`, but we check it here to satisfy our
                     * nullness checker.
                     */
                    if (contains(o) && o instanceof Entry) {
                        Entry<?, ?> entry = (Entry<?, ?>) o;
                        keys.add(entry.getKey());
                    }
                }
                return map().keySet().retainAll(keys);
            }
        }
    }
}
