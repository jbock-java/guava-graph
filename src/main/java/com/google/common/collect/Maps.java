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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

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
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    private enum EntryFunction implements Function<Entry<?, ?>, Object> {
        KEY {
            @Override
            public Object apply(Entry<?, ?> entry) {
                return entry.getKey();
            }
        },
        VALUE {
            @Override
            public Object apply(Entry<?, ?> entry) {
                return entry.getValue();
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <V> Function<Entry<?, V>, V> valueFunction() {
        return (Function) EntryFunction.VALUE;
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
     * Creates a <i>mutable</i>, empty {@code TreeMap} instance using the given comparator.
     *
     * <p><b>Note:</b> if mutability is not required, use {@code
     * ImmutableSortedMap.orderedBy(comparator).build()} instead.
     *
     * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
     * deprecated. Instead, use the {@code TreeMap} constructor directly, taking advantage of the new
     * <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
     *
     * @param comparator the comparator to sort the keys with
     * @return a new, empty {@code TreeMap}
     */
    public static <C, K extends C, V>
    TreeMap<K, V> newTreeMap(Comparator<C> comparator) {
        // Ideally, the extra type parameter "C" shouldn't be necessary. It is a
        // work-around of a compiler type inference quirk that prevents the
        // following code from being compiled:
        // Comparator<Class<?>> comparator = null;
        // Map<Class<? extends Throwable>, String> map = newTreeMap(comparator);
        return new TreeMap<>(comparator);
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

    /**
     * Returns a view of the sorted set as a map, mapping keys from the set according to the specified
     * function.
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
    public static <K, V> SortedMap<K, V> asMap(
            SortedSet<K> set, Function<? super K, V> function) {
        return new SortedAsMapView<>(set, function);
    }

    /**
     * Returns a view of the navigable set as a map, mapping keys from the set according to the
     * specified function.
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
    @GwtIncompatible // NavigableMap
    public static <K, V> NavigableMap<K, V> asMap(
            NavigableSet<K> set, Function<? super K, V> function) {
        return new NavigableAsMapView<>(set, function);
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

    private static class SortedAsMapView<K, V>
            extends AsMapView<K, V> implements SortedMap<K, V> {

        SortedAsMapView(SortedSet<K> set, Function<? super K, V> function) {
            super(set, function);
        }

        @Override
        SortedSet<K> backingSet() {
            return (SortedSet<K>) super.backingSet();
        }

        @Override
        public Comparator<? super K> comparator() {
            return backingSet().comparator();
        }

        @Override
        public Set<K> keySet() {
            return removeOnlySortedSet(backingSet());
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return asMap(backingSet().subSet(fromKey, toKey), function);
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return asMap(backingSet().headSet(toKey), function);
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return asMap(backingSet().tailSet(fromKey), function);
        }

        @Override
        public K firstKey() {
            return backingSet().first();
        }

        @Override
        public K lastKey() {
            return backingSet().last();
        }
    }

    @GwtIncompatible // NavigableMap
    private static final class NavigableAsMapView<
            K, V>
            extends AbstractNavigableMap<K, V> {
        /*
         * Using AbstractNavigableMap is simpler than extending SortedAsMapView and rewriting all the
         * NavigableMap methods.
         */

        private final NavigableSet<K> set;
        private final Function<? super K, V> function;

        NavigableAsMapView(NavigableSet<K> ks, Function<? super K, V> vFunction) {
            this.set = checkNotNull(ks);
            this.function = checkNotNull(vFunction);
        }

        @Override
        public NavigableMap<K, V> subMap(
                K fromKey,
                boolean fromInclusive,
                K toKey,
                boolean toInclusive) {
            return asMap(set.subSet(fromKey, fromInclusive, toKey, toInclusive), function);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return asMap(set.headSet(toKey, inclusive), function);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return asMap(set.tailSet(fromKey, inclusive), function);
        }

        @Override
        public Comparator<? super K> comparator() {
            return set.comparator();
        }

        @Override
        public V get(Object key) {
            return getOrDefault(key, null);
        }

        @Override
        public V getOrDefault(Object key, V defaultValue) {
            if (Collections2.safeContains(set, key)) {
                @SuppressWarnings("unchecked") // unsafe, but Javadoc warns about it
                K k = (K) key;
                return function.apply(k);
            } else {
                return defaultValue;
            }
        }

        @Override
        public void clear() {
            set.clear();
        }

        @Override
        Iterator<Entry<K, V>> entryIterator() {
            return asMapEntryIterator(set, function);
        }

        @Override
        Spliterator<Entry<K, V>> entrySpliterator() {
            return CollectSpliterators.map(set.spliterator(), e -> immutableEntry(e, function.apply(e)));
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            set.forEach(k -> action.accept(k, function.apply(k)));
        }

        @Override
        Iterator<Entry<K, V>> descendingEntryIterator() {
            return descendingMap().entrySet().iterator();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return removeOnlyNavigableSet(set);
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return asMap(set.descendingSet(), function);
        }
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

    private static <E> SortedSet<E> removeOnlySortedSet(
            final SortedSet<E> set) {
        return new ForwardingSortedSet<E>() {
            @Override
            protected SortedSet<E> delegate() {
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

            @Override
            public SortedSet<E> headSet(E toElement) {
                return removeOnlySortedSet(super.headSet(toElement));
            }

            @Override
            public SortedSet<E> subSet(
                    E fromElement, E toElement) {
                return removeOnlySortedSet(super.subSet(fromElement, toElement));
            }

            @Override
            public SortedSet<E> tailSet(E fromElement) {
                return removeOnlySortedSet(super.tailSet(fromElement));
            }
        };
    }

    @GwtIncompatible // NavigableSet
    private static <E> NavigableSet<E> removeOnlyNavigableSet(
            final NavigableSet<E> set) {
        return new ForwardingNavigableSet<E>() {
            @Override
            protected NavigableSet<E> delegate() {
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

            @Override
            public SortedSet<E> headSet(E toElement) {
                return removeOnlySortedSet(super.headSet(toElement));
            }

            @Override
            public NavigableSet<E> headSet(E toElement, boolean inclusive) {
                return removeOnlyNavigableSet(super.headSet(toElement, inclusive));
            }

            @Override
            public SortedSet<E> subSet(
                    E fromElement, E toElement) {
                return removeOnlySortedSet(super.subSet(fromElement, toElement));
            }

            @Override
            public NavigableSet<E> subSet(
                    E fromElement,
                    boolean fromInclusive,
                    E toElement,
                    boolean toInclusive) {
                return removeOnlyNavigableSet(
                        super.subSet(fromElement, fromInclusive, toElement, toInclusive));
            }

            @Override
            public SortedSet<E> tailSet(E fromElement) {
                return removeOnlySortedSet(super.tailSet(fromElement));
            }

            @Override
            public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
                return removeOnlyNavigableSet(super.tailSet(fromElement, inclusive));
            }

            @Override
            public NavigableSet<E> descendingSet() {
                return removeOnlyNavigableSet(super.descendingSet());
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
     * Returns an unmodifiable view of the specified map entry. The {@link Entry#setValue} operation
     * throws an {@link UnsupportedOperationException}. This also has the side-effect of redefining
     * {@code equals} to comply with the Entry contract, to avoid a possible nefarious implementation
     * of equals.
     *
     * @param entry the entry for which to return an unmodifiable view
     * @return an unmodifiable view of the entry
     */
    static <K, V> Entry<K, V> unmodifiableEntry(
            final Entry<? extends K, ? extends V> entry) {
        checkNotNull(entry);
        return new AbstractMapEntry<K, V>() {
            @Override
            public K getKey() {
                return entry.getKey();
            }

            @Override
            public V getValue() {
                return entry.getValue();
            }
        };
    }

    /**
     * Returns a view of a sorted map whose values are derived from the original sorted map's entries.
     *
     * <p>All other properties of the transformed map, such as iteration order, are left intact. For
     * example, the code:
     *
     * <pre>{@code
     * Map<String, Boolean> options =
     *     ImmutableSortedMap.of("verbose", true, "sort", false);
     * EntryTransformer<String, Boolean, String> flagPrefixer =
     *     new EntryTransformer<String, Boolean, String>() {
     *       public String transformEntry(String key, Boolean value) {
     *         return value ? key : "yes" + key;
     *       }
     *     };
     * SortedMap<String, String> transformed =
     *     Maps.transformEntries(options, flagPrefixer);
     * System.out.println(transformed);
     * }</pre>
     *
     * ... prints {@code {sort=yessort, verbose=verbose}}.
     *
     * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
     * removal operations, and these are reflected in the underlying map.
     *
     * <p>It's acceptable for the underlying map to contain null keys and null values provided that
     * the transformer is capable of accepting null inputs. The transformed map might contain null
     * values if the transformer sometimes gives a null result.
     *
     * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
     *
     * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
     * map to be a view, but it means that the transformer will be applied many times for bulk
     * operations like {@link Map#containsValue} and {@link Object#toString}. For this to perform
     * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned map
     * doesn't need to be a view, copy the returned map into a new map of your choosing.
     *
     * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
     * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
     * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
     * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
     * transformed map.
     *
     * @since 11.0
     */
    static <
            K, V1, V2>
    SortedMap<K, V2> transformEntries(
            SortedMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
        return new TransformedEntriesSortedMap<>(fromMap, transformer);
    }

    /**
     * Returns a view of a navigable map whose values are derived from the original navigable map's
     * entries.
     *
     * <p>All other properties of the transformed map, such as iteration order, are left intact. For
     * example, the code:
     *
     * <pre>{@code
     * NavigableMap<String, Boolean> options = Maps.newTreeMap();
     * options.put("verbose", false);
     * options.put("sort", true);
     * EntryTransformer<String, Boolean, String> flagPrefixer =
     *     new EntryTransformer<String, Boolean, String>() {
     *       public String transformEntry(String key, Boolean value) {
     *         return value ? key : ("yes" + key);
     *       }
     *     };
     * NavigableMap<String, String> transformed =
     *     LabsMaps.transformNavigableEntries(options, flagPrefixer);
     * System.out.println(transformed);
     * }</pre>
     *
     * ... prints {@code {sort=yessort, verbose=verbose}}.
     *
     * <p>Changes in the underlying map are reflected in this view. Conversely, this view supports
     * removal operations, and these are reflected in the underlying map.
     *
     * <p>It's acceptable for the underlying map to contain null keys and null values provided that
     * the transformer is capable of accepting null inputs. The transformed map might contain null
     * values if the transformer sometimes gives a null result.
     *
     * <p>The returned map is not thread-safe or serializable, even if the underlying map is.
     *
     * <p>The transformer is applied lazily, invoked when needed. This is necessary for the returned
     * map to be a view, but it means that the transformer will be applied many times for bulk
     * operations like {@link Map#containsValue} and {@link Object#toString}. For this to perform
     * well, {@code transformer} should be fast. To avoid lazy evaluation when the returned map
     * doesn't need to be a view, copy the returned map into a new map of your choosing.
     *
     * <p><b>Warning:</b> This method assumes that for any instance {@code k} of {@code
     * EntryTransformer} key type {@code K}, {@code k.equals(k2)} implies that {@code k2} is also of
     * type {@code K}. Using an {@code EntryTransformer} key type for which this may not hold, such as
     * {@code ArrayList}, may risk a {@code ClassCastException} when calling methods on the
     * transformed map.
     *
     * @since 13.0
     */
    @GwtIncompatible // NavigableMap
    static <
            K, V1, V2>
    NavigableMap<K, V2> transformEntries(
            NavigableMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
        return new TransformedEntriesNavigableMap<>(fromMap, transformer);
    }

    /**
     * A transformation of the value of a key-value pair, using both key and value as inputs.
     *
     * @param <K> the key type of the input and output entries
     * @param <V1> the value type of the input entry
     * @param <V2> the value type of the output entry
     * @since 7.0
     */
    @FunctionalInterface
    public interface EntryTransformer<
            K, V1, V2> {
        /**
         * Determines an output value based on a key-value pair. This method is <i>generally
         * expected</i>, but not absolutely required, to have the following properties:
         *
         * <ul>
         *   <li>Its execution does not cause any observable side effects.
         *   <li>The computation is <i>consistent with equals</i>; that is, {@link Objects#equal
         *       Objects.equal}{@code (k1, k2) &&} {@link Objects#equal}{@code (v1, v2)} implies that
         *       {@code Objects.equal(transformer.transform(k1, v1), transformer.transform(k2, v2))}.
         * </ul>
         *
         * @throws NullPointerException if the key or value is null and this transformer does not accept
         *     null arguments
         */
        V2 transformEntry(K key, V1 value);
    }

    /** Views a function as an entry transformer that ignores the entry key. */
    static <K, V1, V2>
    EntryTransformer<K, V1, V2> asEntryTransformer(final Function<? super V1, V2> function) {
        checkNotNull(function);
        return new EntryTransformer<K, V1, V2>() {
            @Override
            public V2 transformEntry(K key, V1 value) {
                return function.apply(value);
            }
        };
    }

    /** Returns a view of an entry transformed by the specified transformer. */
    static <V2, K, V1>
    Entry<K, V2> transformEntry(
            final EntryTransformer<? super K, ? super V1, V2> transformer, final Entry<K, V1> entry) {
        checkNotNull(transformer);
        checkNotNull(entry);
        return new AbstractMapEntry<K, V2>() {
            @Override
            public K getKey() {
                return entry.getKey();
            }

            @Override
            public V2 getValue() {
                return transformer.transformEntry(entry.getKey(), entry.getValue());
            }
        };
    }

    /** Views an entry transformer as a function from entries to entries. */
    static <K, V1, V2>
    Function<Entry<K, V1>, Entry<K, V2>> asEntryToEntryFunction(
            final EntryTransformer<? super K, ? super V1, V2> transformer) {
        checkNotNull(transformer);
        return new Function<Entry<K, V1>, Entry<K, V2>>() {
            @Override
            public Entry<K, V2> apply(final Entry<K, V1> entry) {
                return transformEntry(transformer, entry);
            }
        };
    }

    static class TransformedEntriesMap<
            K, V1, V2>
            extends IteratorBasedAbstractMap<K, V2> {
        final Map<K, V1> fromMap;
        final EntryTransformer<? super K, ? super V1, V2> transformer;

        TransformedEntriesMap(
                Map<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
            this.fromMap = checkNotNull(fromMap);
            this.transformer = checkNotNull(transformer);
        }

        @Override
        public int size() {
            return fromMap.size();
        }

        @Override
        public boolean containsKey(Object key) {
            return fromMap.containsKey(key);
        }

        @Override
        public V2 get(Object key) {
            return getOrDefault(key, null);
        }

        // safe as long as the user followed the <b>Warning</b> in the javadoc
        @SuppressWarnings("unchecked")
        @Override
        public V2 getOrDefault(Object key, V2 defaultValue) {
            V1 value = fromMap.get(key);
            if (value != null || fromMap.containsKey(key)) {
                // The cast is safe because of the containsKey check.
                return transformer.transformEntry((K) key, value);
            }
            return defaultValue;
        }

        // safe as long as the user followed the <b>Warning</b> in the javadoc
        @SuppressWarnings("unchecked")
        @Override
        public V2 remove(Object key) {
            return fromMap.containsKey(key)
                    // The cast is safe because of the containsKey check.
                    ? transformer.transformEntry((K) key, fromMap.remove(key))
                    : null;
        }

        @Override
        public void clear() {
            fromMap.clear();
        }

        @Override
        public Set<K> keySet() {
            return fromMap.keySet();
        }

        @Override
        Iterator<Entry<K, V2>> entryIterator() {
            return Iterators.transform(
                    fromMap.entrySet().iterator(), Maps.<K, V1, V2>asEntryToEntryFunction(transformer));
        }

        @Override
        Spliterator<Entry<K, V2>> entrySpliterator() {
            return CollectSpliterators.map(
                    fromMap.entrySet().spliterator(), Maps.<K, V1, V2>asEntryToEntryFunction(transformer));
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V2> action) {
            checkNotNull(action);
            // avoids creating new Entry<K, V2> objects
            fromMap.forEach((k, v1) -> action.accept(k, transformer.transformEntry(k, v1)));
        }

        @Override
        public Collection<V2> values() {
            return new Values<>(this);
        }
    }

    static class TransformedEntriesSortedMap<
            K, V1, V2>
            extends TransformedEntriesMap<K, V1, V2> implements SortedMap<K, V2> {

        protected SortedMap<K, V1> fromMap() {
            return (SortedMap<K, V1>) fromMap;
        }

        TransformedEntriesSortedMap(
                SortedMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
            super(fromMap, transformer);
        }

        @Override
        public Comparator<? super K> comparator() {
            return fromMap().comparator();
        }

        @Override
        public K firstKey() {
            return fromMap().firstKey();
        }

        @Override
        public SortedMap<K, V2> headMap(K toKey) {
            return transformEntries(fromMap().headMap(toKey), transformer);
        }

        @Override
        public K lastKey() {
            return fromMap().lastKey();
        }

        @Override
        public SortedMap<K, V2> subMap(K fromKey, K toKey) {
            return transformEntries(fromMap().subMap(fromKey, toKey), transformer);
        }

        @Override
        public SortedMap<K, V2> tailMap(K fromKey) {
            return transformEntries(fromMap().tailMap(fromKey), transformer);
        }
    }

    @GwtIncompatible // NavigableMap
    private static class TransformedEntriesNavigableMap<
            K, V1, V2>
            extends TransformedEntriesSortedMap<K, V1, V2> implements NavigableMap<K, V2> {

        TransformedEntriesNavigableMap(
                NavigableMap<K, V1> fromMap, EntryTransformer<? super K, ? super V1, V2> transformer) {
            super(fromMap, transformer);
        }

        @Override
        public Entry<K, V2> ceilingEntry(K key) {
            return transformEntry(fromMap().ceilingEntry(key));
        }

        @Override
        public K ceilingKey(K key) {
            return fromMap().ceilingKey(key);
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return fromMap().descendingKeySet();
        }

        @Override
        public NavigableMap<K, V2> descendingMap() {
            return transformEntries(fromMap().descendingMap(), transformer);
        }

        @Override
        public Entry<K, V2> firstEntry() {
            return transformEntry(fromMap().firstEntry());
        }

        @Override
        public Entry<K, V2> floorEntry(K key) {
            return transformEntry(fromMap().floorEntry(key));
        }

        @Override
        public K floorKey(K key) {
            return fromMap().floorKey(key);
        }

        @Override
        public NavigableMap<K, V2> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public NavigableMap<K, V2> headMap(K toKey, boolean inclusive) {
            return transformEntries(fromMap().headMap(toKey, inclusive), transformer);
        }

        @Override
        public Entry<K, V2> higherEntry(K key) {
            return transformEntry(fromMap().higherEntry(key));
        }

        @Override
        public K higherKey(K key) {
            return fromMap().higherKey(key);
        }

        @Override
        public Entry<K, V2> lastEntry() {
            return transformEntry(fromMap().lastEntry());
        }

        @Override
        public Entry<K, V2> lowerEntry(K key) {
            return transformEntry(fromMap().lowerEntry(key));
        }

        @Override
        public K lowerKey(K key) {
            return fromMap().lowerKey(key);
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return fromMap().navigableKeySet();
        }

        @Override
        public Entry<K, V2> pollFirstEntry() {
            return transformEntry(fromMap().pollFirstEntry());
        }

        @Override
        public Entry<K, V2> pollLastEntry() {
            return transformEntry(fromMap().pollLastEntry());
        }

        @Override
        public NavigableMap<K, V2> subMap(
                K fromKey,
                boolean fromInclusive,
                K toKey,
                boolean toInclusive) {
            return transformEntries(
                    fromMap().subMap(fromKey, fromInclusive, toKey, toInclusive), transformer);
        }

        @Override
        public NavigableMap<K, V2> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public NavigableMap<K, V2> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public NavigableMap<K, V2> tailMap(K fromKey, boolean inclusive) {
            return transformEntries(fromMap().tailMap(fromKey, inclusive), transformer);
        }

        private Entry<K, V2> transformEntry(Entry<K, V1> entry) {
            return (entry == null) ? null : Maps.transformEntry(transformer, entry);
        }

        @Override
        protected NavigableMap<K, V1> fromMap() {
            return (NavigableMap<K, V1>) super.fromMap();
        }
    }

    /**
     * Returns a sorted map containing the mappings in {@code unfiltered} that satisfy a predicate.
     * The returned map is a live view of {@code unfiltered}; changes to one affect the other.
     *
     * <p>The resulting map's {@code keySet()}, {@code entrySet()}, and {@code values()} views have
     * iterators that don't support {@code remove()}, but all other methods are supported by the map
     * and its views. When given a key/value pair that doesn't satisfy the predicate, the map's {@code
     * put()} and {@code putAll()} methods throw an {@link IllegalArgumentException}. Similarly, the
     * map's entries have a {@link Entry#setValue} method that throws an {@link
     * IllegalArgumentException} when the existing key and the provided value don't satisfy the
     * predicate.
     *
     * <p>When methods such as {@code removeAll()} and {@code clear()} are called on the filtered map
     * or its views, only mappings that satisfy the filter will be removed from the underlying map.
     *
     * <p>The returned map isn't threadsafe or serializable, even if {@code unfiltered} is.
     *
     * <p>Many of the filtered map's methods, such as {@code size()}, iterate across every key/value
     * mapping in the underlying map and determine which satisfy the filter. When a live view is
     * <i>not</i> needed, it may be faster to copy the filtered map and use the copy.
     *
     * <p><b>Warning:</b> {@code entryPredicate} must be <i>consistent with equals</i>, as documented
     * at {@link Predicate#apply}.
     *
     * @since 14.0
     */
    @GwtIncompatible // NavigableMap
    public static <K, V>
    NavigableMap<K, V> filterEntries(
            NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
        checkNotNull(entryPredicate);
        return (unfiltered instanceof FilteredEntryNavigableMap)
                ? filterFiltered((FilteredEntryNavigableMap<K, V>) unfiltered, entryPredicate)
                : new FilteredEntryNavigableMap<K, V>(checkNotNull(unfiltered), entryPredicate);
    }

    /**
     * Support {@code clear()}, {@code removeAll()}, and {@code retainAll()} when filtering a filtered
     * navigable map.
     */
    @GwtIncompatible // NavigableMap
    private static <K, V>
    NavigableMap<K, V> filterFiltered(
            FilteredEntryNavigableMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
        Predicate<Entry<K, V>> predicate =
                Predicates.<Entry<K, V>>and(map.entryPredicate, entryPredicate);
        return new FilteredEntryNavigableMap<>(map.unfiltered, predicate);
    }

    private abstract static class AbstractFilteredMap<
            K, V>
            extends ViewCachingAbstractMap<K, V> {
        final Map<K, V> unfiltered;
        final Predicate<? super Entry<K, V>> predicate;

        AbstractFilteredMap(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
            this.unfiltered = unfiltered;
            this.predicate = predicate;
        }

        boolean apply(Object key, V value) {
            // This method is called only when the key is in the map (or about to be added to the map),
            // implying that key is a K.
            @SuppressWarnings({"unchecked", "nullness"})
            K k = (K) key;
            return predicate.apply(Maps.immutableEntry(k, value));
        }

        @Override
        public V put(K key, V value) {
            checkArgument(apply(key, value));
            return unfiltered.put(key, value);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) {
            for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                checkArgument(apply(entry.getKey(), entry.getValue()));
            }
            unfiltered.putAll(map);
        }

        @Override
        public boolean containsKey(Object key) {
            return unfiltered.containsKey(key) && apply(key, unfiltered.get(key));
        }

        @Override
        public V get(Object key) {
            V value = unfiltered.get(key);
            return ((value != null) && apply(key, value)) ? value : null;
        }

        @Override
        public boolean isEmpty() {
            return entrySet().isEmpty();
        }

        @Override
        public V remove(Object key) {
            return containsKey(key) ? unfiltered.remove(key) : null;
        }

        @Override
        Collection<V> createValues() {
            return new FilteredMapValues<>(this, unfiltered, predicate);
        }
    }

    private static final class FilteredMapValues<
            K, V>
            extends Maps.Values<K, V> {
        final Map<K, V> unfiltered;
        final Predicate<? super Entry<K, V>> predicate;

        FilteredMapValues(
                Map<K, V> filteredMap, Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
            super(filteredMap);
            this.unfiltered = unfiltered;
            this.predicate = predicate;
        }

        @Override
        public boolean remove(Object o) {
            Iterator<Entry<K, V>> entryItr = unfiltered.entrySet().iterator();
            while (entryItr.hasNext()) {
                Entry<K, V> entry = entryItr.next();
                if (predicate.apply(entry) && Objects.equal(entry.getValue(), o)) {
                    entryItr.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            Iterator<Entry<K, V>> entryItr = unfiltered.entrySet().iterator();
            boolean result = false;
            while (entryItr.hasNext()) {
                Entry<K, V> entry = entryItr.next();
                if (predicate.apply(entry) && collection.contains(entry.getValue())) {
                    entryItr.remove();
                    result = true;
                }
            }
            return result;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            Iterator<Entry<K, V>> entryItr = unfiltered.entrySet().iterator();
            boolean result = false;
            while (entryItr.hasNext()) {
                Entry<K, V> entry = entryItr.next();
                if (predicate.apply(entry) && !collection.contains(entry.getValue())) {
                    entryItr.remove();
                    result = true;
                }
            }
            return result;
        }

        @Override
        public Object[] toArray() {
            // creating an ArrayList so filtering happens once
            return Lists.newArrayList(iterator()).toArray();
        }

        @Override
        @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
        public <T> T[] toArray(T[] array) {
            return Lists.newArrayList(iterator()).toArray(array);
        }
    }

    static class FilteredEntryMap<K, V>
            extends AbstractFilteredMap<K, V> {
        /**
         * Entries in this set satisfy the predicate, but they don't validate the input to {@code
         * Entry.setValue()}.
         */
        final Set<Entry<K, V>> filteredEntrySet;

        FilteredEntryMap(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
            super(unfiltered, entryPredicate);
            filteredEntrySet = Sets.filter(unfiltered.entrySet(), predicate);
        }

        @Override
        protected Set<Entry<K, V>> createEntrySet() {
            return new EntrySet();
        }

        private class EntrySet extends ForwardingSet<Entry<K, V>> {
            @Override
            protected Set<Entry<K, V>> delegate() {
                return filteredEntrySet;
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new TransformedIterator<Entry<K, V>, Entry<K, V>>(filteredEntrySet.iterator()) {
                    @Override
                    Entry<K, V> transform(final Entry<K, V> entry) {
                        return new ForwardingMapEntry<K, V>() {
                            @Override
                            protected Entry<K, V> delegate() {
                                return entry;
                            }

                            @Override
                            public V setValue(V newValue) {
                                checkArgument(apply(getKey(), newValue));
                                return super.setValue(newValue);
                            }
                        };
                    }
                };
            }
        }

        @Override
        Set<K> createKeySet() {
            return new KeySet();
        }

        static <K, V> boolean removeAllKeys(
                Map<K, V> map, Predicate<? super Entry<K, V>> entryPredicate, Collection<?> keyCollection) {
            Iterator<Entry<K, V>> entryItr = map.entrySet().iterator();
            boolean result = false;
            while (entryItr.hasNext()) {
                Entry<K, V> entry = entryItr.next();
                if (entryPredicate.apply(entry) && keyCollection.contains(entry.getKey())) {
                    entryItr.remove();
                    result = true;
                }
            }
            return result;
        }

        static <K, V> boolean retainAllKeys(
                Map<K, V> map, Predicate<? super Entry<K, V>> entryPredicate, Collection<?> keyCollection) {
            Iterator<Entry<K, V>> entryItr = map.entrySet().iterator();
            boolean result = false;
            while (entryItr.hasNext()) {
                Entry<K, V> entry = entryItr.next();
                if (entryPredicate.apply(entry) && !keyCollection.contains(entry.getKey())) {
                    entryItr.remove();
                    result = true;
                }
            }
            return result;
        }

        class KeySet extends Maps.KeySet<K, V> {
            KeySet() {
                super(FilteredEntryMap.this);
            }

            @Override
            public boolean remove(Object o) {
                if (containsKey(o)) {
                    unfiltered.remove(o);
                    return true;
                }
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                return removeAllKeys(unfiltered, predicate, collection);
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                return retainAllKeys(unfiltered, predicate, collection);
            }

            @Override
            public Object[] toArray() {
                // creating an ArrayList so filtering happens once
                return Lists.newArrayList(iterator()).toArray();
            }

            @Override
            @SuppressWarnings("nullness") // b/192354773 in our checker affects toArray declarations
            public <T> T[] toArray(T[] array) {
                return Lists.newArrayList(iterator()).toArray(array);
            }
        }
    }

    @GwtIncompatible // NavigableMap
    private static class FilteredEntryNavigableMap<
            K, V>
            extends AbstractNavigableMap<K, V> {
        /*
         * It's less code to extend AbstractNavigableMap and forward the filtering logic to
         * FilteredEntryMap than to extend FilteredEntrySortedMap and reimplement all the NavigableMap
         * methods.
         */

        private final NavigableMap<K, V> unfiltered;
        private final Predicate<? super Entry<K, V>> entryPredicate;
        private final Map<K, V> filteredDelegate;

        FilteredEntryNavigableMap(
                NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
            this.unfiltered = checkNotNull(unfiltered);
            this.entryPredicate = entryPredicate;
            this.filteredDelegate = new FilteredEntryMap<>(unfiltered, entryPredicate);
        }

        @Override
        public Comparator<? super K> comparator() {
            return unfiltered.comparator();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return new Maps.NavigableKeySet<K, V>(this) {
                @Override
                public boolean removeAll(Collection<?> collection) {
                    return FilteredEntryMap.removeAllKeys(unfiltered, entryPredicate, collection);
                }

                @Override
                public boolean retainAll(Collection<?> collection) {
                    return FilteredEntryMap.retainAllKeys(unfiltered, entryPredicate, collection);
                }
            };
        }

        @Override
        public Collection<V> values() {
            return new FilteredMapValues<>(this, unfiltered, entryPredicate);
        }

        @Override
        Iterator<Entry<K, V>> entryIterator() {
            return Iterators.filter(unfiltered.entrySet().iterator(), entryPredicate);
        }

        @Override
        Iterator<Entry<K, V>> descendingEntryIterator() {
            return Iterators.filter(unfiltered.descendingMap().entrySet().iterator(), entryPredicate);
        }

        @Override
        public int size() {
            return filteredDelegate.size();
        }

        @Override
        public boolean isEmpty() {
            return !Iterables.any(unfiltered.entrySet(), entryPredicate);
        }

        @Override
        public V get(Object key) {
            return filteredDelegate.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return filteredDelegate.containsKey(key);
        }

        @Override
        public V put(K key, V value) {
            return filteredDelegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return filteredDelegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            filteredDelegate.putAll(m);
        }

        @Override
        public void clear() {
            filteredDelegate.clear();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return filteredDelegate.entrySet();
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            return Iterables.removeFirstMatching(unfiltered.entrySet(), entryPredicate);
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            return Iterables.removeFirstMatching(unfiltered.descendingMap().entrySet(), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return filterEntries(unfiltered.descendingMap(), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> subMap(
                K fromKey,
                boolean fromInclusive,
                K toKey,
                boolean toInclusive) {
            return filterEntries(
                    unfiltered.subMap(fromKey, fromInclusive, toKey, toInclusive), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return filterEntries(unfiltered.headMap(toKey, inclusive), entryPredicate);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return filterEntries(unfiltered.tailMap(fromKey, inclusive), entryPredicate);
        }
    }

    /**
     * Returns an unmodifiable view of the specified navigable map. Query operations on the returned
     * map read through to the specified map, and attempts to modify the returned map, whether direct
     * or via its views, result in an {@code UnsupportedOperationException}.
     *
     * <p>The returned navigable map will be serializable if the specified navigable map is
     * serializable.
     *
     * <p>This method's signature will not permit you to convert a {@code NavigableMap<? extends K,
     * V>} to a {@code NavigableMap<K, V>}. If it permitted this, the returned map's {@code
     * comparator()} method might return a {@code Comparator<? extends K>}, which works only on a
     * particular subtype of {@code K}, but promise that it's a {@code Comparator<? super K>}, which
     * must work on any type of {@code K}.
     *
     * @param map the navigable map for which an unmodifiable view is to be returned
     * @return an unmodifiable view of the specified navigable map
     * @since 12.0
     */
    @GwtIncompatible // NavigableMap
    static <K, V>
    NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> map) {
        checkNotNull(map);
        if (map instanceof UnmodifiableNavigableMap) {
            @SuppressWarnings("unchecked") // covariant
            NavigableMap<K, V> result = (NavigableMap<K, V>) map;
            return result;
        } else {
            return new UnmodifiableNavigableMap<>(map);
        }
    }

    private static <K, V>
    Entry<K, V> unmodifiableOrNull(Entry<K, ? extends V> entry) {
        return (entry == null) ? null : Maps.unmodifiableEntry(entry);
    }

    static class UnmodifiableNavigableMap<K, V>
            extends ForwardingSortedMap<K, V> implements NavigableMap<K, V> {
        private final NavigableMap<K, ? extends V> delegate;

        UnmodifiableNavigableMap(NavigableMap<K, ? extends V> delegate) {
            this.delegate = delegate;
        }

        UnmodifiableNavigableMap(
                NavigableMap<K, ? extends V> delegate, UnmodifiableNavigableMap<K, V> descendingMap) {
            this.delegate = delegate;
            this.descendingMap = descendingMap;
        }

        @Override
        protected SortedMap<K, V> delegate() {
            return Collections.unmodifiableSortedMap(delegate);
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            return unmodifiableOrNull(delegate.lowerEntry(key));
        }

        @Override
        public K lowerKey(K key) {
            return delegate.lowerKey(key);
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            return unmodifiableOrNull(delegate.floorEntry(key));
        }

        @Override
        public K floorKey(K key) {
            return delegate.floorKey(key);
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            return unmodifiableOrNull(delegate.ceilingEntry(key));
        }

        @Override
        public K ceilingKey(K key) {
            return delegate.ceilingKey(key);
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            return unmodifiableOrNull(delegate.higherEntry(key));
        }

        @Override
        public K higherKey(K key) {
            return delegate.higherKey(key);
        }

        @Override
        public Entry<K, V> firstEntry() {
            return unmodifiableOrNull(delegate.firstEntry());
        }

        @Override
        public Entry<K, V> lastEntry() {
            return unmodifiableOrNull(delegate.lastEntry());
        }

        @Override
        public final Entry<K, V> pollFirstEntry() {
            throw new UnsupportedOperationException();
        }

        @Override
        public final Entry<K, V> pollLastEntry() {
            throw new UnsupportedOperationException();
        }

        private transient UnmodifiableNavigableMap<K, V> descendingMap;

        @Override
        public NavigableMap<K, V> descendingMap() {
            UnmodifiableNavigableMap<K, V> result = descendingMap;
            return (result == null)
                    ? descendingMap = new UnmodifiableNavigableMap<>(delegate.descendingMap(), this)
                    : result;
        }

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public NavigableSet<K> navigableKeySet() {
            return Sets.unmodifiableNavigableSet(delegate.navigableKeySet());
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return Sets.unmodifiableNavigableSet(delegate.descendingKeySet());
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public NavigableMap<K, V> subMap(
                K fromKey,
                boolean fromInclusive,
                K toKey,
                boolean toInclusive) {
            return Maps.unmodifiableNavigableMap(
                    delegate.subMap(fromKey, fromInclusive, toKey, toInclusive));
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return Maps.unmodifiableNavigableMap(delegate.headMap(toKey, inclusive));
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return Maps.unmodifiableNavigableMap(delegate.tailMap(fromKey, inclusive));
        }
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

    abstract static class IteratorBasedAbstractMap<
            K, V>
            extends AbstractMap<K, V> {
        @Override
        public abstract int size();

        abstract Iterator<Entry<K, V>> entryIterator();

        Spliterator<Entry<K, V>> entrySpliterator() {
            return Spliterators.spliterator(
                    entryIterator(), size(), Spliterator.SIZED | Spliterator.DISTINCT);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new EntrySet<K, V>() {
                @Override
                Map<K, V> map() {
                    return IteratorBasedAbstractMap.this;
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return entryIterator();
                }

                @Override
                public Spliterator<Entry<K, V>> spliterator() {
                    return entrySpliterator();
                }

                @Override
                public void forEach(Consumer<? super Entry<K, V>> action) {
                    forEachEntry(action);
                }
            };
        }

        void forEachEntry(Consumer<? super Entry<K, V>> action) {
            entryIterator().forEachRemaining(action);
        }

        @Override
        public void clear() {
            Iterators.clear(entryIterator());
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

    static <V> V valueOrNull(Entry<?, V> entry) {
        return (entry == null) ? null : entry.getValue();
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

    @GwtIncompatible // NavigableMap
    abstract static class DescendingMap<K, V>
            extends ForwardingMap<K, V> implements NavigableMap<K, V> {

        abstract NavigableMap<K, V> forward();

        @Override
        protected final Map<K, V> delegate() {
            return forward();
        }

        private transient Comparator<? super K> comparator;

        @SuppressWarnings("unchecked")
        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> result = comparator;
            if (result == null) {
                Comparator<? super K> forwardCmp = forward().comparator();
                if (forwardCmp == null) {
                    forwardCmp = (Comparator) Ordering.natural();
                }
                result = comparator = reverse(forwardCmp);
            }
            return result;
        }

        // If we inline this, we get a javac error.
        private static <T> Ordering<T> reverse(Comparator<T> forward) {
            return Ordering.from(forward).reverse();
        }

        @Override
        public K firstKey() {
            return forward().lastKey();
        }

        @Override
        public K lastKey() {
            return forward().firstKey();
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            return forward().higherEntry(key);
        }

        @Override
        public K lowerKey(K key) {
            return forward().higherKey(key);
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            return forward().ceilingEntry(key);
        }

        @Override
        public K floorKey(K key) {
            return forward().ceilingKey(key);
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            return forward().floorEntry(key);
        }

        @Override
        public K ceilingKey(K key) {
            return forward().floorKey(key);
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            return forward().lowerEntry(key);
        }

        @Override
        public K higherKey(K key) {
            return forward().lowerKey(key);
        }

        @Override
        public Entry<K, V> firstEntry() {
            return forward().lastEntry();
        }

        @Override
        public Entry<K, V> lastEntry() {
            return forward().firstEntry();
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            return forward().pollLastEntry();
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            return forward().pollFirstEntry();
        }

        @Override
        public NavigableMap<K, V> descendingMap() {
            return forward();
        }

        private transient Set<Entry<K, V>> entrySet;

        @Override
        public Set<Entry<K, V>> entrySet() {
            Set<Entry<K, V>> result = entrySet;
            return (result == null) ? entrySet = createEntrySet() : result;
        }

        abstract Iterator<Entry<K, V>> entryIterator();

        Set<Entry<K, V>> createEntrySet() {
            class EntrySetImpl extends EntrySet<K, V> {
                @Override
                Map<K, V> map() {
                    return DescendingMap.this;
                }

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return entryIterator();
                }
            }
            return new EntrySetImpl();
        }

        @Override
        public Set<K> keySet() {
            return navigableKeySet();
        }

        private transient NavigableSet<K> navigableKeySet;

        @Override
        public NavigableSet<K> navigableKeySet() {
            NavigableSet<K> result = navigableKeySet;
            return (result == null) ? navigableKeySet = new NavigableKeySet<>(this) : result;
        }

        @Override
        public NavigableSet<K> descendingKeySet() {
            return forward().navigableKeySet();
        }

        @Override
        public NavigableMap<K, V> subMap(
                K fromKey,
                boolean fromInclusive,
                K toKey,
                boolean toInclusive) {
            return forward().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
        }

        @Override
        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return forward().tailMap(toKey, inclusive).descendingMap();
        }

        @Override
        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return forward().headMap(fromKey, inclusive).descendingMap();
        }

        @Override
        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public Collection<V> values() {
            return new Values<>(this);
        }

        @Override
        public String toString() {
            return standardToString();
        }
    }

    /** Returns a map from the ith element of list to i. */
    static <E> ImmutableMap<E, Integer> indexMap(Collection<E> list) {
        ImmutableMap.Builder<E, Integer> builder = new ImmutableMap.Builder<>(list.size());
        int i = 0;
        for (E e : list) {
            builder.put(e, i++);
        }
        return builder.build();
    }
}
