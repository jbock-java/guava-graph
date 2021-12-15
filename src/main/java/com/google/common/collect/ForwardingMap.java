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
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A map which forwards all its method calls to another map. Subclasses should override one or more
 * methods to modify the behavior of the backing map as desired per the <a
 * href="http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p><b>Warning:</b> The methods of {@code ForwardingMap} forward <i>indiscriminately</i> to the
 * methods of the delegate. For example, overriding {@link #put} alone <i>will not</i> change the
 * behavior of {@link #putAll}, which can lead to unexpected behavior. In this case, you should
 * override {@code putAll} as well, either providing your own implementation, or delegating to the
 * provided {@code standardPutAll} method.
 *
 * <p><b>{@code default} method warning:</b> This class does <i>not</i> forward calls to {@code
 * default} methods. Instead, it inherits their default implementations. When those implementations
 * invoke methods, they invoke methods on the {@code ForwardingMap}.
 *
 * <p>Each of the {@code standard} methods, where appropriate, use {@link Objects#equal} to test
 * equality for both keys and values. This may not be the desired behavior for map implementations
 * that use non-standard notions of key equality, such as a {@code SortedMap} whose comparator is
 * not consistent with {@code equals}.
 *
 * <p>The {@code standard} methods and the collection views they return are not guaranteed to be
 * thread-safe, even when all of the methods that they depend on are thread-safe.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0
 */
public abstract class ForwardingMap<K, V>
        extends ForwardingObject implements Map<K, V> {
    // TODO(lowasser): identify places where thread safety is actually lost

    /** Constructor for use by subclasses. */
    protected ForwardingMap() {
    }

    @Override
    protected abstract Map<K, V> delegate();

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public V remove(Object key) {
        return delegate().remove(key);
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return delegate().get(key);
    }

    @Override
    public V put(K key, V value) {
        return delegate().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        delegate().putAll(map);
    }

    @Override
    public Set<K> keySet() {
        return delegate().keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate().entrySet();
    }

    @Override
    public boolean equals(Object object) {
        return object == this || delegate().equals(object);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    /**
     * A sensible definition of {@link #putAll(Map)} in terms of {@link #put(Object, Object)}. If you
     * override {@link #put(Object, Object)}, you may wish to override {@link #putAll(Map)} to forward
     * to this implementation.
     *
     * @since 7.0
     */
    protected void standardPutAll(Map<? extends K, ? extends V> map) {
        Maps.putAllImpl(this, map);
    }




    /**
     * A sensible definition of {@link #toString} in terms of the {@code iterator} method of {@link
     * #entrySet}. If you override {@link #entrySet}, you may wish to override {@link #toString} to
     * forward to this implementation.
     *
     * @since 7.0
     */
    protected String standardToString() {
        return Maps.toStringImpl(this);
    }
}
