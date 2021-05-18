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
 * Zobrist hashing function.
 */
public class ZobristHash implements HashFunction {

    /** Zobrist random numbers */
    protected final long[][] randoms;

    /** Length of the integer array */
    protected final int length;

    /** Maximum value an array can contain */
    protected final int count;


    /**
     * Instantiates this object.
     *
     * @param count     Maximum value on the arrays
     * @param length    Fixed length of the array
     */
    public ZobristHash(int count, int length) {
        this.randoms = new long[count][length];
        this.length = length;
        this.count = count;
        initialize();
    }


    /**
     * Precomputes the binomial coefficients table.
     */
    private void initialize() {
        for (int c = 0; c < count; c++) {
            for (int i = 0; i < length; i++) {
                final int k = count - c - 1;
                randoms[c][i] = binomial(k + i + 1, k);
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

        for (int i = 0; i < length; i++) {
            final int c = state[i];
            hash ^= randoms[c][i];
        }

        return hash;
    }
}
