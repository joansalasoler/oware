package com.joansala.oware;

/*
 * Aalina oware engine.
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import static com.joansala.oware.Oware.*;


/**
 * Reperesents an oware abapa game between two players.
 *
 * @author    Joan Sala Soler
 * @version   2.0.0
 */
public class OwareGame implements Game {

    /** Maximum score to which positions are evaluated */
    public static final int MAX_SCORE = 1000;

    /** The maximum number of moves this object can store */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE >> 4;

    /** Default capacity for this object */
    private static final int DEFAULT_CAPACITY = 254;

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

    /** Number of moves this game can store */
    private int capacity;

    /** Current state index */
    private int index;

    /** Player to move on the current position */
    private int turn;

    /** Performed move to reach current position */
    private int move;

    /** Index of the last capture move */
    private int capture;

    /** Bitmask of player houses */
    private int playerMask;

    /** Bitmask of opponent houses */
    private int rivalMask;

    /** Bitmask of empty houses */
    private int empty;

    /** Leftmost house of the current player */
    private int left;

    /** Rightmost house of the current player */
    private int right;

    /** First house of the rival player */
    private int stop;

    /** Current position hash code */
    private long hash;

    /** Current move generation stage */
    private int stage;

    /** Current move generation house */
    private int next;

    /** Position and move generation state */
    private int[] state;

    /** Performed moves history */
    private int[] moves;

    /** Last capture move history */
    private int[] captures;

    /** Empty houses history */
    private int[] empties;

    /** Board states history */
    private int[] states;

    /** Hash code history */
    private long[] hashes;


    /**
     * Creates a new {@code OwareGame} object.
     */
    public OwareGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Creates a new {@code OwareGame} object with the given capacity.
     *
     * @param size      Capacity as a number of moves
     */
    public OwareGame(int size) {
        capacity = size;
        states = new int[capacity << 4];
        moves = new int[capacity];
        hashes = new long[capacity];
        captures = new int[capacity];
        empties = new int[capacity];
        state = new int[4 + BOARD_SIZE];
        setStart(START_POSITION, SOUTH);
    }


    /**
     * Sets a new position and turn as the initial board for the game.
     *
     * @param position  An array representation of a position
     * @param turn      The player to move on the position. Must be
     *                  either {@code SOUTH} or {@code NORTH}.
     *
     * @throws IllegalArgumentException  if {@code turn} is not valid or
     *      {@code postion} is not a valid position representation
     */
    public void setStart(Object position, int turn) {
        if (turn != SOUTH && turn != NORTH) {
            throw new IllegalArgumentException(
                "Game turn is not a valid");
        }

        int[] positionArray = (int[]) position;

        if (isValidPosition(positionArray) == false) {
            throw new IllegalArgumentException(
                "Position representation is not valid");
        }

        setStart(positionArray, turn);
    }


    /**
     * Sets a new initial game position and turn. Thus, discarding all
     * the internally stored data.
     *
     * @param position  The new position as an array
     * @param turn      The turn value for the player to move
     */
    private void setStart(int[] position, int turn) {
        index = -1;
        capture = -1;
        move = NULL_MOVE;

        setTurn(turn);
        resetCursor();

        System.arraycopy(position, 0, state, 0, 2 + BOARD_SIZE);
        empty = computeEmpty();
        hash = computeHash();
    }


    /**
     * Returns true if the array is a valid representation of a board
     * position. A valid position contains exactly fourty eight seeds
     * distributed in fourteen houses.
     *
     * @param position  An array representation of a position
     * @return          {@code true} if position is valid
     */
    private static boolean isValidPosition(int[] position) {
        if (position == null) {
            return false;
        }

        if (position.length != 2 + BOARD_SIZE) {
            return false;
        }

        int seeds = 0;

        for (int i = 0; i < 2 + BOARD_SIZE; i++) {
            seeds += position[i];

            if (position[i] < 0) {
                return false;
            }
        }

        return (seeds == SEED_COUNT);
    }


