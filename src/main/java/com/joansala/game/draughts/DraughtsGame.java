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

import java.util.Arrays;
import com.joansala.engine.Board;
import com.joansala.engine.base.BaseGame;
import com.joansala.util.hash.ZobristHash;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.draughts.Draughts.*;


/**
 * Reperesents a Draughts game between two players.
 */
public class DraughtsGame extends BaseGame {

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = 0;

    /** Maximum number of plies this object can store */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE / PIECE_COUNT;

    /** Capacity increases at least this value each time */
    private static final int CAPACITY_INCREMENT = 128;

    /** Flags the draw clock as resetable with each capture */
    private static final int RESETABLE = 0x40;

    /** Mobility flag (contains capture moves) */
    private static final long CAPTURES = (0x01L << 63);

    /** Mobility flag (moves were generated) */
    private static final long GENERATED = (0x01L << 62);

    /** Hash code generator */
    private static final ZobristHash hasher = hashFunction();

    /** Moves generator */
    private DraughtsGenerator movegen;

    /** Start position and turn */
    private DraughtsBoard board;

    /** Player to move */
    private Player player;

    /** Player to move opponent */
    private Player rival;

    /** Move generation cursors */
    private int[] cursors;

    /** Draw countdown clock history */
    private int[] clocks;

    /** Last crown or capture history */
    private int[] advances;

    /** Hash code history */
    private long[] hashes;

    /** Board states history */
    private long[] states;

    /** Legal moves history */
    private long[] mobilities;

    /** Current position bitboards */
    private long[] state;

    /** Bitboard of pseudo-legal moves */
    private long mobility;

    /** Bitboard of player pieces */
    private long friends;

    /** Bitboard of rival pieces */
    private long rivals;

    /** Bitboard of unoccupied checkers */
    private long free;

    /** Current move generation cursor */
    private int cursor;

    /** Draw countdown clock */
    private int clock;

    /** Index of the last capture or crown move */
    private int advance;


    /**
     * Instantiate a new game on the start state.
     */
    public DraughtsGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Instantiate a new game on the start state.
     *
     * @param capacity      Initial capacity
     */
    public DraughtsGame(int capacity) {
        super(capacity);
        movegen = new DraughtsGenerator();
        advances = new int[capacity];
        clocks = new int[capacity];
        cursors = new int[capacity];
        hashes = new long[capacity];
        mobilities = new long[capacity];
        states = new long[capacity << 2];
        setBoard(new DraughtsBoard());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int turn() {
        return player.turn;
    }


    /**
     * Bitboard of rival pieces.
     */
    protected long rivals() {
        return rivals;
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
        setBoard((DraughtsBoard) board);
    }


    /**
     * {@see #setBoard(Board)}
     */
    public void setBoard(DraughtsBoard board) {
        this.index = -1;
        this.advance = -1;
        this.board = board;
        this.move = NULL_MOVE;
        this.state = board.position();

        setTurn(board.turn());
        this.clock = board.clock() | RESETABLE;
        this.clock = Math.min(clock, nextClock());
        this.hash = computeHash();
        computeMobility();
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
            updateBitboards();
        } else {
            player = Player.NORTH;
            rival = Player.SOUTH;
            updateBitboards();
        }
    }


    /**
     * Toggles the player to move.
     */
    private void switchTurn() {
        Player player = this.player;
        this.player = rival;
        this.rival = player;
        updateBitboards();
    }


