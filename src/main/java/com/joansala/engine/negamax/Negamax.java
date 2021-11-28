package com.joansala.engine.negamax;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import com.google.inject.Inject;
import java.util.function.Consumer;

import com.joansala.engine.*;
import com.joansala.engine.base.*;


/**
 * Implements a game engine using a negamax framework.
 *
 * @author    Joan Sala Soler
 * @version   1.1.0
 */
public class Negamax extends BaseEngine implements HasLeaves, HasCache {

    /** The minimum depth allowed for a search */
    public static final int MIN_DEPTH = 2;

    /** An exact score was returned */
    public static final int EXACT = 0;

    /** An heuristic score may have been returned */
    public static final int FUZZY = 1;

    /** Fallback empty cache instance */
    private final Cache<Game> baseCache = new BaseCache();

    /** Fallback empty endgames instance */
    private final Leaves<Game> baseLeaves = new BaseLeaves();

    /** References the {@code Game} to search */
    protected Game game = null;

    /** The transpositions table */
    protected Cache<Game> cache = null;

    /** Endgame database */
    protected Leaves<Game> leaves = null;

    /** The minimum possible score value */
    private int minScore = -Integer.MAX_VALUE;

    /** Holds the best score found so far */
    private int bestScore = Integer.MAX_VALUE;

    /** Depth of the last completed search */
    private int scoreDepth = 0;


    /**
     * Initializes a new {@code Negamax} object.
     */
    public Negamax() {
        super();
        this.cache = baseCache;
        this.leaves = baseLeaves;
    }


    /**
     * {@inheritDoc}
     */
    public Cache<Game> getCache() {
        return cache;
    }


