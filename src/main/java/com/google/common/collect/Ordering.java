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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;

/**
 * A comparator, with additional methods to support common operations. This is an "enriched" version
 * of {@code Comparator} for pre-Java-8 users, in the same sense that {@link FluentIterable} is an
 * enriched {@link Iterable} for pre-Java-8 users.
 *
 * <p><b>Three types of methods</b>
 *
 * Like other fluent types, there are three types of methods present: methods for <i>acquiring</i>,
 * <i>chaining</i>, and <i>using</i>.
 *
 * <p><b>Acquiring</b>
 *
 * <p>The common ways to get an instance of {@code Ordering} are:
 *
 * <ul>
 *   <li>Subclass it and implement {@link #compare} instead of implementing {@link Comparator}
 *       directly
 *   <li>Pass a <i>pre-existing</i> {@link Comparator} instance to {@link #from(Comparator)}
 * </ul>
 *
 * <p><b>Chaining</b>
 *
 * <p>Then you can use the <i>chaining</i> methods to get an altered version of that {@code
 * Ordering}, including:
 *
 * <ul>
 *   <li>{@link #onResultOf(Function)}
 * </ul>
 *
 * <p><b>Using</b>
 *
 * <p>Finally, use the resulting {@code Ordering} anywhere a {@link Comparator} is required, or use
 * any of its special operations, such as:
 *
 * <ul>
 *   <li>{@link #min} / {@link #max}
 * </ul>
 *
 * <p><b>Understanding complex orderings</b>
 *
 * <p>Complex chained orderings like the following example can be challenging to understand.
 *
 * <pre>{@code
 * Ordering<Foo> ordering =
 *     Ordering.natural()
 *         .nullsFirst()
 *         .onResultOf(getBarFunction)
 *         .nullsLast();
 * }</pre>
 *
 * Note that each chaining method returns a new ordering instance which is backed by the previous
 * instance, but has the chance to act on values <i>before</i> handing off to that backing instance.
 * As a result, it usually helps to read chained ordering expressions <i>backwards</i>. For example,
 * when {@code compare} is called on the above ordering:
 *
 * <ol>
 *   <li>First, if only one {@code Foo} is null, that null value is treated as <i>greater</i>
 *   <li>Next, non-null {@code Foo} values are passed to {@code getBarFunction} (we will be
 *       comparing {@code Bar} values from now on)
 *   <li>Next, if only one {@code Bar} is null, that null value is treated as <i>lesser</i>
 *   <li>Finally, natural ordering is used (i.e. the result of {@code Bar.compareTo(Bar)} is
 *       returned)
 * </ol>
 *
 * <p><b>Additional notes</b>
 *
 * <p>Except as noted, the orderings returned by the factory methods of this class are serializable
 * if and only if the provided instances that back them are. For example, if {@code ordering} and
 * {@code function} can themselves be serialized, then {@code ordering.onResultOf(function)} can as
 * well.
 *
 * <p><b>For Java 8 users</b>
 *
 * <p>Many replacements involve adopting {@code Stream}, and these changes can sometimes make your
 * code verbose. Whenever following this advice, you should check whether {@code Stream} could be
 * adopted more comprehensively in your code; the end result may be quite a bit simpler.
 *
 * <p><b>See also</b>
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/OrderingExplained">{@code Ordering}</a>.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 * @since 2.0
 */
public abstract class Ordering<T> implements Comparator<T> {
    // Natural order

    /**
     * Returns a serializable ordering that uses the natural order of the values. The ordering throws
     * a {@link NullPointerException} when passed a null parameter.
     *
     * <p>The type specification is {@code <C extends Comparable>}, instead of the technically correct
     * {@code <C extends Comparable<? super C>>}, to support legacy types from before Java 5.
     *
     * <p><b>Java 8 users:</b> use {@link Comparator#naturalOrder} instead.
     */
    @GwtCompatible(serializable = true)
    @SuppressWarnings("unchecked") // TODO(kevinb): right way to explain this??
    public static <C extends Comparable> Ordering<C> natural() {
        return (Ordering<C>) NaturalOrdering.INSTANCE;
    }

    // Static factories

