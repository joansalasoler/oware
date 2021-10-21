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
public class Algebraic {

    /** Element not found in array */
    public static final int NOT_FOUND = -1;

    /** Notation for each board checker */
    private final String[] checkers;


    /**
     * Creates a new algebraic converter.
     *
     * @param checkers      Coordinate notations
     */
    public Algebraic(String[] checkers) {
        this.checkers = checkers;
    }


    /**
     * Converts a checker index to a checker notation.
     *
     * @param checker       Checker index
     * @return              Coordinate notation
     *
     * @throws IllegalMoveException     If checker is not valid
     */
    public String toCoordinate(int checker) {
        if (checker < 0 || checker >= checkers.length) {
            throw new IllegalMoveException(
                "Not a valid checker: " + checker);
        }

        return checkers[checker];
    }


    /**
     * Converts a checker notation to a checker index.
     *
     * @param coordinate    Coordinate notation
     * @return              Checker index
     *
     * @throws IllegalMoveException     If coordinate is not valid
     */
    public int toChecker(String coordinate) {
        for (int index = 0; index < checkers.length; index++) {
            if (coordinate.equals(checkers[index])) {
                return index;
            }
        }

        throw new IllegalMoveException(
            "Not a valid notation: " + coordinate);
    }
}
