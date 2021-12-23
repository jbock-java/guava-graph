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
import com.google.common.annotations.GwtIncompatible;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link NavigableSet} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * <p><b>Warning:</b> as with any sorted collection, you are strongly advised not to use a {@link
 * Comparator} or {@link Comparable} type whose comparison behavior is <i>inconsistent with
 * equals</i>. That is, {@code a.compareTo(b)} or {@code comparator.compare(a, b)} should equal zero
 * <i>if and only if</i> {@code a.equals(b)}. If this advice is not followed, the resulting
 * collection will not correctly obey its specification.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0 (implements {@code NavigableSet} since 12.0)
 */
// TODO(benyu): benchmark and optimize all creation paths, which are a mess now
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableSortedSet<E> extends ImmutableSet.CachingAsList<E>
        implements NavigableSet<E>, SortedIterable<E> {
    static final int SPLITERATOR_CHARACTERISTICS =
            ImmutableSet.SPLITERATOR_CHARACTERISTICS | Spliterator.SORTED;

    static <E> RegularImmutableSortedSet<E> emptySet(Comparator<? super E> comparator) {
        if (Comparator.naturalOrder().equals(comparator)) {
            return (RegularImmutableSortedSet<E>) RegularImmutableSortedSet.NATURAL_EMPTY_SET;
        } else {
            return new RegularImmutableSortedSet<E>(ImmutableList.<E>of(), comparator);
        }
    }

    /** Returns an immutable sorted set containing a single element. */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(E element) {
        return new RegularImmutableSortedSet<E>(ImmutableList.of(element), Comparator.naturalOrder());
    }

    int unsafeCompare(Object a, Object b) {
        return unsafeCompare(comparator, a, b);
    }

    static int unsafeCompare(Comparator<?> comparator, Object a, Object b) {
        // Pretend the comparator can compare anything. If it turns out it can't
        // compare a and b, we should get a CCE or NPE on the subsequent line. Only methods
        // that are spec'd to throw CCE and NPE should call this.
        @SuppressWarnings({"unchecked", "nullness"})
        Comparator<Object> unsafeComparator = (Comparator<Object>) comparator;
        return unsafeComparator.compare(a, b);
    }

    final transient Comparator<? super E> comparator;

    ImmutableSortedSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the comparator that orders the elements, which is {@code Comparator#naturalOrder()} ()} when the
     * natural ordering of the elements is used. Note that its behavior is not consistent with {@link
     * SortedSet#comparator()}, which returns {@code null} to indicate natural ordering.
     */
    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override // needed to unify the iterator() methods in Collection and SortedIterable
    public abstract UnmodifiableIterator<E> iterator();

    /**
     * {@inheritDoc}
     *
     * <p>This method returns a serializable {@code ImmutableSortedSet}.
     *
     * <p>The {@link SortedSet#headSet} documentation states that a subset of a subset throws an
     * {@link IllegalArgumentException} if passed a {@code toElement} greater than an earlier {@code
     * toElement}. However, this method doesn't throw an exception in that situation, but instead
     * keeps the original {@code toElement}.
     */
    @Override
    public ImmutableSortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    /** @since 12.0 */
    @Override
    public ImmutableSortedSet<E> headSet(E toElement, boolean inclusive) {
        return headSetImpl(checkNotNull(toElement), inclusive);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method returns a serializable {@code ImmutableSortedSet}.
     *
     * <p>The {@link SortedSet#subSet} documentation states that a subset of a subset throws an {@link
     * IllegalArgumentException} if passed a {@code fromElement} smaller than an earlier {@code
     * fromElement}. However, this method doesn't throw an exception in that situation, but instead
     * keeps the original {@code fromElement}. Similarly, this method keeps the original {@code
     * toElement}, instead of throwing an exception, if passed a {@code toElement} greater than an
     * earlier {@code toElement}.
     */
    @Override
    public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    /** @since 12.0 */
    @GwtIncompatible // NavigableSet
    @Override
    public ImmutableSortedSet<E> subSet(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        checkNotNull(fromElement);
        checkNotNull(toElement);
        checkArgument(comparator.compare(fromElement, toElement) <= 0);
        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method returns a serializable {@code ImmutableSortedSet}.
     *
     * <p>The {@link SortedSet#tailSet} documentation states that a subset of a subset throws an
     * {@link IllegalArgumentException} if passed a {@code fromElement} smaller than an earlier {@code
     * fromElement}. However, this method doesn't throw an exception in that situation, but instead
     * keeps the original {@code fromElement}.
     */
    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    /** @since 12.0 */
    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement, boolean inclusive) {
        return tailSetImpl(checkNotNull(fromElement), inclusive);
    }

    /*
     * These methods perform most headSet, subSet, and tailSet logic, besides
     * parameter validation.
     */
    abstract ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive);

    abstract ImmutableSortedSet<E> subSetImpl(
            E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);

    abstract ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive);

    /** @since 12.0 */
    @GwtIncompatible // NavigableSet
    @Override
    public E lower(E e) {
        return Iterators.getNext(headSet(e, false).descendingIterator(), null);
    }

    /** @since 12.0 */
    @Override
    public E floor(E e) {
        return Iterators.getNext(headSet(e, true).descendingIterator(), null);
    }

    /** @since 12.0 */
    @Override
    public E ceiling(E e) {
        return Iterables.getFirst(tailSet(e, true), null);
    }

    /** @since 12.0 */
    @GwtIncompatible // NavigableSet
    @Override
    public E higher(E e) {
        return Iterables.getFirst(tailSet(e, false), null);
    }

    @Override
    public E first() {
        return iterator().next();
    }

    @Override
    public E last() {
        return descendingIterator().next();
    }

    /**
     * Guaranteed to throw an exception and leave the set unmodified.
     *
     * @since 12.0
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @GwtIncompatible // NavigableSet
    @Override
    public final E pollFirst() {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the set unmodified.
     *
     * @since 12.0
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @GwtIncompatible // NavigableSet
    @Override
    public final E pollLast() {
        throw new UnsupportedOperationException();
    }

    @GwtIncompatible // NavigableSet
    transient ImmutableSortedSet<E> descendingSet;

    /** @since 12.0 */
    @GwtIncompatible // NavigableSet
    @Override
    public ImmutableSortedSet<E> descendingSet() {
        // racy single-check idiom
        ImmutableSortedSet<E> result = descendingSet;
        if (result == null) {
            result = descendingSet = createDescendingSet();
            result.descendingSet = this;
        }
        return result;
    }

    // Most classes should implement this as new DescendingImmutableSortedSet<E>(this),
    // but we push down that implementation because ProGuard can't eliminate it even when it's always
    // overridden.
    @GwtIncompatible // NavigableSet
    abstract ImmutableSortedSet<E> createDescendingSet();

    @Override
    public Spliterator<E> spliterator() {
        return new Spliterators.AbstractSpliterator<E>(
                size(), SPLITERATOR_CHARACTERISTICS | Spliterator.SIZED) {
            final UnmodifiableIterator<E> iterator = iterator();

            @Override
            public boolean tryAdvance(Consumer<? super E> action) {
                if (iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public Comparator<? super E> getComparator() {
                return comparator;
            }
        };
    }

    /** @since 12.0 */
    @GwtIncompatible // NavigableSet
    @Override
    public abstract UnmodifiableIterator<E> descendingIterator();

    /** Returns the position of an element within the set, or -1 if not present. */
    abstract int indexOf(Object target);
}