    /**
     * Returns an ordering based on an <i>existing</i> comparator instance. Note that it is
     * unnecessary to create a <i>new</i> anonymous inner class implementing {@code Comparator} just
     * to pass it in here. Instead, simply subclass {@code Ordering} and implement its {@code compare}
     * method directly.
     *
     * <p><b>Java 8 users:</b> this class is now obsolete as explained in the class documentation, so
     * there is no need to use this method.
     *
     * @param comparator the comparator that defines the order
     * @return comparator itself if it is already an {@code Ordering}; otherwise an ordering that
     *     wraps that comparator
     */
    @GwtCompatible(serializable = true)
    public static <T> Ordering<T> from(Comparator<T> comparator) {
        return (comparator instanceof Ordering)
                ? (Ordering<T>) comparator
                : new ComparatorOrdering<T>(comparator);
    }

    /**
     * Constructs a new instance of this class (only invokable by the subclass constructor, typically
     * implicit).
     */
    protected Ordering() {
    }

    // Instance-based factories (and any static equivalents)

    /**
     * Returns the reverse of this ordering; the {@code Ordering} equivalent to {@link
     * Collections#reverseOrder(Comparator)}.
     *
     * <p><b>Java 8 users:</b> Use {@code thisComparator.reversed()} instead.
     */
    // type parameter <S> lets us avoid the extra <String> in statements like:
    // Ordering<String> o = Ordering.<String>natural().reverse();
    @GwtCompatible(serializable = true)
    public <S extends T> Ordering<S> reverse() {
        return new ReverseOrdering<S>(this);
    }

    /**
     * Returns a new ordering on {@code F} which orders elements by first applying a function to them,
     * then comparing those results using {@code this}. For example, to compare objects by their
     * string forms, in a case-insensitive manner, use:
     *
     * <pre>{@code
     * Ordering.from(String.CASE_INSENSITIVE_ORDER)
     *     .onResultOf(Functions.toStringFunction())
     * }</pre>
     *
     * <p><b>Java 8 users:</b> Use {@code Comparator.comparing(function, thisComparator)} instead (you
     * can omit the comparator if it is the natural order).
     */
    @GwtCompatible(serializable = true)
    public <F> Ordering<F> onResultOf(Function<F, ? extends T> function) {
        return new ByFunctionOrdering<>(function, this);
    }

    // Regular instance methods

    @Override
    public abstract int compare(T left, T right);

    /**
     * Returns the least of the specified values according to this ordering. If there are multiple
     * least values, the first of those is returned. The iterator will be left exhausted: its {@code
     * hasNext()} method will return {@code false}.
     *
     * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterator).min(thisComparator).get()} instead
     * (but note that it does not guarantee which tied minimum element is returned).
     *
     * @param iterator the iterator whose minimum element is to be determined
     * @throws NoSuchElementException if {@code iterator} is empty
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     * @since 11.0
     */
    public <E extends T> E min(Iterator<E> iterator) {
        // let this throw NoSuchElementException as necessary
        E minSoFar = iterator.next();

        while (iterator.hasNext()) {
            minSoFar = min(minSoFar, iterator.next());
        }

        return minSoFar;
    }

    /**
     * Returns the least of the specified values according to this ordering. If there are multiple
     * least values, the first of those is returned.
     *
     * <p><b>Java 8 users:</b> If {@code iterable} is a {@link Collection}, use {@code
     * Collections.min(collection, thisComparator)} instead. Otherwise, use {@code
     * Streams.stream(iterable).min(thisComparator).get()} instead. Note that these alternatives do
     * not guarantee which tied minimum element is returned)
     *
     * @param iterable the iterable whose minimum element is to be determined
     * @throws NoSuchElementException if {@code iterable} is empty
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     */
    public <E extends T> E min(Iterable<E> iterable) {
        return min(iterable.iterator());
    }

    /**
     * Returns the lesser of the two values according to this ordering. If the values compare as 0,
     * the first is returned.
     *
     * <p><b>Implementation note:</b> this method is invoked by the default implementations of the
     * other {@code min} overloads, so overriding it will affect their behavior.
     *
     * <p><b>Note:</b> Consider using {@code Comparators.min(a, b, thisComparator)} instead. If {@code
     * thisComparator} is {@link Comparator#naturalOrder()}, then use {@code Comparators.min(a, b)}.
     *
     * @param a value to compare, returned if less than or equal to b.
     * @param b value to compare.
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     */
    public <E extends T> E min(E a, E b) {
        return (compare(a, b) <= 0) ? a : b;
    }

