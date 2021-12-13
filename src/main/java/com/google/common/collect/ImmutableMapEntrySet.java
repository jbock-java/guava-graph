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

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * {@code entrySet()} implementation for {@link ImmutableMap}.
 *
 * @author Jesse Wilson
 * @author Kevin Bourrillion
 */
@GwtCompatible(emulated = true)
abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet.CachingAsList<Entry<K, V>> {
    static final class RegularEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
        private final transient ImmutableMap<K, V> map;
        private final transient ImmutableList<Entry<K, V>> entries;

        RegularEntrySet(ImmutableMap<K, V> map, Entry<K, V>[] entries) {
            this(map, ImmutableList.<Entry<K, V>>asImmutableList(entries));
        }

        RegularEntrySet(ImmutableMap<K, V> map, ImmutableList<Entry<K, V>> entries) {
            this.map = map;
            this.entries = entries;
        }

        @Override
        ImmutableMap<K, V> map() {
            return map;
        }

        @Override
        @GwtIncompatible("not used in GWT")
        int copyIntoArray(Object[] dst, int offset) {
            return entries.copyIntoArray(dst, offset);
        }

        @Override
        public UnmodifiableIterator<Entry<K, V>> iterator() {
            return entries.iterator();
        }

        @Override
        public Spliterator<Entry<K, V>> spliterator() {
            return entries.spliterator();
        }

        @Override
        public void forEach(Consumer<? super Entry<K, V>> action) {
            entries.forEach(action);
        }

        @Override
        ImmutableList<Entry<K, V>> createAsList() {
            return new RegularImmutableAsList<>(this, entries);
        }
    }

    ImmutableMapEntrySet() {
    }

    abstract ImmutableMap<K, V> map();

    @Override
    public int size() {
        return map().size();
    }

    @Override
    public boolean contains(Object object) {
        if (object instanceof Entry) {
            Entry<?, ?> entry = (Entry<?, ?>) object;
            V value = map().get(entry.getKey());
            return value != null && value.equals(entry.getValue());
        }
        return false;
    }

    @Override
    boolean isPartialView() {
        return map().isPartialView();
    }

    @Override
            // not used in GWT
    boolean isHashCodeFast() {
        return map().isHashCodeFast();
    }

    @Override
    public int hashCode() {
        return map().hashCode();
    }
}
