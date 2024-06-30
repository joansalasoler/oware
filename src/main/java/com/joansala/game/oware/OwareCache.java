package com.joansala.game.oware;

/*
 * Aalina oware engine.
 * Copyright (C) 2014-2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Cache;
import com.joansala.engine.Flag;
import com.joansala.engine.Game;


/**
 * Implements a transposition table for an oware game.
 */
public class OwareCache implements Cache<OwareGame> {

    /** Default size of the cache in bytes */
    public static final long CACHE_SIZE = 256 << 20;

    /** Size of a table slot in bytes */
    public static final int SLOT_SIZE = 8;

    /** Minimum possible capacity in number of slots */
    public static final int MIN_CAPACITY = (1 << 12);

    /** Maximum possible capacity in number of slots */
    public static final int MAX_CAPACITY = (1 << 30);

    /** The number of different positions this table can store minus
        one. This value is used to compute the index for a hash key */
    private int entries;

    /** The number of slots for this hashtable */
    private int capacity;

    /** Stores the hash, score, flag and move for each position */
    private long[] data;

    /** Current entry flag (2 bits) */
    private int flag = Flag.EMPTY;

    /** Current entry hash move (4 bits) */
    private int move = Game.NULL_MOVE;

    /** Current entry depth (7 bits) */
    private int depth = 0;

    /** Current entry score (11 bits) */
    private int score = 0;

    /** Current stamp for new sored entries (2 bits) */
    private long stamp = 0x000000000L;

    /** Current entries overwrite stamp */
    private long reset = 0x100000000L;


    /**
     * Creates a new empty cache.
     */
    public OwareCache() {
        this(toCapacity(CACHE_SIZE));
    }


    /**
     * Creates a new empty transposition table with the specified
     * capacity in bytes.
     *
     * <p>The capacity of the table must be a power of two greater or
     * equal to 2 ^ 12. If size is not a power of two the table capacity
     * is set to the lowest near power of two.</p>
     *
     * @param capacity  The capacity of the table in bytes
     */
    protected OwareCache(int capacity) {
        this.entries = (capacity >> 1) - 1;
        this.capacity = capacity;
        this.data = new long[capacity];
    }


    /**
     * Creates a new empty transposition with the given size.
     *
     * @param memory  Memory size request
     */
    public OwareCache(long memory) {
        this(toCapacity(memory));
    }


    /**
     * Returns the stored score value for the last position found.
     *
     * @return  Stored score value or zero
     */
    @Override
    public int getScore() {
        return this.score;
    }


    /**
     * Returns the stored move value for the last position found.
     *
     * @return  Stored move value or {@code Game.NULL_MOVE}
     */
    @Override
    public int getMove() {
        return (this.move == 0x0F) ? Game.NULL_MOVE : move;
    }


    /**
     * Returns the stored depth value for the last position found.
     *
     * @return  Stored depth value or zero
     */
    @Override
    public int getDepth() {
        return this.depth;
    }


    /**
     * Returns the stored flag value for the last position found.
     *
     * @return  Stored flag value or {@code Flag.EMPTY}
     */
    @Override
    public int getFlag() {
        return this.flag;
    }


    /**
     * Search a position provided by a {@code Game} object and sets it
     * as the current position on the transposition table.
     *
     * <p>When a position is found subsequent calls to the getter methods
     * of this object will return the values stored for the position.</p>
     *
     * @return  {@code true} if valid information for the position
     *          could be found; {@code false} otherwise.
     */
    @Override
    public synchronized boolean find(OwareGame game) {
        final long mask = 0x00FFFFFFFFL;
        final long hash = game.hash();
        final long id = (hash >>> 12);

        int index = (int) ((hash & entries) << 1);

        if (id == (mask & data[index]) || id == (mask & data[++index])) {
            final long cdata = data[index];

            move = (int) (cdata >>> 35 & 0x0F);
            flag = (int) (cdata >>> 39 & 0x03);
            depth = (int) (cdata >>> 41 & 0x3FF);
            score = (int) (cdata >> 52);

            return true;
        }

        return false;
    }


