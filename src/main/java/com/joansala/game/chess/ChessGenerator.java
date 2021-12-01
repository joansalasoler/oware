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

import java.util.Arrays;
import com.joansala.game.chess.attacks.*;
import static com.joansala.engine.Game.*;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.chess.Chess.*;


/**
 * Move generator for chess.
 */
public class ChessGenerator {

    /** First move generation cursor */
    public static final int UNGENERATED = 0;

    /** Default number of slots */
    private static final int DEFAULT_CAPACITY = 255;

    /** Maximum possible moves by a single stage */
    private static final int MAX_MOVES = 218;

    /** No move generation was performed */
    private static final int START_STAGE = -1;

    /** Generate king moves stage */
    private static final int KING_STAGE = 0;

    /** Move generation was completed */
    private static final int END_STAGE = 14;

    /** Current capacity */
    private int capacity = DEFAULT_CAPACITY;

    /** Stores moves and remnants for each slot */
    private Entry[] store = new Entry[DEFAULT_CAPACITY];

    /** Current player to move */
    private Player player;

    /** Current evasions mask */
    private long evasions;

    /** Current position bitboards */
    private long[] state;

    /** Moves generated (current slot) */
    private int[] moves = null;

    /** Next move index */
    private int index = 0;


    /**
     * Create a new move generator.
     */
    public ChessGenerator() {
        for (int i = 0; i < store.length; i++) {
            store[i] = new Entry();
        }
    }


    /**
     * Move generation stage for a cursor.
     */
    public int getStage(int cursor) {
        return (cursor >> 8) & 0x1F;
    }


    /**
     * Generated move for a cursor.
     */
    public int getMove(int cursor) {
        return (cursor >> 14);
    }


    /**
     * Returns if a player's king is in check on a game state.
     */
    public boolean isInCheck(long[] state, Player player) {
        final long evasions = computeEvasions(state, player);
        return evasions != FULL_BOARD;
    }


    /**
     * Returns if a game state contains legal moves. This method generates
     * the first set of moves and returns true if it is empty.
     */
    public boolean cannotMove(int slot, long[] state, Player player) {
        final Entry entry = store[slot];
        generate(slot, UNGENERATED, state, player);
        return 0 == entry.length;
    }


    /**
     * Next move generation cursor for a slot.
     *
     * @param slot      Storage slot
     * @param cursor    Generation cursor
     * @return          Next generation cursor
     */
    public int nextCursor(int slot, int cursor) {
        final Entry entry = store[slot];

        int stage = getStage(cursor);
        int index = (cursor & 0xFF);
        int move = entry.moves[index];

        if (index == entry.length - 1) {
            stage = entry.nextStage;
            index = 0;
        } else {
            index++;
        }

        return (move << 14) | (stage << 8) | (index);
    }


    /**
     * Clear moves from the given slot.
     *
     * @param slot      Storage slot
     */
    public void clear(int slot) {
        store[slot].clear();
    }


    /**
     * Generate the set of moves required by a cursor and store them on
     * the given slot if they weren't already generated.
     *
     * @param slot      Storage slot
     * @param cursor    Current generation cursor
     * @param state     Position bitboards
     * @param player    Player to move
     */
    public void generate(int slot, int cursor, long[] state, Player player) {
        final Entry entry = store[slot];
        long evasions = entry.evasions;
        int stage = getStage(cursor);

        if (entry.isCurrentStage(stage)) {
            return;
        }

        if (entry.isStartStage()) {
            evasions = computeEvasions(state, player);
            entry.evasions = evasions;
        }

        this.index = 0;
        this.state = state;
        this.player = player;
        this.evasions = evasions;
        this.moves = entry.moves;
        entry.currentStage = stage;

        if (isKingInDoubleCheck()) {
            if (stage == KING_STAGE) {
                storeStageMoves(stage, evasions);
                stage = END_STAGE;
            }
        } else {
            for (int i = index; i == index && stage < END_STAGE; stage++) {
                storeStageMoves(stage, evasions);
            }
        }

        entry.length = index;
        entry.nextStage = stage;
        moves[index] = NULL_MOVE;
    }


