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

import java.util.Random;


/**
 * Zobrist hashing function.
 */
public class ZobristHash implements HashFunction {

    /** Random number generator */
    private Random random = new Random();

    /** Zobrist random numbers */
    protected final long[][] keys;

    /** Length of the integer array */
    protected final int length;

    /** Maximum value an array can contain */
    protected final int count;


    /**
     * Instantiates this object.
     *
     * @param count     Number of distinct values
     * @param length    Fixed length of the array
     */
    public ZobristHash(int count, int length) {
        this.keys = new long[count][length];
        this.length = length;
        this.count = count;
        initialize();
    }


    /**
     * Instantiates this object using a random seed number.
     *
     * @param seed      Pseudorandom number generator seed
     * @param count     Number of distinct values
     * @param length    Fixed length of the array
     */
    public ZobristHash(long seed, int count, int length) {
        this.keys = new long[count][length];
        this.length = length;
        this.count = count;
        random.setSeed(seed);
        initialize();
    }


    /**
     * Precomputes the binomial coefficients table.
     */
    private void initialize() {
        for (int c = 0; c < count; c++) {
            for (int i = 0; i < length; i++) {
                keys[c][i] = random.nextLong();
            }
        }
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
            hash ^= keys[c][i];
        }

        return hash;
    }


    /**
     * Update a hash by adding a value at the given index.
     *
     * @param hash      Hash to update
     * @param index     Index on the state array
     * @param value     Value on the state array
     *
     * @return          Updated hash
     */
    public final long insert(long hash, int index, int value) {
        return update(hash, index, value);
    }


    /**
     * Update a hash by removing a value from the given index.
     *
     * @param hash      Hash to update
     * @param index     Index on the state array
     * @param value     Value on the state array
     *
     * @return          Updated hash
     */
    public final long remove(long hash, int index, int value) {
        return update(hash, index, value);
    }


    /**
     * Update a hash by removing a value from an index and adding.
     * it to another index.
     *
     * @param hash      Hash to update
     * @param from      Index to remove from
     * @param to        Index to insert to
     * @param value     Value on the state array
     *
     * @return          Updated hash
     */
    public final long toggle(long hash, int from, int to, int value) {
        return hash ^ keys[value][from] ^ keys[value][to];
    }


    /**
     * Update a hash by xoring the given value at the given index.
     */
    private long update(long hash, int index, int value) {
        return hash ^ keys[value][index];
    }
}
