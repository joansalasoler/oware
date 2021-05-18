package com.joansala.util.hash;

/*
 * Aalina oware engine.
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * Minimal perfect hashing of integer arrays.
 *
 * Uses an optimized binomial hashing algorithm. Requires that all the
 * arrays to hash have the same length and that the sum of the values
 * on the arrays is equal or lower than a predefined number.
 *
 * This class produces hashes that are consecutive, uniquely identify an
 * array and can be converted back to its original array form. Because the
 * hashes are consecutive the number of bits required is minimal.
 */
public class BinomialHash implements HashFunction {

    /** Binomial coefficients */
    private final long[][] binomials;

    /** Length of the integer array */
    private final int length;

    /** Maximum sum of the array */
    private final int count;


    /**
     * Instantiates this object.
     *
     * @param count     Maximum sum of array values
     * @param length    Fixed length of the array
     */
    public BinomialHash(int count, int length) {
        this.binomials = new long[count][length - 1];
        this.length = length;
        this.count = count;
        initialize();
    }


    /**
     * Precomputes the binomial coefficients table.
     */
    private void initialize() {
        for (int c = 0; c < count; c++) {
            for (int i = 0; i < length - 1; i++) {
                final int k = count - c - 1;
                binomials[c][i] = binomial(k + i + 1, k);
            }
        }
    }


    /**
     * Computes the binomial coefficient C(n, k).
     *
     * @param n     Number of objects
     * @param k     Numner of choices
     * @return      Binomial coeficient
     */
    private static long binomial(int n, int k) {
        long value = 1L;

        if (k > n - k) {
            k = n - k;
        }

        for (int i = 0; i < k; i++) {
            value *= (n - i);
            value /= (i + 1);
        }

        return value;
    }


    /**
     * Number of possible arrays for which its sum is equal or
     * less than {@code count}.
     *
     * @param count     Maximum sum of array values
     * @return          Population count
     */
    public long offset(int count) {
        return offset(count, length);
    }


    /**
     * Number of possible arrays of {@code length} length for which
     * its sum is equal or less than {@code count}.
     *
     * @param count     Maximum sum of array values
     * @param length    Length of the array
     * @return          Population count
     */
    public static long offset(int count, int length) {
        return binomial(count + length, length);
    }


    /**
     * Required bits to hash all the arrays of size {@code length}
     * if its sum of values is equal or less than {@code count}.
     *
     * @param count     Maximum sum of array values
     * @param length    Length of the array
     * @return          Required bits
     */
    public static int bits(int count, int length) {
        final long size = offset(count, length);
        final double bits = Math.log(size) / Math.log(2);
        return (int) Math.ceil(bits);
    }


    /**
     * {@inheritDoc}
     */
    public long hash(Object state) {
        return hash((int[]) state);
    }


    /**
     * Compute the hash of an integer array.
     *
     * @param state     An array
     * @return          Hash value
     */
    public long hash(int[] state) {
        long hash = 0x0L;
        int c = state[length - 1];

        for (int i = length - 2; c < count && i >= 0; i--) {
            hash += binomials[c][i];
            c += state[i];
        }

        return hash;
    }


    /**
     * Convert a hash into its array representation.
     *
     * @param hash      Hash value
     * @return          A new array
     */
    public int[] unhash(long hash) {
        int c = 0;
        int v = 0;
        int i = length - 2;
        int[] state = new int[length];

        while (i >= 0 && c < count) {
            final long value = binomials[c][i];

            if (hash >= value) {
                hash -= value;
                state[i + 1] = v;
                v = 0;
                i--;
            } else {
                v++;
                c++;
            }
        }

        state[i + 1] = count - c + v;

        return state;
    }
}
