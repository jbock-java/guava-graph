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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;
import static com.google.common.collect.ObjectArrays.checkElementsNotNull;
import static com.google.common.collect.RegularImmutableList.EMPTY;
import static java.util.Objects.requireNonNull;

/**
 * A {@link List} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableList<E> extends ImmutableCollection<E>
        implements List<E>, RandomAccess {

    /**
     * Returns the empty immutable list. This list behaves and performs comparably to {@link
     * Collections#emptyList}, and is preferable mainly for consistency and maintainability of your
     * code.
     *
     * <p><b>Performance note:</b> the instance returned is a singleton.
     */
    // Casting to any type is safe because the list will never hold any elements.
    @SuppressWarnings("unchecked")
    public static <E> ImmutableList<E> of() {
        return (ImmutableList<E>) EMPTY;
    }

    /**
     * Returns an immutable list containing a single element. This list behaves and performs
     * comparably to {@link Collections#singletonList}, but will not accept a null element. It is
     * preferable mainly for consistency and maintainability of your code.
     *
     * @throws NullPointerException if {@code element} is null
     */
    public static <E> ImmutableList<E> of(E element) {
        return new SingletonImmutableList<E>(element);
    }

    /**
     * Views the array as an immutable list. Does not check for nulls; does not copy.
     *
     * <p>The array must be internally created.
     */
    static <E> ImmutableList<E> asImmutableList(Object[] elements) {
        return asImmutableList(elements, elements.length);
    }

    /**
     * Views the array as an immutable list. Copies if the specified range does not cover the complete
     * array. Does not check for nulls.
     */
    static <E> ImmutableList<E> asImmutableList(Object[] elements, int length) {
        switch (length) {
            case 0:
                return of();
            case 1:
                /*
                 * requireNonNull is safe because the callers promise to put non-null objects in the first
                 * `length` array elements.
                 */
                @SuppressWarnings("unchecked") // our callers put only E instances into the array
                E onlyElement = (E) requireNonNull(elements[0]);
                return of(onlyElement);
            default:
                /*
                 * The suppression is safe because the callers promise to put non-null objects in the first
                 * `length` array elements.
                 */
                @SuppressWarnings("nullness")
                Object[] elementsWithoutTrailingNulls =
                        length < elements.length ? Arrays.copyOf(elements, length) : elements;
                return new RegularImmutableList<E>(elementsWithoutTrailingNulls);
        }
    }

    ImmutableList() {
    }

    // This declaration is needed to make List.iterator() and
    // ImmutableCollection.iterator() consistent.
    @Override
    public UnmodifiableIterator<E> iterator() {
        return listIterator();
    }

    @Override
    public UnmodifiableListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int index) {
        return new AbstractIndexedListIterator<E>(size(), index) {
            @Override
            protected E get(int index) {
                return ImmutableList.this.get(index);
            }
        };
    }

    @Override
    public void forEach(Consumer<? super E> consumer) {
        checkNotNull(consumer);
        int n = size();
        for (int i = 0; i < n; i++) {
            consumer.accept(get(i));
        }
    }

    @Override
    public int indexOf(Object object) {
        return (object == null) ? -1 : Lists.indexOfImpl(this, object);
    }

    @Override
    public int lastIndexOf(Object object) {
        return (object == null) ? -1 : Lists.lastIndexOfImpl(this, object);
    }

    @Override
    public boolean contains(Object object) {
        return indexOf(object) >= 0;
    }

    // constrain the return type to ImmutableList<E>

    /**
     * Returns an immutable list of the elements between the specified {@code fromIndex}, inclusive,
     * and {@code toIndex}, exclusive. (If {@code fromIndex} and {@code toIndex} are equal, the empty
     * immutable list is returned.)
     */
    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        checkPositionIndexes(fromIndex, toIndex, size());
        int length = toIndex - fromIndex;
        if (length == size()) {
            return this;
        } else if (length == 0) {
            return of();
        } else if (length == 1) {
            return of(get(fromIndex));
        } else {
            return subListUnchecked(fromIndex, toIndex);
        }
    }

    /**
     * Called by the default implementation of {@link #subList} when {@code toIndex - fromIndex > 1},
     * after index validation has already been performed.
     */
    ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
        return new SubList(fromIndex, toIndex - fromIndex);
    }

    class SubList extends ImmutableList<E> {
        final transient int offset;
        final transient int length;

        SubList(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public int size() {
            return length;
        }

        @Override
        public E get(int index) {
            checkElementIndex(index, length);
            return ImmutableList.this.get(index + offset);
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, length);
            return ImmutableList.this.subList(fromIndex + offset, toIndex + offset);
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    /**
     * Guaranteed to throw an exception and leave the list unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean addAll(int index, Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the list unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the list unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the list unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the list unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }

    /**
     * Guaranteed to throw an exception and leave the list unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns this list instance.
     *
     * @since 2.0
     * @deprecated There is no reason to use this; it always returns {@code this}.
     */
    @Deprecated
    @Override
    public final ImmutableList<E> asList() {
        return this;
    }

    @Override
    public Spliterator<E> spliterator() {
        return CollectSpliterators.indexed(size(), SPLITERATOR_CHARACTERISTICS, this::get);
    }

    @Override
    int copyIntoArray(Object[] dst, int offset) {
        // this loop is faster for RandomAccess instances, which ImmutableLists are
        int size = size();
        for (int i = 0; i < size; i++) {
            dst[offset + i] = get(i);
        }
        return offset + size;
    }

    /**
     * Returns a view of this immutable list in reverse order. For example, {@code ImmutableList.of(1,
     * 2, 3).reverse()} is equivalent to {@code ImmutableList.of(3, 2, 1)}.
     *
     * @return a view of this immutable list in reverse order
     * @since 7.0
     */
    public ImmutableList<E> reverse() {
        return (size() <= 1) ? this : new ReverseImmutableList<E>(this);
    }

    private static class ReverseImmutableList<E> extends ImmutableList<E> {
        private final transient ImmutableList<E> forwardList;

        ReverseImmutableList(ImmutableList<E> backingList) {
            this.forwardList = backingList;
        }

        private int reverseIndex(int index) {
            return (size() - 1) - index;
        }

        private int reversePosition(int index) {
            return size() - index;
        }

        @Override
        public ImmutableList<E> reverse() {
            return forwardList;
        }

        @Override
        public boolean contains(Object object) {
            return forwardList.contains(object);
        }

        @Override
        public int indexOf(Object object) {
            int index = forwardList.lastIndexOf(object);
            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public int lastIndexOf(Object object) {
            int index = forwardList.indexOf(object);
            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            checkPositionIndexes(fromIndex, toIndex, size());
            return forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)).reverse();
        }

        @Override
        public E get(int index) {
            checkElementIndex(index, size());
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        boolean isPartialView() {
            return forwardList.isPartialView();
        }
    }

    @Override
    public boolean equals(Object obj) {
        return Lists.equalsImpl(this, obj);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        int n = size();
        for (int i = 0; i < n; i++) {
            hashCode = 31 * hashCode + get(i).hashCode();

            hashCode = ~~hashCode;
            // needed to deal with GWT integer overflow
        }
        return hashCode;
    }

    /**
     * Returns a new builder. The generated builder is equivalent to the builder created by the {@link
     * Builder} constructor.
     */
    public static <E> Builder<E> builder() {
        return new Builder<E>();
    }

    /**
     * A builder for creating immutable list instances, especially {@code public static final} lists
     * ("constant lists"). Example:
     *
     * <pre>{@code
     * public static final ImmutableList<Color> GOOGLE_COLORS
     *     = new ImmutableList.Builder<Color>()
     *         .addAll(WEBSAFE_COLORS)
     *         .add(new Color(0, 191, 255))
     *         .build();
     * }</pre>
     *
     * <p>Elements appear in the resulting list in the same order they were added to the builder.
     *
     * @since 2.0
     */
    public static final class Builder<E> extends ImmutableCollection.Builder<E> {
        // The first `size` elements are non-null.
        @VisibleForTesting
        Object[] contents;
        private int size;
        private boolean forceCopy;

        public Builder() {
            this(DEFAULT_INITIAL_CAPACITY);
        }

        Builder(int capacity) {
            this.contents = new Object[capacity];
            this.size = 0;
        }

        private void getReadyToExpandTo(int minCapacity) {
            if (contents.length < minCapacity) {
                this.contents = Arrays.copyOf(contents, expandedCapacity(contents.length, minCapacity));
                forceCopy = false;
            } else if (forceCopy) {
                contents = Arrays.copyOf(contents, contents.length);
                forceCopy = false;
            }
        }

        /**
         * Adds {@code element} to the {@code ImmutableList}.
         *
         * @param element the element to add
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code element} is null
         */
        @Override
        public Builder<E> add(E element) {
            checkNotNull(element);
            getReadyToExpandTo(size + 1);
            contents[size++] = element;
            return this;
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableList}.
         *
         * @param elements the {@code Iterable} to add to the {@code ImmutableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        @Override
        public Builder<E> add(E... elements) {
            checkElementsNotNull(elements);
            add(elements, elements.length);
            return this;
        }

        private void add(Object[] elements, int n) {
            getReadyToExpandTo(size + n);
            /*
             * The following call is not statically checked, since arraycopy accepts plain Object for its
             * parameters. If it were statically checked, the checker would still be OK with it, since
             * we're copying into a `contents` array whose type allows it to contain nulls. Still, it's
             * worth noting that we promise not to put nulls into the array in the first `size` elements.
             * We uphold that promise here because our callers promise that `elements` will not contain
             * nulls in its first `n` elements.
             */
            System.arraycopy(elements, 0, contents, size, n);
            size += n;
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableList}.
         *
         * @param elements the {@code Iterable} to add to the {@code ImmutableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            checkNotNull(elements);
            if (elements instanceof Collection) {
                Collection<?> collection = (Collection<?>) elements;
                getReadyToExpandTo(size + collection.size());
                if (collection instanceof ImmutableCollection) {
                    ImmutableCollection<?> immutableCollection = (ImmutableCollection<?>) collection;
                    size = immutableCollection.copyIntoArray(contents, size);
                    return this;
                }
            }
            super.addAll(elements);
            return this;
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableList}.
         *
         * @param elements the {@code Iterator} to add to the {@code ImmutableList}
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        @Override
        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        Builder<E> combine(Builder<E> builder) {
            checkNotNull(builder);
            add(builder.contents, builder.size);
            return this;
        }

        /**
         * Returns a newly-created {@code ImmutableList} based on the contents of the {@code Builder}.
         */
        @Override
        public ImmutableList<E> build() {
            forceCopy = true;
            return asImmutableList(contents, size);
        }
    }
}
