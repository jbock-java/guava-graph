/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Optional;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * A discouraged (but not deprecated) precursor to Java's superior {@link Stream} library.
 *
 * <p>Several lesser-used features are currently available only as static methods on the {@link
 * Iterables} class.
 *
 * <p><a id="streams"></a>
 *
 * <p><b>Comparison to streams</b>
 *
 * <p>{@link Stream} is similar to this class, but generally more powerful, and certainly more
 * standard. Key differences include:
 *
 * <ul>
 *   <li>A stream is <i>single-use</i>; it becomes invalid as soon as any "terminal operation" such
 *       as {@code findFirst()} or {@code iterator()} is invoked. (Even though {@code Stream}
 *       contains all the right method <i>signatures</i> to implement {@link Iterable}, it does not
 *       actually do so, to avoid implying repeat-iterability.) {@code FluentIterable}, on the other
 *       hand, is multiple-use, and does implement {@link Iterable}.
 *   <li>Streams offer many features not found here, including {@code min/max}, {@code distinct},
 *       {@code reduce}, {@code sorted}, the very powerful {@code collect}, and built-in support for
 *       parallelizing stream operations.
 *   <li>{@code FluentIterable} contains several features not available on {@code Stream}, which are
 *       noted in the method descriptions below.
 *   <li>Streams include primitive-specialized variants such as {@code IntStream}, the use of which
 *       is strongly recommended.
 *   <li>Streams are standard Java, not requiring a third-party dependency.
 * </ul>
 *
 * <p><b>Example</b>
 *
 * <p>Here is an example that accepts a list from a database call, filters it based on a predicate,
 * transforms it by invoking {@code toString()} on each element, and returns the first 10 elements
 * as a {@code List}:
 *
 * <pre>{@code
 * ImmutableList<String> results =
 *     FluentIterable.from(database.getClientList())
 *         .filter(Client::isActiveInLastMonth)
 *         .transform(Object::toString)
 *         .limit(10)
 *         .toList();
 * }</pre>
 *
 * The approximate stream equivalent is:
 *
 * <pre>{@code
 * List<String> results =
 *     database.getClientList()
 *         .stream()
 *         .filter(Client::isActiveInLastMonth)
 *         .map(Object::toString)
 *         .limit(10)
 *         .collect(Collectors.toList());
 * }</pre>
 *
 * @author Marcin Mikosik
 * @since 12.0
 */
@GwtCompatible(emulated = true)
public abstract class FluentIterable<E> implements Iterable<E> {
    // We store 'iterable' and use it instead of 'this' to allow Iterables to perform instanceof
    // checks on the _original_ iterable when FluentIterable.from is used.
    // To avoid a self retain cycle under j2objc, we store Optional.absent() instead of
    // Optional.of(this). To access the delegate iterable, call #getDelegate(), which converts to
    // absent() back to 'this'.
    private final Optional<Iterable<E>> iterableDelegate;

    /** Constructor for use by subclasses. */
    protected FluentIterable() {
        this.iterableDelegate = Optional.absent();
    }

    FluentIterable(Iterable<E> iterable) {
        this.iterableDelegate = Optional.of(iterable);
    }

    private Iterable<E> getDelegate() {
        return iterableDelegate.or(this);
    }

    /**
     * Returns a fluent iterable that wraps {@code iterable}, or {@code iterable} itself if it is
     * already a {@code FluentIterable}.
     */
    public static <E> FluentIterable<E> from(final Iterable<E> iterable) {
        return (iterable instanceof FluentIterable)
                ? (FluentIterable<E>) iterable
                : new FluentIterable<E>(iterable) {
            @Override
            public Iterator<E> iterator() {
                return iterable.iterator();
            }
        };
    }

    /**
     * Returns a string representation of this fluent iterable, with the format {@code [e1, e2, ...,
     * en]}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code stream.collect(Collectors.joining(", ", "[", "]"))}
     * or (less efficiently) {@code stream.collect(Collectors.toList()).toString()}.
     */
    @Override
    public String toString() {
        return Iterables.toString(getDelegate());
    }
}
