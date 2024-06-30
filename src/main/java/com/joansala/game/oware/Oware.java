package com.joansala.game.oware;

/*
 * Aalina oware engine.
 * Copyright (C) 2014-2024 Joan Sala Soler <contact@joansala.com>
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
 * Definitions for the Oware Abapa game.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class Oware {

    // -------------------------------------------------------------------
    // Game logic constants
    // -------------------------------------------------------------------

    /** Number of pits on the board */
    public static final int BOARD_SIZE = 12;

    /** Number of seeds on the board */
    public static final int SEED_COUNT = 48;

    /** Number of seeds to surpass to win */
    public static final int SEED_GOAL = 24;

    /** Leftmost player house from south's perspective */
    public static final int SOUTH_LEFT = 0;

    /** Rightmost player house from south's perspective */
    public static final int SOUTH_RIGHT = 5;

    /** Store house for south */
    public static final int SOUTH_STORE = 12;

    /** Leftmost player house from north's perspective */
    public static final int NORTH_LEFT = 6;

    /** Rightmost player house from north's perspective */
    public static final int NORTH_RIGHT = 11;

    /** Store house for north */
    public static final int NORTH_STORE = 13;

    /** Length of the position array */
    public static final int POSITION_SIZE = 14;

    // -------------------------------------------------------------------
    // Board representation
    // -------------------------------------------------------------------

    /** South player name */
    public static final String SOUTH_NAME = "South";

    /** North player name */
    public static final String NORTH_NAME = "North";

    /** South player symbol */
    public static final char SOUTH_SYMBOL = 'S';

    /** North player symbol */
    public static final char NORTH_SYMBOL = 'N';

    // -------------------------------------------------------------------
    // Bitboard masks
    // -------------------------------------------------------------------

    /** South houses bitmask */
    public static final int SOUTH_MASK = 0b000000111111;

    /** North houses bitmask */
    public static final int NORTH_MASK = 0b111111000000;

    // -------------------------------------------------------------------
    // Binomial hashing
    // -------------------------------------------------------------------

    /** Hash sign for positions were south is to move */
    public static final long SOUTH_SIGN = 0x80000000000L;

    /** Hash sign for positions were north is to move */
    public static final long NORTH_SIGN = 0x00000000000L;

    // -------------------------------------------------------------------
    // Heuristic evaluation weights
    // -------------------------------------------------------------------

    /** Weight of the captured seeds difference */
    public static final int TALLY_WEIGHT = 25;

    /** Weight of houses that contain more than 12 seeds */
    public static final int ATTACK_WEIGHT = 28;

    /** Weight of houses that contain 1 or 2 seeds */
    public static final int DEFENSE_WEIGHT = -36;

    /** Weight of houses that do not contain any seeds */
    public static final int MOBILITY_WEIGHT = -54;

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = -9;

    /** Score to which to evaluate won repetitions */
    public static final int REPETITION_SCORE = 990;

    // -------------------------------------------------------------------
    // Openings book
    // -------------------------------------------------------------------

    /** Minimum score for an opening move to be chosen */
    public static final double ROOT_THRESHOLD = -27.0D;

    /** Threshold on the highest opening move reward */
    public static final double ROOT_DISTURBANCE = -13.5D;

    // -------------------------------------------------------------------
    // Board definitions
    // -------------------------------------------------------------------

    /** Default start position */
    static final int[] START_POSITION = {
        4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0
    };

    /** Indexed board cell names */
    static final String[] HOUSES = {
        "A", "B", "C", "D", "E", "F",
        "a", "b", "c", "d", "e", "f"
    };

    // -------------------------------------------------------------------
    // Player definitions
    // -------------------------------------------------------------------

    static class Player {
        int turn;  // Player turn
        int left;  // Left house
        int right; // Right house
        int store; // Store house
        int mask;  // Houses mask
        long sign; // Hash sign

        static final Player SOUTH = new Player() {{
            sign =  SOUTH_SIGN;
            turn =  Game.SOUTH;
            left =  SOUTH_LEFT;
            right = SOUTH_RIGHT;
            store = SOUTH_STORE;
            mask =  SOUTH_MASK;
        }};

        static final Player NORTH = new Player() {{
            sign =  NORTH_SIGN;
            turn =  Game.NORTH;
            left =  NORTH_LEFT;
            right = NORTH_RIGHT;
            store = NORTH_STORE;
            mask =  NORTH_MASK;
        }};
    }

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
