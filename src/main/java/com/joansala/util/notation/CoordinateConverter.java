package com.joansala.util.notation;

/*
 * Aalina othello engine.
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

import com.joansala.except.IllegalMoveException;


/**
 * Simple algebraic move notation converter.
 */
public class CoordinateConverter {

    /** Element not found in array */
    public static final int NOT_FOUND = -1;

    /** Notation for each board coordinate */
    private final String[] coordinates;


    /**
     * Creates a new algebraic converter.
     *
     * @param coordinates      Coordinate notations
     */
    public CoordinateConverter(String[] coordinates) {
        this.coordinates = coordinates;
    }


    /**
     * Converts a coordinate index to a coordinate notation.
     *
     * @param index         Coordinate index
     * @return              Coordinate notation
     *
     * @throws IllegalMoveException     If index is not valid
     */
    public String toCoordinate(int index) {
        if (index < 0 || index >= coordinates.length) {
            throw new IllegalMoveException(
                "Not a valid coordinate index: " + index);
        }

        return coordinates[index];
    }


    /**
     * Converts a coordinate notation to a coordinate index.
     *
     * @param coordinate    Coordinate notation
     * @return              Coordinate index
     *
     * @throws IllegalMoveException     If coordinate is not valid
     */
    public int toIndex(String coordinate) {
        for (int index = 0; index < coordinates.length; index++) {
            if (coordinate.equals(coordinates[index])) {
                return index;
            }
        }

        throw new IllegalMoveException(
            "Not a valid coordinate notation: " + coordinate);
    }
}
