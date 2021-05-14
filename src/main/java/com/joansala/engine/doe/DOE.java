package com.joansala.engine.doe;

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

import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Set;

import com.joansala.engine.*;


/**
 * A multithreaded UCT engine wich reads from and stores nodes on a
 * key/value database. It can be used to build opening books for a game.
 */
public class DOE implements Engine {

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.353;

    /** Maximum depth allowed for a search */
    public static final int MAX_DEPTH = 254;

    /** Executes evaluations on a thread pool */
    private final DOEExecutor executor;

    /** Stores expanded nodes */
    private final DOEStore store;

    /** Current computation root node */
    private DOENode root;

    /** References the {@code Game} to search */
    private Game game;

    /** Consumer of best moves */
    private Set<Consumer<Report>> consumers = new HashSet<>();

    /** Maximum expansion depth */
    private int maxDepth = MAX_DEPTH;

    /** Maximum time allowed for the current search */
    private long moveTime = DEFAULT_MOVETIME;

    /** The maximum possible score value */
    private int maxScore = Integer.MAX_VALUE;

    /** Contempt factor used to evaluaty draws */
    private int contempt = Game.DRAW_SCORE;

    /** Exploration bias parameter */
    public double biasFactor = DEFAULT_BIAS;

    /** Exploration priority multiplier */
    private double bias = DEFAULT_BIAS * maxScore;

    /** This flag is set to true to abort a computation */
    private volatile boolean aborted = false;

    /** Task synchronization lock */
    private final Object lock = new Object();


    /**
     * Create a new search engine.
     */
    public DOE(DOEStore store) {
        this.store = store;
        this.executor = new DOEExecutor();
        setExplorationBias(DEFAULT_BIAS);
    }


    /**
     * Returns the maximum depth allowed for the search
     *
     * @return   The depth value
     */
    public int getDepth() {
        return maxDepth;
    }


    /**
     * Returns the maximum time allowed for a move computation
     * in milliseconds
     *
     * @return   The new search time in milliseconds
     */
    public long getMoveTime() {
        return moveTime;
    }


    /**
     * Returns current the comptempt factor of the engine.
     */
    public int getContempt() {
        return contempt;
    }


    /**
     * Returns the current infinity score of the engine.
     */
    public int getInfinity() {
        return maxScore;
    }


    /**
     * {@inheritDoc}
     */
    public int getPonderMove(Game game) {
        return Game.NULL_MOVE;
    }


    /**
     * Sets the maximum depth for subsequent computations.
     *
     * @param depth  Requested maximum depth
     */
    public synchronized void setDepth(int depth) {
        maxDepth = Math.min(depth, MAX_DEPTH);
    }


    /**
     * Sets the maximum time allowed for subsequent computations
     *
     * @param delay    The new time value in milliseconds as a
     *                 positive number greater than zero
     */
    public synchronized void setMoveTime(long delay) {
        moveTime = Math.max(delay, 1);
    }


    /**
     * Sets the contempt factor. That is the score to which end game
     * positions that are draw will be evaluated.
     *
     * @param score     Score for draw positions
     */
    public synchronized void setContempt(int score) {
        contempt = score;
    }


    /**
     * Sets the maximum score a position can obtain.
     *
     * @param score     Infinite value as a positive integer
     */
    public synchronized void setInfinity(int score) {
        maxScore = Math.max(score, 1);
        bias = biasFactor * maxScore;
    }