    /**
     * Sets the current player to move.
     *
     * @param turn      {@code SOUTH} or {@code NORTH}
     */
    private void setTurn(int turn) {
        this.turn = turn;

        if (turn == SOUTH) {
            left = SOUTH_LEFT;
            right = SOUTH_RIGHT;
            stop = NORTH_LEFT;
            playerMask = SOUTH_MASK;
            rivalMask = NORTH_MASK;
        } else {
            left = NORTH_LEFT;
            right = NORTH_RIGHT;
            stop = SOUTH_LEFT;
            playerMask = NORTH_MASK;
            rivalMask = SOUTH_MASK;
        }
    }


    /**
     * Adds a new history entry with the current game state.
     */
    private void pushState() {
        index++;
        moves[index] = move;
        captures[index] = capture;
        hashes[index] = hash;
        empties[index] = empty;
        state[2 + BOARD_SIZE] = next;
        state[3 + BOARD_SIZE] = stage;
        System.arraycopy(state, 0, states, index << 4, 4 + BOARD_SIZE);
    }


    /**
     * Retrieve the current game state from the history.
     */
    private void popState() {
        System.arraycopy(states, index << 4, state, 0, 4 + BOARD_SIZE);
        stage = state[3 + BOARD_SIZE];
        next = state[2 + BOARD_SIZE];
        empty = empties[index];
        hash = hashes[index];
        capture = captures[index];
        move = moves[index];
        index--;
    }


    /**
     * Number of moves performed on this game.
     *
     * @return  Number of moves
     */
    public int length() {
        return 1 + index;
    }


    /**
     * Playing turn of the current game.
     *
     * @return  {@code SOUTH} or {@code NORTH}
     */
    public int turn() {
        return turn;
    }


    /**
     * Unique hash code of the current position.
     *
     * @return  A hash code
     */
    public long hash() {
        return hash;
    }


    /**
     * Returns an array representation of the current position.
     *
     * @return      A new position array
     */
    public int[] position() {
        return Arrays.copyOf(state, 2 + BOARD_SIZE);
    }


