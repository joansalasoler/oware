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

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import com.joansala.engine.Roots;
import com.joansala.engine.base.BaseBook;
import com.joansala.oware.OwareGame;
import static com.joansala.engine.Game.*;
import static com.joansala.oware.Oware.*;


/**
 * Opening book implementation for oware.
 */
public class OwareRoots extends BaseBook implements Roots<OwareGame> {

    /** Default path to the openings book binary file */
    public static final String ROOTS_PATH = "/oware-book.bin";

    /** Size in bytes of a database entry */
    public static final int ENTRY_SIZE = 20;

    /** Number of entries on the database */
    private final long ENTRY_COUNT;

    /** Random number generator */
    private final Random random;

    /** True if no more moves can be returned for the current match */
    private boolean outOfBook = false;

    /** Increases variety by playing weaker moves */
    private int margin = 10;


    /**
     * Create a new endgames book instance.
     */
    public OwareRoots() throws IOException {
        this(getResourcePath(ROOTS_PATH));
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
    public OwareRoots(String path) throws IOException {
        super(path);
        random = new Random();
        ENTRY_COUNT = (file.length() - offset) / ENTRY_SIZE;
    }


    /**
     * Obtain a path to the given resource file.
     */
    private static String getResourcePath(String path) {
        return BaseBook.class.getResource(path).getFile();
    }


    /**
     * Notifies the book intance that the next positions are going to
     * be from a different match.
     */
    @Override
    public void newMatch() {
        this.outOfBook = false;
    }


    /**
     * Sets the score margin for the book. Setting this margin to a value
     * greater than zero increases playing variety by allowing the book to
     * play suboptimal moves.</p>
     *
     * @param margin    an integer value greater or equal to zero
     */
    public void setScoreMargin(int margin) {
        this.margin = Math.max(0, margin);
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
    @Override
    public int pickBestMove(OwareGame game) throws IOException {
        if (outOfBook == true) {
            return NULL_MOVE;
        }

        int move = NULL_MOVE;
        int[] moves = findBestMoves(game);

        if (moves.length > 0) {
            int choice = random.nextInt(moves.length);
            move = moves[choice];
        } else {
            outOfBook = true;
        }

        return move;
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
    public int[] findBestMoves(OwareGame game) throws IOException {
        final int size = BOARD_SIZE / 2;
        final int[] moves = new int[size];
        final short[] scores = readScores(game);

        final int turn = game.turn();
        final int left = (turn == SOUTH) ? SOUTH_LEFT : NORTH_LEFT;

        short bestScore = Short.MIN_VALUE;

        for (int i = 0; i < size; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
            }
        }

        int length = 0;
        int minScore = Math.max(bestScore - margin, -margin);

        if (bestScore > Short.MIN_VALUE) {
            for (int i = 0; i < size; i++) {
                final short score = scores[i];

                if (score == bestScore || score >= minScore) {
                    moves[length++] = i + left;
                }
            }
        }

        return Arrays.copyOf(moves, length);
    }


    /**
     * Performs a binary search on the openings book file to find a position
     * and returns the scores of its moves.
     *
     * @param game      Game state to read
     * @return          New move scores array
     */
    private short[] readScores(OwareGame game) throws IOException {
        final int size = BOARD_SIZE / 2;
        final long gameHash = game.hash();
        final short[] scores = new short[size];
        Arrays.fill(scores, Short.MIN_VALUE);

        long hash = -1;
        long first = 0;
        long middle = 0;
        long last = ENTRY_COUNT;

        while (hash != gameHash && first <= last) {
            middle = (first + last) / 2;
            file.seek(offset + middle * ENTRY_SIZE);
            hash = file.readLong();

            if (hash < gameHash) {
                first = middle + 1;
            } else if (hash > gameHash) {
                last = middle - 1;
            }
        }

        if (hash == gameHash) {
            for (int i = 0; i < size; i++) {
                scores[i] = file.readShort();
            }
        }

        return scores;
    }
}
