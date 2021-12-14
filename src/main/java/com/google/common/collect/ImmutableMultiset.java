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

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link Multiset} whose contents will never change, with many other important properties
 * detailed at {@link ImmutableCollection}.
 *
 * <p><b>Grouped iteration.</b> In all current implementations, duplicate elements always appear
 * consecutively when iterating. Elements iterate in order by the <i>first</i> appearance of that
 * element when the multiset was created.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/ImmutableCollectionsExplained"> immutable collections</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
@SuppressWarnings("serial") // we're overriding default serialization
public abstract class ImmutableMultiset<E> extends ImmutableCollection<E>
        implements Multiset<E> {

    /**
     * Returns the empty immutable multiset.
     *
     * <p><b>Performance note:</b> the instance returned is a singleton.
     */
    @SuppressWarnings("unchecked") // all supported methods are covariant
    public static <E> ImmutableMultiset<E> of() {
        return (ImmutableMultiset<E>) RegularImmutableMultiset.EMPTY;
    }

    /**
     * Returns an immutable multiset containing a single element.
     *
     * @throws NullPointerException if {@code element} is null
     * @since 6.0 (source-compatible since 2.0)
     */
    public static <E> ImmutableMultiset<E> of(E element) {
        return copyFromElements(element);
    }

    /**
     * Returns an immutable multiset containing the given elements, in order.
     *
     * @throws NullPointerException if any element is null
     * @since 6.0 (source-compatible since 2.0)
     */
    public static <E> ImmutableMultiset<E> of(E e1, E e2) {
        return copyFromElements(e1, e2);
    }

    /**
     * Returns an immutable multiset containing the given elements, in the "grouped iteration order"
     * described in the class documentation.
     *
     * @throws NullPointerException if any element is null
     * @since 6.0 (source-compatible since 2.0)
     */
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3) {
        return copyFromElements(e1, e2, e3);
    }

    /**
     * Returns an immutable multiset containing the given elements, in the "grouped iteration order"
     * described in the class documentation.
     *
     * @throws NullPointerException if any element is null
     * @since 6.0 (source-compatible since 2.0)
     */
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3, E e4) {
        return copyFromElements(e1, e2, e3, e4);
    }

    /**
     * Returns an immutable multiset containing the given elements, in the "grouped iteration order"
     * described in the class documentation.
     *
     * @throws NullPointerException if any element is null
     * @since 6.0 (source-compatible since 2.0)
     */
    public static <E> ImmutableMultiset<E> of(E e1, E e2, E e3, E e4, E e5) {
        return copyFromElements(e1, e2, e3, e4, e5);
    }

    /**
     * Returns an immutable multiset containing the given elements, in the "grouped iteration order"
     * described in the class documentation.
     *
     * @throws NullPointerException if any of {@code elements} is null
     */
    public static <E> ImmutableMultiset<E> copyOf(Iterable<? extends E> elements) {
        if (elements instanceof ImmutableMultiset) {
            @SuppressWarnings("unchecked") // all supported methods are covariant
            ImmutableMultiset<E> result = (ImmutableMultiset<E>) elements;
            if (!result.isPartialView()) {
                return result;
            }
        }

        Multiset<? extends E> multiset =
                (elements instanceof Multiset)
                        ? Multisets.cast(elements)
                        : LinkedHashMultiset.create(elements);

        return copyFromEntries(multiset.entrySet());
    }

    /**
     * Returns an immutable multiset containing the given elements, in the "grouped iteration order"
     * described in the class documentation.
     *
     * @throws NullPointerException if any of {@code elements} is null
     */
    public static <E> ImmutableMultiset<E> copyOf(Iterator<? extends E> elements) {
        Multiset<E> multiset = LinkedHashMultiset.create();
        Iterators.addAll(multiset, elements);
        return copyFromEntries(multiset.entrySet());
    }

    private static <E> ImmutableMultiset<E> copyFromElements(E... elements) {
        Multiset<E> multiset = LinkedHashMultiset.create();
        Collections.addAll(multiset, elements);
        return copyFromEntries(multiset.entrySet());
    }

    static <E> ImmutableMultiset<E> copyFromEntries(
            Collection<? extends Entry<? extends E>> entries) {
        if (entries.isEmpty()) {
            return of();
        } else {
            return RegularImmutableMultiset.create(entries);
        }
    }

    ImmutableMultiset() {
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        final Iterator<Entry<E>> entryIterator = entrySet().iterator();
        return new UnmodifiableIterator<E>() {
            int remaining;
            E element;

            @Override
            public boolean hasNext() {
                return (remaining > 0) || entryIterator.hasNext();
            }

            @Override
            public E next() {
                if (remaining <= 0) {
                    Entry<E> entry = entryIterator.next();
                    element = entry.getElement();
                    remaining = entry.getCount();
                }
                remaining--;
                /*
                 * requireNonNull is safe because `remaining` starts at 0, forcing us to initialize
                 * `element` above. After that, we never clear it.
                 */
                return requireNonNull(element);
            }
        };
    }

    private transient ImmutableList<E> asList;

    @Override
    public ImmutableList<E> asList() {
        ImmutableList<E> result = asList;
        return (result == null) ? asList = super.asList() : result;
    }

    @Override
    public boolean contains(Object object) {
        return count(object) > 0;
    }

    /**
     * Guaranteed to throw an exception and leave the collection unmodified.
     *
     * @throws UnsupportedOperationException always
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final int add(E element, int occurrences) {
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
    public final int remove(Object element, int occurrences) {
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
    public final int setCount(E element, int count) {
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
    public final boolean setCount(E element, int oldCount, int newCount) {
        throw new UnsupportedOperationException();
    }

    @GwtIncompatible // not present in emulated superclass
    @Override
    int copyIntoArray(Object[] dst, int offset) {
        for (Multiset.Entry<E> entry : entrySet()) {
            Arrays.fill(dst, offset, offset + entry.getCount(), entry.getElement());
            offset += entry.getCount();
        }
        return offset;
    }

    @Override
    public boolean equals(Object object) {
        return Multisets.equalsImpl(this, object);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(entrySet());
    }

    @Override
    public String toString() {
        return entrySet().toString();
    }

    /** @since 21.0 (present with return type {@code Set} since 2.0) */
    @Override
    public abstract ImmutableSet<E> elementSet();

    private transient ImmutableSet<Entry<E>> entrySet;

    @Override
    public ImmutableSet<Entry<E>> entrySet() {
        ImmutableSet<Entry<E>> es = entrySet;
        return (es == null) ? (entrySet = createEntrySet()) : es;
    }

    private ImmutableSet<Entry<E>> createEntrySet() {
        return isEmpty() ? ImmutableSet.<Entry<E>>of() : new EntrySet();
    }

    abstract Entry<E> getEntry(int index);

    private final class EntrySet extends IndexedImmutableSet<Entry<E>> {
        @Override
        boolean isPartialView() {
            return ImmutableMultiset.this.isPartialView();
        }

        @Override
        Entry<E> get(int index) {
            return getEntry(index);
        }

        @Override
        public int size() {
            return elementSet().size();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?> entry = (Entry<?>) o;
                if (entry.getCount() <= 0) {
                    return false;
                }
                int count = count(entry.getElement());
                return count == entry.getCount();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return ImmutableMultiset.this.hashCode();
        }
    }
    
    static final class ElementSet<E> extends ImmutableSet.Indexed<E> {
        private final List<Entry<E>> entries;
        // TODO(cpovirk): @Weak?
        private final Multiset<E> delegate;

        ElementSet(List<Entry<E>> entries, Multiset<E> delegate) {
            this.entries = entries;
            this.delegate = delegate;
        }

        @Override
        E get(int index) {
            return entries.get(index).getElement();
        }

        @Override
        public boolean contains(Object object) {
            return delegate.contains(object);
        }

        @Override
        boolean isPartialView() {
            return true;
        }

        @Override
        public int size() {
            return entries.size();
        }
    }
}
