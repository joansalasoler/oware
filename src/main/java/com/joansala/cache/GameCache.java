package com.joansala.cache;

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

import com.joansala.engine.Cache;
import com.joansala.engine.Game;


/**
 * Implements a transposition table to use by an engine.
 */
public class GameCache implements Cache<Game> {

    /** Default size of the cache in bytes */
    public static final long DEFAULT_SIZE = 192L << 20;

    /** Minimum capacity in number of slots */
    public static final int MIN_CAPACITY = (1 << 24);

    /** Size of each table slot in bytes */
    public static final int SLOT_SIZE = 12;

    /** Increase stamp by this value on each discharge */
    protected static long INCREMENT = (0b01L << 40);

    /** Bits used as an entry time stamp */
    protected static long STAMP_MASK = (0b11L << 40);

    /** Bits used as an entry identifier */
    protected static long ID_MASK = 0xFFFFFFFFFFL;

    /** Last entry found on the store */
    protected long entry = 0x00L;

    /** Move for the last entry found */
    protected int move = Game.NULL_MOVE;

    /** Aging overwrite time stamp */
    protected long reset = (0b01L << 40);

    /** Time stamp for new entries */
    protected long stamp = (0b00L << 40);

    /** Half the capacity minus one */
    protected int entries;

    /** Transposition data store */
    protected long[] store;

    /** Best moves store */
    protected int[] moves;


    /**
     * Creates a new cache with a default size.
     */
    public GameCache() {
        this(toCapacity(DEFAULT_SIZE));
    }


    /**
     * Creates a new cache with an initial size.
     *
     * @param size      Cache size in bytes
     */
    public GameCache(long size) {
        this(toCapacity(size));
    }


    /**
     * Creates a new cache with an initial capacity.
     *
     * @param capacity  Number of slots
     */
    protected GameCache(int capacity) {
        this.entries = (capacity >> 1) - 1;
        this.store = new long[capacity];
        this.moves = new int[capacity];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getMove() {
        return move;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return score(entry);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getDepth() {
        return depth(entry);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlag() {
        return flag(entry);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        return store.length * SLOT_SIZE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean find(Game game) {
        final long hash = game.hash();
        final long id = hash >>> 24;
        int slot = slot(hash);

        if (id == id(store[slot]) || id == id(store[++slot])) {
            this.entry = store[slot];
            this.move = moves[slot];
            return true;
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void store(Game game, int score, int move, int depth, int flag) {
        final long hash = game.hash();
        int slot = slot(hash);

        if (depth <= depth(store[slot])) {
            if (reset != stamp(store[slot])) {
                slot++;
            }
        }

        moves[slot] = move;
        store[slot] = encode(hash, score, depth, flag);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void resize(long size) {
        final int capacity = toCapacity(size);

        if (capacity != store.length) {
            moves = new int[capacity];
            store = new long[capacity];
            entries = (capacity >> 1) - 1;
            entry = 0x00L;
            System.gc();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void discharge() {
        stamp = (stamp + INCREMENT) & STAMP_MASK;
        reset = (reset + INCREMENT) & STAMP_MASK;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear() {
        store = new long[store.length];
        entry = 0x00L;
    }


    /**
     * Converts a size request to a number of slots. Notice that this
     * transpositions table requires a minimum capacity.
     *
     * @param size      Size request in bytes
     * @return          Number of slots
     */
    protected static int toCapacity(long size) {
        int slots = (int) (size / SLOT_SIZE);
        slots = (int) Math.pow(2, (int) (Math.log(slots) / Math.log(2)));
        return Math.max(MIN_CAPACITY, slots);
    }


    /**
     * Packs the given data to be stored in a table slot.
     */
    protected long encode(long hash, long score, long depth, long flag) {
        final long facts = (score << 52) | (depth << 44) | (flag << 42);
        return facts | stamp | (hash >>> 24);
    }


    /**
     * Slot where the given hash must be stored.
     */
    protected int slot(long hash) {
        return (int) ((hash & entries) << 1);
    }


    /**
     * Identifier of an entry (hash >>> 24).
     */
    protected long id(long entry) {
        return entry & ID_MASK;
    }


    /**
     * Time stamp of an entry.
     */
    protected long stamp(long entry) {
        return entry & STAMP_MASK;
    }


    /**
     * Search depth of an entry.
     */
    protected int depth(long entry) {
        return (int) (entry >>> 44 & 0xFF);
    }


    /**
     * Type of evaluation score of an entry.
     */
    protected int flag(long entry) {
        return (int) (entry >>> 42 & 0x03);
    }


    /**
     * Evaluation score of an entry.
     */
    protected int score(long entry) {
        return (int) (entry >> 52);
    }
}
