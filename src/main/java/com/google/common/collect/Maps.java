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
import com.google.common.primitives.Ints;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

/**
 * Static utility methods pertaining to {@link Map} instances (including instances of {@link
 * SortedMap}). Also see this class's counterparts {@link Lists} and {@link Sets}.
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
        LinkedHashMap<K, V> result = new LinkedHashMap<>((int) (set.size() * 1.5));
        for (K k : set) {
            result.put(k, function.apply(k));
        }
        return result;
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
}
