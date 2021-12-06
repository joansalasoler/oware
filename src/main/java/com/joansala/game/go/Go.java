package com.joansala.game.go;

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
import com.joansala.util.bits.Bitset;


/**
 * Definitions for Go.
 */
final class Go {

    // -------------------------------------------------------------------
    // Game logic constants
    // -------------------------------------------------------------------

    /** Number of intersections on the board */
    static final int BOARD_SIZE = 361;

    /** Number of rows on the board */
    static final int BOARD_RANKS = 19;

    /** Number of columns on the board */
    static final int BOARD_FILES = 19;

    /** Number of distinc stones */
    static final int PIECE_COUNT = 2;

    /** Number of words on each bitset */
    static final int BITSET_SIZE = 6;

    /** Player fortfeits its turn */
    static final int FORFEIT_MOVE = 361;

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

    /** Empty intersection symbol */
    static final char EMPTY_SYMBOL = '·';

    /** Empty star point symbol */
    static final char STAR_SYMBOL = '+';

    // -------------------------------------------------------------------
    // Piece indices
    // -------------------------------------------------------------------

    static final int BLACK =  0;
    static final int WHITE =  1;

    // -------------------------------------------------------------------
    // Zobrist hashing
    // -------------------------------------------------------------------

    static final long RANDOM_SEED = 0x6622E46E1DB096FAL;
    static final long WHITE_SIGN =  0x506AACF489889342L;
    static final long BLACK_SIGN =  0xD2B7ADEEDED1F73FL;

    // -------------------------------------------------------------------
    // Board definitions
    // -------------------------------------------------------------------

    /** Indexed game piece symbols */
    static final char[] PIECES = {
        'X', 'O'
    };

    /** Indexed board cell names */
    static final String[] COORDINATES = {
         "a1",  "b1",  "c1",  "d1",  "e1",  "f1",  "g1",  "h1",  "j1",
         "k1",  "l1",  "m1",  "n1",  "o1",  "p1",  "q1",  "r1",  "s1",
         "t1",  "a2",  "b2",  "c2",  "d2",  "e2",  "f2",  "g2",  "h2",
         "j2",  "k2",  "l2",  "m2",  "n2",  "o2",  "p2",  "q2",  "r2",
         "s2",  "t2",  "a3",  "b3",  "c3",  "d3",  "e3",  "f3",  "g3",
         "h3",  "j3",  "k3",  "l3",  "m3",  "n3",  "o3",  "p3",  "q3",
         "r3",  "s3",  "t3",  "a4",  "b4",  "c4",  "d4",  "e4",  "f4",
         "g4",  "h4",  "j4",  "k4",  "l4",  "m4",  "n4",  "o4",  "p4",
         "q4",  "r4",  "s4",  "t4",  "a5",  "b5",  "c5",  "d5",  "e5",
         "f5",  "g5",  "h5",  "j5",  "k5",  "l5",  "m5",  "n5",  "o5",
         "p5",  "q5",  "r5",  "s5",  "t5",  "a6",  "b6",  "c6",  "d6",
         "e6",  "f6",  "g6",  "h6",  "j6",  "k6",  "l6",  "m6",  "n6",
         "o6",  "p6",  "q6",  "r6",  "s6",  "t6",  "a7",  "b7",  "c7",
         "d7",  "e7",  "f7",  "g7",  "h7",  "j7",  "k7",  "l7",  "m7",
         "n7",  "o7",  "p7",  "q7",  "r7",  "s7",  "t7",  "a8",  "b8",
         "c8",  "d8",  "e8",  "f8",  "g8",  "h8",  "j8",  "k8",  "l8",
         "m8",  "n8",  "o8",  "p8",  "q8",  "r8",  "s8",  "t8",  "a9",
         "b9",  "c9",  "d9",  "e9",  "f9",  "g9",  "h9",  "j9",  "k9",
         "l9",  "m9",  "n9",  "o9",  "p9",  "q9",  "r9",  "s9",  "t9",
        "a10", "b10", "c10", "d10", "e10", "f10", "g10", "h10", "j10",
        "k10", "l10", "m10", "n10", "o10", "p10", "q10", "r10", "s10",
        "t10", "a11", "b11", "c11", "d11", "e11", "f11", "g11", "h11",
        "j11", "k11", "l11", "m11", "n11", "o11", "p11", "q11", "r11",
        "s11", "t11", "a12", "b12", "c12", "d12", "e12", "f12", "g12",
        "h12", "j12", "k12", "l12", "m12", "n12", "o12", "p12", "q12",
        "r12", "s12", "t12", "a13", "b13", "c13", "d13", "e13", "f13",
        "g13", "h13", "j13", "k13", "l13", "m13", "n13", "o13", "p13",
        "q13", "r13", "s13", "t13", "a14", "b14", "c14", "d14", "e14",
        "f14", "g14", "h14", "j14", "k14", "l14", "m14", "n14", "o14",
        "p14", "q14", "r14", "s14", "t14", "a15", "b15", "c15", "d15",
        "e15", "f15", "g15", "h15", "j15", "k15", "l15", "m15", "n15",
        "o15", "p15", "q15", "r15", "s15", "t15", "a16", "b16", "c16",
        "d16", "e16", "f16", "g16", "h16", "j16", "k16", "l16", "m16",
        "n16", "o16", "p16", "q16", "r16", "s16", "t16", "a17", "b17",
        "c17", "d17", "e17", "f17", "g17", "h17", "j17", "k17", "l17",
        "m17", "n17", "o17", "p17", "q17", "r17", "s17", "t17", "a18",
        "b18", "c18", "d18", "e18", "f18", "g18", "h18", "j18", "k18",
        "l18", "m18", "n18", "o18", "p18", "q18", "r18", "s18", "t18",
        "a19", "b19", "c19", "d19", "e19", "f19", "g19", "h19", "j19",
        "k19", "l19", "m19", "n19", "o19", "p19", "q19", "r19", "s19",
        "t19", "-"
    };