    /**
     * Stores information about the current position in the {@code Game}
     * object on this transposition table.
     *
     * <p>For each parameter value only the lower bits are stored:
     * score (11 bits), depth (7 bits), flag (2 bits), move (4 bits),
     * game (44 bits, hash code of the current position).</p>
     *
     * <p>For the hash code of the position bits 44 to 12 (32 bits) are
     * stored explicitly on the corresponding table slot and the lower
     * 12 bits implicitly as part of the slot index.</p>
     *
     * @param game   The game for which the information about its current
     *               state must be stored
     * @param score  The evaluated score for the position
     * @param depth  The search depth with which the score was evaluated
     * @param flag   The score type as a lower bound, an upper bound,
     *               an exact value or empty.
     * @param move   The best move found so far for the position
     */
    @Override
    public synchronized void store(OwareGame game, int score, int move, int depth, int flag) {
        final long mask = 0x300000000L;
        final long hash = game.hash();

        int index = (int) ((hash & entries) << 1);

        // The first slot is a depth tier and the second is an always
        // replace slot. If the first slot is empty (depth = 0) use it.

        if (depth <= (data[index] >> 41 & 0x3FF)) {
            if (reset != (data[index] & mask))
                index++;
        }

        // Store position data on the chosen slot

        data[index] = (long) score << 52 |
                      (long) depth << 41 |
                      (long) flag << 39 |
                      (long) (move & 0x0F) << 35 |
                      stamp | hash >>> 12;
    }


    /**
     * Asks the cache to make room for new entries.
     *
     * <p>This implementation does not clear old entries immediately.
     * Instead, it increments an internal clock that marks stored
     * positions as old entries. Must be called periodically to make
     * room for new entries to be stored.</p>
     */
    @Override
    public synchronized void discharge() {
        stamp = (stamp + 0x100000000L) & 0x300000000L;
        reset = (reset + 0x100000000L) & 0x300000000L;
    }


    /**
     * Resizes this transposition table clearing all the stored data.
     *
     * @param memory  The new memory request in bytes
     */
    @Override
    public synchronized void resize(long memory) {
        int capacity = toCapacity(memory);

        if (capacity == this.capacity)
            return;

        this.entries = (capacity >> 1) - 1;
        this.capacity = capacity;

        clear();
    }


    /**
     * Clears all the information stored in this transposition table.
     */
    @Override
    public synchronized void clear() {
        this.score = 0;
        this.depth = 0;
        this.flag = Flag.EMPTY;
        this.move = Game.NULL_MOVE;
        this.data = null;

        System.gc();
        this.data = new long[capacity];
    }


    /**
     * Returns the current capacity of this cache in bytes.
     *
     * @return  Allocated bytes for the cache
     */
    @Override
    public long size() {
        return capacity * SLOT_SIZE;
    }


    /**
     * Given a memory size in bytes returns the appropiate table capacity
     * in number of slots. The returned value will be in the range
     * {@code MIN_CAPACITY}-{@code MAX_CAPACITY} always rounding the
     * number of slots to a power of two.
     */
    private static int toCapacity(long memory) {
        // The maximum array size is limited

        if (memory > (long) MAX_CAPACITY * SLOT_SIZE)
            memory = (long) MAX_CAPACITY * SLOT_SIZE;

        // Get the total number of possible slots

        int slots = (int) (memory / SLOT_SIZE);

        // Round to the nearest lower or equal power of two

        slots = (int) Math.pow(2, (int) (Math.log(slots) / Math.log(2)));

        // Each entry has two slots of size SLOT_SIZE and the minimal
        // allowed size is 4096 (11 bits, the part of the hash which
        // is not explicitly stored)

        return Math.max(MIN_CAPACITY, slots);
    }
}
