package com.joansala.util.bits;

/*
 * Aalina engine.
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

import java.util.function.Consumer;
import com.joansala.util.bits.Bits;


/**
 * A bitset with multiple words.
 */
public class Bitset implements Cloneable {

    /** Bits on the bitset */
    private final long[] words;


    /**
     * Creates a bitset of a fixed size.
     *
     * @param size      Number of 64-bit words
     */
    public Bitset(int size) {
        this.words = new long[size];
    }


    /**
     * Creates a bitset initialized with the given words.
     *
     * @param words     Bitset words array
     */
    public Bitset(long[] words) {
        this.words = words;
    }


    /**
     * Obtain the word were a bit is stored.
     */
    private long word(int index) {
        return words[index >> 6];
    }


    /**
     * Obtain a word with a given bit set.
     */
    private long bit(int index) {
        return 1L << index;
    }


    /**
     * Set the bit at the given index.
     *
     * @param index     Bit index
     */
    public void insert(int index) {
        words[index >> 6] |= bit(index);
    }


    /**
     * Clears the bit at the given index.
     *
     * @param index     Bit index
     */
    public void remove(int index) {
        words[index >> 6] &= ~bit(index);
    }


    /**
     * Toggles the bit at the given index.
     *
     * @param index     Bit index
     */
    public void toggle(int index) {
        words[index >> 6] ^= bit(index);
    }


    /**
     * Check if the bit at the given index is set.
     *
     * @param index     Bit index
     * @return          True if set
     */
    public boolean contains(int index) {
        return (bit(index) & word(index)) != 0L;
    }


    /**
     * Counts the number of bits that are set.
     *
     * @return              Number of bits
     */
    public int count() {
        int count = 0;

        for (int i = 0; i < words.length; i++) {
            count += Long.bitCount(words[i]);
        }

        return count;
    }


    /**
     * Perform an action on each bit that is set.
     *
     * @param action    Action to perperform
     */
    public void forEach(Consumer<Integer> action) {
        for (int i = 0; i < words.length; i++) {
            long bits = words[i];

            while (Bits.empty(bits) == false) {
                final int index = Bits.first(bits);
                action.accept(index + (i << 6));
                bits ^= Bits.bit(index);
            }
        }
    }


    /**
     * Copy the words of this bitset from an array.
     *
     * @param array     Source array
     * @param index     Source start index
     */
     public void copyFrom(long[] array, int index) {
        System.arraycopy(array, index, words, 0, words.length);
    }


    /**
     * Copy the words of this bitset to an array.
     *
     * @param array     Destination array
     * @param index     Destination start index
     */
    public void copyTo(long[] array, int index) {
        System.arraycopy(words, 0, array, index, words.length);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Bitset clone() {
        return new Bitset(words.clone());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        for (int i = words.length - 1; i >= 0; i--) {
            String b = Long.toBinaryString(words[i]);
            String w = String.format("%64s", b);
            result.append(w.replace(" ", "0"));
        }

        return result.toString();
    }
}