    /**
     * Generates the set of moves for the given stage appending them
     * to the current slot if any legal moves are found.
     *
     * @param stage         Generation stage
     * @param evasions      Check evasions mask
     */
    private void storeStageMoves(int stage, long evasions) {
        final long friends = state[player.side];
        final long rivals = state[1 ^ player.side];
        final long taken = state[WHITE] | state[BLACK];
        final long bishops = state[BISHOP] & friends;
        final long knights = state[KNIGHT] & friends;
        final long queens = state[QUEEN] & friends;
        final long rooks = state[ROOK] & friends;
        final long pawns = state[PAWN] & friends;
        final long king = state[KING] & friends;
        final long flags = state[FLAGS];

        // If the king is in check generate only check evasions, otherwise
        // all the rival pieces can be captured except for the king.

        final long checkers = ~(friends | state[KING]);
        final long mask = isKingInCheck() ? evasions : checkers;

        switch (stage) {
            case  0: storeKingMoves(king, taken, checkers); break;
            case  1: storeCastlings(taken, flags); break;
            case  2: storePromotions(pawns, taken, rivals, mask); break;
            case  3: storePawnCaptures(pawns, taken, mask & rivals); break;
            case  4: storeKnightMoves(knights, taken, mask & rivals); break;
            case  5: storeBishopMoves(bishops, taken, mask & rivals); break;
            case  6: storeRookMoves(rooks, taken, mask & rivals); break;
            case  7: storeQueenMoves(queens, taken, mask & rivals); break;
            case  8: storeEnPassants(pawns, taken, flags); break;
            case  9: storeKnightMoves(knights, taken, mask & ~rivals); break;
            case 10: storeBishopMoves(bishops, taken, mask & ~rivals); break;
            case 11: storeRookMoves(rooks, taken, mask & ~rivals); break;
            case 12: storeQueenMoves(queens, taken, mask & ~rivals); break;
            case 13: storePawnMoves(pawns, taken, mask); break;
        }
    }


    /**
     * Generate the legal moves of the King.
     *
     * @param king      Player king bitboard
     * @param taken     Board of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storeKingMoves(long king, long taken, long mask) {
        final int from = first(king);
        long attacks = King.attacks(from) & mask;

        while (empty(attacks) == false) {
            int to = first(attacks);
            attacks ^= bit(to);

            if (isAttacked(to, taken ^ king) == false) {
                store(UNFLAGGED, KING, from, to);
            }
        }
    }


    /**
     * Generate the legal moves of knights.
     *
     * @param knights   Player knights bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storeKnightMoves(long knights, long taken, long mask) {
        while (empty(knights) == false) {
            final int from = first(knights);
            final long attacks = Knight.attacks(from);
            final long targets = attacks & mask & pins(from, taken);
            store(UNFLAGGED, KNIGHT, from, targets);
            knights ^= bit(from);
        }
    }


    /**
     * Generate the legal moves of bishops.
     *
     * @param bishops   Player bishops bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storeBishopMoves(long bishops, long taken, long mask) {
        while (empty(bishops) == false) {
            final int from = first(bishops);
            final long attacks = Bishop.attacks(from, taken);
            final long targets = attacks & mask & pins(from, taken);
            store(UNFLAGGED, BISHOP, from, targets);
            bishops ^= bit(from);
        }
    }


    /**
     * Generate the legal moves of rooks.
     *
     * @param rooks     Player rooks bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storeRookMoves(long rooks, long taken, long mask) {
        while (empty(rooks) == false) {
            final int from = first(rooks);
            final long attacks = Rook.attacks(from, taken);
            final long targets = attacks & mask & pins(from, taken);
            store(UNFLAGGED, ROOK, from, targets);
            rooks ^= bit(from);
        }
    }


    /**
     * Generate the legal moves of queens.
     *
     * @param queens    Player queens bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storeQueenMoves(long queens, long taken, long mask) {
        while (empty(queens) == false) {
            final int from = first(queens);
            final long attacks = Queen.attacks(from, taken);
            final long targets = attacks & mask & pins(from, taken);
            store(UNFLAGGED, QUEEN, from, targets);
            queens ^= bit(from);
        }
    }


    /**
     * Generate the legal castling moves.
     *
     * @param taken     Bitboard of occupied checkers
     * @param flags     Bitboard of rooks that can castle
     */
    private void storeCastlings(long taken, long flags) {
        if (isKingInCheck()) return;

        for (Castle castle : player.castlings) {
            final boolean hasRight = contains(flags, castle.flag);
            final boolean hasPath = empty(taken & castle.path);

            if (hasRight && hasPath && !areAttacked(castle.spots, taken)) {
                store(castle.move);
            }
        }
    }


