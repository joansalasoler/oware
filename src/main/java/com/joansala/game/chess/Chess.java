package com.joansala.game.chess;

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
 * Definitions for Chess.
 *
 *   +---+---+---+---+---+---+---+---+
 * 8 |56 |57 |58 |59 |60 |61 |62 |63 |
 *   +---+---+---+---+---+---+---+---+
 * 7 |48 |49 |50 |51 |52 |53 |54 |55 |
 *   +---+---+---+---+---+---+---+---+
 * 6 |40 |41 |42 |43 |44 |45 |46 |47 |
 *   +---+---+---+---+---+---+---+---+
 * 5 |32 |33 |34 |35 |36 |37 |38 |39 |
 *   +---+---+---+---+---+---+---+---+
 * 4 |24 |25 |26 |27 |28 |29 |30 |31 |
 *   +---+---+---+---+---+---+---+---+
 * 3 |16 |17 |18 |19 |20 |21 |22 |23 |
 *   +---+---+---+---+---+---+---+---+
 * 2 | 8 | 9 |10 |11 |12 |13 |14 |15 |
 *   +---+---+---+---+---+---+---+---+
 * 1 | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
 *   +---+---+---+---+---+---+---+---+
 *     a   b   c   d   e   f   g   h
 *
 * +--------------------------------------------------+
 * |    move = (types | piece | from | to)            |
 * +--------------------------------------------------+
 * |      to = target checker (6 bits)                |
 * |    from = origin checker (6 bits)                |
 * |   piece = piece to place at target (3 bits)      |
 * |   types = move types (2 bits)                    |
 * +--------------------------------------------------+
 * |   FLAGS = bitboard with the en-passant checker   |
 * |           and all the rooks that can castle      |
 * +--------------------------------------------------+
 */
final class Chess {

    // -------------------------------------------------------------------
    // Game logic constants
    // -------------------------------------------------------------------

    /** Number of checkers on the board */
    static final int BOARD_SIZE = 64;

    /** Number of rows on the board */
    static final int BOARD_RANKS = 8;

    /** Number of columns on the board */
    static final int BOARD_FILES = 8;

    /** Number of distinct pieces */
    static final int PIECE_COUNT = 8;

    /** Number of bitboards on a position */
    static final int STATE_SIZE = 9;

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

    // -------------------------------------------------------------------
    // Sense of movement for pawns
    // -------------------------------------------------------------------

    static final int UP   =  8;
    static final int DOWN =  72;

    // -------------------------------------------------------------------
    // Piece indices
    // -------------------------------------------------------------------

    static final int KING   =  0;
    static final int QUEEN  =  1;
    static final int ROOK   =  2;
    static final int BISHOP =  3;
    static final int KNIGHT =  4;
    static final int PAWN   =  5;
    static final int WHITE  =  6;
    static final int BLACK  =  7;
    static final int FLAGS  =  8;

    // -------------------------------------------------------------------
    // Move type flags
    // -------------------------------------------------------------------

    static final int UNFLAGGED    = (0b00 << 15);
    static final int PROMOTION    = (0b01 << 15);
    static final int ENPASSANT    = (0b10 << 15);
    static final int CASTLING     = (0b11 << 15);

    // -------------------------------------------------------------------
    // Bitboard masks
    // -------------------------------------------------------------------

    static final long RANK_ONE =    0x00000000000000FFL;
    static final long RANK_TWO =    0x000000000000FF00L;
    static final long RANK_SEVEN =  0x00FF000000000000L;
    static final long RANK_EIGHT =  0xFF00000000000000L;

    static final long RIGHT_FILES = 0xFEFEFEFEFEFEFEFEL;
    static final long LEFT_FILES =  0x7F7F7F7F7F7F7F7FL;
    static final long CASTLE_MASK = 0x8100000000000081L;

    static final long FULL_BOARD =  0xFFFFFFFFFFFFFFFFL;
    static final long EMPTY_BOARD = 0x0000000000000000L;
    static final long BOARD_SIDES = 0XFF818181818181FFL;

    // -------------------------------------------------------------------
    // Zobrist hashing
    // -------------------------------------------------------------------

    static final long RANDOM_SEED = 0x6622E46E1DB096FAL;
    static final long WHITE_SIGN =  0x506AACF489889342L;
    static final long BLACK_SIGN =  0xD2B7ADEEDED1F73FL;

    // -------------------------------------------------------------------
    // Heuristic evaluation weights
    // -------------------------------------------------------------------

    static final int QUEEN_WEIGHT =  100;
    static final int ROOK_WEIGHT =    54;
    static final int BISHOP_WEIGHT =  46;
    static final int KNIGHT_WEIGHT =  31;
    static final int PAWN_WEIGHT =     8;

