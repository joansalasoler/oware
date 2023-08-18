package com.joansala.game.oware.egtb;

/*
 * Copyright (c) 2023 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.game.oware.OwareBoard;

import static com.joansala.engine.Game.SOUTH;
import static com.joansala.game.oware.Oware.*;


public class EGTBBoard extends OwareBoard {

    /**
     * Creates a new board instance.
     */
    public EGTBBoard(int[] state) {
        turn = SOUTH;
        position = new int[POSITION_SIZE];
        System.arraycopy(state, 0, position, 0, POSITION_SIZE - 2);
    }
}
