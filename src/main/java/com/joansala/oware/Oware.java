package com.joansala.oware;

/*
 * Aalina oware engine.
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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
 * Definitions for the Oware Abapa game.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
final class Oware {

    // -------------------------------------------------------------------
    // Game logic constants
    // -------------------------------------------------------------------

    /** Number of pits on the board */
    static final int BOARD_SIZE = 12;

    /** Number of seeds on the board */
    static final int SEED_COUNT = 48;

    /** Number of seeds to surpass to win */
    static final int SEED_GOAL = 24;

    /** Leftmost player house from south's perspective */
    static final int SOUTH_LEFT = 0;

    /** Rightmost player house from south's perspective */
    static final int SOUTH_RIGHT = 5;

    /** Store house for south */
    static final int SOUTH_STORE = 12;

    /** Leftmost player house from north's perspective */
    static final int NORTH_LEFT = 6;

    /** Rightmost player house from north's perspective */
    static final int NORTH_RIGHT = 11;

    /** Store house for north */
    static final int NORTH_STORE = 13;

    // -------------------------------------------------------------------
    // Bitboard masks
    // -------------------------------------------------------------------

    /** South houses bitmask */
    static final int SOUTH_MASK = 0b000000111111;

    /** North houses bitmask */
    static final int NORTH_MASK = 0b111111000000;

    /** Hash sign for positions were south is to move */
    static final long SOUTH_SIGN = 0x80000000000L;

    /** Hash sign for positions were north is to move */
    static final long NORTH_SIGN = 0x00000000000L;

    // -------------------------------------------------------------------
    // Evaluation function weights
    // -------------------------------------------------------------------

    /** Weight of the captured seeds difference */
    static final int TALLY_WEIGHT = 25;

    /** Weight of houses that contain more than 12 seeds */
    static final int ATTACK_WEIGHT = 28;

    /** Weight of houses that contain 1 or 2 seeds */
    static final int DEFENSE_WEIGHT = -36;

    /** Weight of houses that do not contain any seeds */
    static final int MOBILITY_WEIGHT = -54;

    // -------------------------------------------------------------------
    // Board definitions
    // -------------------------------------------------------------------

    /** Default start position */
    static final int[] START_POSITION = {
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0
    };

    /** Indexed moves strings */
    static final String[] MOVES = {
        "A", "B", "C", "D", "E", "F",
        "a", "b", "c", "d", "e", "f"
    };

    // -------------------------------------------------------------------
    // Move generation tables
    // -------------------------------------------------------------------

    /** Used to determine the pit where a seeding lands. Helps in
        determining when a move could be a capture */
    static final int[][] REAPER = {
        {-1, -1, -1, -1, -1, -1,  6,  7,  8,  9, 10, 11,
         -1, -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1,
         -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1,
         -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1,
         -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1,
         -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1,
         -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1,
         -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1,
         -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1,
         -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,
          6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,
          6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,  6,
          7,  8,  9, 10, 11, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5,
         -1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1,
         -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1,
         -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1,
         -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1,
         -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1,
         -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1,
         -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1,
         -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1,
         -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,
          0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,
          0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,  0,
          1,  2,  3,  4,  5, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
    };
}
