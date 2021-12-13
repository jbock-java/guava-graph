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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A discouraged (but not deprecated) precursor to Java's superior {@link Stream} library.
 *
 * <p>The following types of methods are provided:
 *
 * <ul>
 *   <li>chaining methods which return a new {@code FluentIterable} based in some way on the
 *       contents of the current one (for example {@link #transform})
 *   <li>element extraction methods which facilitate the retrieval of certain elements (for example
 *       {@link #last})
 * </ul>
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
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Collection#stream} if {@code iterable} is a {@link
     * Collection}; {@link Streams#stream(Iterable)} otherwise.
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
     * Returns a fluent iterable containing {@code elements} in the specified order.
     *
     * <p>The returned iterable is an unmodifiable view of the input array.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link java.util.stream.Stream#of(Object[])
     * Stream.of(T...)}.
     *
     * @since 20.0 (since 18.0 as an overload of {@code of})
     */
    @Beta
    public static <E> FluentIterable<E> from(E[] elements) {
        return from(Arrays.asList(elements));
    }

    /**
     * Construct a fluent iterable from another fluent iterable. This is obviously never necessary,
     * but is intended to help call out cases where one migration from {@code Iterable} to {@code
     * FluentIterable} has obviated the need to explicitly convert to a {@code FluentIterable}.
     *
     * @deprecated instances of {@code FluentIterable} don't need to be converted to {@code
     *     FluentIterable}
     */
    @Deprecated
    public static <E> FluentIterable<E> from(FluentIterable<E> iterable) {
        return checkNotNull(iterable);
    }

    /**
     * Returns a fluent iterable that combines two iterables. The returned iterable has an iterator
     * that traverses the elements in {@code a}, followed by the elements in {@code b}. The source
     * iterators are not polled until necessary.
     *
     * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
     * iterator supports it.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#concat}.
     *
     * @since 20.0
     */
    @Beta
    public static <T> FluentIterable<T> concat(
            Iterable<? extends T> a, Iterable<? extends T> b) {
        return concatNoDefensiveCopy(a, b);
    }

    /**
     * Returns a fluent iterable that combines three iterables. The returned iterable has an iterator
     * that traverses the elements in {@code a}, followed by the elements in {@code b}, followed by
     * the elements in {@code c}. The source iterators are not polled until necessary.
     *
     * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
     * iterator supports it.
     *
     * <p><b>{@code Stream} equivalent:</b> use nested calls to {@link Stream#concat}, or see the
     * advice in {@link #concat(Iterable...)}.
     *
     * @since 20.0
     */
    @Beta
    public static <T> FluentIterable<T> concat(
            Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c) {
        return concatNoDefensiveCopy(a, b, c);
    }

    /**
     * Returns a fluent iterable that combines four iterables. The returned iterable has an iterator
     * that traverses the elements in {@code a}, followed by the elements in {@code b}, followed by
     * the elements in {@code c}, followed by the elements in {@code d}. The source iterators are not
     * polled until necessary.
     *
     * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
     * iterator supports it.
     *
     * <p><b>{@code Stream} equivalent:</b> use nested calls to {@link Stream#concat}, or see the
     * advice in {@link #concat(Iterable...)}.
     *
     * @since 20.0
     */
    @Beta
    public static <T> FluentIterable<T> concat(
            Iterable<? extends T> a,
            Iterable<? extends T> b,
            Iterable<? extends T> c,
            Iterable<? extends T> d) {
        return concatNoDefensiveCopy(a, b, c, d);
    }

    /**
     * Returns a fluent iterable that combines several iterables. The returned iterable has an
     * iterator that traverses the elements of each iterable in {@code inputs}. The input iterators
     * are not polled until necessary.
     *
     * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
     * iterator supports it.
     *
     * <p><b>{@code Stream} equivalent:</b> to concatenate an arbitrary number of streams, use {@code
     * Stream.of(stream1, stream2, ...).flatMap(s -> s)}. If the sources are iterables, use {@code
     * Stream.of(iter1, iter2, ...).flatMap(Streams::stream)}.
     *
     * @throws NullPointerException if any of the provided iterables is {@code null}
     * @since 20.0
     */
    @Beta
    public static <T> FluentIterable<T> concat(
            Iterable<? extends T>... inputs) {
        return concatNoDefensiveCopy(Arrays.copyOf(inputs, inputs.length));
    }

    /**
     * Returns a fluent iterable that combines several iterables. The returned iterable has an
     * iterator that traverses the elements of each iterable in {@code inputs}. The input iterators
     * are not polled until necessary.
     *
     * <p>The returned iterable's iterator supports {@code remove()} when the corresponding input
     * iterator supports it. The methods of the returned iterable may throw {@code
     * NullPointerException} if any of the input iterators is {@code null}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code streamOfStreams.flatMap(s -> s)} or {@code
     * streamOfIterables.flatMap(Streams::stream)}. (See {@link Streams#stream}.)
     *
     * @since 20.0
     */
    @Beta
    public static <T> FluentIterable<T> concat(
            final Iterable<? extends Iterable<? extends T>> inputs) {
        checkNotNull(inputs);
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.concat(Iterators.transform(inputs.iterator(), Iterables.<T>toIterator()));
            }
        };
    }

    /** Concatenates a varargs array of iterables without making a defensive copy of the array. */
    private static <T> FluentIterable<T> concatNoDefensiveCopy(
            final Iterable<? extends T>... inputs) {
        for (Iterable<? extends T> input : inputs) {
            checkNotNull(input);
        }
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.concat(
                        /* lazily generate the iterators on each input only as needed */
                        new AbstractIndexedListIterator<Iterator<? extends T>>(inputs.length) {
                            @Override
                            public Iterator<? extends T> get(int i) {
                                return inputs[i].iterator();
                            }
                        });
            }
        };
    }

    /**
     * Returns a fluent iterable containing no elements.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#empty}.
     *
     * @since 20.0
     */
    @Beta
    public static <E> FluentIterable<E> of() {
        return FluentIterable.from(Collections.<E>emptyList());
    }

    /**
     * Returns a fluent iterable containing the specified elements in order.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link java.util.stream.Stream#of(Object[])
     * Stream.of(T...)}.
     *
     * @since 20.0
     */
    @Beta
    public static <E> FluentIterable<E> of(
            E element, E... elements) {
        return from(Lists.asList(element, elements));
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

    /**
     * Returns the number of elements in this fluent iterable.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#count}.
     */
    public final int size() {
        return Iterables.size(getDelegate());
    }

    /**
     * Returns {@code true} if this fluent iterable contains any object for which {@code
     * equals(target)} is true.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code stream.anyMatch(Predicate.isEqual(target))}.
     */
    public final boolean contains(Object target) {
        return Iterables.contains(getDelegate(), target);
    }

    /**
     * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
     * followed by those of {@code other}. The iterators are not polled until necessary.
     *
     * <p>The returned iterable's {@code Iterator} supports {@code remove()} when the corresponding
     * {@code Iterator} supports it.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#concat}.
     *
     * @since 18.0
     */
    @Beta
    public final FluentIterable<E> append(Iterable<? extends E> other) {
        return FluentIterable.concat(getDelegate(), other);
    }

    /**
     * Returns a fluent iterable whose iterators traverse first the elements of this fluent iterable,
     * followed by {@code elements}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code Stream.concat(thisStream, Stream.of(elements))}.
     *
     * @since 18.0
     */
    @Beta
    public final FluentIterable<E> append(E... elements) {
        return FluentIterable.concat(getDelegate(), Arrays.asList(elements));
    }

    /**
     * Returns the elements from this fluent iterable that satisfy a predicate. The resulting fluent
     * iterable's iterator does not support {@code remove()}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#filter} (same).
     */
    public final FluentIterable<E> filter(Predicate<? super E> predicate) {
        return from(Iterables.filter(getDelegate(), predicate));
    }

    /**
     * Returns the elements from this fluent iterable that are instances of class {@code type}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code stream.filter(type::isInstance).map(type::cast)}.
     * This does perform a little more work than necessary, so another option is to insert an
     * unchecked cast at some later point:
     *
     * <pre>
     * {@code @SuppressWarnings("unchecked") // safe because of ::isInstance check
     * ImmutableList<NewType> result =
     *     (ImmutableList) stream.filter(NewType.class::isInstance).collect(toImmutableList());}
     * </pre>
     */
    @GwtIncompatible // Class.isInstance
    public final <T> FluentIterable<T> filter(Class<T> type) {
        return from(Iterables.filter(getDelegate(), type));
    }

    /**
     * Returns a fluent iterable that applies {@code function} to each element of this fluent
     * iterable.
     *
     * <p>The returned fluent iterable's iterator supports {@code remove()} if this iterable's
     * iterator does. After a successful {@code remove()} call, this fluent iterable no longer
     * contains the corresponding element.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#map}.
     */
    public final <T> FluentIterable<T> transform(
            Function<? super E, T> function) {
        return from(Iterables.transform(getDelegate(), function));
    }

    /**
     * Returns an {@link Optional} containing the first element in this fluent iterable. If the
     * iterable is empty, {@code Optional.absent()} is returned.
     *
     * <p><b>{@code Stream} equivalent:</b> if the goal is to obtain any element, {@link
     * Stream#findAny}; if it must specifically be the <i>first</i> element, {@code Stream#findFirst}.
     *
     * @throws NullPointerException if the first element is null; if this is a possibility, use {@code
     *     iterator().next()} or {@link Iterables#getFirst} instead.
     */
    @SuppressWarnings("nullness") // Unsafe, but we can't do much about it now.
    public final Optional<E> first() {
        Iterator<E> iterator = getDelegate().iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.<E>absent();
    }

    /**
     * Returns an {@link Optional} containing the last element in this fluent iterable. If the
     * iterable is empty, {@code Optional.absent()} is returned. If the underlying {@code iterable} is
     * a {@link List} with {@link java.util.RandomAccess} support, then this operation is guaranteed
     * to be {@code O(1)}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code stream.reduce((a, b) -> b)}.
     *
     * @throws NullPointerException if the last element is null; if this is a possibility, use {@link
     *     Iterables#getLast} instead.
     */
    @SuppressWarnings("nullness") // Unsafe, but we can't do much about it now.
    public final Optional<E> last() {
        // Iterables#getLast was inlined here so we don't have to throw/catch a NSEE

        // TODO(kevinb): Support a concurrently modified collection?
        Iterable<E> iterable = getDelegate();
        if (iterable instanceof List) {
            List<E> list = (List<E>) iterable;
            if (list.isEmpty()) {
                return Optional.absent();
            }
            return Optional.of(list.get(list.size() - 1));
        }
        Iterator<E> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return Optional.absent();
        }

        /*
         * TODO(kevinb): consider whether this "optimization" is worthwhile. Users with SortedSets tend
         * to know they are SortedSets and probably would not call this method.
         */
        if (iterable instanceof SortedSet) {
            SortedSet<E> sortedSet = (SortedSet<E>) iterable;
            return Optional.of(sortedSet.last());
        }

        while (true) {
            E current = iterator.next();
            if (!iterator.hasNext()) {
                return Optional.of(current);
            }
        }
    }

    /**
     * Returns a view of this fluent iterable that skips its first {@code numberToSkip} elements. If
     * this fluent iterable contains fewer than {@code numberToSkip} elements, the returned fluent
     * iterable skips all of its elements.
     *
     * <p>Modifications to this fluent iterable before a call to {@code iterator()} are reflected in
     * the returned fluent iterable. That is, the its iterator skips the first {@code numberToSkip}
     * elements that exist when the iterator is created, not when {@code skip()} is called.
     *
     * <p>The returned fluent iterable's iterator supports {@code remove()} if the {@code Iterator} of
     * this fluent iterable supports it. Note that it is <i>not</i> possible to delete the last
     * skipped element by immediately calling {@code remove()} on the returned fluent iterable's
     * iterator, as the {@code Iterator} contract states that a call to {@code * remove()} before a
     * call to {@code next()} will throw an {@link IllegalStateException}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#skip} (same).
     */
    public final FluentIterable<E> skip(int numberToSkip) {
        return from(Iterables.skip(getDelegate(), numberToSkip));
    }

    /**
     * Creates a fluent iterable with the first {@code size} elements of this fluent iterable. If this
     * fluent iterable does not contain that many elements, the returned fluent iterable will have the
     * same behavior as this fluent iterable. The returned fluent iterable's iterator supports {@code
     * remove()} if this fluent iterable's iterator does.
     *
     * <p><b>{@code Stream} equivalent:</b> {@link Stream#limit} (same).
     *
     * @param maxSize the maximum number of elements in the returned fluent iterable
     * @throws IllegalArgumentException if {@code size} is negative
     */
    public final FluentIterable<E> limit(int maxSize) {
        return from(Iterables.limit(getDelegate(), maxSize));
    }

    /**
     * Determines whether this fluent iterable is empty.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code !stream.findAny().isPresent()}.
     */
    public final boolean isEmpty() {
        return !getDelegate().iterator().hasNext();
    }

    /**
     * Returns a {@link String} containing all of the elements of this fluent iterable joined with
     * {@code joiner}.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code joiner.join(stream.iterator())}, or, if you are not
     * using any optional {@code Joiner} features, {@code
     * stream.collect(Collectors.joining(delimiter)}.
     *
     * @since 18.0
     */
    @Beta
    public final String join(Joiner joiner) {
        return joiner.join(this);
    }

    /**
     * Returns the element at the specified position in this fluent iterable.
     *
     * <p><b>{@code Stream} equivalent:</b> {@code stream.skip(position).findFirst().get()} (but note
     * that this throws different exception types, and throws an exception if {@code null} would be
     * returned).
     *
     * @param position position of the element to return
     * @return the element at the specified position in this fluent iterable
     * @throws IndexOutOfBoundsException if {@code position} is negative or greater than or equal to
     *     the size of this fluent iterable
     */
    public final E get(int position) {
        return Iterables.get(getDelegate(), position);
    }

    /**
     * Returns a stream of this fluent iterable's contents (similar to calling {@link
     * Collection#stream} on a collection).
     *
     * <p><b>Note:</b> the earlier in the chain you can switch to {@code Stream} usage (ideally not
     * going through {@code FluentIterable} at all), the more performant and idiomatic your code will
     * be. This method is a transitional aid, to be used only when really necessary.
     *
     * @since 21.0
     */
    public final Stream<E> stream() {
        return Streams.stream(getDelegate());
    }
}
