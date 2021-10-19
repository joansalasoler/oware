package com.joansala.engine.base;

/*
 * Aalina oware engine.
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
import java.util.List;
import java.util.LinkedList;
import com.joansala.engine.Board;
import com.joansala.engine.Game;


/**
 * Abstract game logic implementation.
 */
public abstract class BaseGame implements Game {

    /** Maximum score to which states are evaluated */
    public static final int MAX_SCORE = 1000;

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = 0;

    /** Default capacity of this object */
    public static final int DEFAULT_CAPACITY = 254;

    /** Number of moves this game can store */
    protected int capacity;

    /** Current state index */
    protected int index;

    /** Player to move on current state */
    protected int turn;

    /** Performed move to reach current state */
    protected int move;

    /** Performed moves array */
    protected int[] moves;

    /** Current state hash code */
    protected long hash;


    /**
     * Instantiate a new game on the start state.
     */
    public BaseGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Instantiate a new game on the start state.
     *
     * @param capacity      Initial capacity
     */
    public BaseGame(int capacity) {
        this.index = -1;
        this.turn = Game.SOUTH;
        this.move = Game.NULL_MOVE;
        this.moves = new int[capacity];
        this.capacity = capacity;
        this.setBoard(defaultBoard());
        this.hash = computeHash();
    }


    /**
     * Computes a unique hash for the current state.
     *
     * @return          Unique hash code
     */
    protected abstract long computeHash();


    /**
     * Obtain the default start board of a game.
     *
     * @return      A board instance
     */
    public abstract Board defaultBoard();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean hasEnded();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int outcome();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int score();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int getCursor();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void setCursor(int cursor);


    /**
     * {@inheritDoc}
     */
    public abstract void resetCursor();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void makeMove(int move);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void unmakeMove();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int nextMove();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Board getBoard();


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void setBoard(Board board);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Board toBoard();


    /**
     * {@inheritDoc}
     */
    @Override
    public void endMatch() {
        // Does nothing
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return 1 + index;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int turn() {
        return turn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long hash() {
        return hash;
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
    public int infinity() {
        return MAX_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public int[] moves() {
        int[] moves = new int[length()];

        if (index >= 0) {
            System.arraycopy(this.moves, 1, moves, 0, index);
            moves[index] = this.move;
        }

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLegal(int move) {
        for (int house : legalMoves()) {
            if (house == move) {
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] legalMoves() {
        int move = NULL_MOVE;
        int cursor = getCursor();
        List<Integer> moves = new LinkedList<>();

        resetCursor();

        while ((move = nextMove()) != NULL_MOVE) {
            moves.add(move);
        }

        setCursor(cursor);

        int length = moves.size();
        int[] array = new int[length];

        for (int i = 0; i < length; i++) {
            array[i] = moves.get(i);
        }

        return array;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureCapacity(int size) {
        System.gc();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int toCentiPawns(int score) {
        return score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Game cast() {
        return this;
    }
}
