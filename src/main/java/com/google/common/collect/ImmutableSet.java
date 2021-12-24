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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * A {@link Set} whose contents will never change, with many other important properties detailed at
 * {@link ImmutableCollection}.
 *
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
    static final int SPLITERATOR_CHARACTERISTICS =
            ImmutableCollection.SPLITERATOR_CHARACTERISTICS | Spliterator.DISTINCT;

    ImmutableSet() {
    }

    /** Returns {@code true} if the {@code hashCode()} method runs quickly. */
    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof ImmutableSet
                && isHashCodeFast()
                && ((ImmutableSet<?>) object).isHashCodeFast()
                && hashCode() != object.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    // This declaration is needed to make Set.iterator() and
    // ImmutableCollection.iterator() consistent.
    @Override
    public abstract UnmodifiableIterator<E> iterator();

    abstract static class CachingAsList<E> extends ImmutableSet<E> {
        private transient ImmutableList<E> asList;

        @Override
        public ImmutableList<E> asList() {
            ImmutableList<E> result = asList;
            if (result == null) {
                return asList = createAsList();
            } else {
                return result;
            }
        }

        ImmutableList<E> createAsList() {
            return new RegularImmutableAsList<E>(this, toArray());
        }
    }

    /**
     * A builder for creating {@code ImmutableSet} instances. Example:
     *
     * <pre>{@code
     * static final ImmutableSet<Color> GOOGLE_COLORS =
     *     ImmutableSet.<Color>builder()
     *         .addAll(WEBSAFE_COLORS)
     *         .add(new Color(0, 191, 255))
     *         .build();
     * }</pre>
     *
     * <p>Elements appear in the resulting set in the same order they were first added to the builder.
     *
     * <p>Building does not change the state of the builder, so it is still possible to add more
     * elements and to build again.
     *
     * @since 2.0
     */
    public static class Builder<E> extends ImmutableCollection.Builder<E> {
        /*
         * `impl` is null only for instances of the subclass, ImmutableSortedSet.Builder. That subclass
         * overrides all the methods that access it here. Thus, all the methods here can safely assume
         * that this field is non-null.
         */
        private SetBuilderImpl<E> impl;
        boolean forceCopy;

        final void copyIfNecessary() {
            if (forceCopy) {
                copy();
                forceCopy = false;
            }
        }

        void copy() {
            requireNonNull(impl); // see the comment on the field
            impl = impl.copy();
        }

        @Override
        public Builder<E> add(E element) {
            requireNonNull(impl); // see the comment on the field
            checkNotNull(element);
            copyIfNecessary();
            impl = impl.add(element);
            return this;
        }

        @Override
        public Builder<E> add(E... elements) {
            super.add(elements);
            return this;
        }

        /**
         * Adds each element of {@code elements} to the {@code ImmutableSet}, ignoring duplicate
         * elements (only the first duplicate element is added).
         *
         * @param elements the elements to add
         * @return this {@code Builder} object
         * @throws NullPointerException if {@code elements} is null or contains a null element
         */
        @Override
        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        @Override
        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        @Override
        public Set<E> build() {
            requireNonNull(impl); // see the comment on the field
            forceCopy = true;
            impl = impl.review();
            return impl.build();
        }
    }

    /** Swappable internal implementation of an ImmutableSet.Builder. */
    private abstract static class SetBuilderImpl<E> {
        // The first `distinct` elements are non-null.
        // Since we can never access null elements, we don't mark this nullable.
        E[] dedupedElements;
        int distinct;

        @SuppressWarnings("unchecked")
        SetBuilderImpl(int expectedCapacity) {
            this.dedupedElements = (E[]) new Object[expectedCapacity];
            this.distinct = 0;
        }

        /** Initializes this SetBuilderImpl with a copy of the deduped elements array from toCopy. */
        SetBuilderImpl(SetBuilderImpl<E> toCopy) {
            this.dedupedElements = Arrays.copyOf(toCopy.dedupedElements, toCopy.dedupedElements.length);
            this.distinct = toCopy.distinct;
        }

        /**
         * Resizes internal data structures if necessary to store the specified number of distinct
         * elements.
         */
        private void ensureCapacity(int minCapacity) {
            if (minCapacity > dedupedElements.length) {
                int newCapacity =
                        ImmutableCollection.Builder.expandedCapacity(dedupedElements.length, minCapacity);
                dedupedElements = Arrays.copyOf(dedupedElements, newCapacity);
            }
        }

        /** Adds e to the insertion-order array of deduplicated elements. Calls ensureCapacity. */
        final void addDedupedElement(E e) {
            ensureCapacity(distinct + 1);
            dedupedElements[distinct++] = e;
        }

        /**
         * Adds e to this SetBuilderImpl, returning the updated result. Only use the returned
         * SetBuilderImpl, since we may switch implementations if e.g. hash flooding is detected.
         */
        abstract SetBuilderImpl<E> add(E e);

        /**
         * Creates a new copy of this SetBuilderImpl. Modifications to that SetBuilderImpl will not
         * affect this SetBuilderImpl or sets constructed from this SetBuilderImpl via build().
         */
        abstract SetBuilderImpl<E> copy();

        /**
         * Call this before build(). Does a final check on the internal data structures, e.g. shrinking
         * unnecessarily large structures or detecting previously unnoticed hash flooding.
         */
        SetBuilderImpl<E> review() {
            return this;
        }

        abstract Set<E> build();
    }

}
