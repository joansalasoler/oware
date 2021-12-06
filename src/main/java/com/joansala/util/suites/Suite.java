package com.joansala.util.suites;

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
 * Encapsulates a game suite.
 */
public class Suite {

    /** Initial board diagram */
    private String diagram;

    /** Performed moves notation */
    private String notation;


    /**
     * Create a new game suite.
     */
    public Suite(String diagram, String notation) {
        this.diagram = diagram;
        this.notation = notation;
    }


    /**
     * Start board diagram.
     */
    public String diagram() {
        return diagram;
    }


    /**
     * Performed moves.
     */
    public String notation() {
        return notation;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s", diagram, notation);
    }
}
