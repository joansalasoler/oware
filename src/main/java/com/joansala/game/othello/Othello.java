package com.joansala.game.othello;

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
 * Definitions for Othello.
 *
 * +---+---+---+---+---+---+---+---+
 * |56 |57 |58 |59 |60 |61 |62 |63 |
 * +---+---+---+---+---+---+---+---+
 * |48 |49 |50 |51 |52 |53 |54 |55 |
 * +---+---+---+---+---+---+---+---+
 * |40 |41 |42 |43 |44 |45 |46 |47 |
 * +---+---+---+---+---+---+---+---+
 * |32 |33 |34 |35 |36 |37 |38 |39 |
 * +---+---+---+---+---+---+---+---+
 * |24 |25 |26 |27 |28 |29 |30 |31 |
 * +---+---+---+---+---+---+---+---+
 * |16 |17 |18 |19 |20 |21 |22 |23 |
 * +---+---+---+---+---+---+---+---+
 * | 8 | 9 |10 |11 |12 |13 |14 |15 |
 * +---+---+---+---+---+---+---+---+
 * | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
 * +---+---+---+---+---+---+---+---+
 */
final class Othello {

    // -------------------------------------------------------------------
    // Game logic constants
    // -------------------------------------------------------------------

    /** Number of checkers on the board */
    static final int BOARD_SIZE = 64;

    /** Number of rows on the board */
    static final int BOARD_RANKS = 8;

    /** Number of columns on the board */
    static final int BOARD_FILES = 8;

    /** Number of distinc stones */
    static final int PIECE_COUNT = 2;

    // -------------------------------------------------------------------
    // Board representation
    // -------------------------------------------------------------------

    /** South player name */
    static final String SOUTH_NAME = "Black";

    /** North player name */
    static final String NORTH_NAME = "White";

    /** South player symbol */
    static final char SOUTH_SYMBOL = 'b';

    /** North player symbol */
    static final char NORTH_SYMBOL = 'w';

    // -------------------------------------------------------------------
    // Piece indices
    // -------------------------------------------------------------------

    static final int SOUTH_STONE =  0;
    static final int NORTH_STONE =  1;

    // -------------------------------------------------------------------
    // Bitboard masks
    // -------------------------------------------------------------------

    /** Direction shifts */
    static final int[] DIRECTION_SHIFT = {
        1, 7, 8, 9, 65, 71, 72, 73
    };

    /** Direction shift masks */
    static final long[] DIRECTION_MASK = {
        0xFEFEFEFEFEFEFEFEL, // Left
        0x7F7F7F7F7F7F7F00L, // Top-Right
        0XFFFFFFFFFFFFFF00L, // Top
        0xFEFEFEFEFEFEFE00L, // Top-Left
        0x7F7F7F7F7F7F7F7FL, // Right
        0x00FEFEFEFEFEFEFEL, // Bottom-Left
        0X00FFFFFFFFFFFFFFL, // Bottom
        0x007F7F7F7F7F7F7FL, // Bottom-Right
    };

    // -------------------------------------------------------------------
    // Zobrist hashing
    // -------------------------------------------------------------------

    /** Zobrist hashing random seed */
    static final long RANDOM_SEED =
        0x6622E46E1DB096FAL;

    /** Zobrist keys for the player to move */
    static final long[] HASH_SIGN = {
        0x506AACF489889342L, // South sign
        0xD2B7ADEEDED1F73FL  // North sign
    };

    // -------------------------------------------------------------------
    // Board definitions
    // -------------------------------------------------------------------

    /** Indexed game piece symbols */
    static final char[] PIECES = {
        'X', 'O'
    };

    /** Indexed board cell names */
    static final String[] COORDINATES = {
        "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
        "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
        "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
        "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
        "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
        "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
        "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
        "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1",
        "-"
    };

    /** Bit indices of the checkers */
    static final int[] BITS = {
        0,  1,  2,  3,  4,  5,  6,  7,
        8,  9, 10, 11, 12, 13, 14, 15,
       16, 17, 18, 19, 20, 21, 22, 23,
       24, 25, 26, 27, 28, 29, 30, 31,
       32, 33, 34, 35, 36, 37, 38, 39,
       40, 41, 42, 43, 44, 45, 46, 47,
       48, 49, 50, 51, 52, 53, 54, 55,
       56, 57, 58, 59, 60, 61, 62, 63
    };

    /** Start position bitboards */
    static final long[] START_POSITION = {
        0x0000001008000000L, // South pieces
        0x0000000810000000L, // North pieces
    };
}
