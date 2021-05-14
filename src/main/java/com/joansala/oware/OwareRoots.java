package com.joansala.oware;

/*
 * Aalina oware engine.
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Random;

import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import com.joansala.engine.base.BaseBook;


/**
 * Opening book implementation for oware.
 */
public class OwareRoots extends BaseBook implements Roots {

    /** Header signature for the book format */
    public static final String SIGNATURE = "Oware Opening Book ";

    /** Default path to the openings book binary file */
    public static final String ROOTS_PATH = "/oware-book.bin";

    /** Size in bytes of a database entry */
    public static final int ENTRY_SIZE = 20;

    /** Start offset for the the database entries */
    private final long OFFSET;

    /** Position of the last entry on the database */
    private final int MAX_POSITION;

    /** The database file */
    private final RandomAccessFile database;

    /** Random number generator */
    private final Random generator;

    /** Increases variety by playing weaker moves */
    private int scoreMargin = 10;

    /** True if no more moves can be returned for the current match */
    private boolean outOfBook = false;


    /**
     * Create a new endgames book instance.
     */
    public OwareRoots() throws IOException {
        this(getResourceFile(ROOTS_PATH));
    }


    /**
     * Initializes a new {@code Book} object wich will read entries from
     * a file stored on disk.
     *
     * @param file  the database file for the book
     *
     * @throws FileNotFoundException  If the file could not be opened
     * @throws IOException  If an I/O exception occurred
     */
    public OwareRoots(File file) throws IOException {
        super(file, SIGNATURE);

        generator = new Random();
        database = getDatabase();
        OFFSET = database.getFilePointer();
        MAX_POSITION = (int) ((database.length() - OFFSET) / ENTRY_SIZE);
        outOfBook = false;
    }


    /**
     * Sets the score margin for the book. Setting this margin to a value
     * greater than zero increases playing variety by allowing the book to
     * play suboptimal moves.</p>
     *
     * @param margin    an integer value greater or equal to zero
     */
    public void setScoreMargin(int margin) {
        if (margin < 0)
            throw new IllegalArgumentException(
                "Margin must be greater or equal to zero");

        this.scoreMargin = margin;
    }


    /**
     * Notifies the book intance that the next positions are going to
     * be from a different match.
     */
    public void newMatch() {
        this.outOfBook = false;
    }


    /**
     * Chooses one move at random from the best moves found on the book
     * for a given game state.
     *
     * @param game  The game to search a move for
     * @return      A random move or {@code Game.NULL_MOVE} if the
     *              position does not exist on the database or the
     *              book got out of moves for the current game
     *
     * @throws IOException  If an I/O exception occurred
     */
    public int pickBestMove(Game game) throws IOException {
        if (outOfBook == true)
            return Game.NULL_MOVE;

        int[] moves = findBestMoves(game);

        if (moves == null) {
            outOfBook = true;
            return Game.NULL_MOVE;
        }

        int index = generator.nextInt(moves.length);

        return moves[index];
    }


    /**
     * Returns a list of best moves found on the book for the provided
     * position on the {@code Game} object.
     *
     * @param game  The game to search a move for
     * @return      A list of moves
     *
     * @throws IOException  If an I/O exception occurred
     */
    public int[] findBestMoves(Game game) throws IOException {
        // Perform a binary search on the database

        int min = 0;
        int max = MAX_POSITION;
        long hash = game.hash();

        while (min <= max) {
            // Seek the position on the database

            int middle = (min + max) >>> 1;
            database.seek(OFFSET + middle * ENTRY_SIZE);

            long chash = database.readLong();

            if (chash < hash) {
                min = middle + 1;
                continue;
            }

            if (chash > hash) {
                max = middle - 1;
                continue;
            }

            // We found the position, read the scores

            short[] scores = new short[6];
            short bestScore = Short.MIN_VALUE;

            for (int i = 0; i < 6; i++) {
                scores[i] = database.readShort();

                if (scores[i] > bestScore)
                    bestScore = scores[i];
            }

            // If no best move exists, return null

            if (bestScore == Short.MIN_VALUE)
                return null;

            // Select the moves that can be played

            int[] moves = new int[6];
            int increment = (game.turn() == Game.SOUTH) ? 0 : 6;
            int length = 0;

            for (int i = 0; i < 6; i++) {
                if (bestScore >= -scoreMargin) {
                    int allowedScore = Math.max(
                        bestScore - scoreMargin,
                        -scoreMargin
                    );

                    if (scores[i] < allowedScore)
                        continue;
                } else if (scores[i] != bestScore) {
                    continue;
                }

                moves[length++] = (byte) (i + increment);
            }

            // Copy the moves into a new array and return it

            return Arrays.copyOf(moves, length);
        }

        return null;
    }

}
