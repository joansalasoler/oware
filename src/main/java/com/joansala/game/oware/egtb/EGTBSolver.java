package com.joansala.game.oware.egtb;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.util.hash.BinomialHash;
import com.joansala.engine.*;
import com.joansala.game.oware.*;
import static com.joansala.engine.Game.*;


/**
 * Oware endgames book builder.
 */
public class EGTBSolver {

    /** Hash function */
    private BinomialHash hasher = new BinomialHash(15, 13);

    /** Database of expanded nodes */
    private EGTBStore store;

    /** Current game state */
    private EGTBGame game = new EGTBGame();

    /** Placeholder for rotated game states */
    private int[] rotated = new int[13];

    /** Set to true to abort all expansions */
    private volatile boolean aborted = false;


    /**
     * Creates a new endgames solver instance.
     */
    public EGTBSolver(EGTBStore store) {
        this.store = store;
    }


    /**
     * Stops all the ongoing computations.
     */
    public void abortComputation() {
        this.aborted = true;
    }


    /**
     * Solves all positions with up to {@code maxSeeds} seeds.
     *
     * Because position repetitions change the evaluation of a node, not
     * all the nodes will receive an exact score.
     *
     * @param maxSeeds      Maximum number of seeds on a board
     */
    public synchronized void solve(int maxSeeds) throws Exception {
        aborted = false;

        for (int seeds = 1; !aborted && seeds <= maxSeeds; seeds++) {
            System.out.format("Solving nodes with %d seeds%n", seeds);

            final long first = hasher.offset(seeds - 1);
            final long last = hasher.offset(seeds) - 1;

            create(first, last, seeds);
            propagate(first, last);
        }
    }


    /**
     * Creates all the nodes on the given set and initializes them.
     *
     * @param first         Hash of the first node
     * @param last          Hash of the last node
     */
    private void create(long first, long last, int seeds) {
        for (long hash = first; !aborted && hash <= last; hash++) {
            EGTBNode node = new EGTBNode(hash);
            game.setBoard(toBoard(hash));

            if (game.hasEnded()) {
                game.endMatch();
                node.flag = Flag.EXACT;
                node.score = game.outcome();
                node.known = true;
            }

            node.seeds = seeds;
            store.write(node);
        }
    }


    /**
     * Backpropagates the exact scores for the given set of node.
     *
     * @param first         Hash of the first node
     * @param last          Hash of the last node
     */
    private void propagate(long first, long last) {
        boolean assigned = true;

        while (assigned && !aborted) {
            assigned = false;

            for (long hash = first; !aborted && hash <= last; hash++) {
                EGTBNode node = store.read(hash);

                if (node.known == false) {
                    boolean known = assign(node);
                    assigned = assigned || known;
                }
            }
        }
    }


    /**
     * Computes the best score obtainable from the given game state.
     *
     * @param node          Node to evaluate
     * @return              If an exact score was assigned
     */
    private boolean assign(EGTBNode node) {
        int move = NULL_MOVE;
        int bestScore = -node.seeds;
        boolean known = true;

        game.setBoard(toBoard(node.hash));

        while ((move = game.nextMove()) != NULL_MOVE) {
            game.makeMove(move);
            EGTBNode child = store.read(toHash(game));
            int score = score(node, child);
            game.unmakeMove();

            if (score == node.seeds) {
                bestScore = score;
                known = true;
                break;
            }

            bestScore = Math.max(score, bestScore);
            known = known && child.known;
        }

        if (known == true) {
            node.score = bestScore;
            node.flag = Flag.EXACT;
            node.known = true;
            store.write(node);
        }

        return known;
    }


    /**
     * Computes the score of an edge.
     */
    private int score(EGTBNode parent, EGTBNode child) {
        return parent.seeds - child.seeds - child.score;
    }


    /**
     * Converts the current state of a game to a unique binomial
     * hash code suitable for use as an endgames book entry.
     */
    private long toHash(EGTBGame game) {
        final OwareBoard board = game.toBoard();
        final int[] state = board.position();
        return hasher.hash(rotate(state));
    }


    /**
     * Convert a hash to its board representation.
     */
    private Board toBoard(long hash) {
        final int[] state = hasher.unhash(hash);
        return new EGTBBoard(state);
    }


    /**
     * Rotates an Oware position array so it is seen from the point
     * of view of the opponent player.
     */
    private int[] rotate(int[] state) {
        System.arraycopy(state, 6, rotated, 0, 6);
        System.arraycopy(state, 0, rotated, 6, 6);

        rotated[12] = 15;

        for (int i = 0; i < 12; i++) {
            rotated[12] -= state[i];
        }

        return rotated;
    }
}