    /**
     * Generate the legal en-passant pawn captures.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param flags     En-passant bitboard
     */
    private void storeEnPassants(long pawns, long taken, long flags) {
        final long mask = shift(evasions, player.sense);
        final long target = (flags & ~CASTLE_MASK);

        if (!isKingInCheck() || contains(mask, target)) {
            final int to = first(target);
            long attackers = pawns & Pawn.attacks(to, 64 ^ player.sense);

            while (empty(attackers) == false) {
                final int from = first(attackers);
                final long pins = pins(from, taken & ~bit(to ^ 0x8));
                attackers ^= bit(from);

                if (contains(pins, target)) {
                    store(ENPASSANT, PAWN, from, to);
                }
            }
        }
    }


    /**
     * Generate the legal capture moves of pawns.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storePawnCaptures(long pawns, long taken, long mask) {
        if (empty(pawns = pawns & ~player.seventh) == false) {
            final int sense = player.sense;
            final int oneRow = player.turn << 3;

            long lefts = Pawn.lefts(pawns, sense) & mask;
            long rights = Pawn.rights(pawns, sense) & mask;

            while (empty(lefts) == false) {
                final int to = first(lefts);
                final int from = to - oneRow + 1;
                final long pins = pins(from, taken);
                lefts ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(UNFLAGGED, PAWN, from, to);
                }
            }

            while (empty(rights) == false) {
                final int to = first(rights);
                final int from = to - oneRow - 1;
                final long pins = pins(from, taken);
                rights ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(UNFLAGGED, PAWN, from, to);
                }
            }
        }
    }


    /**
     * Generate the legal non-capturing moves of pawns.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storePawnMoves(long pawns, long taken, long mask) {
        if (empty(pawns = pawns & ~player.seventh) == false) {
            final int sense = player.sense;
            final int oneRow = player.turn << 3;
            final int twoRows = player.turn << 4;
            final long bases = pawns & player.base;

            long doubles = Pawn.doubles(bases, taken, sense) & mask;
            long singles = Pawn.singles(pawns, taken, sense) & mask;

            while (empty(doubles) == false) {
                final int to = first(doubles);
                final int from = to - twoRows;
                final long pins = pins(from, taken);
                doubles ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(UNFLAGGED, PAWN, from, to);
                }
            }

            while (empty(singles) == false) {
                final int to = first(singles);
                final int from = to - oneRow;
                final long pins = pins(from, taken);
                singles ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(UNFLAGGED, PAWN, from, to);
                }
            }
        }
    }


    /**
     * Generate the legal pawn promotion moves.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param taken     Bitboard of rival pieces
     * @param mask      Bitboard of allowed checkers
     */
    private void storePromotions(long pawns, long taken, long rivals, long mask) {
        if (empty(pawns = pawns & player.seventh) == false) {
            final int sense = player.sense;

            long lefts = Pawn.lefts(pawns, sense) & rivals & mask;
            long rights = Pawn.rights(pawns, sense) & rivals & mask;
            long singles = Pawn.singles(pawns, taken, sense) & mask;

            while (empty(lefts) == false) {
                final int to = first(lefts);
                final int from = (to ^ 0x8) + 1;
                final long pins = pins(from, taken);
                lefts ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(PROMOTION, QUEEN, from, to);
                    store(PROMOTION, KNIGHT, from, to);
                    store(PROMOTION, BISHOP, from, to);
                    store(PROMOTION, ROOK, from, to);
                }
            }

            while (empty(rights) == false) {
                final int to = first(rights);
                final int from = (to ^ 0x8) - 1;
                final long pins = pins(from, taken);
                rights ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(PROMOTION, QUEEN, from, to);
                    store(PROMOTION, KNIGHT, from, to);
                    store(PROMOTION, BISHOP, from, to);
                    store(PROMOTION, ROOK, from, to);
                }
            }

