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
import com.joansala.engine.Game;
import com.joansala.except.IllegalTurnException;


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
     *
     * @throws IllegalTurnException  If turn is not valid
     */
    public BaseBoard(P position, int turn) {
        validateTurn(turn);
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
     * State of the board and its pieces.
     *
     * @return      Position instance
     */
    public P position() {
        return position;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toNotation(int[] moves) {
        StringJoiner joiner = new StringJoiner(" ");

        for (int move : moves) {
            joiner.add(toCoordinate(move));
        }

        return joiner.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] toMoves(String notation) {
        if (notation == null || notation.isBlank()) {
            return new int[0];
        }

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
    public abstract String toCoordinate(int move);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Board toBoard(String notation);


    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String toDiagram();


    /**
     * Asserts a value represents a valid turn for a game.
     * @throws GameEngineException If not valid
     */
    protected static void validateTurn(int turn) {
        if (turn != Game.SOUTH && turn != Game.NORTH) {
            throw new IllegalTurnException(
                "Turn is not valid");
        }
    }
}
