/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.primitives;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndexes;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Converter;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * Static utility methods pertaining to {@code int} primitives, that are not already found in either
 * {@link Integer} or {@link Arrays}.
 *
 * <p>See the Guava User Guide article on <a
 * href="https://github.com/google/guava/wiki/PrimitivesExplained">primitive utilities</a>.
 *
 * @author Kevin Bourrillion
 * @since 1.0
 */
@GwtCompatible(emulated = true)
public final class Ints {
    private Ints() {
    }

    /**
     * The number of bytes required to represent a primitive {@code int} value.
     *
     * <p><b>Java 8 users:</b> use {@link Integer#BYTES} instead.
     */
    public static final int BYTES = Integer.SIZE / Byte.SIZE;

    /**
     * The largest power of two that can be represented as an {@code int}.
     *
     * @since 10.0
     */
    public static final int MAX_POWER_OF_TWO = 1 << (Integer.SIZE - 2);

    /**
     * Returns a hash code for {@code value}; equal to the result of invoking {@code ((Integer)
     * value).hashCode()}.
     *
     * <p><b>Java 8 users:</b> use {@link Integer#hashCode(int)} instead.
     *
     * @param value a primitive {@code int} value
     * @return a hash code for the value
     */
    public static int hashCode(int value) {
        return value;
    }

    /**
     * Returns the {@code int} value that is equal to {@code value}, if possible.
     *
     * @param value any value in the range of the {@code int} type
     * @return the {@code int} value that equals {@code value}
     * @throws IllegalArgumentException if {@code value} is greater than {@link Integer#MAX_VALUE} or
     *     less than {@link Integer#MIN_VALUE}
     */
    public static int checkedCast(long value) {
        int result = (int) value;
        checkArgument(result == value, "Out of range: %s", value);
        return result;
    }