    /**
     * {@inheritDoc}
     */
    public Leaves<Game> getLeaves() {
        return leaves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getPonderMove(Game game) {
        int move = Game.NULL_MOVE;

        if (cache != null && cache.find(game)) {
            if (cache.getFlag() == Flag.EXACT) {
                move = cache.getMove();
            }
        }

        return move;
    }


    /**
     * Depth of the last completed search iteration.
     */
    public synchronized int getScoreDepth() {
        return scoreDepth;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setDepth(int depth) {
        super.setDepth(depth + depth % 2);
    }


    /**
     * Sets the infinity score. Setting this value to the maximum score
     * a game object can possibly be evaluated will improve the engine
     * performance by producing more cut-offs.
     *
     * @param score     Infinite value as apositive integer
     */
    @Override
    public synchronized void setInfinity(int score) {
        super.setInfinity(score);
        minScore = -maxScore;
    }


    /**
     * Sets the transposition table to use.
     *
     * @param cache     A cache object or {@code null} to disable
     *                  the transposition table
     */
    @Override
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized void setCache(Cache cache) {
        this.cache = (cache != null) ? cache : baseCache;
    }


    /**
     * Sets the endgames database to use.
     *
     * @param leaves    A leaves object or {@code null} to disable
     *                  the use of precomputed endgames
     */
    @Override
    @Inject(optional=true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized void setLeaves(Leaves leaves) {
        this.leaves = (leaves != null) ? leaves : baseLeaves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void newMatch() {
        super.newMatch();
        cache.clear();
    }


    /**
     * Computes a best move for the current position of a game and
     * returns its score. A positive score means an advantage for the
     * player to move.
     *
     * @param game  The game for which a score must be computed
     * @return      The best score found for the current game position
     */
    public synchronized int computeBestScore(Game game) {
        computeBestMove(game);
        return -bestScore;
    }


    /**
     * Computes a best move for the current position of a game.
     *
     * <p>Note that the search is performed on the provided game object,
     * thus, the game object will change during the computation and its
     * capacity may be increased. The provided game object must not be
     * manipulated while a computation is ongoing.</p>
     *
     * @param game  The game for which a best move must be computed
     * @return      The best move found for the current game position
     *              or {@code Game.NULL_MOVE} if the game already ended
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        this.game = game;

        // If the game ended on that position return a null move
        // and set the best score acordingly

        if (game.hasEnded()) {
            bestScore = -(game.outcome() * game.turn());
            return Game.NULL_MOVE;
        }

        // Get ready for the move computation

        scheduleCountDown(moveTime);
        game.ensureCapacity(MAX_DEPTH + game.length());
        cache.discharge();

        // Compute all legal moves for the game

        int[] rootMoves = game.legalMoves();

        // Check for a hash move and reorder moves accordingly

        if (cache.find(game) && cache.getMove() != Game.NULL_MOVE) {
            final int hashMove = cache.getMove();

            for (int index = 0; index < 6; index++) {
                if (rootMoves[index] == hashMove) {
                    System.arraycopy(rootMoves, 0, rootMoves, 1, index);
                    rootMoves[0] = hashMove;
                    break;
                }
            }
        }

        // Iterative deepening search for a best move

        int score;
        int depth = MIN_DEPTH;
        int beta = maxScore;
        int lastScore = maxScore;
        int lastMove = Game.NULL_MOVE;
        int bestMove = rootMoves[0];

        bestScore = Game.DRAW_SCORE;
        scoreDepth = 0;

        while (!aborted() || depth == MIN_DEPTH) {
            for (int move : rootMoves) {
                game.makeMove(move);
                score = search(minScore, beta, depth);
                game.unmakeMove();

                if (aborted() && depth > MIN_DEPTH) {
                    bestMove = lastMove;
                    bestScore = lastScore;
                    break;
                }

                if (score < beta) {
                    bestMove = move;
                    bestScore = score;
                    beta = score;
                } else if (score == beta) {
                    bestScore = score;
                }
            }

            // Stop if an exact score was found

            if (!aborted() || depth == MIN_DEPTH) {
                scoreDepth = depth;
            }

            if (Math.abs(bestScore) == maxScore) {
                break;
            }

            // Stop on timeout elaspe or maximum recursion

            if (aborted() || depth >= maxDepth) {
                break;
            }

            // Create a report of the current search results

            if (depth > MIN_DEPTH) {
                if (bestMove != lastMove || bestScore != lastScore) {
                    invokeConsumers(game, bestMove);
                }
            }

            // She's heading for the disco…

            beta = maxScore;
            lastMove = bestMove;
            lastScore = bestScore;
            depth += 2;
        }

        invokeConsumers(game, bestMove);
        cancelCountDown();

        return bestMove;
    }


    /**
     * Performs a recursive search for a best move
     *
     * @param alpha  The propagated alpha value
     * @param beta   The propagated beta value
     * @param depth  Search depth of the node. Defines the maximum number
     *               of recursive calls that could be made for the node
     */
    private int search(int alpha, int beta, int depth) {
        if (aborted() && depth > MIN_DEPTH) {
            return minScore;
        }

        // Return the utility score of the node

        if (game.hasEnded()) {
            final int score = game.outcome();

            return (score == Game.DRAW_SCORE) ?
                contempt * game.turn() : score * game.turn();
        }

        // Return an endgame score if possible

        if (leaves.find(game)) {
            final int score = leaves.getScore();

            return (score == Game.DRAW_SCORE) ?
                contempt * game.turn() : score * game.turn();
        }

        // Return the heuristic score of the node

        if (depth == 0) {
            return game.score() * game.turn();
        }

        // Hash table lookup

        int hashMove = Game.NULL_MOVE;

        if (depth > 2 && cache.find(game)) {
            // Check for a possible cut-off

            if (cache.getDepth() >= depth) {
                switch (cache.getFlag()) {
                    case Flag.UPPER:
                        if (cache.getScore() >= beta)
                            return beta;
                        break;
                    case Flag.LOWER:
                        if (cache.getScore() <= alpha)
                            return alpha;
                        break;
                    case Flag.EXACT:
                        return cache.getScore();
                }
            }

            // Get the hash move

            hashMove = cache.getMove();
        }

        // Initialize score and flags

        int score = minScore;
        int flag = Flag.LOWER;

        // Try the hash move first

        if (hashMove != Game.NULL_MOVE) {
            game.makeMove(hashMove);
            score = -search(-beta, -alpha, depth - 1);
            game.unmakeMove();

            if (score >= beta && aborted() == false) {
                cache.store(game, score, hashMove, depth, Flag.UPPER);
                return beta;
            }

            if (score > alpha) {
                alpha = score;
                flag = Flag.EXACT;
            }
        }

        // Iterate through generated moves

        int cmove;

        while ((cmove = game.nextMove()) != Game.NULL_MOVE) {
            if (cmove == hashMove)
                continue;

            game.makeMove(cmove);
            score = -search(-beta, -alpha, depth - 1);
            game.unmakeMove();

            if (score >= beta) {
                alpha = beta;
                hashMove = cmove;
                flag = Flag.UPPER;
                break;
            }

            if (score > alpha) {
                alpha = score;
                hashMove = cmove;
                flag = Flag.EXACT;
            }
        }

        // Store the transposition ignoring pre-frontier subtrees

        if (depth > 2 && aborted() == false) {
            cache.store(game, alpha, hashMove, depth, flag);
        }

        // Return the best score found

        return alpha;
    }


    /**
     * Notifies registered consumers of a state change.
     *
     * @param game          Game state before a search
     * @param bestMove      Best move found so far
     */
    protected void invokeConsumers(Game game, int bestMove) {
        Report report = new CacheReport(game, cache, bestMove);

        for (Consumer<Report> consumer : consumers) {
            consumer.accept(report);
        }
    }
}
