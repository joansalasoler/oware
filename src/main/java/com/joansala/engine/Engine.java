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

import java.util.function.Consumer;


/**
 * An engine searches a game and returns a best move for its current state.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public interface Engine {

    /** Default depth limit per move */
    int DEFAULT_DEPTH = 127;

    /** Default time limit per move */
    long DEFAULT_MOVETIME = 3600;


    /**
     * Returns the maximum depth allowed for the search
     *
     * @return   The depth value
     */
    int getDepth();


    /**
     * Returns the maximum time allowed for a move computation
     * in milliseconds
     *
     * @return   The new search time in milliseconds
     */
    long getMoveTime();


    /**
     * Returns the current the contempt factor of the engine.
     */
    int getContempt();


    /**
     * Returns the current infinity score of the engine.
     */
    int getInfinity();


    /**
     * Obtains the move this engine would like to ponder on.
     */
    int getPonderMove(Game game);


    /**
     * Sets the maximum search depth for subsequent computations
     *
     * @param depth  The new depth value
     */
    void setDepth(int depth);


    /**
     * Sets the maximum search time allowed for subsequent computations
     *
     * @param delay    The new time value in milliseconds as a
     *                 positive number greater than zero
     */
    void setMoveTime(long delay);


    /**
     * Sets the contempt factor. That is, the score to which end game
     * positions that are draw will be evaluated. The contempt score
     * must be a value in the range +-infinity for the engine to work
     * properly.
     *
     * @see Engine#setInfinity(int)
     * @param score     Score for draw positions
     */
    void setContempt(int score);


    /**
     * Sets the infinity score. Setting this value to the maximum score
     * a game object can possibly be evaluated to is likely to improve
     * the engine performance.
     *
     * @param score     Infinite value as apositive integer
     */
    void setInfinity(int score);


    /**
     * Attaches a move consumer to the engine.
     *
     * Consumers may be invoked by the engine whenever some search information
     * changes. They are granteed to run till completion before the engine
     * continues with normal operations. Each consumer receives as a parameter
     * the best move found so far by the engine.
     *
     * @param consumer  Best move consumer
     */
    void attachConsumer(Consumer<Report> consumer);


    /**
     * Detaches a move consumer from the engine.
     *
     * @param consumer  Best move consumer
     */
    void detachConsumer(Consumer<Report> consumer);


    /**
     * Tells the engine that the next positions are going to be from
     * a different match. This method must be called before a new game
     * is started to ask the engine to set up its parameters acordingly.
     */
    void newMatch();


    /**
     * Best score obtainable for the current game state.
     *
     * @param game  Initial game state
     * @return      Computed score
     */
    int computeBestScore(Game game);


    /**
     * Computes a best move for the current position of a game.
     *
     * @param game  The game for which a best move must be computed
     * @return      The best move found for the current game position
     *              or {@code Game.NULL_MOVE} if the game already ended
     */
    int computeBestMove(Game game);


    /**
     * Aborts the current search. After calling this method any move
     * computations must return a result immediately.
     *
     * @see Engine#computeBestMove(Game)
     */
    void abortComputation();


    /**
     * Stop the current search after a specified delay.
     *
     * @param delay      Delay in milliseconds
     */
    void abortComputation(long delay);
}