    // -------------------------------------------------------------------
    // Openings book
    // -------------------------------------------------------------------

    /** Minimum score for an opening move to be choosen */
    static final double ROOT_THRESHOLD = -250.0D;

    /** Threshold on the highest opening move reward */
    static final double ROOT_DISTURBANCE = 100.0D;

    // -------------------------------------------------------------------
    // Board definitions
    // -------------------------------------------------------------------

    /** Indexed game piece symbols */
    static final char[] PIECES = {
        'K', 'Q', 'R', 'B', 'N', 'P',
        'k', 'q', 'r', 'b', 'n', 'p'
    };

    /** Indexed castling rights */
    static final String[] CASTLINGS = {
        "-",    "K",    "Q",    "KQ",
        "q",    "Qq",   "Kq",   "KQq",
        "k",    "Qk",   "Kk",   "KQk",
        "kq",   "Qkq",  "Kkq",  "KQkq"
    };

    /** Indexed board cell names */
    static final String[] COORDINATES = {
        "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1",
        "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
        "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
        "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
        "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
        "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
        "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
        "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
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
        0x1000000000000010L, // Kings
        0x0800000000000008L, // Queens
        0x8100000000000081L, // Rooks
        0x2400000000000024L, // Bishops
        0x4200000000000042L, // Knights
        0x00FF00000000FF00L, // Pawns
        0x000000000000FFFFL, // White pieces
        0xFFFF000000000000L, // Black pieces
        0x8100000000000081L  // Match flags
    };

    // -------------------------------------------------------------------
    // Castling definitions
    // -------------------------------------------------------------------

    static abstract class Castle {
        int move;       // Move encoding
        long path;      // Path bitboard
        long flag;      // Rook bitboard flag
        long kings;     // King origin and target
        long rooks;     // Rook origin and target
        long hash;      // Hash code
        int[] spots;    // Checkers traveled by king
    }

    /** White king side castle */
    static final Castle WHITE_SHORT = new Castle() {{
        move =      (CASTLING | (4 << 6) | 6);
        flag =      (0b100000L << 2);
        path =      (0b011000L << 2);
        kings =     (0b010100L << 2);
        rooks =     (0b101000L << 2);
        spots =     new int[] { 5, 6 };
    }};

    /** White queen side castle */
    static final Castle WHITE_LONG = new Castle() {{
        move =      (CASTLING | (4 << 6) | 2);
        flag =      (0b000001L << 0);
        path =      (0b001110L << 0);
        kings =     (0b010100L << 0);
        rooks =     (0b001001L << 0);
        spots =     new int[] { 2, 3 };
    }};

    /** Black king side castle */
    static final Castle BLACK_SHORT = new Castle() {{
        move =      (CASTLING | (60 << 6) | 62);
        flag =      (0b100000L << 58);
        path =      (0b011000L << 58);
        kings =     (0b010100L << 58);
        rooks =     (0b101000L << 58);
        spots =     new int[] { 61, 62 };
    }};

    /** Black queen side castle */
    static final Castle BLACK_LONG = new Castle() {{
        move =      (CASTLING | (60 << 6) | 58);
        flag =      (0b000001L << 56);
        path =      (0b001110L << 56);
        kings =     (0b010100L << 56);
        rooks =     (0b001001L << 56);
        spots =     new int[] { 58, 59 };
    }};

    // -------------------------------------------------------------------
    // Player definitions
    // -------------------------------------------------------------------

    static abstract class Player {
        int turn;           // Player turn
        int side;           // Player color index
        int sense;          // Sense of movement
        long sign;          // Player hash sign
        long base;          // Pawns start row
        long eighth;        // Promotion row
        long seventh;       // Row before pormotion
        Castle[] castlings; // Castling moves

        static final Player SOUTH = new Player() {{
            sense =     UP;
            side =      WHITE;
            sign =      WHITE_SIGN;
            base =      RANK_TWO;
            eighth =    RANK_EIGHT;
            seventh =   RANK_SEVEN;
            turn =      Game.SOUTH;
            castlings = new Castle[] { WHITE_SHORT, WHITE_LONG };
        }};

        static final Player NORTH = new Player() {{
            sense =     DOWN;
            side =      BLACK;
            sign =      BLACK_SIGN;
            base =      RANK_SEVEN;
            eighth =    RANK_ONE;
            seventh =   RANK_TWO;
            turn =      Game.NORTH;
            castlings = new Castle[] { BLACK_SHORT, BLACK_LONG };
        }};
    }
}
