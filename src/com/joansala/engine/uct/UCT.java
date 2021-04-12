package com.joansala.engine.uct;

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
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.joansala.engine.*;


/**
 * Best-first search using Upper Confidence Bounds (UCT).
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class UCT implements Engine {

    /** The default time per move for a search */
    public static final long DEFAULT_MOVETIME = 3600;

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.176;

    /** Maximum depth allowed for a search */
    public static final int MAX_DEPTH = 254;

    /** Minimum number of node expansions */
    private static final int MIN_PROBES = 1000;

    /** Minimum number of expansions between reports */
    private static final int REPORT_PROBES = 250000;

    /** Search timer */
    private final Timer timer;

    /** Current computation root node */
    private UCTNode root = new UCTNode();

    /** References the {@code Game} to search */
    private Game game = null;

    /** Endgame database */
    private Leaves leaves = null;

    /** Consumer of best moves */
    private Set<Consumer<Report>> consumers = new HashSet<>();

    /** The maximum depth allowed for the current search */
    private int maxDepth = MAX_DEPTH;

    /** The maximum time allowed for the current search */
    private long moveTime = DEFAULT_MOVETIME;

    /** The maximum possible score value */
    private int maxScore = Integer.MAX_VALUE;

    /** Contempt factor used to evaluaty draws */
    private int contempt = Game.DRAW_SCORE;

    /** Exploration bias parameter */
    private double biasFactor = DEFAULT_BIAS;

    /** Exploration priority multiplier */
    private double bias = DEFAULT_BIAS * maxScore;

    /** This flag is set to true to abort a computation */
    private volatile boolean aborted = false;

    /** Maximum score found so far */
    private double beta = -maxScore;

    /** Minimum score found so far */
    private double alpha = maxScore;


    /**
     * Create a new search engine.
     */
    public UCT() {
        timer = new Timer(true);
        this.leaves = dummyLeaves;
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
    }


    /**
     * Preference for exploring suboptimal moves.
     *
     * @param factor    Exploration parameter
     */
    public synchronized void setExplorationBias(double factor) {
        this.biasFactor = factor;
    }


    /**
     * Sets an endgames database to use by the engine.
     *
     * @param leaves    Leaves instance or {@code null}
     */
    public synchronized void setLeaves(Leaves leaves) {
        this.leaves = (leaves != null) ? leaves : dummyLeaves;
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
    public synchronized void newMatch() {
        root = new UCTNode();
        timer.purge();
        System.gc();
    }


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
    public synchronized int computeBestMove(Game game) {
        this.game = game;

        if (game.hasEnded()) {
            return Game.NULL_MOVE;
        }

        final TimerTask countDown = new TimerTask() {
            public void run() {
                aborted = true;
            }
        };

        game.ensureCapacity(MAX_DEPTH + game.length());
        timer.schedule(countDown, moveTime);
        root = findRootNode(game);

        beta = -maxScore;
        alpha = maxScore;

        int sampleCount = 0;
        UCTNode bestChild = null;
        double bestScore = Game.DRAW_SCORE;

        while (!aborted || root.count < MIN_PROBES) {
            bias = biasFactor * Math.abs(beta - alpha);
            search(root, maxDepth);

            if (sampleCount++ > REPORT_PROBES) {
                sampleCount = 0;

                UCTNode child = pickBestChild(root);
                double change = Math.abs(child.score - bestScore);

                if (child != bestChild || change > 1.0) {
                    bestChild = child;
                    bestScore = child.score;
                    invokeConsumers(game);
                }
            }
        }

        bestChild = pickBestChild(root);
        invokeConsumers(game);
        countDown.cancel();
        aborted = false;

        return bestChild.move;
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
    private double computePriority(UCTNode child, double factor) {
        final double E = Math.sqrt(factor / child.count);
        final double priority = E * bias + child.score;

        return priority;
    }


    /**
     * Best child found so far for the given node.
     *
     * @param node      Parent node
     * @return          Child node
     */
    protected UCTNode pickBestChild(UCTNode node) {
        UCTNode child = node.child;
        UCTNode bestChild = node.child;
        double bestScore = child.score;

        while ((child = child.sibling) != null) {
            if (child.score >= bestScore) {
                bestScore = child.score;
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
    private UCTNode pickLeadChild(UCTNode parent) {
        UCTNode child = parent.child;
        UCTNode bestNode = parent.child;
        double factor = Math.log(parent.count);
        double bestScore = computePriority(child, factor);

        while ((child = child.sibling) != null) {
            double score = computePriority(child, factor);

            if (score >= bestScore) {
                bestScore = score;
                bestNode = child;
            }
        }

        return bestNode;
    }


    /**
     * Obtains a tree node for the given game position. If the node
     * exists on the tree returns it; otherwise returns a new node.
     *
     * @param game      Game state
     * @return          A root node
     */
    private UCTNode findRootNode(Game game) {
        final long hash = game.hash();
        final UCTNode node;

        if ((node = findNode(root, hash, 2)) != null) {
            node.detachNode();
            return node;
        }

        UCTNode root = new UCTNode();
        root.updateState(game);

        return root;
    }


    /**
     * Recursive lookup for a node given its hash code.
     *
     * @param node      Root node
     * @param hash      Hash of the searched node
     * @param depth     Maximum recursion depth
     *
     * @return          Matching node or null
     */
    private UCTNode findNode(UCTNode node, long hash, int depth) {
        if (node.hash == hash) {
            return node;
        }

        UCTNode match = null;

        if (depth > 0 && (node = node.child) != null) {
            match = findNode(node, hash, depth - 1);

            while (match == null && (node = node.sibling) != null) {
                match = findNode(node, hash, depth - 1);
            }
        }

        return match;
    }


    /**
     * Obtain the next move to expand for a node.
     *
     * @param game      Game state
     * @param node      Node to expand
     *
     * @return          Next move or {@code NULL_MOVE}
     */
    private int getNextMove(Game game, UCTNode node) {
        final int move;

        if (node.expanded) {
            move = Game.NULL_MOVE;
        } else {
            game.setCursor(node.cursor);
            move = game.nextMove();
            node.cursor = game.getCursor();
            node.expanded = (move == Game.NULL_MOVE);
        }

        return move;
    }


    /**
     * Scores the current game position for the given node.
     *
     * @param node      Tree node to evaluate
     * @param depth     Maximum search depth
     *
     * @return          Score of the game
     */
    private int score(UCTNode node, int depth) {
        int score;

        if (node.terminal) {
            score = game.outcome();
        } else if (leaves.find(game)) {
            score = leaves.getScore();
        } else {
            score = game.score();
        }

        if (score == Game.DRAW_SCORE) {
            score = contempt;
        }

        return score * game.turn();
    }


    /**
     * Evaluate a node and return its score.
     *
     * @param node      Node to evaluate
     * @param depth     Maximum search depth
     *
     * @return          Score of the node
     */
    private double evaluate(UCTNode node, int depth) {
        final double score = -score(node, depth);
        node.updateScore(score);

        return score;
    }


    /**
     * Expands a node with a new child and returns its score.
     *
     * @param node      Node to expand
     * @param move      Move to perform
     *
     * @return          New child node
     */
    private UCTNode expandChild(UCTNode parent, int move) {
        final UCTNode node = new UCTNode();

        node.updateState(game);
        node.updateParent(parent);
        node.move = move;

        return node;
    }


    /**
     * Expands the most prioritary tree node.
     *
     * @param game      Game
     * @param node      Root node
     */
    private double search(UCTNode node, int depth) {
        final int move;
        final UCTNode child;
        final double score;

        if (node.terminal || depth == 0) {
            score = node.score;
            node.increaseCount();

            return score;
        }

        move = getNextMove(game, node);

        if (move != Game.NULL_MOVE) {
            game.makeMove(move);
            child = expandChild(node, move);
            score = -evaluate(child, depth - 1);
            game.unmakeMove();
        } else {
            child = pickLeadChild(node);
            game.makeMove(child.move);
            score = -search(child, depth - 1);
            game.unmakeMove();
        }

        if (child.terminal && score == -maxScore) {
            node.setScore(score);
            node.terminal = true;
        } else {
            node.updateScore(score);
        }

        beta = Math.max(beta, score);
        alpha = Math.min(alpha, score);

        return score;
    }


    /**
     * Notifies registered consumers of a state change.
     *
     * @param game          Game state before a search
     * @param bestMove      Best move found so far
     */
    protected void invokeConsumers(Game game) {
        Report report = new UCTReport(this, game, root);

        for (Consumer<Report> consumer : consumers) {
            consumer.accept(report);
        }
    }


    /**
     * Empty endgames database implementation.
     */
    private final Leaves dummyLeaves = new Leaves() {
        public int getScore() { return 0; }
        public int getFlag() { return Flag.EMPTY; }
        public boolean find(Game g) { return false; }
    };
}
