package com.joansala.util.bench;

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

import com.joansala.engine.Cache;
import com.joansala.engine.Game;

/**
 * A decorated cache that accumulates statistics.
 */
public final class BenchCache implements Cache<Game> {

    /** Decorated cache instance */
    private Cache<Game> cache;

    /** Statistics accumulator */
    private BenchStats stats;


    /**
     * Decorates a cache object.
     */
    public BenchCache(BenchStats stats, Cache<Game> cache) {
        this.stats = stats;
        this.cache = cache;
    }


    /** {@inheritDoc} */
    public Cache<Game> cast() {
        return cache;
    }


    /** {@inheritDoc} */
    @Override public boolean find(Game game) {
        return stats.cache.test(cache.find(game));
    }


    /** {@inheritDoc} */
    @Override public int getScore() {
        return cache.getScore();
    }


    /** {@inheritDoc} */
    @Override public int getMove() {
        return cache.getMove();
    }


    /** {@inheritDoc} */
    @Override public int getDepth() {
        return cache.getDepth();
    }


    /** {@inheritDoc} */
    @Override public int getFlag() {
        return cache.getFlag();
    }


    /** {@inheritDoc} */
    @Override
    public void store(Game game, int score, int move, int depth, int flag) {
        cache.store(game, score, move, depth, flag);
    }


    /** {@inheritDoc} */
    @Override public void discharge() {
        cache.discharge();
    }


    /** {@inheritDoc} */
    @Override public void resize(long memory) {
        cache.resize(memory);
    }


    /** {@inheritDoc} */
    @Override public void clear() {
        cache.clear();
    }


    /** {@inheritDoc} */
    @Override public long size() {
        return cache.size();
    }
}
