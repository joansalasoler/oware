package com.joansala.game.chess.attacks;

import static com.joansala.util.bits.Bits.*;


/**
 * Attacks and advances for pawns.
 */
public final class Pawn {

    /** Every row except the leftmost one */
    private static long RIGHT_FILES = 0xFEFEFEFEFEFEFEFEL;

    /** Every row except the rightmost one */
    private static long LEFT_FILES =  0x7F7F7F7F7F7F7F7FL;


    /**
     * Bitboard of pawn attacks from the given checker.
     *
     * @param checker   Checker were the piece resides
     * @param sense     Sense of movement
     */
    public static final long attacks(int checker, int sense) {
        final long left = (bit(checker) & RIGHT_FILES) >>> 1;
        final long right = (bit(checker) & LEFT_FILES) << 1;
        return shift(left | right, sense);
    }


    /**
     * Single advances by a set of pawns.
     *
     * @param pawns     Bitboard of pawn placements
     * @param taken     Bitboard of occupied checkers
     * @param sense     Sense of movement
     */
    public static final long singles(long pawns, long taken, int sense) {
        return shift(pawns, sense) & ~taken;
    }


    /**
     * Double advances by a set of pawns.
     *
     * @param pawns     Bitboard of pawn placements
     * @param taken     Bitboard of occupied checkers
     * @param sense     Sense of movement
     */
    public static final long doubles(long pawns, long taken, int sense) {
        return shift(pawns, sense << 1) & shift(~taken, sense) & ~taken;
    }


    /**
     * Captures on the white's right by a set of pawns.
     *
     * @param pawns     Bitboard of pawn placements
     * @param rivals    Bitboard of rival pieces
     * @param sense     Sense of movement
     */
    public static final long rights(long pawns, int sense) {
        return shift((pawns & LEFT_FILES) << 1, sense);
    }


    /**
     * Captures on the white's left by a set of pawns.
     *
     * @param pawns     Bitboard of pawn placements
     * @param rivals    Bitboard of rival pieces
     * @param sense     Sense of movement
     */
    public static final long lefts(long pawns, int sense) {
        return shift((pawns & RIGHT_FILES) >>> 1, sense);
    }
}