    /**
     *
     */
    private void updateBitboards() {
        friends = state[player.man] | state[player.king];
        rivals = state[rival.man] | state[rival.king];
        free = BOARD_BITS ^ friends ^ rivals;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DraughtsBoard toBoard() {
        return new DraughtsBoard(state, player.turn, clock & 0x3F);
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
        return (int) (2.0 * score);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnded() {
        return cannotMove() || isStagnant() || isRepetition();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        return cannotMove() ? rival.turn * MAX_SCORE : DRAW_SCORE;
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
        cursor = cannotMove() ? 0 : 1;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void makeMove(int move) {
        pushState();
        movePieces(move);
        switchTurn();
        computeMobility();
        resetCursor();
        this.move = move;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMove() {
        popState();
        switchTurn();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int nextMove() {
        if (cursor == 0) {
            return NULL_MOVE;
        }

        generateMoves(1 + index);
        int move = movegen.getMove(1 + index, cursor);
        cursor = (move == NULL_MOVE) ? 0 : 1 + cursor;

        return move;
    }


    /**
     * Generates legal moves for the current slot.
     *
     * @param slot      Current ply
     */
    private void generateMoves(int slot) {
        if (empty(mobility & GENERATED)) {
            final int sense = player.sense;
            final long kings = state[player.king];
            movegen.generate(slot, sense, mobility, free, rivals, kings);
            mobility ^= GENERATED;
        }
    }


    /**
     * Traces a path of captures for the given move.
     *
     * @param move      Move encoding
     * @return          Traveled checker indices
     */
    protected int[] traceCaptures(int move) {
        final int sense = player.sense;
        final long kings = state[player.king];
        return movegen.trace(move, sense, mobility, free, rivals, kings);
    }


    /**
     * Plays a move on the board.
     *
     * @param move      Move to perform
     */
    private void movePieces(int move) {
        final long bases = BASES_BITS[player.man];
        final long men = state[player.man];

        final int to = move & 0x3F;
        final int from = (move >> 6) & 0x3F;
        final int capture = (move >> 12);

        // Capture any rival pieces

        if (capture > 0) {
            generateMoves(index);
            long remnants = movegen.getRemnants(index, capture);
            capture(rival.man, remnants);
            capture(rival.king, remnants);
            registerAdvance();
        }

        // Move the piece and crown

        if (empty(men & bit(from))) {
            move(player.king, from, to);
        } else if (empty(bases & bit(to))) {
            move(player.man, from, to);
        } else {
            crown(player.man, player.king, from, to);
            registerAdvance();
        }

        // Toggle the hash code turn

        this.hash ^= HASH_SIGN[player.man];
        this.hash ^= HASH_SIGN[rival.man];

        // Update or reset the draw countdown clock

        if (empty(clock & RESETABLE)) {
            clock--;
        } else if (capture > 0) {
            clock = nextClock();
        } else if (contains(men, bit(from))) {
            clock = 50 | RESETABLE;
        } else {
            clock--;
        }
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
        this.hash = hasher.remove(hash, from, piece);
        this.hash = hasher.insert(hash, to, piece);
    }


    /**
     * Moves a man from a checker and crowns it.
     *
     * @param man       Man piece identifier
     * @param king      King piece identifier
     * @param from      Origin checker
     * @param to        Base checker
     */
    private void crown(int man, int king, int from, int to) {
        state[man] ^= bit(from);
        state[king] ^= bit(to);
        this.hash = hasher.remove(hash, from, man);
        this.hash = hasher.insert(hash, to, king);
    }


    /**
     * Captures a set of pieces.
     *
     * @param piece     Piece identifier
     * @param remnants  Pieces not captured
     */
    private void capture(int piece, long remnants) {
        long captures = state[piece] & ~remnants;
        state[piece] &= remnants;

        while (empty(captures) == false) {
            final int index = first(captures);
            this.hash = hasher.remove(hash, index, piece);
            captures ^= bit(index);
        }
    }


    /**
     * Bitboard of pseudo-legal moves for the current player.
     *
     * If any capture is possible returns a bitboard of player pieces
     * that may be able to jump over other pieces on the current turn;
     * otherwise returns a bitboard of pieces that can move without
     * jumping over rival pieces. If the first bit of the bitboard is
     * set it means that it is a bitboard of captures.
     */
    private void computeMobility() {
        if (CAPTURES == (mobility = computeJumps())) {
            mobility = computeSlides();
        }
    }


    /**
     * Bitboard of pieces that can jump over rival pieces.
     *
     * @return      Legal capture moves bitboard
     */
    private long computeJumps() {
        long moves = CAPTURES;

        // Forward and backward king capture moves

        final long kings = state[player.king];

        if (empty(kings) == false) {
            moves |= kings & shift(rays(rivals, free, NW), NW);
            moves |= kings & shift(rays(rivals, free, NE), NE);
            moves |= kings & shift(rays(rivals, free, SW), SW);
            moves |= kings & shift(rays(rivals, free, SE), SE);
        }

        // Forward and backward men capture moves

        final long men = state[player.man];

        if (empty(men) == false) {
            moves |= men & shift(rivals & shift(free, NW), NW);
            moves |= men & shift(rivals & shift(free, NE), NE);
            moves |= men & shift(rivals & shift(free, SW), SW);
            moves |= men & shift(rivals & shift(free, SE), SE);
        }

        return moves;
    }


    /**
     * Bitboard of pieces that can move without jumping.
     *
     * @return      Legal capture moves bitboard
     */
    private long computeSlides() {
        long moves = 0x00L;

        // Backward and forward king non-capturing moves

        final long kings = state[player.king];

        if (empty(kings) == false) {
            moves |= kings & shift(free, NW);
            moves |= kings & shift(free, NE);
            moves |= kings & shift(free, SW);
            moves |= kings & shift(free, SE);
        }

        // Forward men non-capturing moves

        final long men = state[player.man];

        if (empty(men) == false) {
            moves |= men & shift(free, SW ^ player.sense);
            moves |= men & shift(free, SE ^ player.sense);
        }

        return moves;
    }


    /**
     * Projects a set of pieces on the given direction.
     */
    private long rays(long pieces, long mask, int direction) {
        long rays = pieces & shift(mask, direction);

        for (int rank = 0; rank < BOARD_RANKS - 3; rank++) {
            rays |= mask & shift(rays, direction);
        }

        return rays;
    }


    /**
     * Updates the last advance index.
     */
    private void registerAdvance() {
        this.advance = index;
    }


    /**
     * Check if the current player does not have legal moves.
     *
     * @return      If the player cannot move
     */
    private boolean cannotMove() {
        return empty(mobility);
    }


    /**
     * Checks if the match has made no progress.
     *
     * + 25 moves without a capture or men move.
     * + 16 moves with (1K) vs (3K | 2K+1m | 1K+2m).
     * +  5 moves with (1K) vs (2K | 1K+1m | 1K).
     *
     * @return      If no progress is being made
     */
    private boolean isStagnant() {
        return (clock & 0x3F) == 0;
    }


    /**
     * Checks if the same position and turn occurred three times.
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
     * Computes the next draw cowntdown clock.
     * @see #isStagnant
     *
     * @return      Clock value
     */
    private int nextClock() {
        if (count(state[rival.king]) == 1) {
            final int k = count(state[player.king]);
            final int c = k + count(state[player.man]);

            if (k >= 1 && c <= 3) {
                return (c > 2) ? 32 : 10;
            }
        }

        return 50 | RESETABLE;
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
        clocks[index] = clock;
        advances[index] = advance;
        System.arraycopy(state, 0, states, index << 2, PIECE_COUNT);
    }


    /**
     * Restore game state from the history.
     */
    private void popState() {
        System.arraycopy(states, index << 2, state, 0, PIECE_COUNT);
        advance = advances[index];
        clock = clocks[index];
        cursor = cursors[index];
        mobility = mobilities[index];
        hash = hashes[index];
        move = moves[index];
        index--;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected long computeHash() {
        long hash = HASH_SIGN[player.man];

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
            clocks = Arrays.copyOf(clocks, size);
            cursors = Arrays.copyOf(cursors, size);
            hashes = Arrays.copyOf(hashes, size);
            mobilities = Arrays.copyOf(mobilities, size);
            states = Arrays.copyOf(states, size << 4);
            capacity = size;

            System.gc();
        }
    }


    /**
     * Initialize the hash code generator.
     */
    private static ZobristHash hashFunction() {
        return new ZobristHash(RANDOM_SEED, PIECE_COUNT, 5 + BOARD_SIZE);
    }
}
