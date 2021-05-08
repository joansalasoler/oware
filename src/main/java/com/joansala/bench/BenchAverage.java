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


/**
 * Accumulates values into an average.
 */
public final class BenchAverage implements Comparable<BenchAverage> {

    /** Number of tests */
    private long count = 0;

    /** Current average */
    private double average = 0.0;

    /** Value offset */
    private int offset = 0;


    /**
     * Number of values aggregated.
     */
    public long count() {
        return count;
    }


    /**
     * Average value.
     */
    public double average() {
        return average;
    }


    /**
     * Sets an offset for the aggregated values.
     */
    public void offset(int value) {
        offset = value;
    }


    /**
     * Adds a new value to the average.
     */
    public int aggregate(int value) {
        average += (value - offset - average) / ++count;
        return value;
    }


    /**
     * Resets this aggregator.
     */
    public void clear() {
        count = 0;
        offset = 0;
        average = 0.0;
    }


    /** {@inheritDoc} */
    @Override public int compareTo(BenchAverage o) {
        return Double.compare(average(), o.average());
    }
}
