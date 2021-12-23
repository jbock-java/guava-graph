/*
 * Copyright (C) 2008 The Guava Authors
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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static java.util.Objects.requireNonNull;

/**
 * A {@link Map} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
public abstract class ImmutableMap<K, V> implements Map<K, V> {

    // looking for of() with > 10 entries? Use the builder or ofEntries instead.

    static void checkNoConflict(
            boolean safe, String conflictDescription, Entry<?, ?> entry1, Entry<?, ?> entry2) {
        if (!safe) {
            throw conflictException(conflictDescription, entry1, entry2);
        }
    }

    static IllegalArgumentException conflictException(
            String conflictDescription, Object entry1, Object entry2) {
        return new IllegalArgumentException(
                "Multiple entries with same " + conflictDescription + ": " + entry1 + " and " + entry2);
    }

    static final Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Entry<?, ?>[0];

    ImmutableMap() {
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V replace(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V computeIfPresent(
            K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V compute(
            K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V merge(
            K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the map unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return values().contains(value);
    }

    // Overriding to mark it Nullable
    @Override
    public abstract V get(Object key);

    /**
     * @since 21.0 (but only since 23.5 in the Android <a
     *     href="https://github.com/google/guava#guava-google-core-libraries-for-java">flavor</a>).
     *     Note, however, that Java 8 users can call this method with any version and flavor of Guava.
     */
    @Override
    public final V getOrDefault(Object key, V defaultValue) {
        V result = get(key);
        // TODO(b/192579700): Use a ternary once it no longer confuses our nullness checker.
        if (result != null) {
            return result;
        } else {
            return defaultValue;
        }
    }

    private transient ImmutableSet<Entry<K, V>> entrySet;

    /**
     * Returns an immutable set of the mappings in this map. The iteration order is specified by the
     * method used to create this map. Typically, this is insertion order.
     */
    @Override
    public ImmutableSet<Entry<K, V>> entrySet() {
        ImmutableSet<Entry<K, V>> result = entrySet;
        return (result == null) ? entrySet = createEntrySet() : result;
    }

    abstract ImmutableSet<Entry<K, V>> createEntrySet();

    private transient ImmutableSet<K> keySet;

    /**
     * Returns an immutable set of the keys in this map, in the same order that they appear in {@link
     * #entrySet}.
     */
    @Override
    public ImmutableSet<K> keySet() {
        ImmutableSet<K> result = keySet;
        return (result == null) ? keySet = createKeySet() : result;
    }

    /*
     * This could have a good default implementation of return new ImmutableKeySet<K, V>(this),
     * but ProGuard can't figure out how to eliminate that default when RegularImmutableMap
     * overrides it.
     */
    abstract ImmutableSet<K> createKeySet();

    UnmodifiableIterator<K> keyIterator() {
        final UnmodifiableIterator<Entry<K, V>> entryIterator = entrySet().iterator();
        return new UnmodifiableIterator<K>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public K next() {
                return entryIterator.next().getKey();
            }
        };
    }

    Spliterator<K> keySpliterator() {
        return CollectSpliterators.map(entrySet().spliterator(), Entry::getKey);
    }

    private transient ImmutableCollection<V> values;

    /**
     * Returns an immutable collection of the values in this map, in the same order that they appear
     * in {@link #entrySet}.
     */
    @Override
    public ImmutableCollection<V> values() {
        ImmutableCollection<V> result = values;
        return (result == null) ? values = createValues() : result;
    }

    /*
     * This could have a good default implementation of {@code return new
     * ImmutableMapValues<K, V>(this)}, but ProGuard can't figure out how to eliminate that default
     * when RegularImmutableMap overrides it.
     */
    abstract ImmutableCollection<V> createValues();

    @Override
    public boolean equals(Object object) {
        return Maps.equalsImpl(this, object);
    }

    abstract boolean isPartialView();

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(entrySet());
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public String toString() {
        return Maps.toStringImpl(this);
    }
}
