package com.joansala.tools.trainer;

/*
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
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Hashtable;
import java.util.SortedSet;

import com.sleepycat.je.DatabaseException;

import com.joansala.engine.Negamax;
import com.joansala.oware.OwareBoard;
import com.joansala.oware.OwareGame;


/**
 * Experimental algorithm for the construction of opening books for
 * oware. This class implements a drop-out expansion algorithm.
 */
public class Trainer {

    /** Maximum possible expansion priority */
    public static final float MAX_PRIORITY = 0.0F;

    /** Minimum possible expansion priority */
    public static final float MIN_PRIORITY = Float.MAX_VALUE;

    /** Known node flag bitmask */
    private static final byte KNOWN_NODE = 0x01;

    /** Propagated node flag bitmask */
    private static final byte PROPAGATED_NODE = 0x02;

    /** Null edge identifier */
    private static final int NULL_EDGE = -1;

    /** Maximum number of leaf nodes that can be enqueued */
    private static final int QUEUE_SIZE = 10;

    /** Paths enqueued for expansion */
    private final ArrayList<int[]> queue;

    /** Enqueued paths nodes */
    private final Hashtable<Long, Integer> queueNodes;

    /** Graph object which stores all the generated nodes */
    private final BGraph graph;

    /** Internal engine for leaves computation */
    private final Negamax engine;

    /** Game state object */
    private final OwareGame game;

    /** Start board for the game */
    private final OwareBoard start;

    /** Root node hash */
    private final long rootHash;

    /** Expansion window */
    private float window = OwareGame.MAX_SCORE;

    /** Weight of the priority penalty */
    private float weight = 0.0F;


    /**
     * Instantiates a new openings book trainer.
     *
     * @throws Exception  if the database cannot be opened
     */
    public Trainer(BGraph graph, Negamax engine) throws DatabaseException {
        this.graph = graph;
        this.engine = engine;
        this.game = new OwareGame();
        this.start = new OwareBoard();
        this.rootHash = game.hash();
        this.queue = new ArrayList<int[]>(QUEUE_SIZE);
        this.queueNodes = new Hashtable<Long, Integer>();
        graph.add(rootHash);
    }


    /**
     * Sets the weight of penalty for the priority function. Note that
     * if the weight changes the graph must be refreshed in order to
     * propagate the new priorities.
     *
     * @param weight  new weight value
     */
    public void setWeight(float weight) {
        this.weight = weight;
    }


    /**
     * Sets the window value for the expanded nodes. If a node's value
     * is greater than +window or lower than -window, and the evaluation
     * of the node indicates that this value is not going to change in
     * the short term, then this node will no longer be expanded.
     *
     * @param window  maximum node value
     */
    public void setWindow(int window) {
        this.window = window;
    }


    /**
     * Returns the current root node's score.
     *
     * @return  a score value
     */
    public int rootScore() throws DatabaseException {
        return graph.get(rootHash).getScore();
    }


    /**
     * Expands the given number of paths using the internal engine.
     *
     * @param numPaths  number of paths to expand
     * @param book      {@code true} if the book player moves first
     */
    public void expandPaths(int numPaths, boolean book) throws DatabaseException {
        // Expand the root if it's not expanded

        if (graph.get(rootHash).numEdges() == 0) {
            System.out.print("root: ");

            game.setStart(start.position(), start.turn());

            expandNode(game);

            BNode root = graph.get(rootHash);
            updateValues(root);
        }

        // Expand the most prioritary paths

        int count = 1;

        while (count <= numPaths) {
            // Check if the root is solved

            BNode root = graph.get(rootHash);

            if (root.getBPriority() == MIN_PRIORITY) {
                System.err.println("Warning: Root node is solved");
                break;
            }

            // Enqueue a new path for expansion

            game.setStart(start.position(), start.turn());

            if (enqueuePath(game, book) == false) {
                System.err.println("Warnning: Cannot enqueue a path");
                System.err.println("Please, refresh the graph and try again");
                break;
            }

            // Setup the game postition and expand the leaf

            System.out.print(String.format("%3d : ", count));

            engine.newMatch();
            int[] moves = queue.get(0);

            for (int move : moves)
                performMove(game, move);

            if (expandNode(game) > 0)
                System.out.println("    : transpositions found");

            // Propagate the node values and remove the path
            // from the expansion queue

            queue.remove(0);

            while (game.length() > 0) {
                long hash = game.hash();
                BNode parent = graph.get(hash);
                queueNodes.remove(hash);
                updateValues(parent);
                game.unmakeMove();
            }

            root = graph.get(rootHash);
            queueNodes.remove(rootHash);
            updateValues(root);

            // Synchronize physical storage

            graph.sync();
            count++;
        }
    }


