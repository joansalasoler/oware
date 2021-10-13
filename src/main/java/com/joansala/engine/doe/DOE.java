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

import com.joansala.engine.*;
import com.joansala.engine.base.*;


/**
 * A multithreaded UCT engine wich reads from and stores nodes on a
 * key/value database. It can be used to build opening books for a game.
 */
public class DOE extends BaseEngine {

    /** Factors the amount of exploration of the tree */
    public static final double DEFAULT_BIAS = 0.707;

    /** Executes evaluations on a thread pool */
    private final DOEExecutor executor;

    /** Stores expanded nodes */
    private final DOEStore store;

    /** Current computation root node */
    private DOENode root;

    /** References the {@code Game} to search */
    private Game game;

    /** Exploration bias parameter */
    public double biasFactor = DEFAULT_BIAS;

    /** Exploration priority multiplier */
    private double bias = DEFAULT_BIAS * maxScore;

    /** Task synchronization lock */
    private final Object lock = new Object();


    /**
     * Create a new search engine.
     */
    public DOE(DOEStore store) {
        super();
        this.store = store;
        this.executor = new DOEExecutor();
        setExplorationBias(DEFAULT_BIAS);
    }


    /**
     * Create a new search engine.
     */
    public DOE(DOEStore store, int poolSize) {
        super();
        this.store = store;
        this.executor = new DOEExecutor(poolSize);
        setExplorationBias(DEFAULT_BIAS);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setInfinity(int score) {
        super.setInfinity(score);
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
    @Override
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
     * @param size      Nodes to expand
     * @param game      Root state
     * @param scorer    Evaluation function
     */
    public synchronized void trainEngine(int size, Game game, DOEScorer scorer) {
        this.game = game;

        int counter = 0;
        game.ensureCapacity(MAX_DEPTH + game.length());
        root = rootNode(game);

        // There may be unevaluated nodes if the executor was shutdown
        // before all tasks were completed. Enqueue them now.

        for (DOENode node : store.values()) {
            if (node.evaluated == false) {
                executor.submit(() -> evaluate(node, scorer));
                counter++;
            }
        }

        // Expand the UCT tree and enqueue the expanded nodes for
        // its asynchronous evaluation.

        while (size > 0 && !aborted()) {
            final DOENode[] nodes;

            synchronized (lock) {
                nodes = expand(root, maxDepth);

                for (DOENode node : nodes) {
                    backpropagate(node, node.score);
                }

                store.commit();
                size--;
            }

            for (DOENode node : nodes) {
                if (!aborted() && !node.evaluated) {
                    executor.submit(() -> evaluate(node, scorer));
                    counter++;
                }
            }

            if (root.expanded && counter >= 10) {
                invokeConsumers(game);
                counter = 0;
            }
        }

        executor.shutdown();
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
            root.evaluated = true;
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
     * Evaluates a node with an evaluation function.
     *
     * @param node      Node to evaluate
     * @param scorer    Evaluation function
     */
    private void evaluate(DOENode node, DOEScorer scorer) {
        int score = scorer.apply(node.moves);

        synchronized (lock) {
            node.evaluated = true;
            backpropagate(node, score);
            store.commit();
        }
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

        node.evaluated = node.terminal;
        node.updateScore(score);
        store.write(node);
        parent.pushChild(node);
        store.write(parent);

        return node;
    }


    /**
     * Expands all the children of a node.
     *
     * @param node      Parent node
     * @param game      Game state
     *
     * @return          Expanded nodes
     */
    private DOENode[] appendChildren(DOENode node, Game game) {
        int[] moves = game.legalMoves();
        DOENode[] childs = new DOENode[moves.length];

        for (int i = 0; i < moves.length; i++) {
            game.makeMove(moves[i]);
            childs[i] = appendChild(node, moves[i]);
            game.unmakeMove();
        }

        node.expanded = true;

        return childs;
    }


    /**
     * Expands the most prioritary tree node.
     *
     * @param game      Game
     * @param node      Root node
     */
    private DOENode[] expand(DOENode node, int depth) {
        DOENode[] selected = { node };

        if (node.terminal || depth == 0) {
            return selected;
        }

        if (node.expanded) {
            DOENode child = pickLeadChild(node);
            game.makeMove(child.move);
            selected = expand(child, depth - 1);
            game.unmakeMove();
        } else {
            selected = appendChildren(node, game);
            store.write(node);
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
