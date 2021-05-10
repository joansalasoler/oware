package com.joansala.util;

/*
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

import java.util.Iterator;


/**
 * Infinite Round-Robin iterator.
 */
public class RoundRobin<T> implements Iterator<T> {

    /** Items to match */
    private T[] items;

    /** Number of tables */
    private int tables;

    /** Number of rounds */
    private int rounds;

    /** Number of items */
    private int size;

    /** Next generation index*/
    private int next;

    /** Current round */
    private int round;


    /**
     * Creates a new iterator.
     */
    public RoundRobin(T[] items) {
        final int length = items.length;
        this.size = length + length % 2;
        this.items = items;
        this.tables = size / 2;
        this.rounds = size - 1;
        this.round = 0;
        this.next = 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override public boolean hasNext() {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override public T next() {
        if (size != items.length && next < 2) {
            next = 2;
        }

        final boolean even = (round % 2 != 0);
        final boolean turn = (next % 2 != 0);
        final boolean invert = (round / rounds) % 2 == 0;
        final int first = (even ? tables - 1 : 0);
        final int offset = (first + (1 + round) / 2);
        final int table = (int) (next / 2);


        int index = size - 1;

        if (table != 0 || turn == even) {
            int o = (turn == invert) ? rounds - table : table;
            index = (offset + o) % rounds;
        }

        if (next >= rounds) {
            next = 0;
            round++;
        } else {
            next++;
        }

        return items[index];
    }
}
