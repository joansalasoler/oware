package com.joansala.game.draughts;

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

import com.joansala.engine.Game;


/**
 * Definitions for Draughts.
 *
 * +---+---+---+---+---+---+---+---+---+---+
 * |   |50 |   |51 |   |52 |   |53 |   |54 |
 * +---+---+---+---+---+---+---+---+---+---+
 * |44 |   |45 |   |46 |   |47 |   |48 |   |
 * +---+---+---+---+---+---+---+---+---+---+
 * |   |39 |   |40 |   |41 |   |42 |   |43 |
 * +---+---+---+---+---+---+---+---+---+---+
 * |33 |   |34 |   |35 |   |36 |   |37 |   |
 * +---+---+---+---+---+---+---+---+---+---+
 * |   |28 |   |29 |   |30 |   |31 |   |32 |
 * +---+---+---+---+---+---+---+---+---+---+
 * |22 |   |23 |   |24 |   |25 |   |26 |   |
 * +---+---+---+---+---+---+---+---+---+---+
 * |   |17 |   |18 |   |19 |   |20 |   |21 |
 * +---+---+---+---+---+---+---+---+---+---+
 * |11 |   |12 |   |13 |   |14 |   |15 |   |
 * +---+---+---+---+---+---+---+---+---+---+
 * |   | 6 |   | 7 |   | 8 |   | 9 |   |10 |
 * +---+---+---+---+---+---+---+---+---+---+
 * | 0 |   | 1 |   | 2 |   | 3 |   | 4 |   |
 * +---+---+---+---+---+---+---+---+---+---+
 */
final class Draughts {

    // -------------------------------------------------------------------
    // Game logic constants
    // -------------------------------------------------------------------

    /** Number of checkers on the board */
    static final int BOARD_SIZE = 50;

    /** Number of rows on the board */
    static final int BOARD_RANKS = 10;

    /** Number of columns on the board */
    static final int BOARD_FILES = 5;

    /** Number of distinc pieces */
    static final int PIECE_COUNT = 4;

    // -------------------------------------------------------------------
    // Board representation
    // -------------------------------------------------------------------

    /** South player name */
    static final String SOUTH_NAME = "White";

    /** North player name */
    static final String NORTH_NAME = "Black";

    /** South player symbol */
    static final char SOUTH_SYMBOL = 'w';

    /** North player symbol */
    static final char NORTH_SYMBOL = 'b';

    /** Empty checker symbol */
    static final char EMPTY_SYMBOL = '·';

    // -------------------------------------------------------------------
    // Piece indices
    // -------------------------------------------------------------------

    static final int SOUTH_MAN  =  0;
    static final int NORTH_MAN  =  1;
    static final int SOUTH_KING =  2;
    static final int NORTH_KING =  3;

    // -------------------------------------------------------------------
    // Direction and sense
    // -------------------------------------------------------------------

    static final int UP   =  0;
    static final int DOWN =  64;

    static final int NW   =  5;
    static final int NE   =  6;
    static final int SW   = 70;
    static final int SE   = 69;

    // -------------------------------------------------------------------
    // Bitboards
    // -------------------------------------------------------------------

    /** Number of bits on each bitboard */
    static final int BIT_SIZE = 55;

    /** Bitboard with all relevant bits set */
    static final long BOARD_BITS = 0X7DFFBFF7FEFFDFL;

    /** Masks for the players bases */
    static final long[] BASES_BITS = {
        0X7C000000000000L, // South bases
        0X0000000000001FL  // North bases
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
        'w', 'b', 'W', 'B'
    };

    /** Indexed board cell names */
    static final String[] COORDINATES = {
        "46", "47", "48", "49", "50", "-",
        "41", "42", "43", "44", "45",
        "36", "37", "38", "39", "40", "-",
        "31", "32", "33", "34", "35",
        "26", "27", "28", "29", "30", "-",
        "21", "22", "23", "24", "25",
        "16", "17", "18", "19", "20", "-",
        "11", "12", "13", "14", "15",
         "6",  "7",  "8",  "9", "10", "-",
         "1",  "2",  "3",  "4",  "5"
    };

    /** Bit indices of the checkers */
    static final int[] BITS = {
         0,  1,  2,  3,  4,
         6,  7,  8,  9, 10,
        11, 12, 13, 14, 15,
        17, 18, 19, 20, 21,
        22, 23, 24, 25, 26,
        28, 29, 30, 31, 32,
        33, 34, 35, 36, 37,
        39, 40, 41, 42, 43,
        44, 45, 46, 47, 48,
        50, 51, 52, 53, 54
    };

    /** Start position bitboards */
    static final long[] START_POSITION = {
        0X000000003EFFDFL, // South Men
        0X7DFFBE00000000L, // North Men
        0x00000000000000L, // South Kings
        0x00000000000000L, // North Kings
    };

    // -------------------------------------------------------------------
    // Player definitions
    // -------------------------------------------------------------------

    static abstract class Player {
        int turn;   // Player turn
        int man;    // Player men index
        int king;   // Player kings index
        int sense;  // Sense of men moves

        static final Player SOUTH = new Player() {{
            turn =  Game.SOUTH;
            man =   SOUTH_MAN;
            king =  SOUTH_KING;
            sense = UP;
        }};

        static final Player NORTH = new Player() {{
            turn =  Game.NORTH;
            man =   NORTH_MAN;
            king =  NORTH_KING;
            sense = DOWN;
        }};
    }
}
