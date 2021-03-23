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

import java.io.IOException;


/**
 * An opening book that returns precomputed moves from a database.
 *
 * @author    Joan Sala Soler
 * @version   1.1.0
 */
public interface Roots {


    /**
     * Notifies the book intance that the next positions are going to
     * be from a different match.
     */
    void newMatch();


    /**
     * Chooses one move at random from the best moves found on the book.
     * An implementation of this interface may decide to return an
     * apparently weaker move from its database in order to increase
     * playing variability.
     *
     * @param game  The game for which a best move must be returned
     * @return      The best move found for the current game position
     *              or {@code Game.NULL_MOVE} if no move could be found
     *
     * @throws IOException  If an I/O exception occurred
     */
    int pickBestMove(Game game) throws IOException;
}
