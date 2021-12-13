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

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps.EntryTransformer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkNonnegative;
import static com.google.common.collect.CollectPreconditions.checkRemove;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

/**
 * Provides static methods acting on or generating a {@code Multimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#multimaps"> {@code
 * Multimaps}</a>.
 *
 * @author Jared Levy
 * @author Robert Konigsberg
 * @author Mike Bostock
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(emulated = true)
public final class Multimaps {
    private Multimaps() {
    }




    static class Keys<K, V>
            extends AbstractMultiset<K> {
        final Multimap<K, V> multimap;

        Keys(Multimap<K, V> multimap) {
            this.multimap = multimap;
        }

        @Override
        Iterator<Multiset.Entry<K>> entryIterator() {
            return new TransformedIterator<Map.Entry<K, Collection<V>>, Multiset.Entry<K>>(
                    multimap.asMap().entrySet().iterator()) {
                @Override
                Multiset.Entry<K> transform(final Map.Entry<K, Collection<V>> backingEntry) {
                    return new Multisets.AbstractEntry<K>() {
                        @Override
                                        public K getElement() {
                            return backingEntry.getKey();
                        }

                        @Override
                        public int getCount() {
                            return backingEntry.getValue().size();
                        }
                    };
                }
            };
        }

        @Override
        public Spliterator<K> spliterator() {
            return CollectSpliterators.map(multimap.entries().spliterator(), Map.Entry::getKey);
        }

        @Override
        public void forEach(Consumer<? super K> consumer) {
            checkNotNull(consumer);
            multimap.entries().forEach(entry -> consumer.accept(entry.getKey()));
        }

        @Override
        int distinctElements() {
            return multimap.asMap().size();
        }

        @Override
        public int size() {
            return multimap.size();
        }

        @Override
        public boolean contains(Object element) {
            return multimap.containsKey(element);
        }

        @Override
        public Iterator<K> iterator() {
            return Maps.keyIterator(multimap.entries().iterator());
        }

        @Override
        public int count(Object element) {
            Collection<V> values = Maps.safeGet(multimap.asMap(), element);
            return (values == null) ? 0 : values.size();
        }

        @Override
        public int remove(Object element, int occurrences) {
            checkNonnegative(occurrences, "occurrences");
            if (occurrences == 0) {
                return count(element);
            }

            Collection<V> values = Maps.safeGet(multimap.asMap(), element);

            if (values == null) {
                return 0;
            }

            int oldCount = values.size();
            if (occurrences >= oldCount) {
                values.clear();
            } else {
                Iterator<V> iterator = values.iterator();
                for (int i = 0; i < occurrences; i++) {
                    iterator.next();
                    iterator.remove();
                }
            }
            return oldCount;
        }

        @Override
        public void clear() {
            multimap.clear();
        }

        @Override
        public Set<K> elementSet() {
            return multimap.keySet();
        }

        @Override
        Iterator<K> elementIterator() {
            throw new AssertionError("should never be called");
        }
    }

    /** A skeleton implementation of {@link Multimap#entries()}. */
    abstract static class Entries<K, V>
            extends AbstractCollection<Map.Entry<K, V>> {
        abstract Multimap<K, V> multimap();

        @Override
        public int size() {
            return multimap().size();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                return multimap().containsEntry(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                return multimap().remove(entry.getKey(), entry.getValue());
            }
            return false;
        }

        @Override
        public void clear() {
            multimap().clear();
        }
    }

    /** A skeleton implementation of {@link Multimap#asMap()}. */
    static final class AsMap<K, V>
            extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
        private final Multimap<K, V> multimap;

        AsMap(Multimap<K, V> multimap) {
            this.multimap = checkNotNull(multimap);
        }

        @Override
        public int size() {
            return multimap.keySet().size();
        }

        @Override
        protected Set<Entry<K, Collection<V>>> createEntrySet() {
            return new EntrySet();
        }

        void removeValuesForKey(Object key) {
            multimap.keySet().remove(key);
        }

        class EntrySet extends Maps.EntrySet<K, Collection<V>> {
            @Override
            Map<K, Collection<V>> map() {
                return AsMap.this;
            }

            @Override
            public Iterator<Entry<K, Collection<V>>> iterator() {
                return Maps.asMapEntryIterator(
                        multimap.keySet(),
                        new Function<K, Collection<V>>() {
                            @Override
                            public Collection<V> apply(K key) {
                                return multimap.get(key);
                            }
                        });
            }

            @Override
            public boolean remove(Object o) {
                if (!contains(o)) {
                    return false;
                }
                // requireNonNull is safe because of the contains check.
                Map.Entry<?, ?> entry = requireNonNull((Map.Entry<?, ?>) o);
                removeValuesForKey(entry.getKey());
                return true;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collection<V> get(Object key) {
            return containsKey(key) ? multimap.get((K) key) : null;
        }

        @Override
        public Collection<V> remove(Object key) {
            return containsKey(key) ? multimap.removeAll(key) : null;
        }

        @Override
        public Set<K> keySet() {
            return multimap.keySet();
        }

        @Override
        public boolean isEmpty() {
            return multimap.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return multimap.containsKey(key);
        }

        @Override
        public void clear() {
            multimap.clear();
        }
    }


    static boolean equalsImpl(Multimap<?, ?> multimap, Object object) {
        if (object == multimap) {
            return true;
        }
        if (object instanceof Multimap) {
            Multimap<?, ?> that = (Multimap<?, ?>) object;
            return multimap.asMap().equals(that.asMap());
        }
        return false;
    }

    // TODO(jlevy): Create methods that filter a SortedSetMultimap.
}
