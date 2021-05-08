package com.joansala.engine;

/*
 * Aalina oware engine.
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
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


/**
 * A board represents an immutable game state and its representations.
 * An implementation of this class must encapsulate a single game state and
 * provide methods for the conversion of the game state between different
 * board representations.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public interface Board {

    /**
     * Returns which player is to move for the board position
     *
     * @return   The player to move
     */
    int turn();


    /**
     * Returns a copy of the position on this the board.
     *
     * @return   The position array
     */
    Object position();


    /**
     * Converts the current state of a game object to its board object
     * representation.
     *
     * @param game  A game object
     * @throws IllegalArgumentException If the game is not valid
     */
    Board toBoard(Game game);


    /**
     * Converts a board notation to a board object.
     *
     * @param notation  A board notation
     * @throws IllegalArgumentException If the notation is not valid
     */
    Board toBoard(String notation);


    /**
     * Converts this board object to its equivalent board notation.
     *
     * @return   String representation of this board
     */
    String toNotation();


    /**
     * Returns an human readable string representation of this object.
     *
     * @return   A formatted string
     */
    @Override
    String toString();


    /**
     * Converts an integer representation of one move to its algebraic
     * representation.
     *
     * @param move  A move identifier
     * @return      Move notation
     * @throws IllegalArgumentException if the move is not valid
     */
    String toAlgebraic(int move);


    /**
     * Converts an integer representation of one or more moves to their
     * algebraic representation.
     *
     * @param moves Moves array
     * @return      Moves notation
     * @throws IllegalArgumentException if a move is not valid
     */
    String toAlgebraic(int[] moves);


    /**
     * Converts an algebraic move notation to an integer representation.
     *
     * @param notation  Move notation
     * @return          Integer representation of the move
     * @throws IllegalArgumentException If the notation does not
     *                  represent a valid move
     */
    int toMove(String notation);


    /**
     * Converts an algebraic moves notation to an integer move array
     * representation.
     *
     * @param notation  Moves notation
     * @return          Array representation of the moves
     * @throws IllegalArgumentException If the notation does not
     *                  represent a valid move sequence
     */
    int[] toMoves(String notation);
}