    /**
     * Returns the least of the specified values according to this ordering. If there are multiple
     * least values, the first of those is returned.
     *
     * <p><b>Java 8 users:</b> Use {@code Collections.min(Arrays.asList(a, b, c...), thisComparator)}
     * instead (but note that it does not guarantee which tied minimum element is returned).
     *
     * @param a value to compare, returned if less than or equal to the rest.
     * @param b value to compare
     * @param c value to compare
     * @param rest values to compare
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     */
    public <E extends T> E min(
            E a, E b, E c, E... rest) {
        E minSoFar = min(min(a, b), c);

        for (E r : rest) {
            minSoFar = min(minSoFar, r);
        }

        return minSoFar;
    }

    /**
     * Returns the greatest of the specified values according to this ordering. If there are multiple
     * greatest values, the first of those is returned. The iterator will be left exhausted: its
     * {@code hasNext()} method will return {@code false}.
     *
     * <p><b>Java 8 users:</b> Use {@code Streams.stream(iterator).max(thisComparator).get()} instead
     * (but note that it does not guarantee which tied maximum element is returned).
     *
     * @param iterator the iterator whose maximum element is to be determined
     * @throws NoSuchElementException if {@code iterator} is empty
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     * @since 11.0
     */
    public <E extends T> E max(Iterator<E> iterator) {
        // let this throw NoSuchElementException as necessary
        E maxSoFar = iterator.next();

        while (iterator.hasNext()) {
            maxSoFar = max(maxSoFar, iterator.next());
        }

        return maxSoFar;
    }

    /**
     * Returns the greatest of the specified values according to this ordering. If there are multiple
     * greatest values, the first of those is returned.
     *
     * <p><b>Java 8 users:</b> If {@code iterable} is a {@link Collection}, use {@code
     * Collections.max(collection, thisComparator)} instead. Otherwise, use {@code
     * Streams.stream(iterable).max(thisComparator).get()} instead. Note that these alternatives do
     * not guarantee which tied maximum element is returned)
     *
     * @param iterable the iterable whose maximum element is to be determined
     * @throws NoSuchElementException if {@code iterable} is empty
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     */
    public <E extends T> E max(Iterable<E> iterable) {
        return max(iterable.iterator());
    }

    /**
     * Returns the greater of the two values according to this ordering. If the values compare as 0,
     * the first is returned.
     *
     * <p><b>Implementation note:</b> this method is invoked by the default implementations of the
     * other {@code max} overloads, so overriding it will affect their behavior.
     *
     * <p><b>Note:</b> Consider using {@code Comparators.max(a, b, thisComparator)} instead. If {@code
     * thisComparator} is {@link Comparator#naturalOrder()}, then use {@code Comparators.max(a, b)}.
     *
     * @param a value to compare, returned if greater than or equal to b.
     * @param b value to compare.
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     */
    public <E extends T> E max(E a, E b) {
        return (compare(a, b) >= 0) ? a : b;
    }

    /**
     * Returns the greatest of the specified values according to this ordering. If there are multiple
     * greatest values, the first of those is returned.
     *
     * <p><b>Java 8 users:</b> Use {@code Collections.max(Arrays.asList(a, b, c...), thisComparator)}
     * instead (but note that it does not guarantee which tied maximum element is returned).
     *
     * @param a value to compare, returned if greater than or equal to the rest.
     * @param b value to compare
     * @param c value to compare
     * @param rest values to compare
     * @throws ClassCastException if the parameters are not <i>mutually comparable</i> under this
     *     ordering.
     */
    public <E extends T> E max(
            E a, E b, E c, E... rest) {
        E maxSoFar = max(max(a, b), c);

        for (E r : rest) {
            maxSoFar = max(maxSoFar, r);
        }

        return maxSoFar;
    }

    // Never make these public
    static final int LEFT_IS_GREATER = 1;
    static final int RIGHT_IS_GREATER = -1;
}
