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
import com.google.common.base.Objects;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;

import java.math.RoundingMode;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;

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
@GwtCompatible(emulated = true)
public final class Lists {
    private Lists() {
    }

    // ArrayList

    /**
     * Creates a <i>mutable</i>, empty {@code ArrayList} instance (for Java 6 and earlier).
     *
     * <p><b>Note for Java 7 and later:</b> this method is now unnecessary and should be treated as
     * deprecated. Instead, use the {@code ArrayList} {@linkplain ArrayList#ArrayList() constructor}
     * directly, taking advantage of the new <a href="http://goo.gl/iz2Wi">"diamond" syntax</a>.
     */
    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements.
     *
     * <p>Note that even when you do need the ability to add or remove, this method provides only a
     * tiny bit of syntactic sugar for {@code newArrayList(}{@link Arrays#asList asList}{@code
     * (...))}, or for creating an empty list then calling {@link Collections#addAll}. This method is
     * not actually very useful and will likely be deprecated in the future.
     */
    @SafeVarargs
    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList(E... elements) {
        checkNotNull(elements); // for GWT
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Creates a <i>mutable</i> {@code ArrayList} instance containing the given elements; a very thin
     * shortcut for creating an empty list and then calling {@link Iterators#addAll}.
     */
    @GwtCompatible(serializable = true)
    public static <E> ArrayList<E> newArrayList(
            Iterator<? extends E> elements) {
        ArrayList<E> list = newArrayList();
        Iterators.addAll(list, elements);
        return list;
    }

    @VisibleForTesting
    static int computeArrayListCapacity(int arraySize) {
        checkNonnegative(arraySize, "arraySize");

        // TODO(kevinb): Figure out the right behavior, and document it
        return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
    }


    // LinkedList

    private static class Partition<T> extends AbstractList<List<T>> {
        final List<T> list;
        final int size;

        Partition(List<T> list, int size) {
            this.list = list;
            this.size = size;
        }

        @Override
        public List<T> get(int index) {
            checkElementIndex(index, size());
            int start = index * size;
            int end = Math.min(start + size, list.size());
            return list.subList(start, end);
        }

        @Override
        public int size() {
            return IntMath.divide(list.size(), size, RoundingMode.CEILING);
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }
    }

    /**
     * Returns a reversed view of the specified list. For example, {@code
     * Lists.reverse(Arrays.asList(1, 2, 3))} returns a list containing {@code 3, 2, 1}. The returned
     * list is backed by this list, so changes in the returned list are reflected in this list, and
     * vice-versa. The returned list supports all of the optional list operations supported by this
     * list.
     *
     * <p>The returned list is random-access if the specified list is random access.
     *
     * @since 7.0
     */
    public static <T> List<T> reverse(List<T> list) {
        if (list instanceof ImmutableList) {
            // Avoid nullness warnings.
            List<?> reversed = ((ImmutableList<?>) list).reverse();
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) reversed;
            return result;
        } else if (list instanceof ReverseList) {
            return ((ReverseList<T>) list).getForwardList();
        } else if (list instanceof RandomAccess) {
            return new RandomAccessReverseList<>(list);
        } else {
            return new ReverseList<>(list);
        }
    }

    private static class ReverseList<T> extends AbstractList<T> {
        private final List<T> forwardList;

        ReverseList(List<T> forwardList) {
            this.forwardList = checkNotNull(forwardList);
        }

        List<T> getForwardList() {
            return forwardList;
        }

        private int reverseIndex(int index) {
            int size = size();
            checkElementIndex(index, size);
            return (size - 1) - index;
        }

        private int reversePosition(int index) {
            int size = size();
            checkPositionIndex(index, size);
            return size - index;
        }

        @Override
        public void add(int index, T element) {
            forwardList.add(reversePosition(index), element);
        }

        @Override
        public void clear() {
            forwardList.clear();
        }

        @Override
        public T remove(int index) {
            return forwardList.remove(reverseIndex(index));
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            subList(fromIndex, toIndex).clear();
        }

        @Override
        public T set(int index, T element) {
            return forwardList.set(reverseIndex(index), element);
        }

        @Override
        public T get(int index) {
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, size());
            return reverse(forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)));
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            int start = reversePosition(index);
            final ListIterator<T> forwardIterator = forwardList.listIterator(start);
            return new ListIterator<T>() {

                boolean canRemoveOrSet;

                @Override
                public void add(T e) {
                    forwardIterator.add(e);
                    forwardIterator.previous();
                    canRemoveOrSet = false;
                }

                @Override
                public boolean hasNext() {
                    return forwardIterator.hasPrevious();
                }

                @Override
                public boolean hasPrevious() {
                    return forwardIterator.hasNext();
                }

                @Override
                        public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    canRemoveOrSet = true;
                    return forwardIterator.previous();
                }

                @Override
                public int nextIndex() {
                    return reversePosition(forwardIterator.nextIndex());
                }

                @Override
                        public T previous() {
                    if (!hasPrevious()) {
                        throw new NoSuchElementException();
                    }
                    canRemoveOrSet = true;
                    return forwardIterator.next();
                }

                @Override
                public int previousIndex() {
                    return nextIndex() - 1;
                }

                @Override
                public void remove() {
                    checkRemove(canRemoveOrSet);
                    forwardIterator.remove();
                    canRemoveOrSet = false;
                }

                @Override
                public void set(T e) {
                    checkState(canRemoveOrSet);
                    forwardIterator.set(e);
                }
            };
        }
    }

    private static class RandomAccessReverseList<T> extends ReverseList<T>
            implements RandomAccess {
        RandomAccessReverseList(List<T> forwardList) {
            super(forwardList);
        }
    }

    /** An implementation of {@link List#hashCode()}. */
    static int hashCodeImpl(List<?> list) {
        // TODO(lowasser): worth optimizing for RandomAccess?
        int hashCode = 1;
        for (Object o : list) {
            hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());

            hashCode = ~~hashCode;
            // needed to deal with GWT integer overflow
        }
        return hashCode;
    }

    /** An implementation of {@link List#equals(Object)}. */
    static boolean equalsImpl(List<?> thisList, Object other) {
        if (other == checkNotNull(thisList)) {
            return true;
        }
        if (!(other instanceof List)) {
            return false;
        }
        List<?> otherList = (List<?>) other;
        int size = thisList.size();
        if (size != otherList.size()) {
            return false;
        }
        if (thisList instanceof RandomAccess && otherList instanceof RandomAccess) {
            // avoid allocation and use the faster loop
            for (int i = 0; i < size; i++) {
                if (!Objects.equal(thisList.get(i), otherList.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return Iterators.elementsEqual(thisList.iterator(), otherList.iterator());
        }
    }

    /** An implementation of {@link List#addAll(int, Collection)}. */
    static <E> boolean addAllImpl(
            List<E> list, int index, Iterable<? extends E> elements) {
        boolean changed = false;
        ListIterator<E> listIterator = list.listIterator(index);
        for (E e : elements) {
            listIterator.add(e);
            changed = true;
        }
        return changed;
    }

    /** An implementation of {@link List#indexOf(Object)}. */
    static int indexOfImpl(List<?> list, Object element) {
        if (list instanceof RandomAccess) {
            return indexOfRandomAccess(list, element);
        } else {
            ListIterator<?> listIterator = list.listIterator();
            while (listIterator.hasNext()) {
                if (Objects.equal(element, listIterator.next())) {
                    return listIterator.previousIndex();
                }
            }
            return -1;
        }
    }

    private static int indexOfRandomAccess(List<?> list, Object element) {
        int size = list.size();
        if (element == null) {
            for (int i = 0; i < size; i++) {
                if (list.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (element.equals(list.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** An implementation of {@link List#lastIndexOf(Object)}. */
    static int lastIndexOfImpl(List<?> list, Object element) {
        if (list instanceof RandomAccess) {
            return lastIndexOfRandomAccess(list, element);
        } else {
            ListIterator<?> listIterator = list.listIterator(list.size());
            while (listIterator.hasPrevious()) {
                if (Objects.equal(element, listIterator.previous())) {
                    return listIterator.nextIndex();
                }
            }
            return -1;
        }
    }

    private static int lastIndexOfRandomAccess(List<?> list, Object element) {
        if (element == null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = list.size() - 1; i >= 0; i--) {
                if (element.equals(list.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Returns an implementation of {@link List#listIterator(int)}. */
    static <E> ListIterator<E> listIteratorImpl(List<E> list, int index) {
        return new AbstractListWrapper<>(list).listIterator(index);
    }

    /** An implementation of {@link List#subList(int, int)}. */
    static <E> List<E> subListImpl(
            final List<E> list, int fromIndex, int toIndex) {
        List<E> wrapper;
        if (list instanceof RandomAccess) {
            wrapper =
                    new RandomAccessListWrapper<E>(list) {
                        @Override
                        public ListIterator<E> listIterator(int index) {
                            return backingList.listIterator(index);
                        }

                        private static final long serialVersionUID = 0;
                    };
        } else {
            wrapper =
                    new AbstractListWrapper<E>(list) {
                        @Override
                        public ListIterator<E> listIterator(int index) {
                            return backingList.listIterator(index);
                        }

                        private static final long serialVersionUID = 0;
                    };
        }
        return wrapper.subList(fromIndex, toIndex);
    }

    private static class AbstractListWrapper<E> extends AbstractList<E> {
        final List<E> backingList;

        AbstractListWrapper(List<E> backingList) {
            this.backingList = checkNotNull(backingList);
        }

        @Override
        public void add(int index, E element) {
            backingList.add(index, element);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            return backingList.addAll(index, c);
        }

        @Override
        public E get(int index) {
            return backingList.get(index);
        }

        @Override
        public E remove(int index) {
            return backingList.remove(index);
        }

        @Override
        public E set(int index, E element) {
            return backingList.set(index, element);
        }

        @Override
        public boolean contains(Object o) {
            return backingList.contains(o);
        }

        @Override
        public int size() {
            return backingList.size();
        }
    }

    private static class RandomAccessListWrapper<E>
            extends AbstractListWrapper<E> implements RandomAccess {
        RandomAccessListWrapper(List<E> backingList) {
            super(backingList);
        }
    }

    /** Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557 */
    static <T> List<T> cast(Iterable<T> iterable) {
        return (List<T>) iterable;
    }
}
