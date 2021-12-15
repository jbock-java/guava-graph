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

import com.google.common.annotations.GwtCompatible;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Methods factored out so that they can be emulated differently in GWT.
 *
 * @author Hayward Chan
 */
@GwtCompatible(emulated = true)
final class Platform {

    /**
     * Returns a new array of the given length with the same type as a reference array.
     *
     * @param reference any array of the desired type
     * @param length the length of the new array
     */
    /*
     * The new array contains nulls, even if the old array did not. If we wanted to be accurate, we
     * would declare a return type of `T[]`. However, we've decided not to think too hard
     * about arrays for now, as they're a mess. (We previously discussed this in the review of
     * ObjectArrays, which is the main caller of this method.)
     */
    static <T> T[] newArray(T[] reference, int length) {
        Class<?> type = reference.getClass().getComponentType();

        // the cast is safe because
        // result.getClass() == reference.getClass().getComponentType()
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, length);
        return result;
    }

    /** Equivalent to Arrays.copyOfRange(source, from, to, arrayOfType.getClass()). */
    /*
     * Arrays are a mess from a nullness perspective, and Class instances for object-array types are
     * even worse. For now, we just suppress and move on with our lives.
     *
     * - https://github.com/jspecify/jspecify/issues/65
     *
     * - https://github.com/jspecify/jdk/commit/71d826792b8c7ef95d492c50a274deab938f2552
     */
    @SuppressWarnings("nullness")
    static <T> T[] copy(Object[] source, int from, int to, T[] arrayOfType) {
        return Arrays.copyOfRange(source, from, to, (Class<? extends T[]>) arrayOfType.getClass());
    }

    private Platform() {
    }
}
