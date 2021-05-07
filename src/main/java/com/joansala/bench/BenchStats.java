package com.joansala.bench;

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

import com.joansala.util.StopWatch;


/**
 * Utility class to accumulate statistics.
 */
public final class BenchStats {

    /** Elapsed time stop-watch */
    public StopWatch watch = new StopWatch();

    /** Searched states (root states) */
    public BenchCounter moves = new BenchCounter();

    /** Visited states (moves made) */
    public BenchCounter visits = new BenchCounter();

    /** Exact evaluations */
    public BenchCounter terminal = new BenchCounter();

    /** Heuristic evaluations */
    public BenchCounter heuristic = new BenchCounter();

    /** Average reached depth */
    public BenchAverage depth = new BenchAverage();

    /** Cache probes */
    public BenchCounter cache = new BenchCounter();

    /** Cache probes */
    public BenchCounter leaves = new BenchCounter();


    /**
     * Number of visited nodes per second.
     */
    public double visitsPerSecond() {
        return visits.count() / watch.seconds();
    }


    /**
     * Average branching factor approximation.
     */
    public double branchingFactor() {
        long count = terminal.count() + heuristic.count();
        return Math.pow(count / moves.count(), 1 / depth.average());
    }
}
