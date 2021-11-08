package com.joansala.util;

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
 * A stop-watch to measure time.
 */
public class StopWatch {

    /** When the watch was started in milliseconds */
    private long start = 0;

    /** Seconds since the timer was started */
    private long elapsed = 0;


    /**
     * Start or resume the watch.
     */
    public void start() {
        start = System.currentTimeMillis();
    }


    /**
     * Stops the watch.
     */
    public void stop() {
        elapsed += System.currentTimeMillis() - start;
    }


    /**
     * Resets the watch without stopping it.
     */
    public void reset() {
        elapsed = 0;
    }


    /**
     * Current elapsed time in milliseconds.
     *
     * @return      Milliseconds
     */
    public long current() {
        return elapsed + System.currentTimeMillis() - start;
    }


    /**
     * Elapsed time in milliseconds. If the clock was never stopped
     * returns zero otherwise the accumulated time between stops.
     *
     * @return      Milliseconds
     */
    public long elapsed() {
        return elapsed;
    }


    /**
     * Elapsed time in seconds.
     *
     * @return      Seconds
     */
    public double seconds() {
        return elapsed / 1000.0;
    }
}
