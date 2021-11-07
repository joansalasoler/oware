package com.joansala.game.draughts;

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
import com.joansala.engine.Game;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.draughts.Draughts.*;


/**
 * Move generator for international draughts.
 */
public class DraughtsGenerator {

    /** Default number of slots */
    private static final int DEFAULT_CAPACITY = 255;

    /** Maximum possible distinct moves per slot */
    private static final int MAX_MOVES = 128;

    /** Maximum possible captures by a piece */
    private static final int MAX_CAPTURES = 20;

    /** Stores moves and remnants for each slot */
    private Store store = new Store();

    /** Stores moves, remnants and the path of a traced move */
    private Trace trace = new Trace();

    /** Current capacity */
    private int capacity = DEFAULT_CAPACITY;

    /** Moves generated (current slot) */
    private int[] moves = null;

    /** Alive rivals after captures (current slot) */
    private long[] remnants = null;

    /** Keeps track of the number of captures */
    private int threshold = MAX_CAPTURES;

    /** Bloom filter of duplicated captures */
    private long filter = 0x00L;

    /** Origin move encoding */
    private int from = 0x00;

    /** Next move index */
    private int index = 1;


    /**
     * Move stored at the given index.
     *
     * @param slot      Storage slot
     * @param cursor    Move index
     * @return          Move identifier
     */
    public int getMove(int slot, int cursor) {
        return store.moves[slot][cursor];
    }


    /**
     * Bitboard of alive rivals after a capture.
     *
     * @param slot      Storage slot
     * @param cursor    Move index
     * @return          Bitboard
     */
    public long getRemnants(int slot, int cursor) {
        return store.remnants[slot][cursor];
    }


    /**
     * Generate moves and store them on the given slot.
     *
     * @param slot      Storage slot
     * @param sense     Sense of movement
     * @param mobility  Mobility bitboard
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     * @param kings     King of the player to move
     */
    public void generate(int slot, int sense, long mobility, long free, long rivals, long kings) {
        this.moves = store.moves[slot];
        this.remnants = store.remnants[slot];
        generate(sense, mobility, free, rivals, kings);
    }


    /**
     * Generate moves and store them on the moves array.
     *
     * @param sense     Sense of movement
     * @param mobility  Mobility bitboard
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     * @param kings     King of the player to move
     */
    private void generate(int sense, long mobility, long free, long rivals, long kings) {
        this.index = 1;
        this.threshold = MAX_CAPTURES;

        if (mobility < 0L) {
            final long pieces = mobility & BOARD_BITS;
            storeKingCaptures(pieces & kings, free, rivals);
            storeManCaptures(pieces & ~kings, free, rivals);
        } else {
            final long pieces = mobility & BOARD_BITS;
            storeManSliders(pieces & ~kings, free, sense);
            storeKingSliders(pieces & kings, free);
        }

        this.moves[index] = Game.NULL_MOVE;
    }


    /**
     * Traces a path of captures for the given move.
     *
     * @param move      Move to trace
     * @param sense     Sense of movement
     * @param mobility  Mobility bitboard
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     * @param kings     King of the player to move
     */
    public int[] trace(int move, int sense, long mobility, long free, long rivals, long kings) {
        this.moves = trace.moves;
        this.remnants = trace.remnants;

        trace.move = move;
        Arrays.fill(trace.path, Game.NULL_MOVE);
        generate(sense, mobility, free, rivals, kings);
        trace.move = Game.NULL_MOVE;

        return toCheckers(trace.path);
    }


    /**
     * Encodes a slider move for its storage.
     *
     * @param to        Destination checker
     */
    private int encodeSlider(int to) {
        return from | to;
    }


    /**
     * Encodes a capture move for its storage.
     *
     * @param to        Destination checker
     * @param index     Storage index
     */
    private int encodeCapture(int to, int index) {
        return (index << 12) | from | to;
    }


    /**
     * Stores an slider on the moves array.
     *
     * @param to        Destination checker
     */
    private void storeSlider(int to) {
        moves[index] = encodeSlider(to);
        index++;
    }


