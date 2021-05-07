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
 * Counts tests.
 */
public final class BenchCounter {

    /** Number of tests */
    private long count = 0;

    /** Number of failed tests */
    private long fails = 0;


    /**
     * Number of tests counted.
     */
    public long count() {
        return count;
    }


    /**
     * Number of successful tests.
     */
    public long success() {
        return count - fails;
    }


    /**
     * Ratio of successful tests.
     */
    public double ratio() {
        return (double) success() / count;
    }


    /**
     * Ratio of successful tests as a percentage.
     */
    public double percentage() {
        return 100 * ratio();
    }


    /**
     * Adds one to the number of successful tests.
     */
    public void increment() {
        count++;
    }


    /**
     * Adds one to the number of successful tests when the given boolean
     * value is true. Otherwise the test is marked as failed.
     */
    public boolean test(boolean value) {
        fails += value ? 0 : 1; count++;
        return value;
    }


    /**
     * Resets this counter.
     */
    public void clear() {
        count = 0;
        fails = 0;
    }
}