            while (empty(singles) == false) {
                final int to = first(singles);
                final int from = (to ^ 0x8);
                final long pins = pins(from, taken);
                singles ^= bit(to);

                if (contains(pins, bit(to))) {
                    store(PROMOTION, QUEEN, from, to);
                    store(PROMOTION, KNIGHT, from, to);
                    store(PROMOTION, BISHOP, from, to);
                    store(PROMOTION, ROOK, from, to);
                }
            }
        }
    }


    /**
     * Returns if the king is currently in check.
     */
    private boolean isKingInCheck() {
        return evasions != FULL_BOARD;
    }


    /**
     * Returns if the king is currently in double check.
     */
    private boolean isKingInDoubleCheck() {
        return evasions == EMPTY_BOARD;
    }


    /**
     * Check if the given square is attacked by rival pieces.
     *
     * @param checker       Checker to examine
     * @param taken         Bitboard of occupied checkers
     * @return              If the checker is attacked
     */
    private boolean isAttacked(int checker, long taken) {
        final long rivals = state[1 ^ player.side];
        final long rooks = (state[QUEEN] | state[ROOK]) & rivals;
        final long bishops = (state[QUEEN] | state[BISHOP]) & rivals;
        final long knights = state[KNIGHT] & rivals;
        final long pawns = state[PAWN] & rivals;
        final long king = state[KING] & rivals;

        return
        contains(pawns, Pawn.attacks(checker, player.sense)) ||
        contains(bishops, Bishop.attacks(checker, taken)) ||
        contains(rooks, Rook.attacks(checker, taken)) ||
        contains(knights, Knight.attacks(checker)) ||
        contains(king, King.attacks(checker));
    }


    /**
     * Checks if two spots are attacked by rival pieces.
     *
     * @param spots         Spots array of lenght two
     * @param taken         Bitboard of occupied checkers
     * @return              If the checkers are attacked
     */
    private boolean areAttacked(int[] spots, long taken) {
        return isAttacked(spots[0], taken) || isAttacked(spots[1], taken);
    }


    /**
     * Bitboard of check evasions by pieces other than the king.
     *
     * The returned bitboard contains the checkers from which the attacking
     * pieces can be captured or their attacks blocked. This does not include
     * en-passant captures. Thus, the following statements hold true for the
     * returned evasions mask:
     *
     * a) If the returned bitboard is full the king is not in check.
     * b) If the returned bitboard is empty the king is in double check.
     * c) A piece cannot move to a square not contained on the evasions.
     *    Except fot the en-passant capture if any.
     */
    private long computeEvasions(long[] state, Player player) {
        final long rivals = state[1 ^ player.side];
        final long taken = state[WHITE] | state[BLACK];
        final long rooks = (state[QUEEN] | state[ROOK]) & rivals;
        final long bishops = (state[QUEEN] | state[BISHOP]) & rivals;
        final long knights = state[KNIGHT] & rivals;
        final long pawns = state[PAWN] & rivals;
        final int target = first(state[KING] & ~rivals);

        // Checkers from which to capture a knight or pawn attacker

        long evasions = pawns & Pawn.attacks(target, player.sense);
        evasions |= knights & Knight.attacks(target);

        // If a knight/pawn is attacking return the evasions, or an
        // empty board if the king is in double check

        final long rookAttacks = Rook.attacks(target, taken);
        final long bishopAttacks = Bishop.attacks(target, taken);
        final int bishopCount = count(bishops & bishopAttacks);
        final int rookCount = count(rooks & rookAttacks);
        final int sliderCount = bishopCount + rookCount;

        if (empty(evasions) == false) {
            return (sliderCount > 0) ? EMPTY_BOARD : evasions;
        }

        // If a sliding piece is attacking return a ray of checkers from
        // which to stop the check, or an empty board if in double check

        if (sliderCount > 1) {
            return EMPTY_BOARD;
        } else if (bishopCount > 0) {
            final int to = first(bishopAttacks & bishops);
            return bishopAttacks & Ray.ray(target, to);
        } else if (rookCount > 0) {
            final int to = first(rookAttacks & rooks);
            return rookAttacks & Ray.ray(target, to);
        }

        return FULL_BOARD;
    }


    /**
     * Returns a bitboard mask of possible moves for a pinned piece.
     *
     * If the piece is not pinned this method returns a full board,
     * otherwise the ray of checkers to which the piece is pinned.
     *
     * @param from      Checker were the piece is placed
     * @param taken     Bitboar of occupied checkers
     * @return          Pinned piece bitboard mask
     */
    private long pins(int from, long taken) {
        final long rivals = state[1 ^ player.side];
        final long rooks = (state[QUEEN] | state[ROOK]) & rivals;
        final long bishops = (state[QUEEN] | state[BISHOP]) & rivals;
        final long king = state[KING] & ~rivals;
        final int target = first(king);
        final long ray = Ray.ray(target, from);

        if (empty(ray) == true) {
            return FULL_BOARD;
        }

        final boolean inSameFile = empty((from ^ target) & 7);
        final boolean inSameRank = empty((from ^ target) & 56);

        if (inSameFile || inSameRank) {
            if (contains(rooks, ray)) {
                final long attacks = Rook.attacks(from, taken);
                final boolean isKing = contains(attacks, king);
                final boolean isRook = contains(ray & attacks, rooks);
                return (isKing && isRook) ? ray : FULL_BOARD;
            }
        } else {
            if (contains(bishops, ray)) {
                final long attacks = Bishop.attacks(from, taken);
                final boolean isKing = contains(attacks, king);
                final boolean isBishop = contains(ray & attacks, bishops);
                return (isKing && isBishop) ? ray : FULL_BOARD;
            }
        }

        return FULL_BOARD;
    }


    /**
     * Unpacks a new set of moves on the current slot.
     *
     * @param move      Base move encoding
     * @param targets   Destination checkers bitboard
     */
    private void unpack(int move, long targets) {
        while (empty(targets) == false) {
            int to = first(targets);
            targets ^= bit(to);
            store(move | to);
        }
    }


    /**
     * Appends a new set of flagged moves to the current slot.
     *
     * @param flag      Move type flag
     * @param piece     Piece to place at target
     * @param from      Origin checker
     * @param targets   Destination checkers bitboard
     */
    private void store(int flag, int piece, int from, long targets) {
        if (empty(targets) == false) {
            unpack(flag | (piece << 12) | (from << 6), targets);
        }
    }


    /**
     * Appends a new flagged move to the current slot.
     *
     * @param flag      Move type flag
     * @param piece     Piece to place at target
     * @param from      Origin checker
     * @param to        Destination checker
     */
    private void store(int flag, int piece, int from, int to) {
        store(flag | (piece << 12) | (from << 6) | to);
    }


    /**
     * Appends an encoded move to the current slot.
     *
     * @param move      Move encoding
     */
    private void store(int move) {
        moves[index++] = move;
    }


    /**
     * Inreases the number of slots of this generator.
     *
     * @param size          New slot size
     */
    public void ensureCapacity(int size) {
        if (size > capacity) {
            store = Arrays.copyOf(store, size);

            for (int slot = capacity; slot < size; slot++) {
                store[slot] = new Entry();
            }

            capacity = size;
            System.gc();
        }
    }


    /**
     * An entry on the generated moves store.
     */
    private class Entry {

        /** Number of moves generated */
        int length = 0;

        /** Moves generated on current stage */
        int[] moves = new int[MAX_MOVES];

        /** Next generation stage */
        int nextStage = KING_STAGE;

        /** Current generation stage */
        int currentStage = START_STAGE;

        /** Bitboard of check evasions */
        long evasions = FULL_BOARD;


        /**
         * Check if moves were  generated.
         */
        boolean isCurrentStage(int stage) {
            return stage >= currentStage && stage < nextStage;
        }


        /**
         * Check if no moves were generated for an entry.
         */
        boolean isStartStage() {
            return currentStage == START_STAGE;
        }


        /**
         * Reset this entry to its initial state.
         */
        void clear() {
            currentStage = START_STAGE;
            nextStage = KING_STAGE;
        }
    }
}
