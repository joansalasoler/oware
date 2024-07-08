package com.joansala.game.oware.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Scorer;
import com.joansala.game.oware.OwareGame;
import static com.joansala.game.oware.Oware.*;


/**
 * Evaluate the current state of an Oware game using only a material
 * balance heuristic.
 *
 * This material heuristic simply counts the difference of captured
 * seeds by each player as a measure of advantage. In Oware, the player
 * with more captured seeds at the end of the game wins.
 */
public final class MaterialScorer implements Scorer<OwareGame> {

    /** Weight of the captured seeds difference */
    public static final int TALLY_WEIGHT = 38;


    /**
     * {@inheritDoc}
     */
    public final int evaluate(OwareGame game) {
        final int south = game.state(SOUTH_STORE);
        final int north = game.state(NORTH_STORE);

        return TALLY_WEIGHT * (south - north);
    }
}
