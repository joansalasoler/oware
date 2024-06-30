package com.joansala.game.oware.egtb;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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

import java.util.Arrays;
import com.joansala.game.oware.OwareGame;
import com.joansala.game.oware.OwareBoard;
import static com.joansala.game.oware.Oware.*;


public class EGTBGame extends OwareGame {

    /**
     * {@inheritDoc}
     */
    @Override
    public OwareBoard toBoard() {
        int[] position = Arrays.copyOf(state(), POSITION_SIZE);
        return new EGTBBoard(position);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int outcome() {
        final int[] state = state();
        int south = state[SOUTH_STORE];
        int north = state[NORTH_STORE];

        for (int house = SOUTH_LEFT; house <= SOUTH_RIGHT; house++) {
            south += state[house];
        }

        for (int house = NORTH_LEFT; house <= NORTH_RIGHT; house++) {
            north += state[house];
        }

        return south - north;
    }


    /** {@inheritDoc} */
    @Override
    public int score() {
        return DRAW_SCORE;
    }
}
