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
import com.joansala.engine.mcts.Montecarlo;
import com.joansala.engine.negamax.Negamax;
import com.joansala.engine.uct.UCT;
import com.joansala.game.oware.OwareGame;
import static com.joansala.game.oware.Oware.*;


/**
 * Evaluate the current state of an Oware game using a material and
 * positional heuristic.
 *
 * This positional heuristic goes beyond simply counting captured seeds
 * and considers the arrangement of seeds on the board. This includes:
 *
 * - Material advantage (captured seeds)
 * - Attack potential (houses with many seeds)
 * - Defensive potential (houses that can be captured)
 * - Mobility (how many moves a player has in hand)
 *
 * This evaluation heuristic, which turns out to be quite good, was
 * created using a simple linear regression method, by analyzing
 * randomly generated game states to identify key factors that improve
 * the accuracy of the simple {@link MaterialScorer} heuristic.
 *
 * Here's the method that was used to derive this function:
 *
 * 1. Generate a set of random game states.
 * 2. For each position, evaluate it using an engine ({@link Negamax},
 *    {@link UCT}) and the {@link MaterialScorer} heuristic. It should
 *    be possible to simply use {@link Montecarlo} without and heuristic
 *    function, but this is much faster.
 * 3. Instead of a fixed depth or spending a long time thinking, we
 *    evaluate the positions for a short time (200ms or 300ms). We do
 *    this because we're more interested in predicting what will happen
 *    next, rather than the final outcome of the game. Also, we want to
 *    evaluate endgames more deeply than the middle game.
 * 4. For each position, choose a set of features that may be suitable
 *    to finetune the {@link MaterialScorer} heuristic. We want features
 *    that can be computed fast during the search. For Oware Some good
 *    clues are the number of seeds in each house and on the stores.
 * 5. Use linear regression to figure out how much weight each feature
 *    should have to improve the accuracy of the {@link MaterialScorer}
 *    heuristic. Some features might not be helpful at all, while others
 *    might be better combined. The Weka machine learning toolkit can be
 *    used to automate this process (https://ml.cms.waikato.ac.nz/weka).
 * 6. We want to find the smallest group of features that can still
 *    predict the scores calculated earlier with reasonable accuracy,
 *    but we don't want too much accuracy. Remove and combine features
 *    until a minimal set of features is found.
 * 7. Once we find the important features, we'll adjust their influence
 *    to make them balanced. Imagine a feature that gets a score of W
 *    (positive) for a player. This same feature should get a score of
 *    -W (negative) for their opponent, to reflect the opposite effect.
 * 8. The final heuristic is the sum of the products of the minimal set
 *    of features we found and their respective weights. Ensure the
 *    heuristic scores are always between {@code +MAX_SCORE} and
 *    {@code -MAX_SCORE} (exclusive) by adjusting the weights.
 */
public final class PositionalScorer implements Scorer<OwareGame> {

    /** Weight of the captured seeds difference */
    public static final int TALLY_WEIGHT = 25;

    /** Weight of houses that contain more than 12 seeds */
    public static final int ATTACK_WEIGHT = 28;

    /** Weight of houses that contain 1 or 2 seeds */
    public static final int DEFENSE_WEIGHT = -36;

    /** Weight of houses that do not contain any seeds */
    public static final int MOBILITY_WEIGHT = -54;


    /**
     * {@inheritDoc}
     */
    public final int evaluate(OwareGame game) {
        final int south = game.state(SOUTH_STORE);
        final int north = game.state(NORTH_STORE);

        int score = TALLY_WEIGHT * (south - north);

        for (int house = SOUTH_LEFT; house <= SOUTH_RIGHT; house++) {
            final int seeds = game.state(house);

            if (seeds > 12) {
                score += ATTACK_WEIGHT;
            } else if (seeds == 0) {
                score += MOBILITY_WEIGHT;
            } else if (seeds < 3) {
                score += DEFENSE_WEIGHT;
            }
        }

        for (int house = NORTH_LEFT; house <= NORTH_RIGHT; house++) {
            final int seeds = game.state(house);

            if (seeds > 12) {
                score -= ATTACK_WEIGHT;
            } else if (seeds == 0) {
                score -= MOBILITY_WEIGHT;
            } else if (seeds < 3) {
                score -= DEFENSE_WEIGHT;
            }
        }

        return score;
    }
}