    /**
     * Refreshes the graph scores and priorities starting from the
     * leaves and propagating the values up to the root.
     *
     * <p>It ensures that all the values are propagated to all the parents
     * of the nodes, thus this method is intended to be called after a
     * large number of transpositions were found.</p>
     *
     * <p>Note that all nodes of the graph are updated during this method
     * call, therefore this operation is expensive to perform.</p>
     */
    public void refreshGraph() throws DatabaseException {
        SortedSet<Long> hashes = graph.keys();
        long knownCount = 0L;

        // Mark only the leaf nodes as known. A node is marked as known
        // after its values are propagated to all its parents

        for (long hash : hashes) {
            BNode node = graph.get(hash);
            byte flag = node.getFlag();

            if (node.numEdges() == 0) {
                knownCount++;
                flag |= PROPAGATED_NODE;
                node.setFlag(flag);
                graph.update(node);
            } else {
                flag &= (PROPAGATED_NODE ^ 0xFF);
                node.setFlag(flag);
                graph.update(node);
            }
        }

        System.out.println("Leaves: " + knownCount);

        // Update interior nodes until no change can be done. That is,
        // till all values are propagated (unless the graph contains
        // cycles, in that case some values are left unpropagated).

        boolean hasChanged = true;

        while (hasChanged == true) {
            hasChanged = false;

            for (long hash : hashes) {
                BNode node = graph.get(hash);
                byte flag = node.getFlag();
                boolean hasUnknown = false;

                if ((flag & PROPAGATED_NODE) != 0)
                    continue;

                if ((flag & KNOWN_NODE) == 0)
                    continue;

                for (BNode child : node.childs()) {
                    byte f = child.getFlag();

                    if ((f & PROPAGATED_NODE) == 0) {
                        hasUnknown = true;
                        break;
                    }
                }

                if (hasUnknown == false) {
                    hasChanged = true;
                    knownCount++;
                    flag |= PROPAGATED_NODE;
                    node.setFlag(flag);
                    updateValues(node);
                }
            }
        }

        // If there are unknown nodes left that means the book contains
        // cycles that must be evaluated manually

        if (knownCount != graph.size())
            System.err.println(
                "Warning: Graph contains cycles");

        // Syncronize to the phystical storage

        graph.sync();
    }


    /**
     * Exports all the positions found in the graph to a book file with a
     * format suitable for a game engine.
     *
     * The exported book contains for each node its hash and a score for
     * each of the six houses of the player. If a house is not a legal
     * move for the player its score is set to {@code Shor.MIN_VALUE}.
     * The nodes are sorted according to their hash number, from the
     * lowest hash to the highest.
     *
     * @param path  file path to which to write the book
     */
    public void exportToBook(String path) throws DatabaseException, IOException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");

        // Write the book header information

        raf.writeChars("Oware Opening Book 1.0\n");
        raf.writeChars("Date: " + (new Date().toString()) + "\n");
        raf.writeChars("Positions: " + graph.size() + "\n");
        raf.writeChars("Name: Aalina's Openings\n");
        raf.writeChars("Author: Joan Sala Soler\n");
        raf.writeChars("License: GNU General Public License version 3\n");
        raf.writeChar('\n');

        // Write all the expanded nodes in ascending order

        for (long hash : graph.keys()) {
            BNode node = graph.get(hash);
            short[] scores = new short[6];

            if ((node.getFlag() & KNOWN_NODE) == 0)
                continue;

            if (node.numEdges() < 1)
                continue;

            for (int i = 0; i < 6; i++)
                scores[i] = Short.MIN_VALUE;

            for (int i = 0; i < node.numEdges(); i++) {
                BNode child = node.getChild(i);
                byte move = node.getMove(i);
                int index = (move > 5) ? move - 6 : move;
                scores[index] = (short) -child.getScore();
            }

            raf.writeLong(hash);

            for (short score : scores)
                raf.writeShort(score);
        }

