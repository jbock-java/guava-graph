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
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2.FilteredCollection;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Static utility methods pertaining to {@link Set} instances. Also see this class's counterparts
 * {@link Lists} and {@link Maps}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#sets"> {@code Sets}</a>.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Chris Povirk
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Sets {
    private Sets() {
    }

    /**
     * {@link AbstractSet} substitute without the potentially-quadratic {@code removeAll}
     * implementation.
     */
    abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
        @Override
        public boolean removeAll(Collection<?> c) {
            return removeAllImpl(this, c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return super.retainAll(checkNotNull(c)); // GWT compatibility
        }
    }

    /**
     * Returns a new hash set using the smallest initial table size that can hold {@code expectedSize}
     * elements without resizing. Note that this is not what {@link HashSet#HashSet(int)} does, but it
     * is what most users want and expect it to do.
     *
     * <p>This behavior can't be broadly guaranteed, but has been tested with OpenJDK 1.7 and 1.8.
     *
     * @param expectedSize the number of elements you expect to add to the returned set
     * @return a new, empty hash set with enough capacity to hold {@code expectedSize} elements
     *     without resizing
     * @throws IllegalArgumentException if {@code expectedSize} is negative
     */
    public static <E> HashSet<E> newHashSetWithExpectedSize(
            int expectedSize) {
        return new HashSet<E>(Maps.capacity(expectedSize));
    }

    // LinkedHashSet

    // TreeSet

    /**
     * An unmodifiable view of a set which may be backed by other sets; this view will change as the
     * backing sets do. Contains methods to copy the data into a new set which will then remain
     * stable. There is usually no reason to retain a reference of type {@code SetView}.
     *
     * @since 2.0
     */
    public abstract static class SetView<E> extends AbstractSet<E> {
        private SetView() {
        } // no subclasses but our own

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final boolean addAll(Collection<? extends E> newElements) {
            throw new UnsupportedOperationException();
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final boolean removeAll(Collection<?> oldElements) {
            throw new UnsupportedOperationException();
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final boolean removeIf(java.util.function.Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final boolean retainAll(Collection<?> elementsToKeep) {
            throw new UnsupportedOperationException();
        }

        /**
         * Guaranteed to throw an exception and leave the collection unmodified.
         *
         * @throws UnsupportedOperationException always
         * @deprecated Unsupported operation.
         */
        @Deprecated
        @Override
        public final void clear() {
            throw new UnsupportedOperationException();
        }

        /**
         * Scope the return type to {@link UnmodifiableIterator} to ensure this is an unmodifiable view.
         *
         * @since 20.0 (present with return type {@link Iterator} since 2.0)
         */
        @Override
        public abstract UnmodifiableIterator<E> iterator();
    }
    
    /** An implementation for {@link Set#hashCode()}. */
    static int hashCodeImpl(Set<?> s) {
        int hashCode = 0;
        for (Object o : s) {
            hashCode += o != null ? o.hashCode() : 0;

            hashCode = ~~hashCode;
            // Needed to deal with unusual integer overflow in GWT.
        }
        return hashCode;
    }

    /** An implementation for {@link Set#equals(Object)}. */
    static boolean equalsImpl(Set<?> s, Object object) {
        if (s == object) {
            return true;
        }
        if (object instanceof Set) {
            Set<?> o = (Set<?>) object;

            try {
                return s.size() == o.size() && s.containsAll(o);
            } catch (NullPointerException | ClassCastException ignored) {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of the specified navigable set. This method allows modules to
     * provide users with "read-only" access to internal navigable sets. Query operations on the
     * returned set "read through" to the specified set, and attempts to modify the returned set,
     * whether direct or via its collection views, result in an {@code UnsupportedOperationException}.
     *
     * <p>The returned navigable set will be serializable if the specified navigable set is
     * serializable.
     *
     * @param set the navigable set for which an unmodifiable view is to be returned
     * @return an unmodifiable view of the specified navigable set
     * @since 12.0
     */
    public static <E> NavigableSet<E> unmodifiableNavigableSet(
            NavigableSet<E> set) {
        if (set instanceof ImmutableCollection || set instanceof UnmodifiableNavigableSet) {
            return set;
        }
        return new UnmodifiableNavigableSet<E>(set);
    }

    static final class UnmodifiableNavigableSet<E>
            extends ForwardingSortedSet<E> implements NavigableSet<E> {
        private final NavigableSet<E> delegate;
        private final SortedSet<E> unmodifiableDelegate;

        UnmodifiableNavigableSet(NavigableSet<E> delegate) {
            this.delegate = checkNotNull(delegate);
            this.unmodifiableDelegate = Collections.unmodifiableSortedSet(delegate);
        }

        @Override
        protected SortedSet<E> delegate() {
            return unmodifiableDelegate;
        }

        // default methods not forwarded by ForwardingSortedSet

        @Override
        public boolean removeIf(java.util.function.Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream<E> stream() {
            return delegate.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return delegate.parallelStream();
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            delegate.forEach(action);
        }

        @Override
        public E lower(E e) {
            return delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        private transient UnmodifiableNavigableSet<E> descendingSet;

        @Override
        public NavigableSet<E> descendingSet() {
            UnmodifiableNavigableSet<E> result = descendingSet;
            if (result == null) {
                result = descendingSet = new UnmodifiableNavigableSet<E>(delegate.descendingSet());
                result.descendingSet = this;
            }
            return result;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.unmodifiableIterator(delegate.descendingIterator());
        }

        @Override
        public NavigableSet<E> subSet(
                E fromElement,
                boolean fromInclusive,
                E toElement,
                boolean toInclusive) {
            return unmodifiableNavigableSet(
                    delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return unmodifiableNavigableSet(delegate.headSet(toElement, inclusive));
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return unmodifiableNavigableSet(delegate.tailSet(fromElement, inclusive));
        }
    }

    /** Remove each element in an iterable from a set. */
    static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
        boolean changed = false;
        while (iterator.hasNext()) {
            changed |= set.remove(iterator.next());
        }
        return changed;
    }

    static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
        checkNotNull(collection); // for GWT
        if (collection instanceof Multiset) {
            collection = ((Multiset<?>) collection).elementSet();
        }
        /*
         * AbstractSet.removeAll(List) has quadratic behavior if the list size
         * is just more than the set's size.  We augment the test by
         * assuming that sets have fast contains() performance, and other
         * collections don't.  See
         * http://code.google.com/p/guava-libraries/issues/detail?id=1013
         */
        if (collection instanceof Set && collection.size() > set.size()) {
            return Iterators.removeAll(set.iterator(), collection);
        } else {
            return removeAllImpl(set, collection.iterator());
        }
    }
}
