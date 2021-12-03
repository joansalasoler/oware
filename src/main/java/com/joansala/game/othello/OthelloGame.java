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

import com.joansala.engine.Board;
import com.joansala.engine.base.BaseGame;
import com.joansala.util.hash.ZobristHash;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.othello.Othello.*;


/**
 * Reperesents a Othello game between two players.
 */
public class OthelloGame extends BaseGame {

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = 0;

    /** Player fortfeits its turn */
    public static final int FORFEIT_MOVE = BOARD_SIZE;

    /** Capacity of this game object */
    private static final int CAPACITY = 2 * BOARD_SIZE;

    /** Hash code generator */
    private static final ZobristHash hasher = hashFunction();

    /** Start position and turn */
    private OthelloBoard board;

    /** Move turns history */
    private int[] turns;

    /** Move generation cursors */
    private int[] cursors;

    /** Hash code history */
    private long[] hashes;

    /** Board states history */
    private long[] states;

    /** Legal moves history */
    private long[] mobilities;

    /** Current position bitboards */
    private long[] state;

    /** Bitboard of legal moves */
    private long mobility;

    /** Set when no player can move */
    private boolean stagnant;

    /** Current player color */
    private int player;

    /** Current opponent color */
    private int rival;

    /** Current move generation cursor */
    private int cursor;


    /**
     * Instantiate a new game on the start state.
     */
    public OthelloGame() {
        super(CAPACITY);
        turns = new int[CAPACITY];
        cursors = new int[CAPACITY];
        hashes = new long[CAPACITY];
        mobilities = new long[CAPACITY];
        states = new long[CAPACITY << 1];
        setBoard(new OthelloBoard());
    }


    /**
     * Initialize the hash code generator.
     */
    private static ZobristHash hashFunction() {
        return new ZobristHash(RANDOM_SEED, PIECE_COUNT, BOARD_SIZE);
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
        setBoard((OthelloBoard) board);
    }


    /**
     * {@see #setBoard(Board)}
     */
    public void setBoard(OthelloBoard board) {
        this.index = -1;
        this.board = board;
        this.move = NULL_MOVE;
        this.stagnant = false;
        this.state = board.position();

        setTurn(board.turn());
        this.hash = computeHash();
        computeMobility();
        resetCursor();
    }


    /**
     * Sets the current player to move.
     *
     * @param turn      {@code SOUTH} or {@code NORTH}
     */
    protected void setTurn(int turn) {
        this.turn = turn;

        if (turn == SOUTH) {
            player = SOUTH_STONE;
            rival = NORTH_STONE;
        } else {
            player = NORTH_STONE;
            rival = SOUTH_STONE;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public OthelloBoard toBoard() {
        return new OthelloBoard(state, turn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLegal(int move) {
        return contains(mobility, bit(move)) || move == FORFEIT_MOVE;
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
        return (int) (6.4 * score);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnded() {
        return stagnant;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        final int south = count(state[SOUTH_STONE]);
        final int north = count(state[NORTH_STONE]);
        if (south < north) return -MAX_SCORE;
        if (south > north) return MAX_SCORE;
        return DRAW_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int score() {
        return 0;
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
        if (stagnant) {
            cursor = NULL_MOVE;
        } else if (empty(mobility)) {
            cursor = FORFEIT_MOVE;
        } else {
            cursor = BOARD_SIZE - 1;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void makeMove(int move) {
        pushState();
        movePieces(move);
        setTurn(-turn);
        computeMobility();
        this.move = move;
        resetCursor();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMove() {
        popState(index);
        index--;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMoves(int length) {
        if (length > 0) {
            index -= length;
            popState(1 + index);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int nextMove() {
        if (cursor == FORFEIT_MOVE) {
            cursor = NULL_MOVE;
            return FORFEIT_MOVE;
        }

        while (cursor >= 0) {
            if (contains(mobility, bit(cursor))) {
                return cursor--;
            } else {
                cursor--;
            }
        }

        return NULL_MOVE;
    }


    /**
     * Places a stone of the current player on the given checker and
     * flips the resulting captured stones if any.
     *
     * @param move       Checker where to place the stone
     */
    private void movePieces(int move) {
        final long checker = bit(move);
        final long rivals = state[rival];
        final long players = state[player];

        // Toggle the hash sign

        hash ^= HASH_SIGN[rival];
        hash ^= HASH_SIGN[player];

        // Player may have forfeit the turn

        if (move == FORFEIT_MOVE) {
            return;
        }

        // Find all the stones that must be flipped

        long captures = 0x00L;

        for (int direction = 0; direction < 8; direction++) {
            final long rays = rays(rivals, checker, direction);

            if ((players & shiftd(rays, direction)) != 0L) {
                captures |= rays;
            }
        }

        // Update the checkerboards

        state[player] ^= checker;
        state[player] ^= captures;
        state[rival] ^= captures;

        // Update the Zobrist hash

        hash = hasher.insert(hash, move, player);

        while (empty(captures) == false) {
            final int index = first(captures);
            hash = hasher.remove(hash, index, rival);
            hash = hasher.insert(hash, index, player);
            captures ^= bit(index);
        }
    }


    /**
     * Bitboard of legal moves for the current player.
     */
    private void computeMobility() {
        stagnant = false;

        if (empty(mobility = computeMobility(player, rival))) {
            stagnant = empty(computeMobility(rival, player));
        }
    }


    /**
     * Bitboard of legal moves for the given player.
     */
    private long computeMobility(int player, int rival) {
        final long rivals = state[rival];
        final long players = state[player];
        final long free = ~(players | rivals);

        long mobility = 0x00L;

        for (int direction = 0; direction < 8; direction++) {
            final long rays = rays(rivals, players, direction);
            mobility |= free & shiftd(rays, direction);
        }

        return mobility;
    }


    /**
     * Projects a set of pieces on the given direction.
     */
    private long rays(long pieces, long mask, int direction) {
        long rays = pieces & shiftd(mask, direction);

        for (int rank = 0; rank < BOARD_RANKS - 3; rank++) {
            rays |= pieces & shiftd(rays, direction);
        }

        return rays;
    }


    /**
     * Store game state on the history.
     */
    private void pushState() {
        index++;
        moves[index] = move;
        hashes[index] = hash;
        mobilities[index] = mobility;
        cursors[index] = cursor;
        turns[index] = turn;
        System.arraycopy(state, 0, states, index << 1, PIECE_COUNT);
    }


    /**
     * Retrieve the current game state from the history.
     */
    private void popState(int index) {
        System.arraycopy(states, index << 1, state, 0, PIECE_COUNT);
        setTurn(turns[index]);
        cursor = cursors[index];
        mobility = mobilities[index];
        hash = hashes[index];
        move = moves[index];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected long computeHash() {
        long hash = HASH_SIGN[player];

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
     * Shifts a bitboard on the given direction.
     *
     * @param bitboard      Bitboard to shift
     * @param direction     Direction identifier
     *
     * @return              Shifted bitboard
     */
    private long shiftd(long bitboard, int direction) {
        final int n = DIRECTION_SHIFT[direction];
        final long mask = DIRECTION_MASK[direction];
        return mask & shift(bitboard, n);
    }
}
