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

import com.joansala.engine.Game;
import com.joansala.engine.Leaves;


/**
 * A decorated endgames book that accumulates statistics.
 */
public final class BenchLeaves implements Leaves<Game> {

    /** Decorated leaves instance */
    private Leaves<Game> leaves;

    /** Statistics accumulator */
    private BenchStats stats;


    /**
     * Decorates an engames book object.
     */
    public BenchLeaves(BenchStats stats, Leaves<Game> leaves) {
        this.stats = stats;
        this.leaves = leaves;
    }


    /** {@inheritDoc} */
    public Leaves<Game> cast() {
        return leaves;
    }


    /** {@inheritDoc} */
    @Override public int getFlag() {
        return leaves.getFlag();
    }


    /** {@inheritDoc} */
    @Override public int getScore() {
        return leaves.getScore();
    }


    /** {@inheritDoc} */
    @Override public boolean find(Game game) {
        return stats.leaves.test(leaves.find(game));
    }
}
