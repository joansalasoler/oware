package com.joansala.uci.util;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.uci.UCIOption;
import com.joansala.uci.UCIService;
import static com.joansala.uci.UCI.*;


/**
 * A spin wheel that can be an integer in a certain range.
 */
public class SpinOption extends UCIOption {

    /** Default option value */
    private int fallback;

    /** Minimum value of this option */
    private int min;

    /** Maximum value of this option */
    private int max;


    /**
     * Creates a new instance.
     *
     * @param fallback      Default value
     * @param min           Minimum value
     * @param max           Maximum value
     */
    public SpinOption(int fallback, int min, int max) {
        super(Type.SPIN_TYPE);
        this.fallback = fallback;
        this.min = min;
        this.max = max;
    }


    /**
     * Sets a new default value for this option.
     */
    protected void setFallback(int fallback) {
        this.fallback = fallback;
    }


    /**
     * Sets a new minimum value for this option.
     */
    protected void setMinimum(int value) {
        this.min = value;
    }


    /**
     * Sets a new maximum value for this option.
     */
    protected void setMaximum(int value) {
        this.max = value;
    }


    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, String token) {
        handle(service, parse(token));
    }


    /**
     * Handle this operation on a parsed value.
     */
    public void handle(UCIService service, int value) {
        // Optional operation
    }


    /**
     * Converts a token to an integer value.
     */
    private int parse(String token) {
        final int value;

        try {
            value = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                token + "is not an integer value");
        }

        if (value < min || value > max) {
            throw new IllegalArgumentException(
                value + " is out of range");
        }

        return value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s %d %s %d %s %d", super.toString(),
            DEFAULT, fallback, MIN, min, MAX, max);
    }
}
