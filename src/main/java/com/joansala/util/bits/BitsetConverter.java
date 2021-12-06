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
 * Converts bitboards to piece-square array representation and back.
 *
 * Each position is encoded as an array of bitboards where each index
 * on the position is a game piece identifier. The occupants array
 * is a bidimensional array that contains for each coordinate of a
 * board the identifier of the piece it contains.
 */
public class BitsetConverter {

    /** Piece not found on the bitboard */
    private static final int NOT_FOUND = -1;

    /** Bits to indices map */
    private final int[] bits;


    /**
     * Creates a new converter.
     *
     * @param bits      Maps bits to coordinates
     */
    public BitsetConverter(int[] bits) {
        this.bits = bits;
    }


    /**
     * Fills a bitboards position array with the information found
     * on the occupants array.
     *
     * @param position      Bitboards array
     * @param occupants     Occupants array
     */
    public long[] toPosition(long[] position, int[][] occupants) {
        for (int rank = 0; rank < occupants.length; rank++) {
            int files = occupants[rank].length;

            for (int file = 0; file < files; file++) {
                int piece = occupants[rank][file];

                if (piece >= 0 && piece < position.length) {
                    int index = files * rank + file;
                    position[piece] |= (1L << bits[index]);
                }
            }
        }

        return position;
    }


    /**
     * Fills an occupants array with the information found on the
     * bitboards position array.
     *
     * @param occupants     Occupants array
     * @param position      Bitboards array
     */
    public int[][] toOccupants(int[][] occupants, long[] position) {
        for (int rank = 0; rank < occupants.length; rank++) {
            int files = occupants[rank].length;

            for (int file = 0; file < files; file++) {
                int index = files * rank + file;
                int piece = piece(position, index);
                occupants[rank][file] = piece;
            }
        }

        return occupants;
    }


    /**
     * Identifier of the piece on the given coordinate index.
     *
     * @param position      Position bitboards
     * @param index         Coordinate index
     *
     * @return              Piece identifier or {@code NOT_FOUND}
     */
    public int piece(long[] position, int index) {
        final long bit = (1L << bits[index]);

        for (int piece = 0; piece < position.length; piece++) {
            if ((bit & position[piece]) != 0L) {
                return piece;
            }
        }

        return NOT_FOUND;
    }


    /**
     * Fills a bitboards position array with the information found
     * on the occupants array.
     *
     * @param position      Bitboards array
     * @param occupants     Occupants array
     */
    public Bitset[] toPosition(Bitset[] position, int[][] occupants) {
        for (int rank = 0; rank < occupants.length; rank++) {
            int files = occupants[rank].length;

            for (int file = 0; file < files; file++) {
                int piece = occupants[rank][file];

                if (piece >= 0 && piece < position.length) {
                    int index = files * rank + file;
                    position[piece].insert(bits[index]);
                }
            }
        }

        return position;
    }


    /**
     * Fills an occupants array with the information found on the
     * bitboards position array.
     *
     * @param occupants     Occupants array
     * @param position      Bitboards array
     */
    public int[][] toOccupants(int[][] occupants, Bitset[] position) {
        for (int rank = 0; rank < occupants.length; rank++) {
            int files = occupants[rank].length;

            for (int file = 0; file < files; file++) {
                int index = files * rank + file;
                int piece = piece(position, index);
                occupants[rank][file] = piece;
            }
        }

        return occupants;
    }


    /**
     * Identifier of the piece on the given coordinate index.
     *
     * @param position      Position bitboards
     * @param index         Coordinate index
     *
     * @return              Piece identifier or {@code NOT_FOUND}
     */
    public int piece(Bitset[] position, int index) {
        final int bit = bits[index];

        for (int piece = 0; piece < position.length; piece++) {
            if (position[piece].contains(bit)) {
                return piece;
            }
        }

        return NOT_FOUND;
    }
}
