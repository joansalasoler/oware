package com.joansala.book.base;

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
import java.util.Random;
import java.util.List;
import java.util.LinkedList;
import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import com.joansala.book.base.BookEntry;
import com.joansala.book.base.BookReader;
import static com.joansala.engine.Game.*;


/**
 * Base opening book implementation.
 */
public class BaseRoots implements Closeable, Roots<Game> {

    /** Reads the book data from a file */
    private final BookReader reader;

    /** Random number generator */
    private final Random random;

    /** Contempt factor to choose suboptimal moves */
    protected int contempt = Game.DRAW_SCORE;

    /** If no more book moves can be found */
    private boolean outOfBook;

    /** Maximum evaluation score */
    private int maxScore = Integer.MAX_VALUE;


    /**
     * Create a book for the given file path.
     */
     public BaseRoots(String path) throws IOException {
         reader = new BookReader(path);
         random = new Random();
         outOfBook = false;
     }


    /**
     *
     *
     * @see Engine#setContempt(int)
     * @param score     Contempt score
     */
    public void setContempt(int score) {
        contempt = score;
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
     * {@inheritDoc}
     */
    @Override
    public int pickBestMove(Game game) throws IOException {
        if (outOfBook == false) {
            List<BookEntry> entries = readChildren(game);
            entries.removeIf(e -> !game.isLegal(e.getMove()));

            if ((outOfBook = entries.isEmpty()) == false) {
                BookEntry bestEntry = pickBestEntry(entries);
                double minScore = computeScore(bestEntry) + contempt;
                entries.removeIf(e -> computeScore(e) > minScore);

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
            return pickPromisingEntry(entries).getMove();
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
    private BookEntry pickBestEntry(List<BookEntry> entries) {
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
    private BookEntry pickPromisingEntry(List<BookEntry> entries) {
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
     * Picks a random element from a list of entries.
     *
     * @param entries       List of entries
     * @return              An entry on the list
     */
    private BookEntry pickRandomEntry(List<BookEntry> entries) {
        return entries.get(random.nextInt(entries.size()));
    }


    /**
     * Compute the selection score of a node.
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
     * Reads all the book entries for the given game state.
     *
     * @param game      Game state
     * @return          Book entries
     */
    private List<BookEntry> readChildren(Game game) throws IOException {
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
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        reader.close();
    }
}
