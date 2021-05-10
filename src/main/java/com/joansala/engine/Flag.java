package com.joansala.engine;

/*
 * Aalina oware engine.
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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
 * Possible score flags.
 */
public final class Flag {

    /** Flag of an unknown score */
    public static final int EMPTY = 0;

    /** Flag of a lower bound score */
    public static final int LOWER = 1;

    /** Flag of an upper bound score */
    public static final int UPPER = 2;

    /** Flag of an exact score */
    public static final int EXACT = 3;
}
