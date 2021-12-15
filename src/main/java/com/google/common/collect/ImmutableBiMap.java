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
import com.google.common.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static java.util.Objects.requireNonNull;

/**
 * A {@link BiMap} whose contents will never change, with many other important properties detailed
 * at {@link ImmutableCollection}.
 *
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
public abstract class ImmutableBiMap<K, V> extends ImmutableMap<K, V>
        implements BiMap<K, V> {


    /**
     * Returns the empty bimap.
     *
     * <p><b>Performance note:</b> the instance returned is a singleton.
     */
    // Casting to any type is safe because the set will never hold any elements.
    @SuppressWarnings("unchecked")
    public static <K, V> ImmutableBiMap<K, V> of() {
        return (ImmutableBiMap<K, V>) RegularImmutableBiMap.EMPTY;
    }

    /** Returns an immutable bimap containing a single entry. */
    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1) {
        return new SingletonImmutableBiMap<>(k1, v1);
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     */
    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2) {
        return RegularImmutableBiMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     */
    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return RegularImmutableBiMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     */
    public static <K, V> ImmutableBiMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     */
    public static <K, V> ImmutableBiMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     * @since 31.0
     */
    public static <K, V> ImmutableBiMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1),
                entryOf(k2, v2),
                entryOf(k3, v3),
                entryOf(k4, v4),
                entryOf(k5, v5),
                entryOf(k6, v6));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     * @since 31.0
     */
    public static <K, V> ImmutableBiMap<K, V> of(
            K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1),
                entryOf(k2, v2),
                entryOf(k3, v3),
                entryOf(k4, v4),
                entryOf(k5, v5),
                entryOf(k6, v6),
                entryOf(k7, v7));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     * @since 31.0
     */
    public static <K, V> ImmutableBiMap<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1),
                entryOf(k2, v2),
                entryOf(k3, v3),
                entryOf(k4, v4),
                entryOf(k5, v5),
                entryOf(k6, v6),
                entryOf(k7, v7),
                entryOf(k8, v8));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     * @since 31.0
     */
    public static <K, V> ImmutableBiMap<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8,
            K k9,
            V v9) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1),
                entryOf(k2, v2),
                entryOf(k3, v3),
                entryOf(k4, v4),
                entryOf(k5, v5),
                entryOf(k6, v6),
                entryOf(k7, v7),
                entryOf(k8, v8),
                entryOf(k9, v9));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are added
     * @since 31.0
     */
    public static <K, V> ImmutableBiMap<K, V> of(
            K k1,
            V v1,
            K k2,
            V v2,
            K k3,
            V v3,
            K k4,
            V v4,
            K k5,
            V v5,
            K k6,
            V v6,
            K k7,
            V v7,
            K k8,
            V v8,
            K k9,
            V v9,
            K k10,
            V v10) {
        return RegularImmutableBiMap.fromEntries(
                entryOf(k1, v1),
                entryOf(k2, v2),
                entryOf(k3, v3),
                entryOf(k4, v4),
                entryOf(k5, v5),
                entryOf(k6, v6),
                entryOf(k7, v7),
                entryOf(k8, v8),
                entryOf(k9, v9),
                entryOf(k10, v10));
    }

    // looking for of() with > 10 entries? Use the builder or ofEntries instead.

    /**
     * Returns an immutable map containing the given entries, in order.
     *
     * @throws IllegalArgumentException if duplicate keys or values are provided
     * @since 31.0
     */
    @SafeVarargs
    public static <K, V> ImmutableBiMap<K, V> ofEntries(Entry<? extends K, ? extends V>... entries) {
        @SuppressWarnings("unchecked") // we will only ever read these
        Entry<K, V>[] entries2 = (Entry<K, V>[]) entries;
        return RegularImmutableBiMap.fromEntries(entries2);
    }
    /**
     * Returns an immutable bimap containing the same entries as {@code map}. If {@code map} somehow
     * contains entries with duplicate keys (for example, if it is a {@code SortedMap} whose
     * comparator is not <i>consistent with equals</i>), the results of this method are undefined.
     *
     * <p>The returned {@code BiMap} iterates over entries in the same order as the {@code entrySet}
     * of the original map.
     *
     * <p>Despite the method name, this method attempts to avoid actually copying the data when it is
     * safe to do so. The exact circumstances under which a copy will or will not be performed are
     * undocumented and subject to change.
     *
     * @throws IllegalArgumentException if two keys have the same value or two values have the same
     *     key
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    public static <K, V> ImmutableBiMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
        if (map instanceof ImmutableBiMap) {
            @SuppressWarnings("unchecked") // safe since map is not writable
            ImmutableBiMap<K, V> bimap = (ImmutableBiMap<K, V>) map;
            // TODO(lowasser): if we need to make a copy of a BiMap because the
            // forward map is a view, don't make a copy of the non-view delegate map
            if (!bimap.isPartialView()) {
                return bimap;
            }
        }
        return copyOf(map.entrySet());
    }

    /**
     * Returns an immutable bimap containing the given entries. The returned bimap iterates over
     * entries in the same order as the original iterable.
     *
     * @throws IllegalArgumentException if two keys have the same value or two values have the same
     *     key
     * @throws NullPointerException if any key, value, or entry is null
     * @since 19.0
     */
    @Beta
    public static <K, V> ImmutableBiMap<K, V> copyOf(
            Iterable<? extends Entry<? extends K, ? extends V>> entries) {
        @SuppressWarnings("unchecked") // we'll only be using getKey and getValue, which are covariant
        Entry<K, V>[] entryArray = (Entry<K, V>[]) Iterables.toArray(entries, EMPTY_ENTRY_ARRAY);
        switch (entryArray.length) {
            case 0:
                return of();
            case 1:
                Entry<K, V> entry = entryArray[0];
                return of(entry.getKey(), entry.getValue());
            default:
                /*
                 * The current implementation will end up using entryArray directly, though it will write
                 * over the (arbitrary, potentially mutable) Entry objects actually stored in entryArray.
                 */
                return RegularImmutableBiMap.fromEntries(entryArray);
        }
    }

    ImmutableBiMap() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>The inverse of an {@code ImmutableBiMap} is another {@code ImmutableBiMap}.
     */
    @Override
    public abstract ImmutableBiMap<V, K> inverse();

    @Override
    public ImmutableSet<V> values() {
        return inverse().keySet();
    }

    @Override
    final ImmutableSet<V> createValues() {
        throw new AssertionError("should never be called");
    }

    /**
     * Guaranteed to throw an exception and leave the bimap unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final V forcePut(K key, V value) {
        throw new UnsupportedOperationException();
    }
}
