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

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

/**
 * Static utility methods pertaining to {@link List} instances. Also see this class's counterparts
 * {@link Sets} and {@link Maps}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#lists"> {@code Lists}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
public final class Lists {
    private Lists() {
    }

    // ArrayList

    /**
     * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements.
     *
     * <p>Note that even when you do need the ability to add or remove, this method provides only a
     * tiny bit of syntactic sugar for {@code newArrayList(}{@link Arrays#asList asList}{@code
     * (...))}, or for creating an empty list then calling {@link Collections#addAll}. This method is
     * not actually very useful and will likely be deprecated in the future.
     */
    @SafeVarargs
    public static <E> ArrayList<E> newArrayList(E... elements) {
        checkNotNull(elements); // for GWT
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    static int computeArrayListCapacity(int arraySize) {
        checkNonnegative(arraySize, "arraySize");

        // TODO(kevinb): Figure out the right behavior, and document it
        return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
    }
}
