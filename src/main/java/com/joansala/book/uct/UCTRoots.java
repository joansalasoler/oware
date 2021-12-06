package com.joansala.book.uct;

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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import com.joansala.book.uct.BookEntry;
import com.joansala.book.uct.BookReader;
import static com.joansala.engine.Game.*;


/**
 * UCT opening book implementation.
 */
public class UCTRoots implements Closeable, Roots<Game> {

    /** Reads the book data from a file */
    private final BookReader reader;

    /** Disturbance score */
    private double disturbance = Game.DRAW_SCORE;

    /** Threshold score */
    private double threshold = Game.DRAW_SCORE;

    /** If no more book moves can be found */
    private boolean outOfBook = false;

    /** Maximum evaluation score */
    private int maxScore = Integer.MAX_VALUE;


    /**
     * Create a book for the given file path.
     */
     public UCTRoots(String path) throws IOException {
         reader = new BookReader(path);
     }


    /**
     * Choose moves only if their score is above this distance from
     * the best score found on its siblings.
     *
     * @param score     Disturbance score
     */
    public void setDisturbance(double score) {
        disturbance = Math.abs(score);
    }


    /**
     * Minimum score a move must have to be playable.
     *
     * @param score     Threshold score
     */
    public void setThreshold(double score) {
        threshold = score;
    }


    /**
     * Sets the maximum score a position can possibly be evaluated.
     *
     * @see Engine#setInfinity(int)
     * @param score     Maximum score
     */
    public void setInfinity(int score) {
        maxScore = Math.max(score, 1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void newMatch() {
        outOfBook = false;
    }


    /**
     * Compute the selection score of a node. This method returns an
     * upper confidence bound on the score of the entry.
     *
     * @param node      A node
     * @return          Score of the node
     */
    private double computeScore(BookEntry entry) {
        final double bound = maxScore / Math.sqrt(entry.getCount());
        final double score = entry.getScore() + bound;

        return score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int pickBestMove(Game game) throws IOException {
        if (outOfBook == true) {
            return NULL_MOVE;
        }

        List<BookEntry> entries = readChildren(game);
        entries.removeIf(e -> !game.isLegal(e.getMove()));

        if ((outOfBook = entries.isEmpty()) == false) {
            BookEntry secure = pickSecureEntry(entries);
            double minScore = disturbance + computeScore(secure);

            entries.removeIf(e -> computeScore(e) > minScore);
            entries.removeIf(e -> computeScore(e) > -threshold);

            if ((outOfBook = entries.isEmpty()) == false) {
                return pickRandomEntry(entries).getMove();
            }
        }

        return NULL_MOVE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int pickPonderMove(Game game) throws IOException {
        List<BookEntry> entries = readChildren(game);
        entries.removeIf(e -> !game.isLegal(e.getMove()));

        if (entries.isEmpty() == false) {
            return pickMaxEntry(entries).getMove();
        }

        return NULL_MOVE;
    }


    /**
     * Picks the entry which provides the best move. This is the
     * entry for which its score's lowest bound is greater.
     *
     * @param entries       List of entries
     * @return              Best entry on the list
     */
    protected BookEntry pickSecureEntry(List<BookEntry> entries) {
        BookEntry bestEntry = entries.get(0);
        double bestScore = computeScore(bestEntry);

        for (BookEntry entry : entries) {
            final double score = computeScore(entry);

            if (score < bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }

        return bestEntry;
    }


    /**
     * Picks the entry with the highest average score.
     *
     * @param entries       List of entries
     * @return              Best entry on the list
     */
    protected BookEntry pickMaxEntry(List<BookEntry> entries) {
        BookEntry bestEntry = entries.get(0);
        double bestScore = bestEntry.getScore();

        for (BookEntry entry : entries) {
            final double score = entry.getScore();

            if (score < bestScore) {
                bestScore = score;
                bestEntry = entry;
            }
        }

        return bestEntry;
    }


    /**
     * Picks a random element from a list of entries. This performs
     * a weighted random choice using entry counts as weights.
     *
     * @param entries       List of entries
     * @return              An entry on the list
     */
    protected BookEntry pickRandomEntry(List<BookEntry> entries) {
        double distance = count(entries) * Math.random();

        for (BookEntry entry : entries) {
            if ((distance -= entry.getCount()) < 0.0D) {
                return entry;
            }
        }

        return entries.get(0);
    }


    /**
     * Reads all the book entries for the given game state.
     *
     * @param game      Game state
     * @return          Book entries
     */
    protected List<BookEntry> readChildren(Game game) throws IOException {
        List<BookEntry> entries = new LinkedList<>();
        long parent = game.hash();

        for (long child : childHashes(game)) {
            final BookEntry entry;

            if ((entry = reader.readEntry(parent, child)) != null) {
                entries.add(entry);
            }
        }

        return entries;
    }


    /**
     * Obtains the hash codes of each child state of a game state.
     *
     * @param game      Game state
     * @return          A new hash array
     */
    private long[] childHashes(Game game) {
        final int[] moves = game.legalMoves();
        final long[] hashes = new long[moves.length];

        int cursor = game.getCursor();
        game.ensureCapacity(1 + game.length());

        for (int i = 0; i < moves.length; i++) {
            game.makeMove(moves[i]);
            hashes[i] = game.hash();
            game.unmakeMove();
        }

        game.setCursor(cursor);

        return hashes;
    }


    /**
     * Sum expansion counts of a list of entries.
     *
     * @param entries       List of entries
     * @return              Sum counts
     */
    private long count(List<BookEntry> entries) {
        long count = 0L;

        for (BookEntry entry : entries) {
            count += entry.getCount();
        }

        return count;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
