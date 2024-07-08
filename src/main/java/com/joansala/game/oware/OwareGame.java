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

import java.util.Arrays;
import com.joansala.engine.Board;
import com.joansala.engine.Scorer;
import com.joansala.engine.base.BaseGame;
import com.joansala.game.oware.Oware.Player;
import com.joansala.game.oware.scorers.OutcomeScorer;
import com.joansala.game.oware.scorers.PositionalScorer;
import com.joansala.util.hash.HashFunction;
import static com.joansala.game.oware.Oware.*;


/**
 * Reperesents an oware abapa game between two players.
 *
 * @author    Joan Sala Soler
 * @version   2.0.0
 */
public class OwareGame extends BaseGame {

    /** The maximum number of moves this object can store */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE >> 4;

    /** Capacity increases at least this value each time */
    private static final int CAPACITY_INCREMENT = 126;

    /** Moves that perform some capture */
    private static final int ATTACKING_MOVES = 0;

    /** Moves that feed the opponent player */
    private static final int MANDATORY_MOVES = 1;

    /** Moves from houses that are at risk of capture */
    private static final int DEFENSIVE_MOVES = 2;

    /** Moves that are not attacks, defenses or mandatory */
    private static final int REMAINING_MOVES = 3;

    /** Utility evaluation function (for leaf nodes) */
    private static final Scorer<OwareGame> outcome = outcomeFunction();

    /** Heuristic evaluation function (for interior nodes) */
    private static final Scorer<OwareGame> scorer = scoreFunction();

    /** Hash code generator */
    private static final HashFunction hasher = hashFunction();

    /** Start position and turn */
    private OwareBoard board;

    /** Player to move */
    private Player player;

    /** Player to move opponent */
    private Player rival;

    /** Hash of the last detected repetition */
    private long repetition;

    /** Index of the last capture move */
    private int capture;

    /** Bitmask of empty houses */
    private int empty;

    /** Current move generation stage */
    private int stage;

    /** Current move generation house */
    private int next;

    /** Position and move generation state */
    private int[] state;

    /** Last capture move history */
    private int[] captures;

    /** Empty houses history */
    private int[] empties;

    /** Board states history */
    private int[] states;

    /** Hash code history */
    private long[] hashes;


    /**
     * Instantiate a new game on the start state.
     */
    public OwareGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Instantiate a new game on the start state.
     *
     * @param capacity      Initial capacity
     */
    public OwareGame(int capacity) {
        super(capacity);
        hashes = new long[capacity];
        captures = new int[capacity];
        empties = new int[capacity];
        states = new int[capacity << 4];
        setBoard(new OwareBoard());
        setTurn(SOUTH);
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
        setBoard((OwareBoard) board);
    }


