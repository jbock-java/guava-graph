/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.math.LongMath;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;
import static java.util.Objects.requireNonNull;

/**
 * Static utility methods related to {@code Stream} instances.
 *
 * @since 21.0
 */
public final class Streams {
    /**
     * Returns a sequential {@link Stream} of the contents of {@code iterable}, delegating to {@link
     * Collection#stream} if possible.
     */
    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return (iterable instanceof Collection)
                ? ((Collection<T>) iterable).stream()
                : StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Returns {@link Collection#stream}.
     *
     * @deprecated There is no reason to use this; just invoke {@code collection.stream()} directly.
     */
    @Beta
    @Deprecated
    public static <T> Stream<T> stream(Collection<T> collection) {
        return collection.stream();
    }

    /**
     * Returns a sequential {@link Stream} of the remaining contents of {@code iterator}. Do not use
     * {@code iterator} directly after passing it to this method.
     */
    @Beta
    public static <T> Stream<T> stream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    /**
     * If a value is present in {@code optional}, returns a stream containing only that element,
     * otherwise returns an empty stream.
     */
    @Beta
    public static <T> Stream<T> stream(com.google.common.base.Optional<T> optional) {
        return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
    }

    /**
     * If a value is present in {@code optional}, returns a stream containing only that element,
     * otherwise returns an empty stream.
     *
     * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
     */
    @Beta
    public static <T> Stream<T> stream(java.util.Optional<T> optional) {
        return optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
    }

    /**
     * If a value is present in {@code optional}, returns a stream containing only that element,
     * otherwise returns an empty stream.
     *
     * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
     */
    @Beta
    public static IntStream stream(OptionalInt optional) {
        return optional.isPresent() ? IntStream.of(optional.getAsInt()) : IntStream.empty();
    }

    /**
     * If a value is present in {@code optional}, returns a stream containing only that element,
     * otherwise returns an empty stream.
     *
     * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
     */
    @Beta
    public static LongStream stream(OptionalLong optional) {
        return optional.isPresent() ? LongStream.of(optional.getAsLong()) : LongStream.empty();
    }

    /**
     * If a value is present in {@code optional}, returns a stream containing only that element,
     * otherwise returns an empty stream.
     *
     * <p><b>Java 9 users:</b> use {@code optional.stream()} instead.
     */
    @Beta
    public static DoubleStream stream(OptionalDouble optional) {
        return optional.isPresent() ? DoubleStream.of(optional.getAsDouble()) : DoubleStream.empty();
    }

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of
     * {@code stream} and their indices in the stream. For example,
     *
     * <pre>{@code
     * mapWithIndex(
     *     Stream.of("a", "b", "c"),
     *     (str, index) -> str + ":" + index)
     * }</pre>
     *
     * <p>would return {@code Stream.of("a:0", "b:1", "c:2")}.
     *
     * <p>The resulting stream is <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * if and only if {@code stream} was efficiently splittable and its underlying spliterator
     * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
     * comes from a data structure supporting efficient indexed random access, typically an array or
     * list.
     *
     * <p>The order of the resulting stream is defined if and only if the order of the original stream
     * was defined.
     */
    @Beta
    public static <T, R> Stream<R> mapWithIndex(
            Stream<T> stream, FunctionWithIndex<? super T, ? extends R> function) {
        checkNotNull(stream);
        checkNotNull(function);
        boolean isParallel = stream.isParallel();
        Spliterator<T> fromSpliterator = stream.spliterator();

        if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            Iterator<T> fromIterator = Spliterators.iterator(fromSpliterator);
            return StreamSupport.stream(
                            new AbstractSpliterator<R>(
                                    fromSpliterator.estimateSize(),
                                    fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                                long index = 0;

                                @Override
                                public boolean tryAdvance(Consumer<? super R> action) {
                                    if (fromIterator.hasNext()) {
                                        action.accept(function.apply(fromIterator.next(), index++));
                                        return true;
                                    }
                                    return false;
                                }
                            },
                            isParallel)
                    .onClose(stream::close);
        }
        class Splitr extends MapWithIndexSpliterator<Spliterator<T>, R, Splitr> implements Consumer<T> {
            T holder;

            Splitr(Spliterator<T> splitr, long index) {
                super(splitr, index);
            }

            @Override
            public void accept(T t) {
                this.holder = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super R> action) {
                if (fromSpliterator.tryAdvance(this)) {
                    try {
                        // The cast is safe because tryAdvance puts a T into `holder`.
                        action.accept(function.apply(uncheckedCastNullableTToT(holder), index++));
                        return true;
                    } finally {
                        holder = null;
                    }
                }
                return false;
            }

            @Override
            Splitr createSplit(Spliterator<T> from, long i) {
                return new Splitr(from, i);
            }
        }
        return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
    }

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of
     * {@code stream} and their indexes in the stream. For example,
     *
     * <pre>{@code
     * mapWithIndex(
     *     IntStream.of(0, 1, 2),
     *     (i, index) -> i + ":" + index)
     * }</pre>
     *
     * <p>...would return {@code Stream.of("0:0", "1:1", "2:2")}.
     *
     * <p>The resulting stream is <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * if and only if {@code stream} was efficiently splittable and its underlying spliterator
     * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
     * comes from a data structure supporting efficient indexed random access, typically an array or
     * list.
     *
     * <p>The order of the resulting stream is defined if and only if the order of the original stream
     * was defined.
     */
    @Beta
    public static <R> Stream<R> mapWithIndex(
            IntStream stream, IntFunctionWithIndex<R> function) {
        checkNotNull(stream);
        checkNotNull(function);
        boolean isParallel = stream.isParallel();
        Spliterator.OfInt fromSpliterator = stream.spliterator();

        if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            PrimitiveIterator.OfInt fromIterator = Spliterators.iterator(fromSpliterator);
            return StreamSupport.stream(
                            new AbstractSpliterator<R>(
                                    fromSpliterator.estimateSize(),
                                    fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                                long index = 0;

                                @Override
                                public boolean tryAdvance(Consumer<? super R> action) {
                                    if (fromIterator.hasNext()) {
                                        action.accept(function.apply(fromIterator.nextInt(), index++));
                                        return true;
                                    }
                                    return false;
                                }
                            },
                            isParallel)
                    .onClose(stream::close);
        }
        class Splitr extends MapWithIndexSpliterator<Spliterator.OfInt, R, Splitr>
                implements IntConsumer, Spliterator<R> {
            int holder;

            Splitr(Spliterator.OfInt splitr, long index) {
                super(splitr, index);
            }

            @Override
            public void accept(int t) {
                this.holder = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super R> action) {
                if (fromSpliterator.tryAdvance(this)) {
                    action.accept(function.apply(holder, index++));
                    return true;
                }
                return false;
            }

            @Override
            Splitr createSplit(Spliterator.OfInt from, long i) {
                return new Splitr(from, i);
            }
        }
        return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
    }

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of
     * {@code stream} and their indexes in the stream. For example,
     *
     * <pre>{@code
     * mapWithIndex(
     *     LongStream.of(0, 1, 2),
     *     (i, index) -> i + ":" + index)
     * }</pre>
     *
     * <p>...would return {@code Stream.of("0:0", "1:1", "2:2")}.
     *
     * <p>The resulting stream is <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * if and only if {@code stream} was efficiently splittable and its underlying spliterator
     * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
     * comes from a data structure supporting efficient indexed random access, typically an array or
     * list.
     *
     * <p>The order of the resulting stream is defined if and only if the order of the original stream
     * was defined.
     */
    @Beta
    public static <R> Stream<R> mapWithIndex(
            LongStream stream, LongFunctionWithIndex<R> function) {
        checkNotNull(stream);
        checkNotNull(function);
        boolean isParallel = stream.isParallel();
        Spliterator.OfLong fromSpliterator = stream.spliterator();

        if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            PrimitiveIterator.OfLong fromIterator = Spliterators.iterator(fromSpliterator);
            return StreamSupport.stream(
                            new AbstractSpliterator<R>(
                                    fromSpliterator.estimateSize(),
                                    fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                                long index = 0;

                                @Override
                                public boolean tryAdvance(Consumer<? super R> action) {
                                    if (fromIterator.hasNext()) {
                                        action.accept(function.apply(fromIterator.nextLong(), index++));
                                        return true;
                                    }
                                    return false;
                                }
                            },
                            isParallel)
                    .onClose(stream::close);
        }
        class Splitr extends MapWithIndexSpliterator<Spliterator.OfLong, R, Splitr>
                implements LongConsumer, Spliterator<R> {
            long holder;

            Splitr(Spliterator.OfLong splitr, long index) {
                super(splitr, index);
            }

            @Override
            public void accept(long t) {
                this.holder = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super R> action) {
                if (fromSpliterator.tryAdvance(this)) {
                    action.accept(function.apply(holder, index++));
                    return true;
                }
                return false;
            }

            @Override
            Splitr createSplit(Spliterator.OfLong from, long i) {
                return new Splitr(from, i);
            }
        }
        return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
    }

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of
     * {@code stream} and their indexes in the stream. For example,
     *
     * <pre>{@code
     * mapWithIndex(
     *     DoubleStream.of(0, 1, 2),
     *     (x, index) -> x + ":" + index)
     * }</pre>
     *
     * <p>...would return {@code Stream.of("0.0:0", "1.0:1", "2.0:2")}.
     *
     * <p>The resulting stream is <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * if and only if {@code stream} was efficiently splittable and its underlying spliterator
     * reported {@link Spliterator#SUBSIZED}. This is generally the case if the underlying stream
     * comes from a data structure supporting efficient indexed random access, typically an array or
     * list.
     *
     * <p>The order of the resulting stream is defined if and only if the order of the original stream
     * was defined.
     */
    @Beta
    public static <R> Stream<R> mapWithIndex(
            DoubleStream stream, DoubleFunctionWithIndex<R> function) {
        checkNotNull(stream);
        checkNotNull(function);
        boolean isParallel = stream.isParallel();
        Spliterator.OfDouble fromSpliterator = stream.spliterator();

        if (!fromSpliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
            PrimitiveIterator.OfDouble fromIterator = Spliterators.iterator(fromSpliterator);
            return StreamSupport.stream(
                            new AbstractSpliterator<R>(
                                    fromSpliterator.estimateSize(),
                                    fromSpliterator.characteristics() & (Spliterator.ORDERED | Spliterator.SIZED)) {
                                long index = 0;

                                @Override
                                public boolean tryAdvance(Consumer<? super R> action) {
                                    if (fromIterator.hasNext()) {
                                        action.accept(function.apply(fromIterator.nextDouble(), index++));
                                        return true;
                                    }
                                    return false;
                                }
                            },
                            isParallel)
                    .onClose(stream::close);
        }
        class Splitr extends MapWithIndexSpliterator<Spliterator.OfDouble, R, Splitr>
                implements DoubleConsumer, Spliterator<R> {
            double holder;

            Splitr(Spliterator.OfDouble splitr, long index) {
                super(splitr, index);
            }

            @Override
            public void accept(double t) {
                this.holder = t;
            }

            @Override
            public boolean tryAdvance(Consumer<? super R> action) {
                if (fromSpliterator.tryAdvance(this)) {
                    action.accept(function.apply(holder, index++));
                    return true;
                }
                return false;
            }

            @Override
            Splitr createSplit(Spliterator.OfDouble from, long i) {
                return new Splitr(from, i);
            }
        }
        return StreamSupport.stream(new Splitr(fromSpliterator, 0), isParallel).onClose(stream::close);
    }

    /**
     * An analogue of {@link java.util.function.Function} also accepting an index.
     *
     * <p>This interface is only intended for use by callers of {@link #mapWithIndex(Stream,
     * FunctionWithIndex)}.
     *
     * @since 21.0
     */
    @Beta
    public interface FunctionWithIndex<T, R> {
        /** Applies this function to the given argument and its index within a stream. */
        R apply(T from, long index);
    }

    private abstract static class MapWithIndexSpliterator<
            F extends Spliterator<?>,
            R,
            S extends MapWithIndexSpliterator<F, R, S>>
            implements Spliterator<R> {
        final F fromSpliterator;
        long index;

        MapWithIndexSpliterator(F fromSpliterator, long index) {
            this.fromSpliterator = fromSpliterator;
            this.index = index;
        }

        abstract S createSplit(F from, long i);

        @Override
        public S trySplit() {
            Spliterator<?> splitOrNull = fromSpliterator.trySplit();
            if (splitOrNull == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            F split = (F) splitOrNull;
            S result = createSplit(split, index);
            this.index += split.getExactSizeIfKnown();
            return result;
        }

        @Override
        public long estimateSize() {
            return fromSpliterator.estimateSize();
        }

        @Override
        public int characteristics() {
            return fromSpliterator.characteristics()
                    & (Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
        }
    }

    /**
     * An analogue of {@link java.util.function.IntFunction} also accepting an index.
     *
     * <p>This interface is only intended for use by callers of {@link #mapWithIndex(IntStream,
     * IntFunctionWithIndex)}.
     *
     * @since 21.0
     */
    @Beta
    public interface IntFunctionWithIndex<R> {
        /** Applies this function to the given argument and its index within a stream. */
        R apply(int from, long index);
    }

    /**
     * An analogue of {@link java.util.function.LongFunction} also accepting an index.
     *
     * <p>This interface is only intended for use by callers of {@link #mapWithIndex(LongStream,
     * LongFunctionWithIndex)}.
     *
     * @since 21.0
     */
    @Beta
    public interface LongFunctionWithIndex<R> {
        /** Applies this function to the given argument and its index within a stream. */
        R apply(long from, long index);
    }

    /**
     * An analogue of {@link java.util.function.DoubleFunction} also accepting an index.
     *
     * <p>This interface is only intended for use by callers of {@link #mapWithIndex(DoubleStream,
     * DoubleFunctionWithIndex)}.
     *
     * @since 21.0
     */
    @Beta
    public interface DoubleFunctionWithIndex<R> {
        /** Applies this function to the given argument and its index within a stream. */
        R apply(double from, long index);
    }

    /**
     * Returns the last element of the specified stream, or {@link java.util.Optional#empty} if the
     * stream is empty.
     *
     * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
     * method's runtime will be between O(log n) and O(n), performing better on <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * streams.
     *
     * <p>If the stream has nondeterministic order, this has equivalent semantics to {@link
     * Stream#findAny} (which you might as well use).
     *
     * @see Stream#findFirst()
     * @throws NullPointerException if the last element of the stream is null
     */
    /*
     * By declaring <T> instead of <T>, we declare this method as requiring a
     * stream whose elements are non-null. However, the method goes out of its way to still handle
     * nulls in the stream. This means that the method can safely be used with a stream that contains
     * nulls as long as the *last* element is *not* null.
     *
     * (To "go out of its way," the method tracks a `set` bit so that it can distinguish "the final
     * split has a last element of null, so throw NPE" from "the final split was empty, so look for an
     * element in the prior one.")
     */
    @Beta
    public static <T> java.util.Optional<T> findLast(Stream<T> stream) {
        class OptionalState {
            boolean set = false;
            T value = null;

            void set(T value) {
                this.set = true;
                this.value = value;
            }

            T get() {
                /*
                 * requireNonNull is safe because we call get() only if we've previously called set().
                 *
                 * (For further discussion of nullness, see the comment above the method.)
                 */
                return requireNonNull(value);
            }
        }
        OptionalState state = new OptionalState();

        Deque<Spliterator<T>> splits = new ArrayDeque<>();
        splits.addLast(stream.spliterator());

        while (!splits.isEmpty()) {
            Spliterator<T> spliterator = splits.removeLast();

            if (spliterator.getExactSizeIfKnown() == 0) {
                continue; // drop this split
            }

            // Many spliterators will have trySplits that are SUBSIZED even if they are not themselves
            // SUBSIZED.
            if (spliterator.hasCharacteristics(Spliterator.SUBSIZED)) {
                // we can drill down to exactly the smallest nonempty spliterator
                while (true) {
                    Spliterator<T> prefix = spliterator.trySplit();
                    if (prefix == null || prefix.getExactSizeIfKnown() == 0) {
                        break;
                    } else if (spliterator.getExactSizeIfKnown() == 0) {
                        spliterator = prefix;
                        break;
                    }
                }

                // spliterator is known to be nonempty now
                spliterator.forEachRemaining(state::set);
                return java.util.Optional.of(state.get());
            }

            Spliterator<T> prefix = spliterator.trySplit();
            if (prefix == null || prefix.getExactSizeIfKnown() == 0) {
                // we can't split this any further
                spliterator.forEachRemaining(state::set);
                if (state.set) {
                    return java.util.Optional.of(state.get());
                }
                // fall back to the last split
                continue;
            }
            splits.addLast(prefix);
            splits.addLast(spliterator);
        }
        return java.util.Optional.empty();
    }

    /**
     * Returns the last element of the specified stream, or {@link OptionalInt#empty} if the stream is
     * empty.
     *
     * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
     * method's runtime will be between O(log n) and O(n), performing better on <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * streams.
     *
     * @see IntStream#findFirst()
     * @throws NullPointerException if the last element of the stream is null
     */
    @Beta
    public static OptionalInt findLast(IntStream stream) {
        // findLast(Stream) does some allocation, so we might as well box some more
        java.util.Optional<Integer> boxedLast = findLast(stream.boxed());
        return boxedLast.map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    /**
     * Returns the last element of the specified stream, or {@link OptionalLong#empty} if the stream
     * is empty.
     *
     * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
     * method's runtime will be between O(log n) and O(n), performing better on <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * streams.
     *
     * @see LongStream#findFirst()
     * @throws NullPointerException if the last element of the stream is null
     */
    @Beta
    public static OptionalLong findLast(LongStream stream) {
        // findLast(Stream) does some allocation, so we might as well box some more
        java.util.Optional<Long> boxedLast = findLast(stream.boxed());
        return boxedLast.map(OptionalLong::of).orElseGet(OptionalLong::empty);
    }

    /**
     * Returns the last element of the specified stream, or {@link OptionalDouble#empty} if the stream
     * is empty.
     *
     * <p>Equivalent to {@code stream.reduce((a, b) -> b)}, but may perform significantly better. This
     * method's runtime will be between O(log n) and O(n), performing better on <a
     * href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">efficiently splittable</a>
     * streams.
     *
     * @see DoubleStream#findFirst()
     * @throws NullPointerException if the last element of the stream is null
     */
    @Beta
    public static OptionalDouble findLast(DoubleStream stream) {
        // findLast(Stream) does some allocation, so we might as well box some more
        java.util.Optional<Double> boxedLast = findLast(stream.boxed());
        return boxedLast.map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }

    private Streams() {
    }
}
