package com.joansala.engine;

/*
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
 * Stores information about a game state.
 *
 * <p>A cache object may be used by an engine as a temporary memory space
 * to store and retrieve information about searched game states. This is
 * usually known as transposition tables.</p>
 *
 * <p>The method {@code store} is used to ask the cache to remember the
 * relevant information for a position. Then this information may be
 * retrieved calling the method {@code find}. After this is called the
 * object getters may be used to retrieve the game state information.</p>
 *
 * <p>Therefore, performing a look up for a game position consists on
 * first searching the position and then retrieving its values:</p>
 * <pre>
 *   if (cache.find(game)) {
 *      int score = cache.getScore();
 *      int move = cache.getMove();
 *   }
 * </pre>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public interface Cache {

    /**
     * Returns the stored score value for the last position found.
     *
     * @return  Stored score value or zero
     */
    int getScore();


    /**
     * Returns the stored move value for the last position found.
     *
     * @return  Stored move value
     */
    int getMove();


    /**
     * Returns the stored depth value for the last position found.
     *
     * @return  Stored depth value or zero
     */
    int getDepth();


    /**
     * Returns the stored flag value for the last position found.
     *
     * @return  Stored flag value
     */
    int getFlag();


    /**
     * Search the current game state provided by a {@code Game} object.
     *
     * <p>When a position is found subsequent calls to the getter methods
     * of this object must return the values stored for the position.</p>
     *
     * @return  {@code true} if valid information for the position
     *          could be found; {@code false} otherwise.
     */
    boolean find(Game game);


    /**
     * Stores information about a game state on the cache.
     *
     * @param game   The game for which the information about its current
     *               state must be stored
     * @param score  Score value for the game state
     * @param depth  Search depth to which the score was evaluated
     * @param flag   Flag for the game state
     * @param move   The best move found for the game state
     */
    void store(Game game, int score, int move, int depth, int flag);


    /**
     * Asks the cache to make room for new entries. This method requests
     * that old cache entries be discarded so new entries may be stored
     * on the cache.
     */
    void discharge();


    /**
     * Resizes the cache and clears all the stored data.
     *
     * @param memory  Memory request in bytes
     */
    void resize(long memory);


    /**
     * Removes all the data stored on the cache.
     */
    void clear();


    /**
     * Returns the current capacity of this cache in bytes.
     *
     * @return  Allocated bytes for the cache
     */
    long size();

}
