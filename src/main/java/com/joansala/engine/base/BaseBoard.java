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

import java.util.StringJoiner;
import com.joansala.engine.Board;


/**
 * Abstract board implementation.
 */
public abstract class BaseBoard<P> implements Board {

    /** State configuration */
    protected P position;

    /** Player to move */
    protected int turn;


    /**
     * Instantiates a new board.
     */
    public BaseBoard(P position, int turn) {
        this.position = position;
        this.turn = turn;
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
    public P position() {
        return position;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toAlgebraic(int[] moves) {
        StringJoiner joiner = new StringJoiner(" ");

        for (int move : moves) {
            joiner.add(toAlgebraic(move));
        }

        return joiner.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] toMoves(String notation) {
        String[] notations = notation.split(" ");
        int[] moves = new int[notations.length];

        for (int i = 0; i < notations.length; i++) {
            moves[i] = toMove(notations[i]);
        }

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int toMove(String notation);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String toAlgebraic(int move);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Board toBoard(String notation);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String toNotation();
}