    /**
     * Preference for exploring suboptimal moves.
     *
     * @param factor    Exploration parameter
     */
    public synchronized void setExplorationBias(double factor) {
        biasFactor = factor;
        bias = biasFactor * maxScore;
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void attachConsumer(Consumer<Report> consumer) {
        consumers.add(consumer);
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void detachConsumer(Consumer<Report> consumer) {
        consumers.remove(consumer);
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void newMatch() {}


    /**
     * {@inheritDoc}
     */
    public void abortComputation() {
        aborted = true;

        synchronized (this) {
            aborted = false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized int computeBestMove(Game game) {
        this.game = game;

        if (game.hasEnded()) {
            return Game.NULL_MOVE;
        }

        DOENode root = rootNode(game);

        if (root.expanded) {
            DOENode child = pickBestChild(root);
            return child.move;
        }

        return Game.NULL_MOVE;
    }


    /**
     * Trains the engine using an evaluation function. This method expands
     * the engine book by using an UCT search algorithm.
     *
     * @param game      Root state
     * @param scorer    Evaluation function
     */
    public synchronized void trainEngine(Game game, DOEScorer scorer) {
        this.game = game;

        int counter = 0;
        game.ensureCapacity(MAX_DEPTH + game.length());
        root = rootNode(game);

        while (aborted == false) {
            final DOENode leaf;

            synchronized (lock) {
                leaf = expand(root, maxDepth);
                backpropagate(leaf, leaf.score);
                store.commit();
            }

            if (leaf.terminal == false) {
                executor.submit(() -> {
                    int score = scorer.apply(leaf.moves);

                    synchronized (lock) {
                        backpropagate(leaf, score);
                        store.commit();
                    }
                });
            }

            if (root.expanded && ++counter >= 10) {
                invokeConsumers(game);
                counter = 0;
            }
        }
    }


    /**
     * Computes the expansion priority of an edge. Guides the selection
     * using Upper Confidence Bounds (UCB1).
     *
     * @param child      Child node
     * @param factor     Parent factor
     *
     * @return           Expansion priority
     */
    private double computePriority(DOENode child, double factor) {
        final double E = Math.sqrt(factor / child.count);
        final double priority = E * bias + child.score;

        return priority;
    }


    /**
     * Compute the selection score of a node.
     *
     * @param node      A node
     * @return          Score of the node
     */
    private double computeScore(DOENode node) {
        final double reward = maxScore / Math.sqrt(node.count);
        final double score = node.score + reward;

        return score;
    }


    /**
     * Best child found so far for the given node.
     *
     * @param node      Parent node
     * @return          Child node
     */
    protected DOENode pickBestChild(DOENode node) {
        DOENode child = store.read(node.child);
        DOENode bestChild = store.read(node.child);
        double bestScore = computeScore(bestChild);

        while ((child = store.read(child.sibling)) != null) {
            double score = computeScore(child);

            if (score >= bestScore) {
                bestScore = score;
                bestChild = child;
            }
        }

        return bestChild;
    }


    /**
     * Pick the child node with the highest expansion priority.
     *
     * @param node      Parent node
     * @return          A child node
     */
    private DOENode pickLeadChild(DOENode parent) {
        DOENode child = store.read(parent.child);
        DOENode bestNode = store.read(parent.child);
        double factor = Math.log(parent.count);
        double bestScore = computePriority(child, factor);

        while ((child = store.read(child.sibling)) != null) {
            double score = computePriority(child, factor);

            if (score >= bestScore) {
                bestScore = score;
                bestNode = child;
            }
        }

        return bestNode;
    }


    /**
     * Obtains a tree node for the given game position.
     *
     * @param game      Game state
     * @return          A root node
     */
    private DOENode rootNode(Game game) {
        DOENode root = store.read(1L);

        // Create a new node if root doesn't exist

        if (root == null) {
            root = new DOENode(game, Game.NULL_MOVE);
            root.updateScore(0.0);
            store.write(root);
            store.commit();
        }

        // Check that the stored root is valid for the training
        // state. Each database must contain exactly one root.

        if (root.hash != game.hash()) {
            throw new RuntimeException(
                "Root state is not valid");
        }

        return root;
    }


    /**
     * Scores the current game position for the given node.
     *
     * @param node      Tree node to evaluate
     * @param depth     Maximum search depth
     *
     * @return          Score of the game
     */
    private int score(DOENode node) {
        int score = (node.terminal) ?
            game.outcome() : game.score();

        if (score == Game.DRAW_SCORE) {
            score = contempt;
        }

        return score * game.turn();
    }


    /**
     * Expands a node with a new child.
     *
     * @param node      Node to expand
     * @param move      Move to perform
     *
     * @return          New child node
     */
    private DOENode appendChild(DOENode parent, int move) {
        final DOENode node = new DOENode(game, move);
        final double score = -score(node);

        node.updateScore(score);
        store.write(node);
        parent.pushChild(node);
        store.write(parent);

        return node;
    }


    /**
     * Expands the most prioritary tree node.
     *
     * @param game      Game
     * @param node      Root node
     */
    private DOENode expand(DOENode node, int depth) {
        DOENode selected = node;

        if (!node.terminal && depth > 0) {
            int move = node.nextMove(game);

            if (move != Game.NULL_MOVE) {
                game.makeMove(move);
                selected = appendChild(node, move);
                game.unmakeMove();
            } else {
                DOENode child = pickLeadChild(node);
                game.makeMove(child.move);
                selected = expand(child, depth - 1);
                game.unmakeMove();
            }
        }

        return selected;
    }


    /**
     * Backpropagates the score of a node.
     *
     * @param node      A node
     */
    private void backpropagate(DOENode node, double score) {
        DOENode parent;

        node.updateScore(score);
        store.write(node);

        while((parent = store.read(node.parent)) != null) {
            parent.updateScore(-node.score);
            store.write(parent);
            node = parent;
        }
    }


    /**
     * Notifies registered consumers of a state change.
     *
     * @param game          Game state before a search
     * @param bestMove      Best move found so far
     */
    protected void invokeConsumers(Game game) {
        Report report = new DOEReport(this, game, root);

        for (Consumer<Report> consumer : consumers) {
            consumer.accept(report);
        }
    }
}
