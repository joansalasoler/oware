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
 * An end games book that returns precomputed scores from a database.
 * The endgames database is usually used by engines during a move
 * computation, thus, is a good idea to keep the database in memory
 * for a faster access by the engines.
 *
 * @author    Joan Sala Soler
 * @version   1.1.0
 */
public interface Leaves<G extends Game> {

    /**
     * Flag of the last found position.
     *
     * @return      Flag value
     */
    int getFlag();


    /**
     * Returns the exact score value for the last position found.
     * The score must be returned from the player to move perspective.
     *
     * @return  the stored score value or zero
     */
    int getScore();


    /**
     * Search a position provided by a {@code Game} object and sets it
     * as the current position on the endgames book.
     *
     * @param game  A game object
     * @return      {@code true} if an exact score for the position
     *              could be found; {@code false} otherwise
     */
    boolean find(G game);
}
