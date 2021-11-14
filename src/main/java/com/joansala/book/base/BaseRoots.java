package com.joansala.book.base;

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

import java.io.IOException;
import com.joansala.engine.Game;
import com.joansala.engine.Roots;


/**
 * An opening book that does not contain any entries.
 */
public class BaseRoots implements Roots<Game> {

    /** {@inheritDoc} */
    @Override public void newMatch() {
        // Does nothing
    }

    /** {@inheritDoc} */
    @Override public int pickBestMove(Game game) throws IOException {
        return Game.NULL_MOVE;
    }

    /** {@inheritDoc} */
    @Override public int pickPonderMove(Game game) throws IOException {
        return Game.NULL_MOVE;
    }
}
