package com.joansala.game.general;

/*
 * Aalina engine.
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;


/**
 * Move generator for the General Game Playing engine.
 */
public class GeneralGenerator {

    /** Default number of slots */
    private static final int DEFAULT_CAPACITY = 255;

    /** State machine for the current game rules */
    private final StateMachine machine;

    /** Stores moves and remnants for each slot */
    private final Store store = new Store();

    /** Current capacity */
    private int capacity = DEFAULT_CAPACITY;
    /** State machine for the current game rules */


    /**
     * Initialize static variables.
     */
    public GeneralGenerator(StateMachine machine) {
        this.machine = machine;
    }


    /**
     * Number of moves stored at the given index.
     *
     * @param slot      Storage slot
     * @return          Number of moves
     */
    public int getLength(int slot) {
        return store.moves[slot].length;
    }


    /**
     * Move stored at the given index.
     *
     * @param slot      Storage slot
     * @param cursor    Move index
     * @return          Move instance
     */
    public Move getMove(int slot, int cursor) {
        return store.moves[slot][cursor];
    }


    /**
     * Generate moves and store them on the given slot.
     *
     * @param slot      Storage slot
     * @param state     Machine state
     * @param role      Player to move
     */
    public void generate(int slot, MachineState state, Role role) {
        try {
            List<Move> moves = machine.getLegalMoves(state, role);
            store.moves[slot] = toSortedArray(moves);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Converts a list of moves to a sorted array. This is done to
     * ensure that moves always map to the same array position, which
     * gives us as unique move identifier on each state.
     */
    private Move[] toSortedArray(List<Move> moves) {
        Move[] result = moves.toArray(new Move[0]);
        Arrays.sort(result, moveComparator);
        return result;
    }


    /**
     * Inreases the number of slots of this generator.
     *
     * @param size          New slot size
     */
    public void ensureCapacity(int size) {
        if (size > capacity) {
            store.moves = Arrays.copyOf(store.moves, size);
            capacity = size;
            System.gc();
        }
    }


    /**
     * Compares nodes using the hash code of its contents.
     */
    private final Comparator<Move> moveComparator = (a, b) -> {
        final int ah = a.toString().hashCode();
        final int bh = b.toString().hashCode();
        return ah - bh;
    };


    /**
     * Store of generated moves at each slot
     */
    private class Store {
        Move[][] moves = new Move[DEFAULT_CAPACITY][];
    }
}
