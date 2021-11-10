package com.joansala.engine.partner;

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

import java.util.Random;

import com.joansala.engine.Game;
import com.joansala.engine.uct.UCT;
import com.joansala.engine.uct.UCTNode;


/**
 * Cooperative MCTS algorithm.
 *
 * This is a collaborative engine with random simulations were north always
 * selects its worst possible move while south selects its best. It requires
 * an utility function (outcome) that evaluates losses as draws, because a
 * loss by one player is also a loss for both players.
 */
public class Partner extends UCT {

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.353;

    /** Random number generator */
    private Random random = new Random();


    /**
     * Create a new search engine.
     */
    public Partner() {
        super(DEFAULT_BIAS);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected double computeScore(UCTNode node) {
        return -node.turn() * super.computeScore(node);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected int simulateMatch(int maxDepth) {
        int depth = 0;

        while (depth < maxDepth && !game.hasEnded()) {
            final int move = getRandomMove(game);
            game.makeMove(move);
            depth++;
        }

        final int score = game.outcome();

        for (int i = 0; i < depth; i++) {
            game.unmakeMove();
        }

        return score;
    }


    /**
     * Pick a random legal move given a game state.
     *
     * @param game      Game state
     * @return          Chosen move
     */
    private int getRandomMove(Game game) {
        int count = 0;
        int move = Game.NULL_MOVE;
        int choice = Game.NULL_MOVE;

        while ((move = game.nextMove()) != Game.NULL_MOVE) {
            if (random.nextInt(++count) == 0) {
                choice = move;
            }
        }

        return choice;
    }
}