    /**
     * {@see #setBoard(Board)}
     */
    public void setBoard(OwareBoard board) {
        this.index = -1;
        this.capture = -1;
        this.move = NULL_MOVE;

        setTurn(board.turn());
        resetCursor();

        int[] position = board.position();

        this.state = Arrays.copyOf(position, 4 + BOARD_SIZE);
        this.empty = computeEmpty();
        this.hash = computeHash();
        this.board = board;
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
     * Store game state on the history.
     */
    private void pushState() {
        index++;
        moves[index] = move;
        captures[index] = capture;
        hashes[index] = hash;
        empties[index] = empty;
        state[POSITION_SIZE] = next;
        state[3 + BOARD_SIZE] = stage;
        System.arraycopy(state, 0, states, index << 4, 4 + BOARD_SIZE);
    }


    /**
     * Restore game state from the history.
     */
    private void popState(int index) {
        System.arraycopy(states, index << 4, state, 0, 4 + BOARD_SIZE);
        stage = state[3 + BOARD_SIZE];
        next = state[POSITION_SIZE];
        empty = empties[index];
        hash = hashes[index];
        capture = captures[index];
        move = moves[index];
    }


    /**
     * Obtain the current bitboard value on the given index.
     *
     * @return      Bitboard value
     */
    public final int state(int index) {
        return state[index];
    }


    /**
     * Returns the current game state.
     *
     * @return      Current game state reference
     */
    protected int[] state() {
        return state;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int contempt() {
        return Oware.CONTEMPT_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public OwareBoard toBoard() {
        int[] position = Arrays.copyOf(state, POSITION_SIZE);
        return new OwareBoard(position, player.turn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int toCentiPawns(int score) {
        return (int) (2.5 * score);
    }


    /**
     * Sets the internal state to an endgame position.
     */
    @Override
    public void endMatch() {
        if (!hasLegalMoves() || isRepetition()) {
            pushState();
            gatherSeeds();
            hash = computeHash();
            move = NULL_MOVE;
        }
    }


    /**
     * Checks if the game has ended on the current position.
     *
     * @return  {@code true} if the game ended
     */
    @Override
    public boolean hasEnded() {
        return state[SOUTH_STORE] > SEED_GOAL ||
               state[NORTH_STORE] > SEED_GOAL ||
               isRepetition() || !hasLegalMoves();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int winner() {
        if (state[SOUTH_STORE] > SEED_GOAL) {
            return SOUTH;
        }

        if (state[NORTH_STORE] > SEED_GOAL) {
            return NORTH;
        }

        int seeds = state[SOUTH_STORE];

        for (int house = SOUTH_LEFT; house <= SOUTH_RIGHT; house++) {
            seeds += state[house];
        }

        if (seeds > SEED_GOAL) {
            return SOUTH;
        }

        if (seeds < SEED_GOAL) {
            return NORTH;
        }

        return DRAW;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        return outcome.evaluate(this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int score() {
        return scorer.evaluate(this);
    }


    /**
     * Computes a unique hash code for the current position.
     *
     * <p>The method used to compute the hash is that of binomial
     * coefficients, which gives a perfect minimal hash for the position.
     * The first bit represents the player to move.</p>
     *
     * @return      Unique hash code
     */
    @Override
    protected long computeHash() {
        return player.sign + hasher.hash(state);
    }


    /**
     * Computes the empty houses mask for the current position. Each
     * bit set represents an empty house on the given index.
     *
     * @return      Mask of empty houses
     */
    private int computeEmpty() {
        int empty = 0x00;

        for (int house = 0; house < BOARD_SIZE; house++) {
            if (state[house] == 0) {
                empty |= (1 << house);
            }
        }

        return empty;
    }


    /**
     * Current move generation cursor.
     *
     * @return  Cursor value
     */
    @Override
    public int getCursor() {
        return ((next << 2) | stage);
    }


    /**
     * Sets the move generation cursor.
     *
     * @param   New cursor
     */
    @Override
    public void setCursor(int cursor) {
        stage = (cursor & 0x03);
        next = (cursor >> 2);
    }


    /**
     * Resets the move generation cursor.
     */
    @Override
    public void resetCursor() {
        stage = ATTACKING_MOVES;
        next = 1 + player.right;
    }


    /**
     * Checks if at least one legal move can be performed for the
     * current position and turn
     *
     * @return  {@code true} if a legal move can be performed for the
     *          player to move or {@code false} otherwise
     */
    public boolean hasLegalMoves() {
        if (!playerHasSeeds()) {
            return false;
        }

        if (rivalHasSeeds()) {
            return true;
        }

        for (int move = player.right; move >= player.left; move--) {
            if (feedsRival(move)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Checks if the current position is a move repetition. A move is
     * a repetition if this {@code OwareGame} object move history contains
     * a move performed by the current player that lead to a position
     * equal to the current position.
     *
     * @return  {@code true} if the last performed move lead to a
     *          position repetition; {@code false} otherwise
     */
    public boolean isRepetition() {
        for (int n = index - BOARD_SIZE + 1; n > capture; n -= 2) {
            if (hashes[n] == hash) {
                repetition = hash;
                return true;
            }
        }

        return false;
    }


    /**
     * Returns if a repetition was found on the current position.
     *
     * Provides a fast way to check if the current game situation has
     * already happened before. If {@link #isRepetition()} was invoked
     * it is more efficient to call this instead of checking the history
     * of positions again. This method will only work correctly if
     * {@link #isRepetition()} has already been called (this happens
     * automatically when checking {@link #hasEnded()}).
     *
     * @return  if the current position is a repetition
     */
    public boolean foundRepetition() {
        return hash == repetition;
    }


    /**
     * Check if a move sows on the rival houses.
     *
     * @param move  A move on the current position
     * @return      If it feeds the opponent
     */
    private boolean feedsRival(int move) {
        return (move + state[move] > player.right);
    }


    /**
     * Check if a move sows one or two seeds.
     *
     * @param move  A move on the current position
     * @return      If it feeds the opponent
     */
    private boolean defendsAttack(int move) {
        return (state[move] < 3 && state[move] != 0);
    }


    /**
     * Check if the player's houses contain at least a seed.
     *
     * @return      False if the pits are empty
     */
    private boolean playerHasSeeds() {
        return (empty & player.mask) != player.mask;
    }


    /**
     * Check if the opponent's houses contain at least a seed.
     *
     * @return      False if the pits are empty
     */
    private boolean rivalHasSeeds() {
        return (empty & rival.mask) != rival.mask;
    }


    /**
     * Check if the opponent's houses contain at least a seed. This
     * method checks only the pits that come after the given house.
     *
     * @param house Only check houses after this one
     * @return      False if the pits are empty
     */
    private boolean rivalHasSeedsAfter(int house) {
        return (empty & rival.mask) != (rival.mask & (0xFFE << house));
    }


    /**
     * Check if the last move was a capture. This returns true also
     * if the last move is unknown (it's a root position).
     *
     * @return      {@code true} if the move captured seeds
     */
    public boolean wasCapture() {
        return (capture == index);
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
        this.hash = computeHash();
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
     * Plays a move on the board.
     *
     * @param move      Move to perform
     */
    private void movePieces(int move) {
        final boolean isCapture = isCapture(move);
        final int house = sowSeeds(move);
        if (isCapture) captureSeeds(house);
    }


    /**
     * Distributes the seeds from the given house.
     *
     * @param move      A move identifier
     * @return          Last house that received a seed
     */
    private int sowSeeds(int move) {
        int house = move;
        int seeds = state[move];

        state[move] = 0;
        empty |= (1 << move);

        while (seeds > 0) {
            if ((house = ++house % BOARD_SIZE) != move) {
                empty &= ~(1 << house);
                state[house]++;
                seeds--;
            }
        }

        return house;
    }


    /**
     * Captures seeds starting from the given house.
     *
     * @param house     A house identifier
     */
    private void captureSeeds(int move) {
        this.capture = index;

        for (int house = move; house >= rival.left; house--) {
            final int seeds = state[house];

            if ((seeds >>> 1) != 1) {
                break;
            }

            empty |= (1 << house);
            state[player.store] += seeds;
            state[house] = 0;
        }
    }


    /**
     * Gather the remaining seeds from the board.
     */
    private void gatherSeeds() {
        empty = SOUTH_MASK | NORTH_MASK;

        for (int move = SOUTH_LEFT; move <= SOUTH_RIGHT; move++) {
            state[SOUTH_STORE] += state[move];
            state[move] = 0;
        }

        for (int move = NORTH_LEFT; move <= NORTH_RIGHT; move++) {
            state[NORTH_STORE] += state[move];
            state[move] = 0;
        }
    }


    /**
     * Checks if the move is a capturing move for the current position.
     *
     * @param move   The move to determine if it's a capture
     * @return       {@code true} if the move would perform a legal
     *               capture or {@code false} otherwise
     */
    public boolean isCapture(int move) {
        final int sown;     // Number of seeds sown by the move
        final int current;  // Number of seeds on the landing house
        final int finish;   // House were the moves lands

        // Check that a capture is possible

        if ((sown = state[move]) == 0) {
            return false;
        }

        if ((finish = REAPER[move][sown]) == NULL_MOVE) {
            return false;
        }

        if ((current = state[finish]) > 2) {
            return false;
        }

        // Check if the capture is happening; taking into account the
        // number of full laps on the board and that capturing all of
        // the opponent seeds is always forbbiden

        if (sown < BOARD_SIZE) { // No laps are happening
            if (current > 0) {
                if (rivalHasSeedsAfter(finish)) {
                    return true;
                }

                for (int house = rival.left; house < finish; house++) {
                    if (state[house] == 0 || state[house] > 2) {
                        return true;
                    }
                }
            }
        } else if (sown < 2 * BOARD_SIZE - 1) { // Exactly one lap
            if (current < 2) {
                if (rival.mask > (2 << finish)) {
                    return true;
                }

                for (int house = rival.left; house < finish; house++) {
                    if (state[house] > 1) {
                        return true;
                    }
                }
            }
        } else if (current == 0) { // Two or more laps happening
            return (rival.mask > (2 << finish)) || rivalHasSeeds();
        }

        return false;
    }


    /**
     * Iterate the next legal move.
     *
     * @return  A move identifier or {@code NULL_MOVE}
     */
    @Override
    public int nextMove() {
        // Captures are disruptive moves that cause many cutouts,
        // thus we want to iterate them first.

        if (stage == ATTACKING_MOVES) {
            while (next > player.left) { next--;
                if (feedsRival(next) && isCapture(next)) {
                    return next;
                }
            }

            next = 1 + player.right;
            stage = rivalHasSeeds() ?
                DEFENSIVE_MOVES : MANDATORY_MOVES;
        }

        // When the opponent doesn't have any seeds, only moves
        // that sow on the opponent houses are legal.

        if (stage == MANDATORY_MOVES) {
            while (next > player.left) { next--;
                if (feedsRival(next) && !isCapture(next)) {
                    return next;
                }
            }

            return NULL_MOVE;
        }

        // After captures, iterate moves that will prevent the opponent
        // from capturing seeds. This also improves the cutouts.

        if (stage == DEFENSIVE_MOVES) {
            while (next > player.left) { next--;
                if (defendsAttack(next) && !isCapture(next)) {
                    return next;
                }
            }

            next = 1 + player.right;
            stage = REMAINING_MOVES;
        }

        // Iterate the remaining moves last; those that do not capture
        // seeds or are less likely to prevent captures.

        while (next > player.left) { next--;
            if (state[next] > 2 && !isCapture(next)) {
                return next;
            }
        }

        return NULL_MOVE;
    }


    /**
     * Ensures this object can store at least the give number of moves.
     *
     * @param size      Number of moves
     * @throws IllegalArgumentException
     */
    @Override
    public void ensureCapacity(int size) {
        if (size <= capacity) {
            return;
        }

        if (size > MAX_CAPACITY) {
            throw new IllegalArgumentException(
                "Requested capacity is above the maximum");
        }

        capacity = capacity + CAPACITY_INCREMENT;
        capacity = Math.max(size, capacity);
        capacity = Math.min(MAX_CAPACITY, capacity);

        states = Arrays.copyOf(states, capacity << 4);
        moves = Arrays.copyOf(moves, capacity);
        hashes = Arrays.copyOf(hashes, capacity);
        captures = Arrays.copyOf(captures, capacity);
        empties = Arrays.copyOf(empties, capacity);

        System.gc();
    }


    /**
     * Initialize the utility evaluation function.
     */
    private static Scorer<OwareGame> outcomeFunction() {
        return new OutcomeScorer();
    }


    /**
     * Initialize the heuristic evaluation function.
     */
    private static Scorer<OwareGame> scoreFunction() {
        return new PositionalScorer();
    }


    /**
     * Initialize the hash code generator.
     */
    private static HashFunction hashFunction() {
        return OwareBoard.hashFunction();
    }
}