    /**
     * Returns the {@code int} nearest in value to {@code value}.
     *
     * @param value any {@code long} value
     * @return the same value cast to {@code int} if it is in the range of the {@code int} type,
     *     {@link Integer#MAX_VALUE} if it is too large, or {@link Integer#MIN_VALUE} if it is too
     *     small
     */
    public static int saturatedCast(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /**
     * Compares the two specified {@code int} values. The sign of the value returned is the same as
     * that of {@code ((Integer) a).compareTo(b)}.
     *
     * <p><b>Note for Java 7 and later:</b> this method should be treated as deprecated; use the
     * equivalent {@link Integer#compare} method instead.
     *
     * @param a the first {@code int} to compare
     * @param b the second {@code int} to compare
     * @return a negative value if {@code a} is less than {@code b}; a positive value if {@code a} is
     *     greater than {@code b}; or zero if they are equal
     */
    public static int compare(int a, int b) {
        return (a < b) ? -1 : ((a > b) ? 1 : 0);
    }

    /**
     * Returns the least value present in {@code array}.
     *
     * @param array a <i>nonempty</i> array of {@code int} values
     * @return the value present in {@code array} that is less than or equal to every other value in
     *     the array
     * @throws IllegalArgumentException if {@code array} is empty
     */
    public static int min(int... array) {
        checkArgument(array.length > 0);
        int min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    /**
     * Returns the greatest value present in {@code array}.
     *
     * @param array a <i>nonempty</i> array of {@code int} values
     * @return the value present in {@code array} that is greater than or equal to every other value
     *     in the array
     * @throws IllegalArgumentException if {@code array} is empty
     */
    public static int max(int... array) {
        checkArgument(array.length > 0);
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    private static final class IntConverter extends Converter<String, Integer>
            implements Serializable {
        static final IntConverter INSTANCE = new IntConverter();

        @Override
        protected Integer doForward(String value) {
            return Integer.decode(value);
        }

        @Override
        protected String doBackward(Integer value) {
            return value.toString();
        }

        @Override
        public String toString() {
            return "Ints.stringConverter()";
        }

        private Object readResolve() {
            return INSTANCE;
        }

        private static final long serialVersionUID = 1;
    }

    /**
     * Returns a serializable converter object that converts between strings and integers using {@link
     * Integer#decode} and {@link Integer#toString()}. The returned converter throws {@link
     * NumberFormatException} if the input string is invalid.
     *
     * <p><b>Warning:</b> please see {@link Integer#decode} to understand exactly how strings are
     * parsed. For example, the string {@code "0123"} is treated as <i>octal</i> and converted to the
     * value {@code 83}.
     *
     * @since 16.0
     */
    @Beta
    public static Converter<String, Integer> stringConverter() {
        return IntConverter.INSTANCE;
    }

    /**
     * Returns an array containing the same values as {@code array}, but guaranteed to be of a
     * specified minimum length. If {@code array} already has a length of at least {@code minLength},
     * it is returned directly. Otherwise, a new array of size {@code minLength + padding} is
     * returned, containing the values of {@code array}, and zeroes in the remaining places.
     *
     * @param array the source array
     * @param minLength the minimum length the returned array must guarantee
     * @param padding an extra amount to "grow" the array by if growth is necessary
     * @throws IllegalArgumentException if {@code minLength} or {@code padding} is negative
     * @return an array containing the values of {@code array}, with guaranteed minimum length {@code
     *     minLength}
     */
    public static int[] ensureCapacity(int[] array, int minLength, int padding) {
        checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
        checkArgument(padding >= 0, "Invalid padding: %s", padding);
        return (array.length < minLength) ? Arrays.copyOf(array, minLength + padding) : array;
    }

    /**
     * Returns a string containing the supplied {@code int} values separated by {@code separator}. For
     * example, {@code join("-", 1, 2, 3)} returns the string {@code "1-2-3"}.
     *
     * @param separator the text that should appear between consecutive values in the resulting string
     *     (but not at the start or end)
     * @param array an array of {@code int} values, possibly empty
     */
    public static String join(String separator, int... array) {
        checkNotNull(separator);
        if (array.length == 0) {
            return "";
        }

        // For pre-sizing a builder, just get the right order of magnitude
        StringBuilder builder = new StringBuilder(array.length * 5);
        builder.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            builder.append(separator).append(array[i]);
        }
        return builder.toString();
    }

    /**
     * Returns a comparator that compares two {@code int} arrays <a
     * href="http://en.wikipedia.org/wiki/Lexicographical_order">lexicographically</a>. That is, it
     * compares, using {@link #compare(int, int)}), the first pair of values that follow any common
     * prefix, or when one array is a prefix of the other, treats the shorter array as the lesser. For
     * example, {@code [] < [1] < [1, 2] < [2]}.
     *
     * <p>The returned comparator is inconsistent with {@link Object#equals(Object)} (since arrays
     * support only identity equality), but it is consistent with {@link Arrays#equals(int[], int[])}.
     *
     * @since 2.0
     */
    public static Comparator<int[]> lexicographicalComparator() {
        return LexicographicalComparator.INSTANCE;
    }

    private enum LexicographicalComparator implements Comparator<int[]> {
        INSTANCE;

        @Override
        public int compare(int[] left, int[] right) {
            int minLength = Math.min(left.length, right.length);
            for (int i = 0; i < minLength; i++) {
                int result = Ints.compare(left[i], right[i]);
                if (result != 0) {
                    return result;
                }
            }
            return left.length - right.length;
        }

        @Override
        public String toString() {
            return "Ints.lexicographicalComparator()";
        }
    }

    /**
     * Sorts the elements of {@code array} in descending order.
     *
     * @since 23.1
     */
    public static void sortDescending(int[] array) {
        checkNotNull(array);
        sortDescending(array, 0, array.length);
    }

    /**
     * Sorts the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
     * exclusive in descending order.
     *
     * @since 23.1
     */
    public static void sortDescending(int[] array, int fromIndex, int toIndex) {
        checkNotNull(array);
        checkPositionIndexes(fromIndex, toIndex, array.length);
        Arrays.sort(array, fromIndex, toIndex);
        reverse(array, fromIndex, toIndex);
    }

    /**
     * Reverses the elements of {@code array}. This is equivalent to {@code
     * Collections.reverse(Ints.asList(array))}, but is likely to be more efficient.
     *
     * @since 23.1
     */
    public static void reverse(int[] array) {
        checkNotNull(array);
        reverse(array, 0, array.length);
    }

    /**
     * Reverses the elements of {@code array} between {@code fromIndex} inclusive and {@code toIndex}
     * exclusive. This is equivalent to {@code
     * Collections.reverse(Ints.asList(array).subList(fromIndex, toIndex))}, but is likely to be more
     * efficient.
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > array.length}, or
     *     {@code toIndex > fromIndex}
     * @since 23.1
     */
    public static void reverse(int[] array, int fromIndex, int toIndex) {
        checkNotNull(array);
        checkPositionIndexes(fromIndex, toIndex, array.length);
        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            int tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }

    /**
     * Parses the specified string as a signed decimal integer value. The ASCII character {@code '-'}
     * (<code>'&#92;u002D'</code>) is recognized as the minus sign.
     *
     * <p>Unlike {@link Integer#parseInt(String)}, this method returns {@code null} instead of
     * throwing an exception if parsing fails. Additionally, this method only accepts ASCII digits,
     * and returns {@code null} if non-ASCII digits are present in the string.
     *
     * <p>Note that strings prefixed with ASCII {@code '+'} are rejected, even under JDK 7, despite
     * the change to {@link Integer#parseInt(String)} for that version.
     *
     * @param string the string representation of an integer value
     * @return the integer value represented by {@code string}, or {@code null} if {@code string} has
     *     a length of zero or cannot be parsed as an integer value
     * @throws NullPointerException if {@code string} is {@code null}
     * @since 11.0
     */
    @Beta
    public static Integer tryParse(String string) {
        return tryParse(string, 10);
    }

    /**
     * Parses the specified string as a signed integer value using the specified radix. The ASCII
     * character {@code '-'} (<code>'&#92;u002D'</code>) is recognized as the minus sign.
     *
     * <p>Unlike {@link Integer#parseInt(String, int)}, this method returns {@code null} instead of
     * throwing an exception if parsing fails. Additionally, this method only accepts ASCII digits,
     * and returns {@code null} if non-ASCII digits are present in the string.
     *
     * <p>Note that strings prefixed with ASCII {@code '+'} are rejected, even under JDK 7, despite
     * the change to {@link Integer#parseInt(String, int)} for that version.
     *
     * @param string the string representation of an integer value
     * @param radix the radix to use when parsing
     * @return the integer value represented by {@code string} using {@code radix}, or {@code null} if
     *     {@code string} has a length of zero or cannot be parsed as an integer value
     * @throws IllegalArgumentException if {@code radix < Character.MIN_RADIX} or {@code radix >
     *     Character.MAX_RADIX}
     * @throws NullPointerException if {@code string} is {@code null}
     * @since 19.0
     */
    @Beta
    public static Integer tryParse(String string, int radix) {
        Long result = Longs.tryParse(string, radix);
        if (result == null || result.longValue() != result.intValue()) {
            return null;
        } else {
            return result.intValue();
        }
    }
}
