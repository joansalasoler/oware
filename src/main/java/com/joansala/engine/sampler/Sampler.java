package com.joansala.engine.sampler;

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
import java.util.TimerTask;
import com.joansala.engine.Game;
import com.joansala.engine.base.BaseEngine;


/**
 * An engine that chooses the best move by random sampling.
 */
public class Sampler extends BaseEngine {

    /** Random number generator */
    private Random random = new Random();

    /** Best score found so far */
    private int bestScore = Integer.MAX_VALUE;


    /**
     * Create a new search engine.
     */
    public Sampler() {
        super();
    }


    /**
     * Computes the best move for a game and returns its score.
     *
     * @param game      Game instance
     * @return          Average outcome score
     */
    public synchronized int computeBestScore(Game game) {
        computeBestMove(game);
        return -bestScore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        if (game.hasEnded()) {
            bestScore = -(game.outcome() * game.turn());
            return Game.NULL_MOVE;
        }

        final TimerTask countDown = scheduleCountDown();
        game.ensureCapacity(MAX_DEPTH + game.length());

        int[] moves = game.legalMoves();
        double[] outcomes = new double[moves.length];
        long count = 1L;

        // Play random maches and average their outcome

        while (!aborted() || count <= 1L) {
            for (int i = 0; i < moves.length; i++) {
                final double outcome = outcomes[i];
                final int move = moves[i];

                game.makeMove(move);
                int value = simulateMatch(game, maxDepth - 1);
                outcomes[i] += (value - outcome) / count;
                game.unmakeMove();
            }

            if (count++ == Long.MAX_VALUE) {
                break;
            }
        }

        // Pick the move with the best average outcome

        double bestOutcome = outcomes[0];
        int bestMove = moves[0];

        for (int i = 1; i < moves.length; i++) {
            if (outcomes[i] > bestOutcome) {
                bestOutcome = outcomes[i];
                bestMove = moves[i];
            }
        }

        bestScore = (int) bestOutcome;
        countDown.cancel();

        return bestMove;
    }


    /**
     * Simulates a match and returns its outcome.
     *
     * @param depth     Maximum simulation depth
     * @return          Outcome of the simulation
     */
    private int simulateMatch(Game game, int maxDepth) {
        int depth = 0;

        while (depth < maxDepth && !game.hasEnded()) {
            final int move = getRandomMove(game);
            game.makeMove(move);
            depth++;
        }

        final int outcome = game.outcome();

        for (int i = 0; i < depth; i++) {
            game.unmakeMove();
        }

        return outcome;
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
