package com.joansala.engine;

/*
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
 * Represents the logic of a game between two players.
 *
 * <p>This interface defines a state machine on which adding a moves
 * to the game transitions the machine to a new state. After calling
 * the method {@code makeMove} the game object must transition to a new
 * state, and calling the method {@code unmakeMove} must revert the
 * the object to the previous state it had. An implementation of this
 * interface stores the performed moves of a match and provides methods
 * for the generation of legal moves and the evaluation of the current
 * game state.</p>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public interface Game {

    /** Returned when the game ended in a draw */
    int DRAW = 0;

    /** South player identifier */
    int SOUTH = 1;

    /** North player identifier */
    int NORTH = -SOUTH;

    /** Null move identifier */
    int NULL_MOVE = -1;

    /** The draw score for the utility function */
    int DRAW_SCORE = 0;


    /**
     * Returns the number of moves performed on this game. From the
     * initial position until the current move.
     *
     * @return  The number of moves performed
     */
     int length();


    /**
     * Returns all the performed moves till the current position.
     *
     * @return  The moves performed for this game or {@code null} if
     *          no moves have been performed
     */
    int[] moves();


    /**
     * Returns the playing turn for the current game position
     *
     * @return  {@code Game.SOUTH} or {@code Game.NORTH}
     */
    int turn();


    /**
     * Returns a representation of the current position.
     *
     * @return  Representation of the position
     */
    Object position();


    /**
     * Sets a new initial position and turn for the game. Setting a new
     * position resets all the game state information, including the
     * performed moves.
     *
     * @param position  A representation of a position
     * @param turn      The player to move on the position. Must be
     *                  either {@code Game.SOUTH} or {@code Game.NORTH}.
     * @throws IllegalArgumentException  if {@code turn} is not valid or
     *      {@code postion} is not a valid position representation
     */
    void setStart(Object position, int turn);


    /**
     * Sets the internal board to the endgame position. This method
     * computes the final board for a match that ends on the current
     * position. It's only needed for games for which the board may
     * change after the game ends.
     */
    void endMatch();


    /**
     * Checks if the game has ended on the current position
     *
     * @return  {@code true} if the game ended or
     *          {@code false} otherwise
     */
    boolean hasEnded();


    /**
     * Returns an identifier for the player that won the game on the
     * current position.
     *
     * @return  One of {@code Game.SOUTH}, {@code Game.NORTH} or
     *          {@code Game.DRAW} if the game ended in a draw or
     *          hasn't ended yet
     */
    int winner();


    /**
     * Returns an heuristic evaluation of the current position.
     *
     * <p>The heuristic score must be a value between the minimum and
     * the maximum scores returned by the method {@code outcome} and
     * must predict how close to the end of the match the game is.</p>
     *
     * <p>A position must be evaluated always from the point of view of
     * south's player. Thus, this method must return increasing positive
     * numbers as the south player approaches winning the game, and
     * decreasing negative numbers if north has the advantage.</p>
     *
     * @see     Game#outcome
     * @return  The heuristic evaluation value
     */
    int score();


    /**
     * Returns an utility evaluation of the current position. This method
     * evaluates the current position as an endgame.
     *
     * <p>The score returned must be computed always from the south player
     * point of view. Thus, it should return the maximum possible score if
     * south wins; the minimum possible score if north wins and {@code
     * Game.DRAW_SCORE} if the game ended in a draw or hasn't ended yet.</p>
     *
     * @see     Game#score
     * @return  The utility evaluation value
     */
    int outcome();


    /**
     * Returns an unique hash code for the current position.
     *
     * @return  The hash code for the current position
     */
    long hash();


    /**
     * Check if a move may be performed on the current position. A
     * move can be performed if it is legal according to the rules of
     * the implemented game.
     *
     * @see Game#makeMove(int)
     * @param move  A move identifier
     * @return      {@code true} if the move is legal
     */
    boolean isLegal(int move);


    /**
     * Performs a move on the internal board.
     *
     * <p>This method must be as efficient as possible; it must not check
     * the legality of the performed move or assert that the game object
     * has enough capacity to store the move.</p>
     *
     * @see Game#unmakeMove()
     * @param move  The move to perform on the internal board
     */
    void makeMove(int move);


    /**
     * Undoes the last performed move on the internal board. Setting
     * the internal board and move generation iterators to their
     * previous state before the method {@code makeMove} was called.
     *
     * @see Game#makeMove(int)
     */
    void unmakeMove();


    /**
     * Returns the next legal move for the current position and turn.
     *
     * <p>This method is provided for efficiency, so it is not necessary
     * to generate all the legal moves on a given position till they
     * are needed. Legal moves are iterated based on the internal move
     * generation status. Note that after a call to {@code unmakeMove}
     * the last generation status must be restored to the previous state
     * it had before calling the method {@code makeMove}.</p>
     *
     * <p>A good move ordering is likely to improve the engine performance.
     * So its always a good idea to return the moves that are possibly
     * best for the player to move first.</p>
     *
     * @see Game#legalMoves
     * @return  A legal move identifier or {@code NULL_MOVE} if no
     *          more moves can be returned
     */
    int nextMove();


    /**
     * Returns all the legal moves that can be performed on the current
     * game position. Calling this method must not change the state of
     * the game in any way.
     *
     * <p>Note that the order of the moves on the result array it's
     * likely to improve/decresase the engine performance. So it may be
     * a good idea to store the best moves first.</p>
     *
     * @see Game#nextMove
     * @return  A legal moves array
     */
    int[] legalMoves();


    /**
     * Converts an evaluation score to centipawns.
     *
     * @param score     Evaluation score
     * @return          Score in centipawns
     */
    int toCentiPawns(int score);


    /**
     * Increases the capacity of the game object if necessary. Calling
     * this method must ensure that the game object can store at least
     * the number of moves specified.
     *
     * @see Game#makeMove
     * @param minCapacity  capacity specified in number of moves
     * @throws IllegalStateException  if {@code minCapacity} is above
     *          the maximum possible capacity for the object
     */
    void ensureCapacity(int minCapacity);


    /**
     * Current move generation cursor.
     *
     * @return  Cursor value
     */
    int getCursor();


    /**
     * Sets the move generation cursor.
     *
     * @param   New cursor
     */
    void setCursor(int cursor);
}