    /** Bit indices of the intersections */
    static final int[] BITS = {
          0,   1,   2,   3,   4,   5,   6,   7,   8,
          9,  10,  11,  12,  13,  14,  15,  16,  17,
         18,  19,  20,  21,  22,  23,  24,  25,  26,
         27,  28,  29,  30,  31,  32,  33,  34,  35,
         36,  37,  38,  39,  40,  41,  42,  43,  44,
         45,  46,  47,  48,  49,  50,  51,  52,  53,
         54,  55,  56,  57,  58,  59,  60,  61,  62,
         63,  64,  65,  66,  67,  68,  69,  70,  71,
         72,  73,  74,  75,  76,  77,  78,  79,  80,
         81,  82,  83,  84,  85,  86,  87,  88,  89,
         90,  91,  92,  93,  94,  95,  96,  97,  98,
         99, 100, 101, 102, 103, 104, 105, 106, 107,
        108, 109, 110, 111, 112, 113, 114, 115, 116,
        117, 118, 119, 120, 121, 122, 123, 124, 125,
        126, 127, 128, 129, 130, 131, 132, 133, 134,
        135, 136, 137, 138, 139, 140, 141, 142, 143,
        144, 145, 146, 147, 148, 149, 150, 151, 152,
        153, 154, 155, 156, 157, 158, 159, 160, 161,
        162, 163, 164, 165, 166, 167, 168, 169, 170,
        171, 172, 173, 174, 175, 176, 177, 178, 179,
        180, 181, 182, 183, 184, 185, 186, 187, 188,
        189, 190, 191, 192, 193, 194, 195, 196, 197,
        198, 199, 200, 201, 202, 203, 204, 205, 206,
        207, 208, 209, 210, 211, 212, 213, 214, 215,
        216, 217, 218, 219, 220, 221, 222, 223, 224,
        225, 226, 227, 228, 229, 230, 231, 232, 233,
        234, 235, 236, 237, 238, 239, 240, 241, 242,
        243, 244, 245, 246, 247, 248, 249, 250, 251,
        252, 253, 254, 255, 256, 257, 258, 259, 260,
        261, 262, 263, 264, 265, 266, 267, 268, 269,
        270, 271, 272, 273, 274, 275, 276, 277, 278,
        279, 280, 281, 282, 283, 284, 285, 286, 287,
        288, 289, 290, 291, 292, 293, 294, 295, 296,
        297, 298, 299, 300, 301, 302, 303, 304, 305,
        306, 307, 308, 309, 310, 311, 312, 313, 314,
        315, 316, 317, 318, 319, 320, 321, 322, 323,
        324, 325, 326, 327, 328, 329, 330, 331, 332,
        333, 334, 335, 336, 337, 338, 339, 340, 341,
        342, 343, 344, 345, 346, 347, 348, 349, 350,
        351, 352, 353, 354, 355, 356, 357, 358, 359,
        360
    };

    /** Star point intersection indices */
    static final int[] STAR_POINTS = {
        60, 66, 72, 174, 180, 186, 288, 294, 300
    };

    /** Start position bitboards */
    static final Bitset[] START_POSITION = {
        new Bitset(BITSET_SIZE), // Black pieces
        new Bitset(BITSET_SIZE)  // White pieces
    };

    // -------------------------------------------------------------------
    // Player definitions
    // -------------------------------------------------------------------

    static abstract class Player {
        int turn;       // Player turn
        int color;      // Player color
        long sign;      // Player hash sign

        static final Player SOUTH = new Player() {{
            color =     BLACK;
            sign =      BLACK_SIGN;
            turn =      Game.SOUTH;
        }};

        static final Player NORTH = new Player() {{
            color =     WHITE;
            sign =      WHITE_SIGN;
            turn =      Game.NORTH;
        }};
    }
}
