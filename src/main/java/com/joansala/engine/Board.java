package com.joansala.engine;

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


/**
 * Immutable state of a match on a given instant.
 *
 * A {@code Board} object encapsulates the state of a {@code Game}, which
 * includes the player to move, the location of the game pieces and any
 * other information relevant for the gameplay; and provides methods for
 * the conversion of the game state between different representations.
 *
 * For example, a chess {@code Board} may encapulate the possibility of
 * an en-passant capture or the number of moves performed along with the
 * position of the pieces and the turn; and use the Forsyth–Edwards Notation
 * to encode the game state as a diagram string ({@see toDiagram}) and an
 * Alegraic Notation to encode sequence of moves ({@see toNotation(int[])}).
 *
 * It is a requirement for the board to be immutable. Any methods that
 * return state data must either return immutable objects or a new copy.
 */
public interface Board {

    /**
     * Which player is to move.
     *
     * @return      Player identifier
     */
    int turn();


    /**
     * Diagram representation of this board.
     *
     * Returns a string that desbribes the state of this board and can
     * be used to construct a new board instance that represents exactly
     * the same state as this board (@see #toBoard(String)).
     *
     * @return      Diagram string
     */
    String toDiagram();


    /**
     * Coordinate representation of a single move.
     *
     * Returns a string that describes the location of a move on this
     * board and can be used to convert back the move coordinate to its
     * identifier (@see #toMove(String)).
     *
     * @param move      Move identifier
     * @return          Move notation
     *
     * @throws GameEngineException  If move is not a valid identifier
     */
    String toCoordinate(int move);


    /**
     * Coordinate representation of a sequence of moves.
     *
     * Returns a string that describes a sequence of moves performed on
     * this board and can be used to convert back the sequence notation
     * to an array of move identifiers (@see #toMoves(int[])). This method
     * may not check if the sequence is valid.
     *
     * @param moves     Move identifiers array
     * @return          Move sequence notation
     *
     * @throws GameEngineException  If any move identifier is not valid
     */
    String toNotation(int[] moves);


    /**
     * Board repesentation of a board diagram.
     *
     * Returns a new board instance that represents exactly the same
     * board state described on the diagram notation (@see #toDiagram).
     *
     * @param diagram   Board diagram notation
     * @return          New board instance
     *
     * @throws GameEngineException  If the diagram is not valid
     */
    Board toBoard(String diagram);


    /**
     * Move identifier of a move coordinate.
     *
     * Returns a move identifier given a move coordinate on this
     * board (@see #toCoordinate(int)). This method may not check if
     * the coordinate is a valid move on this board.
     *
     * @param coordinate    Move notation
     * @return              Move identifier
     *
     * @throws GameEngineException  If coordinate is not a valid
     */
    int toMove(String coordinate);


    /**
     * Notation of a sequence of moves.
     *
     * Returns a sequence of move identifiers given the notation of a
     * sequence of moves (@see #toNotation(int[])). This method may not
     * check if the moves are valid on this board.
     *
     * @param notation      Move sequence notation
     * @return              An array of move identifiers
     *
     * @throws GameEngineException  If the notation is not valid
     */
    int[] toMoves(String notation);


    /**
     * Human readable representation of this board.
     *
     * @return      Board representation
     */
    @Override
    String toString();
}
