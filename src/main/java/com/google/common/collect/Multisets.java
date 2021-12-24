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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Objects;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;

/**
 * Provides static utility methods for creating and working with {@link Multiset} instances.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#multisets"> {@code
 * Multisets}</a>.
 *
 * @author Kevin Bourrillion
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
public final class Multisets {
    private Multisets() {
    }

    /**
     * Returns the expected number of distinct elements given the specified elements. The number of
     * distinct elements is only computed if {@code elements} is an instance of {@code Multiset};
     * otherwise the default value of 11 is returned.
     */
    static int inferDistinctElements(Iterable<?> elements) {
        if (elements instanceof Multiset) {
            return ((Multiset<?>) elements).elementSet().size();
        }
        return 11; // initial capacity will be rounded up to 16
    }

    /**
     * Implementation of the {@code equals}, {@code hashCode}, and {@code toString} methods of {@link
     * Multiset.Entry}.
     */
    abstract static class AbstractEntry<E> implements Multiset.Entry<E> {
        /**
         * Indicates whether an object equals this entry, following the behavior specified in {@link
         * Multiset.Entry#equals}.
         */
        @Override
        public boolean equals(Object object) {
            if (object instanceof Multiset.Entry) {
                Multiset.Entry<?> that = (Multiset.Entry<?>) object;
                return this.getCount() == that.getCount()
                        && Objects.equal(this.getElement(), that.getElement());
            }
            return false;
        }

        /**
         * Return this entry's hash code, following the behavior specified in {@link
         * Multiset.Entry#hashCode}.
         */
        @Override
        public int hashCode() {
            E e = getElement();
            return ((e == null) ? 0 : e.hashCode()) ^ getCount();
        }

        /**
         * Returns a string representation of this multiset entry. The string representation consists of
         * the associated element if the associated count is one, and otherwise the associated element
         * followed by the characters " x " (space, x and space) followed by the count. Elements and
         * counts are converted to strings as by {@code String.valueOf}.
         */
        @Override
        public String toString() {
            String text = String.valueOf(getElement());
            int n = getCount();
            return (n == 1) ? text : (text + " x " + n);
        }
    }

    /** An implementation of {@link Multiset#equals}. */
    static boolean equalsImpl(Multiset<?> multiset, Object object) {
        if (object == multiset) {
            return true;
        }
        if (object instanceof Multiset) {
            Multiset<?> that = (Multiset<?>) object;
            /*
             * We can't simply check whether the entry sets are equal, since that
             * approach fails when a TreeMultiset has a comparator that returns 0
             * when passed unequal elements.
             */

            if (multiset.size() != that.size() || multiset.entrySet().size() != that.entrySet().size()) {
                return false;
            }
            for (Entry<?> entry : that.entrySet()) {
                if (multiset.count(entry.getElement()) != entry.getCount()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** An implementation of {@link Multiset#addAll}. */
    static <E> boolean addAllImpl(
            Multiset<E> self, Collection<? extends E> elements) {
        checkNotNull(self);
        checkNotNull(elements);
        if (elements instanceof Multiset) {
            return addAllImpl(self, cast(elements));
        } else if (elements.isEmpty()) {
            return false;
        } else {
            return Iterators.addAll(self, elements.iterator());
        }
    }

    /** A specialization of {@code addAllImpl} for when {@code elements} is itself a Multiset. */
    private static <E> boolean addAllImpl(
            Multiset<E> self, Multiset<? extends E> elements) {
        if (elements.isEmpty()) {
            return false;
        }
        elements.forEachEntry(self::add);
        return true;
    }

    /** An implementation of {@link Multiset#removeAll}. */
    static boolean removeAllImpl(Multiset<?> self, Collection<?> elementsToRemove) {
        Collection<?> collection =
                (elementsToRemove instanceof Multiset)
                        ? ((Multiset<?>) elementsToRemove).elementSet()
                        : elementsToRemove;

        return self.elementSet().removeAll(collection);
    }

    /** An implementation of {@link Multiset#retainAll}. */
    static boolean retainAllImpl(Multiset<?> self, Collection<?> elementsToRetain) {
        checkNotNull(elementsToRetain);
        Collection<?> collection =
                (elementsToRetain instanceof Multiset)
                        ? ((Multiset<?>) elementsToRetain).elementSet()
                        : elementsToRetain;

        return self.elementSet().retainAll(collection);
    }

    /** An implementation of {@link Multiset#setCount(Object, int)}. */
    static <E> int setCountImpl(
            Multiset<E> self, E element, int count) {
        checkNonnegative(count, "count");

        int oldCount = self.count(element);

        int delta = count - oldCount;
        if (delta > 0) {
            self.add(element, delta);
        } else if (delta < 0) {
            self.remove(element, -delta);
        }

        return oldCount;
    }

    /** An implementation of {@link Multiset#setCount(Object, int, int)}. */
    static <E> boolean setCountImpl(
            Multiset<E> self, E element, int oldCount, int newCount) {
        checkNonnegative(oldCount, "oldCount");
        checkNonnegative(newCount, "newCount");

        if (self.count(element) == oldCount) {
            self.setCount(element, newCount);
            return true;
        } else {
            return false;
        }
    }

    abstract static class ElementSet<E> extends Sets.ImprovedAbstractSet<E> {
        abstract Multiset<E> multiset();

        @Override
        public void clear() {
            multiset().clear();
        }

        @Override
        public boolean contains(Object o) {
            return multiset().contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return multiset().containsAll(c);
        }

        @Override
        public boolean isEmpty() {
            return multiset().isEmpty();
        }

        @Override
        public abstract Iterator<E> iterator();

        @Override
        public boolean remove(Object o) {
            return multiset().remove(o, Integer.MAX_VALUE) > 0;
        }

        @Override
        public int size() {
            return multiset().entrySet().size();
        }
    }

    abstract static class EntrySet<E>
            extends Sets.ImprovedAbstractSet<Entry<E>> {
        abstract Multiset<E> multiset();

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                /*
                 * The GWT compiler wrongly issues a warning here.
                 */
                @SuppressWarnings("cast")
                Entry<?> entry = (Entry<?>) o;
                if (entry.getCount() <= 0) {
                    return false;
                }
                int count = multiset().count(entry.getElement());
                return count == entry.getCount();
            }
            return false;
        }

        // GWT compiler warning; see contains().
        @SuppressWarnings("cast")
        @Override
        public boolean remove(Object object) {
            if (object instanceof Multiset.Entry) {
                Entry<?> entry = (Entry<?>) object;
                Object element = entry.getElement();
                int entryCount = entry.getCount();
                if (entryCount != 0) {
                    // Safe as long as we never add a new entry, which we won't.
                    // (Presumably it can still throw CCE/NPE but only if the underlying Multiset does.)
                    @SuppressWarnings({"unchecked", "nullness"})
                    Multiset<Object> multiset = (Multiset<Object>) multiset();
                    return multiset.setCount(element, entryCount, 0);
                }
            }
            return false;
        }

        @Override
        public void clear() {
            multiset().clear();
        }
    }

    /** An implementation of {@link Multiset#iterator}. */
    static <E> Iterator<E> iteratorImpl(Multiset<E> multiset) {
        return new MultisetIteratorImpl<E>(multiset, multiset.entrySet().iterator());
    }

    static final class MultisetIteratorImpl<E> implements Iterator<E> {
        private final Multiset<E> multiset;
        private final Iterator<Entry<E>> entryIterator;
        private Entry<E> currentEntry;

        /** Count of subsequent elements equal to current element */
        private int laterCount;

        /** Count of all elements equal to current element */
        private int totalCount;

        private boolean canRemove;

        MultisetIteratorImpl(Multiset<E> multiset, Iterator<Entry<E>> entryIterator) {
            this.multiset = multiset;
            this.entryIterator = entryIterator;
        }

        @Override
        public boolean hasNext() {
            return laterCount > 0 || entryIterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (laterCount == 0) {
                currentEntry = entryIterator.next();
                totalCount = laterCount = currentEntry.getCount();
            }
            laterCount--;
            canRemove = true;
            /*
             * requireNonNull is safe because laterCount starts at 0, forcing us to initialize
             * currentEntry above. After that, we never clear it.
             */
            return requireNonNull(currentEntry).getElement();
        }

        @Override
        public void remove() {
            checkRemove(canRemove);
            if (totalCount == 1) {
                entryIterator.remove();
            } else {
                /*
                 * requireNonNull is safe because canRemove is set to true only after we initialize
                 * currentEntry (which we never subsequently clear).
                 */
                multiset.remove(requireNonNull(currentEntry).getElement());
            }
            totalCount--;
            canRemove = false;
        }
    }

    static <E> Spliterator<E> spliteratorImpl(Multiset<E> multiset) {
        Spliterator<Entry<E>> entrySpliterator = multiset.entrySet().spliterator();
        return CollectSpliterators.flatMap(
                entrySpliterator,
                entry -> Collections.nCopies(entry.getCount(), entry.getElement()).spliterator(),
                Spliterator.SIZED
                        | (entrySpliterator.characteristics()
                        & (Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE)),
                multiset.size());
    }

    /** Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557 */
    static <T> Multiset<T> cast(Iterable<T> iterable) {
        return (Multiset<T>) iterable;
    }
}
