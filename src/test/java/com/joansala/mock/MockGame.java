package com.joansala.mock;

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
import com.joansala.engine.Game;


/**
 * Mock game implementation.
 */
public class MockGame implements Game {

    /** Legal moves on each state */
    private static final int[] LEGAL_MOVES = { 0, 1, 2, 3, 4 };

    /** Maximum score to which states are evaluated */
    public static final int MAX_SCORE = 1000;

    /** Recommended score to evaluate draws */
    public static final int CONTEMPT_SCORE = 0;

    /** Current start board */
    private MockBoard board = new MockBoard();

    /** Current state index */
    private int index = -1;

    /** Player to move on current state */
    private int turn = SOUTH;

    /** Move that lead to this position */
    private int move = NULL_MOVE;

    /** Current move cursor */
    private int cursor = 0;

    /** Current position */
    private int position = 0;

    /** Next move cursor history */
    private int[] cursors = new int[1024];

    /** Performed moves array */
    private int[] moves = new int[1024];


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEnded() {
        return (index > 1024);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        return MAX_SCORE * turn;
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
    public void makeMove(int move) {
        index++;
        turn = -turn;
        moves[index] = this.move;
        cursors[index] = this.cursor;
        this.move = move;
        position = move;
        cursor = 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMove() {
        turn = -turn;
        move = moves[index];
        position = moves[index];
        cursor = cursors[index];
        index--;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unmakeMoves(int length) {
        for (int i = 0; i < length; i++) {
            unmakeMove();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int nextMove() {
        if (!hasEnded() && cursor < LEGAL_MOVES.length) {
            return LEGAL_MOVES[cursor++];
        }

        return NULL_MOVE;
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
        this.board = (MockBoard) board;
        this.position = this.board.position();
        this.turn = board.turn();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Board toBoard() {
        return new MockBoard(position, turn);
    }


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
        return position * turn;
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
        for (int i = 0; i < LEGAL_MOVES.length; i++) {
            if (move == LEGAL_MOVES[i]) {
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
        return Arrays.copyOf(LEGAL_MOVES, LEGAL_MOVES.length);
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
