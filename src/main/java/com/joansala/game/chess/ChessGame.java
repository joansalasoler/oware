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
import com.joansala.engine.Board;
import com.joansala.engine.base.BaseGame;
import com.joansala.util.hash.ZobristHash;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.chess.Chess.*;
import static com.joansala.game.chess.ChessGenerator.*;


/**
 * Reperesents a Chess game between two players.
 */
public class ChessGame extends BaseGame {

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = 0;

    /** Maximum number of plies this object can store */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE / STATE_SIZE;

    /** Capacity increases at least this value each time */
    private static final int CAPACITY_INCREMENT = 128;

    /** Hash code generator */
    private static final ZobristHash hasher = hashFunction();

    /** Moves generator */
    private ChessGenerator movegen;

    /** Start position and turn */
    private ChessBoard board;

    /** Player to move */
    private Player player;

    /** Player to move opponent */
    private Player rival;

    /** Move generation cursors */
    private int[] cursors;

    /** Last pawn move or capture history */
    private int[] advances;

    /** Hash code history */
    private long[] hashes;

    /** Board states history */
    private long[] states;

    /** Current position bitboards */
    private long[] state;

    /** Current move generation cursor */
    private int cursor;

    /** Index of the last advance move */
    private int advance;


    /**
     * Instantiate a new game on the start state.
     */
    public ChessGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Instantiate a new game on the start state.
     *
     * @param capacity      Initial capacity
     */
    public ChessGame(int capacity) {
        super(capacity);
        computeCastleHashes();
        movegen = new ChessGenerator();
        advances = new int[capacity];
        cursors = new int[capacity];
        hashes = new long[capacity];
        states = new long[capacity * STATE_SIZE];
        setBoard(new ChessBoard());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int turn() {
        return player.turn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Board getBoard() {
        return board;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setBoard(Board board) {
        setBoard((ChessBoard) board);
    }


    /**
     * {@see #setBoard(Board)}
     */
    public void setBoard(ChessBoard board) {
        this.index = -1;
        this.advance = -1;
        this.board = board;
        this.move = NULL_MOVE;
        this.state = board.position();

        setTurn(board.turn());
        movegen.clear(1 + index);
        hash = computeHash();
        resetCursor();
    }


    /**
     * Sets the current player to move.
     *
     * @param turn      {@code SOUTH} or {@code NORTH}
     */
    private void setTurn(int turn) {
        if (turn == SOUTH) {
            player = Player.SOUTH;
            rival = Player.NORTH;
        } else {
            player = Player.NORTH;
            rival = Player.SOUTH;
        }
    }


    /**
     * Toggles the player to move.
     */
    private void switchTurn() {
        Player player = this.player;
        this.player = rival;
        this.rival = player;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChessBoard toBoard() {
        int turn = player.turn;
        int clock = index - advance;
        int fullmove = board.fullmove() + length() / 2;
        return new ChessBoard(state, turn, clock, fullmove);
    }


    /**
     * Returns the current game bitboards.
     *
     * @return      Bitboards array reference
     */
    protected long[] state() {
        return state;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int contempt() {
        return CONTEMPT_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int toCentiPawns(int score) {
        return (int) (15.0 * score);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnded() {
        return isStagnant() || isRepetition() || cannotMove();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        final boolean inCheck = movegen.isInCheck(state, player);
        return inCheck ? rival.turn * MAX_SCORE : DRAW_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int score() {
        int score = 0;

        final long white = state[WHITE];
        score += BISHOP_WEIGHT * count(white & state[BISHOP]);
        score += KNIGHT_WEIGHT * count(white & state[KNIGHT]);
        score += PAWN_WEIGHT * count(white & state[PAWN]);
        score += QUEEN_WEIGHT * count(white & state[QUEEN]);
        score += ROOK_WEIGHT * count(white & state[ROOK]);

        final long black = state[BLACK];
        score -= BISHOP_WEIGHT * count(black & state[BISHOP]);
        score -= KNIGHT_WEIGHT * count(black & state[KNIGHT]);
        score -= PAWN_WEIGHT * count(black & state[PAWN]);
        score -= QUEEN_WEIGHT * count(black & state[QUEEN]);
        score -= ROOK_WEIGHT * count(black & state[ROOK]);

        return score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getCursor() {
        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void resetCursor() {
        this.cursor = UNGENERATED;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void makeMove(int move) {
        pushState();
        movePieces(move);
        switchTurn();
        resetCursor();
        movegen.clear(1 + index);
        this.move = move;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMove() {
        popState(index);
        switchTurn();
        index--;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMoves(int length) {
        if (length > 0) {
            index -= length;
            setTurn((length & 1) == 0 ? turn() : -turn());
            popState(1 + index);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int nextMove() {
        if (cursor != NULL_MOVE) {
            movegen.generate(1 + index, cursor, state, player);
            cursor = movegen.nextCursor(1 + index, cursor);
            return movegen.getMove(cursor);
        }

        return NULL_MOVE;
    }


    /**
     * Plays a move on the board.
     *
     * @param move      Move to perform
     */
    private void movePieces(int move) {
        final int piece = (move >> 12) & 0x7;
        final int from = (move >> 6) & 0x3F;
        final int to = (move) & 0x3F;

        hash ^= rival.sign;
        hash ^= player.sign;
        hash ^= state[FLAGS];

        clearEnPassant();

        if (CASTLING == (move & CASTLING)) {
            clearCastlings();
            castle(move);
            return;
        }

        if (contains(move, ENPASSANT)) {
            move(PAWN, from, to);
            capture(PAWN, to ^ 0x8);
            registerAdvance();
            return;
        }

        if (contains(state[rival.side], bit(to))) {
            final int capture = captured(to);
            capture(capture, to);
            registerAdvance();

            if (capture == ROOK) {
                updateCastlings(from, to);
            }
        }

        if (contains(move, PROMOTION)) {
            promote(piece, from, to);
            registerAdvance();
            return;
        }

        move(piece, from, to);

        if (piece == PAWN) {
            updateEnPassant(from, to);
            registerAdvance();
        } else if (piece == ROOK) {
            updateCastlings(from, to);
        } else if (piece == KING) {
            clearCastlings();
        }
    }


    /**
     * Captures a piece from a checker.
     *
     * @param piece     Piece identifier
     * @param to        Destination checker
     */
    private void capture(int piece, int to) {
        state[piece] ^= bit(to);
        state[rival.side] ^= bit(to);
        hash = hasher.remove(hash, to, rival.side);
        hash = hasher.remove(hash, to, piece);
    }


    /**
     * Moves a piece from a checker to another checker.
     *
     * @param piece     Piece identifier
     * @param from      Origin checker
     * @param to        Destination checker
     */
    private void move(int piece, int from, int to) {
        state[piece] ^= bit(from);
        state[piece] ^= bit(to);
        state[player.side] ^= bit(from);
        state[player.side] ^= bit(to);
        hash = hasher.toggle(hash, from, to, player.side);
        hash = hasher.toggle(hash, from, to, piece);
    }


    /**
     * Promotes a pawn to a piece on a checker.
     *
     * @param piece     Promoted piece
     * @param from      Origin checker
     * @param to        Destination checker
     */
    private void promote(int piece, int from, int to) {
        state[PAWN] ^= bit(from);
        state[piece] ^= bit(to);
        state[player.side] ^= bit(from);
        state[player.side] ^= bit(to);
        hash = hasher.toggle(hash, from, to, player.side);
        hash = hasher.remove(hash, from, PAWN);
        hash = hasher.insert(hash, to, piece);
    }


    /**
     * Castle a player's king.
     *
     * @param move      Castling move
     */
    private void castle(int move) {
        for (Castle castle : player.castlings) {
            if (move == castle.move) {
                state[KING] ^= castle.kings;
                state[ROOK] ^= castle.rooks;
                state[player.side] ^= castle.kings;
                state[player.side] ^= castle.rooks;
                hash ^= castle.hash;
                break;
            }
        }
    }


    /**
     * Obtain the captured piece on the given bitboard index.
     */
    private int captured(int checker) {
        final long target = bit(checker);

        for (int piece = PAWN; piece > KING; piece--) {
            if (contains(state[piece], target)) {
                return piece;
            }
        }

        return -1;
    }


    /**
     * Updates the last advance index.
     */
    private void registerAdvance() {
        this.advance = index;
    }


    /**
     * Remove the active en-passant capture target.
     */
    private void clearEnPassant() {
        state[FLAGS] &= CASTLE_MASK;
        hash ^= state[FLAGS];
    }


    /**
     * Remove castling rights from the player.
     */
    private void clearCastlings() {
        state[FLAGS] &= ~(state[ROOK] & state[player.side]);
        hash ^= state[FLAGS];
    }


    /**
     * Update the en-passant capture target after a pawn move.
     *
     * @param from      Pawn origin checker
     * @param to        Pawn destination checker
     */
    private void updateEnPassant(int from, int to) {
        state[FLAGS] |= bit(from ^ 0x18) & bit(to ^ 0x8);
        hash ^= state[FLAGS];
    }


    /**
     * Update castling rights after a rook is moved or captured.
     *
     * @param from      Rook origin checker
     * @param to        Rook destination checker
     */
    private void updateCastlings(int from, int to) {
        state[FLAGS] &= ~(CASTLE_MASK & (bit(from) | bit(to)));
        hash ^= state[FLAGS];
    }


    /**
     * Check if the current player does not have legal moves.
     *
     * @return      If the player cannot move
     */
    private boolean cannotMove() {
        return movegen.cannotMove(1 + index, state, player);
    }


    /**
     * Checks if the match has made no progress.
     *
     * @return      If no progress is being made
     */
    private boolean isStagnant() {
        return (index - advance) >= 100;
    }


    /**
     * Checks if the same state occurred three times.
     *
     * @return      If a threefold repetition occurred
     */
    private boolean isRepetition() {
        boolean found = false;

        for (int n = index; n > advance; n -= 2) {
            if (hashes[n] == this.hash) {
                if (found) return true;
                found = true;
            }
        }

        return false;
    }


    /**
     * Store game state on the history.
     */
    private void pushState() {
        index++;
        moves[index] = move;
        hashes[index] = hash;
        cursors[index] = cursor;
        advances[index] = advance;
        System.arraycopy(state, 0, states, index * STATE_SIZE, STATE_SIZE);
    }


    /**
     * Restore game state from the history.
     */
    private void popState(int index) {
        System.arraycopy(states, index * STATE_SIZE, state, 0, STATE_SIZE);
        advance = advances[index];
        cursor = cursors[index];
        hash = hashes[index];
        move = moves[index];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected long computeHash() {
        long hash = player.sign ^ state[FLAGS];

        for (int piece = 0; piece < PIECE_COUNT; piece++) {
            long pieces = state[piece];

            while (empty(pieces) == false) {
                final int index = first(pieces);
                hash = hasher.insert(hash, index, piece);
                pieces ^= bit(index);
            }
        }

        return hash;
    }


    /**
     * Precomutes the hash codes of each castle move.
     */
    private void computeCastleHashes() {
        WHITE_SHORT.hash = hashCastle(WHITE_SHORT, WHITE);
        BLACK_SHORT.hash = hashCastle(BLACK_SHORT, BLACK);
        WHITE_LONG.hash = hashCastle(WHITE_LONG, WHITE);
        BLACK_LONG.hash = hashCastle(BLACK_LONG, BLACK);
    }


    /**
     * Compute a hash code for a castle move.
     *
     * @param castle    Castle move
     * @param side      Side to move
     * @return          Hash code
     */
    private long hashCastle(Castle castle, int side) {
        long hash = 0x00L;
        long kings = castle.kings;
        long rooks = castle.rooks;

        while (empty(kings) == false) {
            int checker = first(kings);
            hash = hasher.insert(hash, checker, KING);
            hash = hasher.insert(hash, checker, side);
            kings ^= bit(checker);
        }

        while (empty(rooks) == false) {
            int checker = first(rooks);
            hash = hasher.insert(hash, checker, ROOK);
            hash = hasher.insert(hash, checker, side);
            rooks ^= bit(checker);
        }

        return hash;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureCapacity(int size) {
        if (size > this.capacity) {
            size = Math.max(size, capacity + CAPACITY_INCREMENT);
            size = Math.min(MAX_CAPACITY, size);

            movegen.ensureCapacity(1 + size);
            moves = Arrays.copyOf(moves, size);
            advances = Arrays.copyOf(advances, size);
            cursors = Arrays.copyOf(cursors, size);
            hashes = Arrays.copyOf(hashes, size);
            states = Arrays.copyOf(states, size * STATE_SIZE);
            capacity = size;

            System.gc();
        }
    }


    /**
     * Initialize the hash code generator.
     */
    private static ZobristHash hashFunction() {
        return new ZobristHash(RANDOM_SEED, PIECE_COUNT, BOARD_SIZE);
    }
}
