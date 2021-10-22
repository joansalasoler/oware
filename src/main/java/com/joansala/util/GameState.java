package com.joansala.util;

/*
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


/**
 * Static game state representation.
 */
public final class GameState {

    /** Initial board of the game */
    private final Board board;

    /** Performed moves on the game */
    private final int[] moves;


    /**
     * Create a new game state.
     */
    public GameState(Board board, int[] moves) {
        this.board = board;
        this.moves = moves;
    }


    /**
     * Obtain the initial board.
     */
    public Board board() {
        return board;
    }


    /**
     * Obtain the performed moves.
     */
    public int[] moves() {
        return moves;
    }


    /**
     * {@inheritDoc}
     */
    @Override public String toString() {
        final String n = board.toDiagram();
        final String m = board.toNotation(moves);
        return String.format("%s %s", n, m).trim();
    }
}