        raf.close();
    }


    /**
     * Outputs all the values of the leaves that can be played by the book
     * player in a comma-separated values format.
     *
     * @param book  {@code true} if the book player moves first
     */
    public void outputBestLeaves(boolean book) throws DatabaseException {
        System.out.println("hash,flag,edges,epb,epo,score,depth");
        outputLeaves(game, book);
    }

    /**
     * Outputs the values of the leaves that can be played by the book
     * player given an root game position.
     *
     * @param game  initial game position
     * @param book  {@code true} if the book player is to move
     */
    private void outputLeaves(OwareGame game, boolean book) throws DatabaseException {
        BNode node = graph.get(game.hash());

        for (int edge = 0; edge < node.numEdges(); edge++) {
            BNode child = node.getChild(edge);
            byte move = node.getMove(edge);
            int score = -child.getScore();

            if (book && score != node.getScore())
                continue;

            performMove(game, move);

            if (game.isRepetition()) {
                System.err.println("Warning: Graph contains cycles");
                continue;
            }

            if (child.numEdges() == 0) {
                if (book == true) {
                    System.out.format(
                        Locale.ENGLISH,
                        "%d,%d,%d,%.2f,%.2f,%d,%d",
                        child.getHash(),
                        child.getFlag(),
                        child.numEdges(),
                        child.getBPriority(),
                        child.getOPriority(),
                        score,
                        game.length() + 1
                    );
                }
            } else {
                outputLeaves(game, !book);
            }

            game.unmakeMove();
        }
    }


    /**
     * Returns the priority penalty for a child. This penalty value is
     * proportional to the difference between the parent node score
     * and the score of the child. Note that the parent score must be
     * negamax propagated before calling this method.
     *
     * @return  penalty value
     */
    private float scorePenalty(BNode node, BNode child) {
        return weight * (child.getScore() + node.getScore());
    }


    /**
     * Returns a priority value for leaf nodes. If the node's static
     * evaluation fails outside the expansion window and the node's
     * score improves the static evaluation then {@code MIN_PRIORITY}
     * is returned; otherwise, {@code MAX_PRIORITY} is returned.
     *
     * @param node  a node object
     * @param game  game position of the node object
     */
    private float leafPenalty(BNode node, OwareGame game) {
        int score = Math.abs(node.getScore());
        int value = Math.abs(game.score());

        float penalty = (value > window && score > value) ?
            MIN_PRIORITY : MAX_PRIORITY;

        return penalty;
    }


    /**
     * Returns the depth penalty for a node. The value is computed as
     * the difference between the best score and the second best score.
     * Note that the parent score must be negamax propagated before
     * calling this method.
     *
     * @return  a penalty value between zero and one
     */
    private float depthPenalty(BNode node) throws DatabaseException {
        // If the node has only one child it is not penalized

        if (node.numEdges() <= 1)
            return 0.0F;

        // Find the difference between the two best scores

        int bestScore = -node.getScore();
        int secondScore = OwareGame.MAX_SCORE;
        boolean hasSecond = false;

        for (BNode child : node.childs()) {
            int score = child.getScore();

            if (score != bestScore) {
                if (score < secondScore)
                    secondScore = score;
                hasSecond = true;
            }
        }

        // Penalty is max when childs have the same score

        if (hasSecond == true)
            return 1.0F;

        // If the difference is big the penalty is low

        float penalty = Math.abs(bestScore - secondScore);
        float range = 2.0F * OwareGame.MAX_SCORE;

        return 1.0F - (penalty / range);
    }


    /**
     * Computes a node score and priority and sets them.
     *
     * When a node is terminal the final score is immediately set and its
     * priority is established to {@code MIN_PRIORITY}; otherwise, the
     * score is computed and the priority set to {@code MAX_PRIORITY}.
     * The score is always set from the player to move perspective.
     *
     * @param node  node for which to compute its values
     * @param game  game state for the node
     */
    private void computeValues(OwareGame game) throws DatabaseException {
        BNode node = graph.get(game.hash());

        if (game.hasEnded()) {
            int score = game.outcome() * game.turn();
            node.setScore(score);
            node.setBPriority(MIN_PRIORITY);
            node.setOPriority(MIN_PRIORITY);
        } else {
            int score = engine.computeBestScore(game);
            float priority = leafPenalty(node, game);

            node.setScore(score);
            node.setBPriority(priority);
            node.setOPriority(priority);
        }

        node.setFlag(KNOWN_NODE);
        graph.update(node);
    }


    /**
     * Updates the score and priorities of a node from the values of its
     * child nodes. Priorities are set to {@code MIN_PRIORITY} if a child
     * reachead the maximum possible value or all the childs have a
     * priority equal to {@code MIN_PRIORITY}.
     *
     * @param node  a node object
     */
    private void updateValues(BNode node) throws DatabaseException {
        // Negamax propagation of the score

        int bestScore = OwareGame.MAX_SCORE;

        for (BNode child : node.childs()) {
            int score = child.getScore();

            if (bestScore > score)
                bestScore = score;

            if (score == -OwareGame.MAX_SCORE)
                break;
        }

        node.setScore(-bestScore);
        graph.update(node);

        // Update this node's priorities

        float bookPriority = MIN_PRIORITY;
        float oppoPriority = MIN_PRIORITY;

        if (bestScore != -OwareGame.MAX_SCORE) {
            // Propagate book and opponent priorities

            for (BNode child : node.childs()) {
                float epb = child.getBPriority();
                float epo = child.getOPriority();

                if (child.getScore() == bestScore) {
                    if (bookPriority > epo)
                        bookPriority = epo;
                }

                if (epb < MIN_PRIORITY)
                    epb += scorePenalty(node, child);

                if (oppoPriority > epb)
                    oppoPriority = epb;
            }

            // Add a depth penalty to priorities

            float penalty = depthPenalty(node);

            if (bookPriority < MIN_PRIORITY)
                bookPriority += penalty;

            if (oppoPriority < MIN_PRIORITY)
                oppoPriority += penalty;
        }

        node.setBPriority(bookPriority);
        node.setOPriority(oppoPriority);

        graph.update(node);
    }


    /**
     * Returns the most prioritary edge of a node.
     *
     * If all childs of the node are solved or the node does not have
     * any childs, {@code NULL_EDGE} is returned. If all the childs are
     * on the expansion queue the chosen edge is that with the highest
     * priority. Otherwise, the returned edge is choosen from the nodes
     * not found in the queue.
     *
     * @param node  a graph node
     * @param book  {@code true} if an edge must be chosen only from
     *              the set of best moves; {@code false} otherwise
     * @return      an edge or {@code NULL_EDGE}
     */
    private int pickBestEdge(BNode node, boolean book) throws DatabaseException {
        int bestEdge = NULL_EDGE;
        int bestScore = OwareGame.MAX_SCORE;
        float bestPriority = MIN_PRIORITY;
        boolean hasUnqueuedChilds = false;

        for (int i = 0; i < node.numEdges(); i++) {
            BNode child = node.getChild(i);
            float priority = (book == true) ?
                child.getBPriority() : child.getOPriority();

            // When priority is min, ignore the child

            if (priority == MIN_PRIORITY)
                continue;

            // When the node has at least one child not in the queue,
            // ignore all childs already in the enqueued

            long hash = child.getHash();
            boolean childInQueue = queueNodes.containsKey(hash);

            if (childInQueue && hasUnqueuedChilds)
                continue;

            // Replace any chosen enqueued childs for a non-enqueued
            // child if we found that one exists

            int score = child.getScore();

            if (!hasUnqueuedChilds && !childInQueue) {
                hasUnqueuedChilds = true;
                bestPriority = priority;
                bestScore = score;
                bestEdge = i;
                continue;
            }

            // The book player picks only best moves and the opponent
            // chooses any of the most prioritary childs

            if (book && bestScore > score) {
                bestPriority = priority;
                bestScore = score;
                bestEdge = i;
                continue;
            }

            if (bestPriority > priority) {
                if (!book || bestScore == score) {
                    bestPriority = priority;
                    bestScore = score;
                    bestEdge = i;
                }
            } else if (bestScore > score) {
                if (bestPriority == priority) {
                    bestPriority = priority;
                    bestScore = score;
                    bestEdge = i;
                }
            }
        }

        return bestEdge;
    }


    /**
     * From all the paths that are not already enqueued for expansion,
     * this method finds the most prioritary one and enqueues it. Note
     * that this recursive method must be called from the root node.
     *
     * @param game  game state of the node
     * @param book  {@code true} if the first player to move is the
     *              book player, which plays only best moves
     * @return      {@code true} if a path was enqueued
     */
    private boolean enqueuePath(OwareGame game, boolean book) throws DatabaseException {
        BNode node = graph.get(game.hash());

        // Enqueue the path if the node is a leaf and the
        // leaf is not already in the queue

        if (node.numEdges() == 0) {
            long hash = node.getHash();

            if (queueNodes.containsKey(hash))
                return false;

            queue.add(game.moves());
            queueNodes.put(hash, 1);

            return true;
        }

        // Recurse though the most prioritary edge

        boolean enqueued = false;
        int edge = pickBestEdge(node, book);

        if (edge != NULL_EDGE) {
            byte move = node.getMove(edge);
            performMove(game, move);

            if (game.isRepetition()) {
                System.err.println("Warning: Cycle detected.");
                return false;
            }

            enqueued = enqueuePath(game, !book);
            game.unmakeMove();
        }

        // If a leaf was enqueued for the followed path, add this
        // node to the table of enqueued nodes

        if (enqueued) {
            long hash = node.getHash();
            Integer c = queueNodes.get(hash);
            int count = (c == null) ? 1 : c + 1;
            queueNodes.put(hash, count);
        }

        return enqueued;
    }


    /**
     * Expands a node by adding an edge to the node for each one of its
     * possible childs and computing the values of newfound childs.
     *
     * @param node  node to expand
     * @param game  game state for the node
     * @return      number of child nodes already present in the
     *              graph before the expansion began
     */
    private int expandNode(OwareGame game) throws DatabaseException {
        BNode node = graph.get(game.hash());

        // Show feedback

        System.out.println(String.format(
            "depth = %d, score = %d",
            game.length() + 1, node.getScore()));

        // Add childs to the node and compute their values

        int[] moves = game.legalMoves();
        int knownNodes = 0;

        for (int move : moves) {
            if (move == OwareGame.NULL_MOVE)
                break;

            performMove(game, move);

            long hash = game.hash();
            BNode child = node.addEdge(hash, (byte) move);
            byte flag = child.getFlag();

            if ((flag & KNOWN_NODE) == 0) {
                computeValues(game);
            } else {
                knownNodes++;
            }

            game.unmakeMove();
        }

        graph.update(node);

        return knownNodes;
    }


    /**
     * Performs a move on the internal board. This method asserts that
     * the move is legal and ensures the game has enough capacity to
     * store it.
     *
     * @param game  Game object where a move must be performed
     * @param move  Move to perform on the game
     * @throws IllegalArgumentException  if the move cannot be
     *      performed on the provided game object
     */
    private void performMove(OwareGame game, int move) {
        if (game.isLegal(move)) {
            game.ensureCapacity(1 + game.length());
            game.makeMove(move);
        } else {
            throw new IllegalArgumentException(
                "The provided move is not legal");
        }
    }


    /**
     * Main method for the class. Builds an openings book using the
     * drop-out expansion method.
     *
     * @param argv  Command line arguments.
     */
    public static void main(String[] argv) throws Exception {
        Negamax engine = null;
        BGraph graph = null;
        Trainer trainer = null;

        // Configuration

        String graphPath = "./res/book/";
        String bookPath = "./res/oware-book.bin";

        int numPaths = 70;
        int window = 68;
        float weight = 1.7F;
        boolean book = true;

        engine = new Negamax();

        engine.setContempt(0);
        engine.setInfinity(OwareGame.MAX_SCORE);
        engine.setDepth(Negamax.MAX_DEPTH);
        engine.setMoveTime(10000);

        // Open the graph and begin training it

        try {
            graph = new BGraph(graphPath);
            trainer = new Trainer(graph, engine);

            trainer.setWindow(window);
            trainer.setWeight(weight);

            /*
            trainer.refreshGraph();
            trainer.outputBestLeaves(true);
            trainer.outputBestLeaves(false);
            System.exit(0);
            */

            System.out.println("Graph size: " + graph.size());
            System.out.println("Root score: " + trainer.rootScore());
            System.out.println();

            System.out.println("> Expanding " + numPaths + " paths");
            trainer.expandPaths(numPaths / 2, book);

            System.out.println("> Expanding " + numPaths + " paths");
            trainer.expandPaths(numPaths / 2, !book);

            System.out.println("> Refreshing the book");
            trainer.refreshGraph();

            System.out.println("> Exporting the book");
            trainer.exportToBook(bookPath);

            System.out.println("New graph size: " + graph.size());
            System.out.println("New root score: " + trainer.rootScore());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            graph.close();
        }
    }

}
