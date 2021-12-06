package com.joansala.game.chess.attacks;


/**
 * Attack table for queens.
 */
public final class Queen {

    /**
     * Bitboard of attacks from the given checker.
     *
     * @param checker   Checker were the piece resides
     * @param taken     Bitboard of occupied checkers
     */
    public static final long attacks(int checker, long taken) {
        return Bishop.attacks(checker, taken) |
               Rook.attacks(checker, taken);
    }
}
