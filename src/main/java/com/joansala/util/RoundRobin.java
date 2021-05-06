package com.joansala.util;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
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
 * Provides methods for the obtainment of pairings on a Round-Robin
 * tournament fashion.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class RoundRobin {

    /** Number of rounds necessary to pair each player */
    private final int NUM_ROUNDS;

    /** Number of pairings for each round */
    private final int NUM_TABLES;

    /** True if last player is bye */
    private final boolean HAS_BYE;

    /** Turn of first player on first round */
    private boolean evenTurn;


    /**
     * Instantiates a new round robin tournament.
     *
     * @param length    number of players
     */
    public RoundRobin(int length) {
        int size = length + length % 2;
        this.NUM_TABLES = size / 2;
        this.NUM_ROUNDS = size - 1;
        this.HAS_BYE = (size != length);
        this.evenTurn = false;
    }


    /**
     * Returns the number of rounds of the tournament
     *
     * @return  number of rounds
     */
    public int numRounds() {
        return NUM_ROUNDS;
    }


    /**
     * Returns the number of tables for each round
     *
     * @return  number of tables
     */
    public int numTables() {
        return NUM_TABLES;
    }


    /**
     * Inverts the order of each individual pairing.
     */
    public void invert() {
        evenTurn = !evenTurn;
    }


    /**
     * Returns the corresponding pairing for a round and table.
     *
     * @param round     round number
     * @param table     table number
     * @return          pairing
     */
    public Integer[] pairing(int round, int table) {
        Integer[] pairing = new Integer[2];
        Integer s, n;

        boolean even = (round % 2 != 0);
        int first = (even ? NUM_TABLES - 1 : 0) + (round + 1) / 2;

        if (table == 0) {
            s = HAS_BYE ? null : NUM_ROUNDS;
            n = (first + NUM_ROUNDS) % NUM_ROUNDS;

            if (even == evenTurn) {
                pairing[0] = n;
                pairing[1] = s;
            } else {
                pairing[0] = s;
                pairing[1] = n;
            }
        } else {
            s = (first + table) % NUM_ROUNDS;
            n = (first + NUM_ROUNDS - table) % NUM_ROUNDS;

            if (evenTurn) {
                pairing[0] = n;
                pairing[1] = s;
            } else {
                pairing[0] = s;
                pairing[1] = n;
            }
        }

        return pairing;
    }

}