    /**
     * Stores an capture on the moves array.
     *
     * Only moves that maximize the number of captured rivals will
     * remain on the moves array. If a capture is already stored on the
     * array —it is a duplicate— it won't be stored again.
     *
     * @param to        Destination checker
     * @param rivals    Rivals after the capture
     */
    private void storeCapture(long target, long rivals) {
        final int count = count(rivals);
        final int to = first(target);

        // Store the capture move

        if (count < threshold) {
            moves[1] = encodeCapture(to, 1);
            remnants[1] = rivals;
            threshold = count;
            filter = ~rivals;
            index = 2;
        } else if (count == threshold) {
            if (duplicated(to, rivals) == false) {
                moves[index] = encodeCapture(to, index);
                remnants[index] = rivals;
                filter |= ~rivals;
                index++;
            }
        }

        // Stop tracing when the traced move is found

        if (trace.move != Game.NULL_MOVE) {
            if (trace.move == moves[index - 1]) {
                trace.move = Game.NULL_MOVE;
            }
        }
    }


    /**
     * Checks if a capture is stored on the moves array.
     *
     * A capture is stored if the moves array contains a move that has
     * the same origin, target checkers and captures the same rivals.
     * To avoid iterating the moves array each time this method uses
     * a simplist Bloom filter of captured rivals.
     *
     * @param to        Destination checker
     * @param rivals    Rivals after the capture
     * @return          If an equal capture is already stored
     */
    private boolean duplicated(int to, long rivals) {
        if (~rivals != (filter & ~rivals)) {
            return false;
        }

        for (int i = index - 1; i >= 1; i--) {
            if (from != (moves[i] & 0xFC0)) {
                return false;
            }

            if (rivals == remnants[i]) {
                if (to == (moves[i] & 0x03F)) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Store slider moves for a set of men on the moves array.
     *
     * @param men       Men checkers
     * @param free      Unoccupied checkers
     * @param sense     Sense of the movement
     */
    private void storeManSliders(long men, long free, int sense) {
        final int right = NE ^ sense;
        final int left = NW ^ sense;

        while (empty(men) == false) {
            final int from = first(men);
            final long man = bit(from);
            final long empty = free | man;

            this.from = from << 6;
            slidersByMan(man, empty, right);
            slidersByMan(man, empty, left);

            men ^= man;
        }
    }


    /**
     * Store slider moves for a set of kings on the moves array.
     *
     * @param king      King checkers
     * @param free      Unoccupied checkers
     */
    private void storeKingSliders(long kings, long free) {
        while (empty(kings) == false) {
            final int from = first(kings);
            final long king = bit(from);
            final long empty = free | king;

            this.from = from << 6;
            slidersByKing(king, empty, NE);
            slidersByKing(king, empty, NW);
            slidersByKing(king, empty, SE);
            slidersByKing(king, empty, SW);

            kings ^= king;
        }
    }


    /**
     * Store capture moves for a set of men on the moves array.
     *
     * @param men       Men checkers
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     */
    private void storeManCaptures(long men, long free, long rivals) {
        while (empty(men) == false) {
            final int from = first(men);
            final long man = bit(from);
            final long empty = free | man;

            this.filter = 0x00L;
            this.from = from << 6;
            capturesByMan(man, empty, rivals, NE);
            capturesByMan(man, empty, rivals, NW);
            capturesByMan(man, empty, rivals, SE);
            capturesByMan(man, empty, rivals, SW);

            men ^= man;
        }
    }


    /**
     * Store capture moves for a set of kings on the moves array.
     *
     * @param king      King checkers
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     */
    private void storeKingCaptures(long kings, long free, long rivals) {
        while (empty(kings) == false) {
            final int from = first(kings);
            final long king = bit(from);
            final long empty = free | king;

            this.filter = 0x00L;
            this.from = from << 6;
            capturesByKing(king, empty, rivals, NE);
            capturesByKing(king, empty, rivals, NW);
            capturesByKing(king, empty, rivals, SE);
            capturesByKing(king, empty, rivals, SW);

            kings ^= king;
        }
    }


    /**
     * Find legal man slider moves on a direction and store them.
     *
     * @param man       Origin checker
     * @param free      Unoccupied checkers
     * @param direction Direction of the slide
     */
    private void slidersByMan(long man, long free, int direction) {
        long checker = shift(man, direction);

        if (contains(free, checker)) {
            storeSlider(first(checker));
        }
    }


    /**
     * Find legal king slider moves on a direction and store them.
     *
     * @param king      Origin checker
     * @param free      Unoccupied checkers
     * @param direction Direction of the slide
     */
    private void slidersByKing(long king, long free, int direction) {
        long checker = shift(king, direction);

        while (contains(free, checker)) {
            storeSlider(first(checker));
            checker = shift(checker, direction);
        }
    }


    /**
     * Search for man captures on the given direction and store them.
     *
     * @param man       Origin checker
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     * @param direction Direction of the slide
     */
    private void capturesByMan(long man, long free, long rivals, int direction) {
        long target = shift(man, direction);
        long empty = shift(free, direction ^ 64);

        if (trace.move != Game.NULL_MOVE) {
            trace.path[count(rivals)] = first(man);
        }

        if (contains(rivals & empty, target)) {
            long checker = shift(target, direction);
            rivals ^= target;

            capturesByMan(checker, free, rivals, direction);
            capturesByMan(checker, free, rivals, direction ^ 3);
            capturesByMan(checker, free, rivals, direction ^ 67);
            storeCapture(checker, rivals);
        }
    }


    /**
     * Search for king captures on the given direction and store them.
     *
     * @param king      Origin checker
     * @param free      Unoccupied checkers
     * @param rivals    Pieces that can be captured
     * @param direction Direction of the slide
     */
    private void capturesByKing(long king, long free, long rivals, int direction) {
        long target = target(king, free, direction);
        long empty = shift(free, direction ^ 64);

        if (trace.move != Game.NULL_MOVE) {
            trace.path[count(rivals)] = first(king);
        }

        if (contains(rivals & empty, target)) {
            long checker = shift(target, direction);
            rivals ^= target;

            capturesByKing(checker, free, rivals, direction);

            while (contains(free, checker)) {
                capturesByKing(checker, free, rivals, direction ^ 3);
                capturesByKing(checker, free, rivals, direction ^ 67);
                storeCapture(checker, rivals);
                checker = shift(checker, direction);
            }
        }
    }


    /**
     * Finds the next target checker of a king. That is, the last
     * free checker a king can move to on a given direction.
     *
     * @param king      Origin checker
     * @param free      Unoccupied checkers
     * @param direction Direction of the slide
     *
     * @return          Last free checker
     */
    private long target(long king, long free, int direction) {
        long target = shift(king, direction);

        while (contains(free, target)) {
            target = shift(target, direction);
        }

        return target;
    }


    /**
     * Converts a traced path into an array of checkers.
     *
     * This method reverts the order of the trace and removes any
     * empty elements from the start and end of the trace array.
     * Returns a new array with the result.
     *
     * @param path      Traced captures path
     * @return          Traveled checkers array
     */
    private int[] toCheckers(int[] path) {
        int[] checkers = new int[MAX_CAPTURES];
        int size = 0;

        for (int i = path.length - 1; i >= 0; i--) {
            if (path[i] != Game.NULL_MOVE) {
                checkers[size++] = path[i];
            }
        }

        return Arrays.copyOf(checkers, size);
    }


    /**
     * Inreases the number of slots of this generator.
     *
     * @param size          New slot size
     */
    public void ensureCapacity(int size) {
        if (size < capacity) {
            return;
        }

        int[][] moves = new int[size][MAX_MOVES];
        long[][] remnants = new long[size][MAX_MOVES];

        for (int slot = 0; slot < store.moves.length; slot++) {
            remnants[slot] = store.remnants[slot];
            moves[slot] = store.moves[slot];
        }

        store.remnants = remnants;
        store.moves = moves;
        capacity = size;
        System.gc();
    }


    /**
     * Store of generated moves at each slot
     */
    private class Store {
        int[][] moves = new int[DEFAULT_CAPACITY][MAX_MOVES];
        long[][] remnants = new long[DEFAULT_CAPACITY][MAX_MOVES];
    }


    /**
     * Store of traced moves.
     */
    private class Trace {
        int move = Game.NULL_MOVE;
        int[] path = new int[1 + MAX_CAPTURES];
        int[] moves = new int[MAX_MOVES];
        long[] remnants = new long[MAX_MOVES];
    }
}
