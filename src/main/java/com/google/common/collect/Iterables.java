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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An assortment of mainly legacy static utility methods that operate on or return objects of type
 * {@code Iterable}. Except as noted, each method has a corresponding {@link Iterator}-based method
 * in the {@link Iterators} class.
 *
 * <p><b>Java 8 users:</b> several common uses for this class are now more comprehensively addressed
 * by the new {@link java.util.stream.Stream} library. Read the method documentation below for
 * comparisons. This class is not being deprecated, but we gently encourage you to migrate to
 * streams.
 *
 * <p><i>Performance notes:</i> Unless otherwise noted, all of the iterables produced in this class
 * are <i>lazy</i>, which means that their iterators only advance the backing iteration when
 * absolutely necessary.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#iterables"> {@code
 * Iterables}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Iterables {
    private Iterables() {
    }

    /**
     * Returns a string representation of {@code iterable}, with the format {@code [e1, e2, ..., en]}
     * (that is, identical to {@link java.util.Arrays Arrays}{@code
     * .toString(Iterables.toArray(iterable))}). Note that for <i>most</i> implementations of {@link
     * Collection}, {@code collection.toString()} also gives the same result, but that behavior is not
     * generally guaranteed.
     */
    public static String toString(Iterable<?> iterable) {
        return Iterators.toString(iterable.iterator());
    }

    /**
     * Returns an iterable whose iterators cycle indefinitely over the elements of {@code iterable}.
     *
     * <p>That iterator supports {@code remove()} if {@code iterable.iterator()} does. After {@code
     * remove()} is called, subsequent cycles omit the removed element, which is no longer in {@code
     * iterable}. The iterator's {@code hasNext()} method returns {@code true} until {@code iterable}
     * is empty.
     *
     * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
     * should use an explicit {@code break} or be certain that you will eventually remove all the
     * elements.
     *
     * <p>To cycle over the iterable {@code n} times, use the following: {@code
     * Iterables.concat(Collections.nCopies(n, iterable))}
     *
     * <p><b>Java 8 users:</b> The {@code Stream} equivalent of this method is {@code
     * Stream.generate(() -> iterable).flatMap(Streams::stream)}.
     */
    public static <T> Iterable<T> cycle(final Iterable<T> iterable) {
        checkNotNull(iterable);
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.cycle(iterable);
            }

            @Override
            public Spliterator<T> spliterator() {
                return Stream.generate(() -> iterable).<T>flatMap(Streams::stream).spliterator();
            }

            @Override
            public String toString() {
                return iterable.toString() + " (cycled)";
            }
        };
    }

    /**
     * Returns an iterable whose iterators cycle indefinitely over the provided elements.
     *
     * <p>After {@code remove} is invoked on a generated iterator, the removed element will no longer
     * appear in either that iterator or any other iterator created from the same source iterable.
     * That is, this method behaves exactly as {@code Iterables.cycle(Lists.newArrayList(elements))}.
     * The iterator's {@code hasNext} method returns {@code true} until all of the original elements
     * have been removed.
     *
     * <p><b>Warning:</b> Typical uses of the resulting iterator may produce an infinite loop. You
     * should use an explicit {@code break} or be certain that you will eventually remove all the
     * elements.
     *
     * <p>To cycle over the elements {@code n} times, use the following: {@code
     * Iterables.concat(Collections.nCopies(n, Arrays.asList(elements)))}
     *
     * <p><b>Java 8 users:</b> If passing a single element {@code e}, the {@code Stream} equivalent of
     * this method is {@code Stream.generate(() -> e)}. Otherwise, put the elements in a collection
     * and use {@code Stream.generate(() -> collection).flatMap(Collection::stream)}.
     */
    @SafeVarargs
    public static <T> Iterable<T> cycle(T... elements) {
        return cycle(List.of(elements));
    }

    /**
     * Returns a view of {@code iterable} containing its first {@code limitSize} elements. If {@code
     * iterable} contains fewer than {@code limitSize} elements, the returned view contains all of its
     * elements. The returned iterable's iterator supports {@code remove()} if {@code iterable}'s
     * iterator does.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#limit}
     *
     * @param iterable the iterable to limit
     * @param limitSize the maximum number of elements in the returned iterable
     * @throws IllegalArgumentException if {@code limitSize} is negative
     * @since 3.0
     */
    public static <T> Iterable<T> limit(
            final Iterable<T> iterable, final int limitSize) {
        checkNotNull(iterable);
        checkArgument(limitSize >= 0, "limit is negative");
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.limit(iterable.iterator(), limitSize);
            }

            @Override
            public Spliterator<T> spliterator() {
                return Streams.stream(iterable).limit(limitSize).spliterator();
            }
        };
    }

    // Methods only in Iterables, not in Iterators
}
