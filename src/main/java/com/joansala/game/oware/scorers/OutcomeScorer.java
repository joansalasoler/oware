package com.joansala.game.oware.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  either version 3 of the License,  or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not,  see <http://www.gnu.org/licenses/>.
 */

import com.joansala.engine.Scorer;
import com.joansala.game.oware.OwareGame;
import static com.joansala.game.oware.Oware.*;
import static com.joansala.engine.Game.DRAW_SCORE;


/**
 * Computes an utility evaluation for the current state of a game.
 *
 * This evaluator checks if the current game is over. If one player has
 * won, returns the highest possible score {@code ±MAX_SCORE} from
 * the point of view of the south player. If the game is still going
 * or ends in a draw, the method returns {@code DRAW_SCORE}.
 *
 * There is a special cases to avoid certain moves: if the game ends
 * because the same position is repeated, this method returns
 * {@code ±REPETITION_SCORE} instead of the win score. This discourages
 * the engine from exploring variations that lead to repetitions.
 */
public final class OutcomeScorer implements Scorer<OwareGame> {

    /**
     * {@inheritDoc}
     */
    public final int evaluate(OwareGame game) {
        final int south = game.state(SOUTH_STORE);
        final int north = game.state(NORTH_STORE);

        // A player captured more than 24 seeds

        if (south > SEED_GOAL) {
            return +game.infinity();
        }

        if (north > SEED_GOAL) {
            return -game.infinity();
        }

        // Either because of a position repetition or because no valid
        // moves are left, both players capture all their remaining seeds
        // on their sides of the board. Here, we calculate the captured
        // seeds for the south player.

        int seeds = south;

        for (int house = SOUTH_LEFT; house <= SOUTH_RIGHT; house++) {
            seeds += game.state(house);
        }

        // Return a score only if the game has ended with a win or a
        // loss. If the game ended because of repeated positions, return
        // a heuristic score instead of the win/loss score.

        if (seeds > SEED_GOAL) {
            if (game.foundRepetition()) {
                return +REPETITION_SCORE;
            }

            return +game.infinity();
        }

        if (seeds < SEED_GOAL) {
            if (game.foundRepetition()) {
                return -REPETITION_SCORE;
            }

            return -game.infinity();
        }

        // The game was drawn or it is not over yet

        return DRAW_SCORE;
    }
}
