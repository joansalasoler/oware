package com.joansala.engine.negamax;

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

import java.util.List;
import java.util.LinkedList;

import com.joansala.engine.*;


/**
 * Obtains search information from an engine's cache.
 */
public class CacheReport implements Report {

    /** Type of the collected score */
    private int flag = Flag.EMPTY;

    /** Maximum search depth reached */
    private int depth = 0;

    /** Current evaluation of the game */
    private int score = 0;

    /** Moves of the principal variation */
    private int[] variation = {};


    /**
     * Create a new report.
     *
     * @param game      Game state
     * @param cache     Engine cache
     * @param move      Best move
     */
    public CacheReport(Game game, Cache cache, int move) {
        if (cache != null && move != Game.NULL_MOVE) {
            collectReport(game, cache, move);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getDepth() {
        return depth;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlag() {
        return flag;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getVariation() {
        return variation;
    }


    /**
     * Collect this report data from the given game and cache.
     *
     * @param game      Game state
     * @param cache     Engine's cache
     * @param bestMove  Best move found
     */
    private void collectReport(Game game, Cache cache, int bestMove) {
        game.makeMove(bestMove);

        // Ensure the game state is cached

        if (cache.find(game) == false) {
            game.unmakeMove();
            return;
        }

        // Collect score information

        flag = cache.getFlag();
        depth = 1 + cache.getDepth();
        score = -game.toCentiPawns(cache.getScore());

        // Collect principal variation

        List<Integer> moves = new LinkedList<>();
        int move = Game.NULL_MOVE;

        while ((move = nextMove(game, cache)) != Game.NULL_MOVE) {
            game.ensureCapacity(1 + game.length());
            game.makeMove(move);
            moves.add(move);
        }

        moves.add(0, bestMove);
        variation = toArray(moves);

        // Undo all the performed moves

        for (int i = 0; i < moves.size(); i++) {
            game.unmakeMove();
        }
    }


    /**
     * Obtain the next move on the cache if it has an exact score.
     *
     * @param game      Game state
     * @param cache     Cache instance
     *
     * @return          A move value or {@code Game.NULL_MOVE}
     */
    private int nextMove(Game game, Cache cache) {
        if (!game.hasEnded() && cache.find(game)) {
            if (cache.getFlag() == Flag.EXACT) {
                return cache.getMove();
            }
        }

        return Game.NULL_MOVE;
    }


    /**
     * Convert an Integer list into an int[].
     *
     * @param values    List of values
     * @return          A new array
     */
    private int[] toArray(List<Integer> values) {
        return values.stream().mapToInt(i -> i).toArray();
    }
}