    /**
     * Moves performed to reach the current position.
     *
     * @return      A new moves array
     */
    public int[] moves() {
        int[] moves = new int[length()];
        System.arraycopy(this.moves, 1, moves, 0, length());
        if (index >= 0) moves[index] = this.move;

        return moves;
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
     * Sets the internal state to an endgame position.
     */
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
    public boolean hasEnded() {
        return state[SOUTH_STORE] > SEED_GOAL ||
               state[NORTH_STORE] > SEED_GOAL ||
               isRepetition() || !hasLegalMoves();
    }


    /**
     * Returns the winner of the game on the current position.
     *
     * @return  {@code SOUTH}, {@code NORTH} or {@code DRAW}
     */
    public int winner() {
        final int score = outcome();

        if (score == MAX_SCORE) {
            return SOUTH;
        }

        if (score == -MAX_SCORE) {
            return NORTH;
        }

        return DRAW;
    }


    /**
     * Returns an utility evaluation of the current position.
     *
     * <p>This method evaluates the current position as an endgame,
     * returning {@code ±MAX_SCORE} if a player won or {@code
     * DRAW_SCORE} if the match hasn't ended or it is drawn.</p>
     *
     * @return  Exact score value
     */
    public int outcome() {
        // The game ended because of captured seeds

        if (state[SOUTH_STORE] > SEED_GOAL) {
            return MAX_SCORE;
        }

        if (state[NORTH_STORE] > SEED_GOAL) {
            return -MAX_SCORE;
        }

        // The game ended because of a move repetition or because no
        // legal moves could be performed. Each player captures all
        // seeds on their side of the board

        int seeds = state[SOUTH_STORE];

        for (int house = SOUTH_LEFT; house <= SOUTH_RIGHT; house++) {
            seeds += state[house];
        }

        if (seeds > SEED_GOAL) {
            return MAX_SCORE;
        }

        if (seeds < SEED_GOAL) {
            return -MAX_SCORE;
        }

        return DRAW_SCORE;
    }


    /**
     * Returns the heuristic evaluation of the current position
     *
     * @return  The heuristic evaluation as a value between
     *          {@code -MAX_SCORE} and {@code MAX_SCORE}
     */
    public int score() {
        int score = 25 * (state[SOUTH_STORE] - state[NORTH_STORE]);

        for (int house = SOUTH_LEFT; house <= SOUTH_RIGHT; house++) {
            final int seeds = state[house];

            if (seeds > 12) {
                score += 28;
            } else if (seeds == 0) {
                score -= 54;
            } else if (seeds < 3) {
                score -= 36;
            }
        }

        for (int house = NORTH_LEFT; house <= NORTH_RIGHT; house++) {
            final int seeds = state[house];

            if (seeds > 12) {
                score -= 28;
            } else if (seeds == 0) {
                score += 54;
            } else if (seeds < 3) {
                score += 36;
            }
        }

        return score;
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
    private long computeHash() {
        long hash = (turn == SOUTH) ? SOUTH_SIGN : NORTH_SIGN;
        int n = state[NORTH_STORE];

        for (int i = SOUTH_STORE; n < SEED_COUNT && i >= 0; i--) {
            hash += COEFFICIENTS[n][i];
            n += state[i];
        }

        return hash;
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
    public int getCursor() {
        return ((next << 2) | stage);
    }


    /**
     * Sets the move generation cursor.
     *
     * @param   New cursor
     */
    public void setCursor(int cursor) {
        stage = (cursor & 0x03);
        next = (cursor >> 2);
    }


    /**
     * Resets the move generation cursor.
     */
    public void resetCursor() {
        stage = ATTACKING_MOVES;
        next = 1 + right;
    }


    /**
     * Check if a move may be performed on the current position.
     *
     * <p>A move is legal if it sows seeds into the opponent houses,
     * unless no such move could be performed, in which case all the
     * moves that sow seeds are legal. Grand Slam moves are legal but
     * they don't capture any seeds.</p>
     *
     * @see Game#makeMove(int)
     * @param move  A move identifier
     * @return      {@code true} if the move is legal
     */
    public boolean isLegal(int move) {
        for (int house : legalMoves()) {
            if (house == move) {
                return true;
            }
        }

        return false;
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

        for (int move = right; move >= left; move--) {
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
                return true;
            }
        }

        return false;
    }


    /**
     * Check if a move sows on the rival houses.
     *
     * @param move  A move on the current position
     * @return      If it feeds the opponent
     */
    private boolean feedsRival(int move) {
        return (move + state[move] > right);
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
        return (empty & playerMask) != playerMask;
    }


    /**
     * Check if the opponent's houses contain at least a seed.
     *
     * @return      False if the pits are empty
     */
    private boolean rivalHasSeeds() {
        return (empty & rivalMask) != rivalMask;
    }


    /**
     * Check if the opponent's houses contain at least a seed. This
     * method checks only the pits that come after the given house.
     *
     * @param house Only check houses after this one
     * @return      False if the pits are empty
     */
    private boolean rivalHasSeedsAfter(int house) {
        return (empty & rivalMask) != (rivalMask & (0xFFE << house));
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
     * Performs a move on the board.
     *
     * @param move      A move indentifier
     * @throws IndexOutOfBoundsException
     */
    public void makeMove(int move) {
        pushState();

        final boolean isCapture = isCapture(move);
        final int house = sowSeeds(move);
        if (isCapture) captureSeeds(house);

        setTurn(-turn);
        resetCursor();
        this.move = move;
        this.hash = computeHash();
    }


    /**
     * Undoes the last performed move.
     */
    public void unmakeMove() {
        setTurn(-turn);
        popState();
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

        final int store = (turn == SOUTH) ?
            SOUTH_STORE : NORTH_STORE;

        for (int house = move; house >= stop; house--) {
            final int seeds = state[house];

            if ((seeds >>> 1) != 1) {
                break;
            }

            empty |= (1 << house);
            state[store] += seeds;
            state[house] = 0;
        }
    }


    /**
     * Gather the remaining seeds from the board.
     */
    private void gatherSeeds() {
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

                for (int house = stop; house < finish; house++) {
                    if (state[house] == 0 || state[house] > 2) {
                        return true;
                    }
                }
            }
        } else if (sown < 2 * BOARD_SIZE - 1) { // Exactly one lap
            if (current < 2) {
                if (rivalMask > (2 << finish)) {
                    return true;
                }

                for (int house = stop; house < finish; house++) {
                    if (state[house] > 1) {
                        return true;
                    }
                }
            }
        } else if (current == 0) { // Two or more laps happening
            return (rivalMask > (2 << finish)) || rivalHasSeeds();
        }

        return false;
    }


    /**
     * Iterate the next legal move.
     *
     * @return  A move identifier or {@code NULL_MOVE}
     */
    public int nextMove() {
        // Captures are disruptive moves that cause many cutouts,
        // thus we want to iterate them first.

        if (stage == ATTACKING_MOVES) {
            while (next > left) { next--;
                if (feedsRival(next) && isCapture(next)) {
                    return next;
                }
            }

            next = 1 + right;
            stage = rivalHasSeeds() ?
                DEFENSIVE_MOVES : MANDATORY_MOVES;
        }

        // When the opponent doesn't have any seeds, only moves
        // that sow on the opponent houses are legal.

        if (stage == MANDATORY_MOVES) {
            while (next > left) { next--;
                if (feedsRival(next) && !isCapture(next)) {
                    return next;
                }
            }

            return NULL_MOVE;
        }

        // After captures, iterate moves that will prevent the opponent
        // from capturing seeds. This also improves the cutouts.

        if (stage == DEFENSIVE_MOVES) {
            while (next > left) { next--;
                if (defendsAttack(next) && !isCapture(next)) {
                    return next;
                }
            }

            next = 1 + right;
            stage = REMAINING_MOVES;
        }

        // Iterate the remaining moves last; those that do not capture
        // seeds or are less likely to prevent captures.

        while (next > left) { next--;
            if (state[next] > 2 && !isCapture(next)) {
                return next;
            }
        }

        return NULL_MOVE;
    }


    /**
     * Obtain all the legal moves.
     *
     * @return      A new array with the moves
     */
    public int[] legalMoves() {
        int length = 0;
        int move = NULL_MOVE;

        final int cursor = getCursor();
        final int[] moves = new int[6];

        resetCursor();

        while ((move = nextMove()) != NULL_MOVE) {
            moves[length++] = move;
        }

        setCursor(cursor);

        return Arrays.copyOf(moves, length);
    }


    /**
     * Ensures this object can store at least the give number of moves.
     *
     * @param size      Number of moves
     * @throws IllegalStateException
     */
    public void ensureCapacity(int size) {
        if (size <= capacity) {
            return;
        }

        if (size > MAX_CAPACITY) {
            throw new IllegalStateException(
                "Requested capacity is above the maximum");
        }

        capacity = capacity + CAPACITY_INCREMENT;
        capacity = Math.max(size, capacity);
        capacity = Math.min(MAX_CAPACITY, capacity);

        int[] states = new int[capacity << 4];
        int[] moves = new int[capacity];
        int[] captures = new int[capacity];
        int[] empties = new int[capacity];
        long[] hashes = new long[capacity];

        System.arraycopy(this.states, 0, states, 0, (index + 1) << 4);
        System.arraycopy(this.moves, 0, moves, 0, index + 1);
        System.arraycopy(this.hashes, 0, hashes, 0, index + 1);
        System.arraycopy(this.captures, 0, captures, 0, index + 1);
        System.arraycopy(this.empties, 0, empties, 0, index + 1);

        this.states = states;
        this.moves = moves;
        this.hashes = hashes;
        this.captures = captures;
        this.empties = empties;

        System.gc();
    }
}
