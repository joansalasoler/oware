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


/**
 * Bitwise operations.
 */
public final class Bits {

    /**
     * Check if none of the bits of a bitboard are set.
     *
     * @param bitboard      Bitboard to check
     * @return              True if no bit is set
     */
    public static final boolean empty(long bitboard) {
        return bitboard == 0L;
    }


    /**
     * Index of the rightmost bit that is set on a bitboard.
     *
     * @param bitboard      Bitboard
     * @return              Bit index
     */
    public static final int first(long bitboard) {
        return Long.numberOfTrailingZeros(bitboard);
    }


    /**
     * Index of the leftmost bit that is set on a bitboard.
     *
     * @param bitboard      Bitboard
     * @return              Bit index
     */
    public static final int last(long bitboard) {
        return Long.numberOfLeadingZeros(bitboard) ^ 63;
    }


    /**
     * Index of the next rightmost bit that is set on a bitboard.
     *
     * @param bitboard      Bitboard
     * @param index         Current bit index
     * @return              Bit index
     */
    public static final int next(long bitboard, int index) {
        return Long.numberOfTrailingZeros(bitboard & (-2L << index));
    }


    /**
     * Check if some bits are set on a bitboard.
     *
     * @param bitboard      Bitboard to check
     * @param bits          Bits to check
     * @return              If one or more bits are set
     */
    public static final boolean contains(long bitboard, long bits) {
        return (bitboard & bits) != 0L;
    }


    /**
     * Counts the number of stones on a bitboard.
     *
     * @param bitboard      Bitboard to count
     * @return              Number of stones
     */
    public static final int count(long bitboard) {
        return Long.bitCount(bitboard);
    }


    /**
     * Returns a bitboard with one bit set at the given index.
     *
     * @param index         Index of the bit to set
     * @return              Bitboard
     */
    public static final long bit(int index) {
        return 1L << index;
    }


    /**
     * Shifts a bitboard the given number of bits.
     *
     * This method shifts to the left or right according to the following
     * logic: if {@code n} is lower than 64 it shifts the bitboard {@code n}
     * bits to the left; otherwise it performs an unsigned shift {@code n % 64}
     * bits to the right.
     *
     * @param bitboard      Bitboard to shift
     * @param n             Number of bits to shift
     * @return              Shifted bitboard
     */
    public static final long shift(long bitboard, int n) {
        return (n < 64 ? bitboard << n : bitboard >>> n);
    }
}
