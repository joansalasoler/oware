package com.joansala.tools.solver;

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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;


/**
 * Experimental oware engames solver algorithm.
 */
public class Solver implements Serializable {

    /** Serialization version number */
    private static final long serialVersionUID = 1;

    /** Number of possible endgame positions */
    public static final int SIZE = 17383861;

    /** Maximum search depth */
    public static final int MAX_DEPTH = 50;

    /** Maximum number of seeds in a position */
    public static final int MAX_SEEDS = 15;

    /** Node type for an unknown node score */
    private static final byte UNKNOWN_SCORE = 0;

    /** Node type for an exact node score */
    private static final byte EXACT_SCORE = 3;

    /** Exact cycle score flag */
    private static final int CYCLE_SCORE = 1;

    /** Unvisited node label */
    private static final int UNVISITED_NODE = -1;

    /** Types of the node scores */
    private final byte[] types = new byte[SIZE];

    /** Terminal nodes marks */
    private final boolean[] terminal = new boolean[SIZE];

    /** Terminal scores for each position */
    private final byte[] scores = new byte[SIZE];

    /** Number of childs of each node */
    private final byte[] edges = new byte[SIZE];

    /** Connected component identifiers */
    private final byte[] flags = new byte[SIZE];

    /** Childs of each position */
    private final int[] childs = new int[6 * SIZE];

    /** Edge scores for each position */
    private final byte[] costs = new byte[6 * SIZE];

    /** Stores provisional values for cycle nodes */
    private final byte[] values = new byte[SIZE];

    /** Visited nodes during a search */
    private final boolean[] visited = new boolean[SIZE];

    /** Solved on a search cycle nodes */
    private final boolean[] solved = new boolean[SIZE];

    /** Counts exact scores for each number of seeds */
    private final int[] exactCount = new int[16];

    /** Last used flag to identify a cycle */
    private byte flag = 0;

    /* Strongly-connected components algorithm */

    /** Stack used for the cycle detection algorithm */
    private transient DStack stack;

    /** Labels for the cycle detection algorithm */
    private int[] labels = new int[SIZE];


    /**
     * Instantiates a new endgame solver.
     */
    public Solver() {
        stack = new DStack(SIZE);
    }


    /**
     * Expands all the positions with exactly {@code numSeeds} seeds
     * with all their children and marks all the terminal nodes as known.
     *
     * @param numSeeds  number of seeds
     */
    public void expand(byte numSeeds) {
        int start = OFFSETS[numSeeds];
        int end = OFFSETS[numSeeds + 1];

        for (int node = start; node < end; node++) {
            byte[] position = unrankPosition(node);

            // Store the terminal score

            scores[node] = terminalScore(position);

            // Flag terminal nodes as known

            if (!hasLegalMoves(position)) {
                types[node] = EXACT_SCORE;
                terminal[node] = true;
                exactCount[numSeeds]++;
                continue;
            }

            // Expand non-terminal nodes

            expandNode(node, position);

            // Flag forced repetitions as terminal

            if (Arrays.binarySearch(FORCED_REPETITIONS, node) >= 0) {
                types[node] = EXACT_SCORE;
                terminal[node] = true;
                exactCount[numSeeds]++;
            }
        }
    }


    /**
     * Performs an iterative minimax propagation of the nodes until no
     * more nodes can obtain it's value. The remaining nodes are either
     * part of a cycle or it's score depends on a node pertaining to
     * a cycle.
     *
     * @param numSeeds  number of seeds for the nodes
     */
    public void propagate(byte numSeeds) {
        int start = OFFSETS[numSeeds];
        int end = OFFSETS[numSeeds + 1];

        // Propagates the scores untill no change can be done

        boolean hasChanged = true;

        while (hasChanged) {
            hasChanged = false;

            for (int node = start; node < end; node++) {
                if (types[node] == EXACT_SCORE)
                    continue;

                int first = node * 6;
                int last = first + edges[node];
                byte bestScore = (byte) -numSeeds;

                // Obtain an exact score for the node if possible

                boolean isExact = true;

                for (int index = first; index < last; index++) {
                    int child = childs[index];
                    int type = types[child];

                    if (type != EXACT_SCORE) {
                        isExact = false;
                        continue;
                    }

                    byte score = edgeScore(node, child);

                    if (score >= numSeeds) {
                        bestScore = score;
                        isExact = true;
                        break;
                    }

                    if (bestScore < score)
                        bestScore = score;
                }

                // Set the obtained score

                if (isExact) {
                    types[node] = EXACT_SCORE;
                    scores[node] = bestScore;
                    exactCount[numSeeds]++;
                    hasChanged = true;
                }
            }
        }
    }


    /**
     * Finds all the unknown nodes with N seeds that are part of a
     * strongly-connected component and flags them appropiately.
     *
     * @param numSeeds  number of seeds
     * @return          number of components marked
     */
    public int markComponents(int numSeeds) {
        int start = OFFSETS[numSeeds];
        int end = OFFSETS[numSeeds + 1];
        int prevFlag = flag;

        // For each unknown and unvisited node find the components

        Arrays.fill(labels, start, end, UNVISITED_NODE);

        int label = Integer.MAX_VALUE;
        int index = 1;

        for (int root = start; root < end; root++) {
            if (types[root] != UNKNOWN_SCORE)
                continue;

            if (labels[root] != UNVISITED_NODE)
                continue;

            stack.push(root);

            while (!stack.empty()) {
                int node = stack.peek();

                if (labels[node] == UNVISITED_NODE) {
                    labels[node] = index++;
                    pushChilds(node);
                } else {
                    stack.pop();

                    if (updateLink(node) == true) {
                        index -= updateLabels(node, label);
                        label--;
                    } else {
                        stack.pushBack(node);
                    }
                }
            }
        }

        return (flag - prevFlag);
    }


    /**
     * Pushes all the unknown childs of a node to the top of the stack.
     *
     * <p>Only unknown childs with positions containing the same number
     * of seeds as their parent are pushed into the stack.</p>
     *
     * @param node  unique node identifier
     */
    private void pushChilds(int node) {
        int first = node * 6;
        int last = first + edges[node];

        for (int i = first; i < last; i++) {
            int child = childs[i];

            if (types[child] != UNKNOWN_SCORE)
                continue;

            if (nodeSeeds(child) != nodeSeeds(node))
                continue;

            if (labels[child] == UNVISITED_NODE) {
                if (stack.contains(child)) {
                    stack.raise(child);
                } else {
                    stack.push(child);
                }
            }
        }
    }


    /**
     * Updates the link number of a node with the minimum link found on
     * its childs and returns {@code true} if the node is found to be
     * a root of a strongly-connected component.
     *
     * <p>Only unknown childs with positions containing the same number
     * of seeds as their parent are considered.</p>
     *
     * @param node  unique node identifier
     * @return      {@code true} if the node is a root
     */
    private boolean updateLink(int node) {
        final int first = node * 6;
        final int last = first + edges[node];

        boolean isRoot = true;
        int min = labels[node];

        for (int i = first; i < last; i++) {
            int child = childs[i];

            if (types[child] != UNKNOWN_SCORE)
                continue;

            if (nodeSeeds(child) != nodeSeeds(node))
                continue;

            if (labels[child] < min) {
                min = labels[child];
                isRoot = false;
            }
        }

        labels[node] = min;

        return isRoot;
    }


    /**
     * Updates the labels for a single node or a strongly-connected
     * component if found. If a component is found their nodes are
     * also flagged with an unique incremental flag. Note that the flags
     * identify each component in a topological order.
     *
     * @param node  a node identifier
     * @param label label for the node
     */
    private int updateLabels(int node, int label) {
        int labeled = 1;
        int link = labels[node];

        flag++;

        while (!stack.emptyBack()) {
            int n = stack.peekBack();

            if (labels[n] < link)
                break;

            stack.popBack();
            labels[n] = label;
            flags[n] = flag;
            labeled++;
        }

        labels[node] = label;

        if (labeled > 1) {
            flags[node] = flag;
        } else {
            flags[node] = 0;
            flag--;
        }

        return labeled;
    }


    /**
     * Individually solves each node pertaining to the flagged strongly
     * connected component. Only positions with the specified number of
     * seeds will be solved.
     *
     * @param numSeeds  number of seeds
     * @param flag      component flag
     */
    public void solveComponent(int numSeeds, int flag) {
        // Preprocess the child nodes for a faster solving

        int numEdges = computeCosts(numSeeds, flag);
        int numNodes = sortChilds(numSeeds, flag);

        // Show some feedback for the component

        System.out.format(
            "Cycle %-2d        %16d nodes%n",
            flag, numNodes
        );

        // For each unknown node search their approximate score

        int start = OFFSETS[numSeeds];
        int end = OFFSETS[numSeeds + 1];
        int depth = MAX_DEPTH;

        for (int node = start; node < end; node++) {
            if (flags[node] != flag)
                continue;

            if (types[node] != UNKNOWN_SCORE)
                continue;

            values[node] = (byte) search(node, -numSeeds, numSeeds, depth);
        }

        System.out.println();

        // Set the computed node's scores and score types

        for (int node = start; node < end; node++) {
            if (types[node] != EXACT_SCORE && flags[node] == flag) {
                scores[node] = values[node];
                types[node] = EXACT_SCORE;
                exactCount[numSeeds]++;
            }
        }
    }


    /**
     * Implements an alpha-beta search for a best path. If a solution can
     * be found an exact score is returned. This search assumes the worst
     * possible outcome when the solution is too far away.
     *
     * @param node      root node
     * @param alpha     minimus score
     * @param beta      maximum score
     * @param depth     maximum depth
     */
    private int search(int node, int alpha, int beta, int depth) {
        final int first = node * 6;
        final int last = first + edges[node];

        visited[node] = true;

        for (int i = first; i < last; i++) {
            int child = childs[i];
            int type = types[child];
            int score = 0;

            if (visited[child] || type == EXACT_SCORE) {
                score = costs[i];
            } else if (depth == 0) {
                score = nodeSeeds(node) - 2 * nodeSeeds(child);
            } else {
                score = -search(child, -beta, -alpha, depth - 1);
                score += nodeSeeds(node) - nodeSeeds(child);
            }

            if (score >= beta) {
                alpha = beta;
                break;
            }

            if (score > alpha)
                alpha = score;
        }

        visited[node] = false;

        return alpha;
    }


    /**
     * Precomputes edge scores for all the unknown nodes with the given
     * number of seeds and component flag.
     *
     * @param numSeeds  number of seeds
     * @param flag      component flag
     * @return          computed edge scores count
     */
    private int computeCosts(int numSeeds, int flag) {
        int start = OFFSETS[numSeeds];
        int end = OFFSETS[numSeeds + 1];
        int count = 0;

        for (int node = start; node < end; node++) {
            if (flags[node] != flag)
                continue;

            if (types[node] != UNKNOWN_SCORE)
                continue;

            int first = node * 6;
            int last = first + edges[node];

            for (int i = first; i < last; i++) {
                costs[i] = edgeScore(node, childs[i]);
                count++;
            }
        }

        return count;
    }


    /**
     * Sorts the childs of all the unknown nodes with the given number
     * of seeds and component flag.
     *
     * <p>The new child order is partitioned with all the known childs
     * first and then the unknown child. On each partition the nodes are
     * sorted according to their terminal scores (higher first).</p>
     *
     * @param numSeeds  number of seeds
     * @param flag      component flag
     * @return          number of unknown nodes in the component
     */
    private int sortChilds(int numSeeds, int flag) {
        int start = OFFSETS[numSeeds];
        int end = OFFSETS[numSeeds + 1];
        int count = 0;

        for (int node = start; node < end; node++) {
            if (flags[node] != flag)
                continue;

            if (types[node] != UNKNOWN_SCORE)
                continue;

            int first = node * 6;
            int last = first + edges[node];

            for (int i = first + 1; i < last; i++) {
                int child = childs[i];
                byte cost = costs[i];

                int n;

                for (n = i - 1; n >= first; n--) {
                    int prev = childs[n];

                    if (types[prev] != types[child]) {
                        if (types[prev] == EXACT_SCORE)
                            break;
                    } else if (costs[n] >= cost) {
                        break;
                    }

                    childs[n + 1] = childs[n];
                    costs[n + 1] = costs[n];
                }

                childs[n + 1] = child;
                costs[n + 1] = cost;
            }

            count++;
        }

        return count;
    }


    /**
     * Returns the score for an edge of the graph. That is the number of
     * seeds captured to reach the child position minus the score of the
     * child node.
     *
     * @param node      parent node hash
     * @param child     child node hash
     * @return          edge score
     */
    private byte edgeScore(int node, int child) {
        int captures = nodeSeeds(node) - nodeSeeds(child);
        int score = captures - scores[child];

        return (byte) score;
    }


    /**
     * Returns the score for a node assuming it's terminal. That is the
     * difference between the number of remaining seeds on the south
     * player houses and the remaining seeds on north's pits.
     *
     * @param position  position representation
     * @return          final score
     */
    private byte terminalScore(byte[] position) {
        byte score = 0;

        for (int i = 0; i < 6; i++) {
            score += position[i];
            score -= position[i + 6];
        }

        return score;
    }


    /**
     * Returns the number of seeds a node's position contains.
     *
     * @param node  node identifier
     */
    private int nodeSeeds(int node) {
        int index = Arrays.binarySearch(OFFSETS, node);
        return index >= 0 ? index : -index - 2;
    }


    /**
     * Computes the hash codes of all the children of a node and stores
     * them in the global array {@code childs}.
     *
     * @param node      node hash code
     * @param position  position representation for the node
     */
    private void expandNode(int node, byte[] position) {
        int offset = node * 6;
        int index = offset;

        // Captures

        for (byte move : SOUTH_HOUSES) {
            if (position[move] > 0 && position[move] > 5 - move
                && isCapture(position, move)) {
                childs[index] = childHash(position, move);
                index++;
            }
        }

        // Check if the opponent must be sowed

        boolean mustsow = true;

        for (byte move : NORTH_HOUSES) {
            if (position[move] != 0) {
                mustsow = false;
                break;
            }
        }

        // Other moves

        if (mustsow) {
            for (byte move : SOUTH_HOUSES) {
                if (position[move] > 0 && position[move] > 5 - move
                    && !isCapture(position, move)) {
                    childs[index] = childHash(position, move);
                    index++;
                }
            }
        } else {
            for (byte move : SOUTH_HOUSES) {
                if (position[move] > 0 && position[move] < 3
                    && !isCapture(position, move)) {
                    childs[index] = childHash(position, move);
                    index++;
                }
            }

            for (byte move : SOUTH_HOUSES) {
                if (position[move] > 2 && !isCapture(position, move)) {
                    childs[index] = childHash(position, move);
                    index++;
                }
            }
        }

        // Store the number of childs of the node

        edges[node] = (byte) (index - offset);
    }


    /**
     * Performs a move on the given position and return the resulting
     * position hash code. Grand Slam moves are legal moves but the player
     * to move does not capture any seeds.
     *
     * @param pos   position representation
     * @param move  the move to perform on the board
     */
    private int childHash(byte[] pos, byte move) {
        // Copy the position array

        byte[] position = new byte[13];
        System.arraycopy(pos, 0, position, 0, 13);

        // Sow

        int house = (int) move;
        int seeds = (int) position[move];

        while (seeds > 0) {
            if ((house = ++house % 12) != move) {
                position[house]++;
                seeds--;
            }
        }

        position[move] = 0;

        // Skip gathering

        if (house < 6 || position[house] > 3 || position[house] < 2)
            return rankNorthPosition(position);

        // Ignore GrandSlam captures

        for (byte i : NORTH_HOUSES) {
            if (i > house) {
                if (position[i] != 0)
                    break;
            } else if (position[i] > 3 || position[i] < 2) {
                break;
            }

            if (i == 6)
                return rankNorthPosition(position);
        }

        // Capture seeds

        byte captures = 0;

        for (byte i : HARVESTER[house]) {
            if (position[i] > 3 || position[i] < 2)
                break;
            captures += position[i];
            position[i] = 0;
        }

        position[12] += captures;

        return rankNorthPosition(position);
    }


    /**
     * Checks if the move is a capturing move for the given position
     *
     * @param position  position representation
     * @param move      the move to determine if it's a capture
     * @return          {@code true} if the move is a legal capture
     */
    public boolean isCapture(byte[] position, byte move) {
        final int last = (int) REAPER[move][position[move]];

        if (last == -1 || position[last] > 2)
            return false;

        final byte seedsSown = position[move];
        final byte seedsLast = position[last];

        if (seedsSown < 12 && seedsLast > 0) {
            for (int i = 11; i > last; i--) {
                if (position[i] != 0)
                    return true;
            }

            for (int i = last - 1; i > 5; i--) {
                if (position[i] == 0 || position[i] > 2)
                    return true;
            }
        } else if (seedsSown > 11 && seedsSown < 23 && seedsLast < 2) {
            if (last < 11)
                return true;

            for (int i = 10; i > 5; i--) {
                if (position[i] > 1)
                    return true;
            }
        } else if (seedsSown > 22 && seedsLast == 0) {
            if (last < 11)
                return true;

            for (int i = 10; i > 5; i--) {
                if (position[i] != 0)
                    return true;
            }
        }

        return false;
    }


    /**
     * Checks if at least one legal move can be performed for the
     * given position, from south's perspective.
     *
     * @param position  position representation
     * @return          {@code true} if a legal move exists
     */
    private boolean hasLegalMoves(byte[] position) {
        for (int n = 5; n >= 0; n--) {
            if (position[n] == 0)
                continue;

            for (int i = 11; i > 5; i--)
                if (position[i] != 0)
                    return true;

            for (int i = n; i >= 0; i--)
                if (position[i] > 5 - i)
                    return true;

            break;
        }

        return false;
    }


    /**
     * Given a position array where the south player is to move returns
     * an unique 25 bits identifier for the position from the player's
     * perspective. The position must contain exactly 16 seeds.
     *
     * @param position  An array representation of the position
     * @return          Hash of the position
     */
    private int rankSouthPosition(byte[] position) {
        int rank = 0;
        int n = (int) position[12];

        for (int i = 11; n < MAX_SEEDS && i >= 0; i--) {
            rank += COEFFICIENTS[n][i];
            n += (int) position[i];
        }

        return rank;
    }


    /**
     * Given a position array where the north player is to move returns
     * an unique 25 bits identifier for the position from the player's
     * perspective. The position must contain exactly 16 seeds.
     *
     * @param position  position representation
     */
    private int rankNorthPosition(byte[] position) {
        int rank = 0;
        int n = (int) position[12];

        for (int i = 5; n < MAX_SEEDS && i >= 0; i--) {
            rank += COEFFICIENTS[n][i + 6];
            n += (int) position[i];
        }

        for (int i = 11; n < MAX_SEEDS && i >= 6; i--) {
            rank += COEFFICIENTS[n][i - 6];
            n += (int) position[i];
        }

        return rank;
    }


    /**
     * Returns a position array parsed from the unique binomial rank
     * number of the position. The position is unranked always from
     * south's perspective. The rank number can be obtained with the
     * methods {@code rankSouthPosition} or {@code rankNorthPosition}.
     *
     * @param rank  position hash code
     * @return      a valid position representation array
     */
    private byte[] unrankPosition(int rank) {
        final byte[] position = new byte[13];

        int i = 11;
        int n = 0;
        byte seeds = 0;

        while (i >= 0 && n < MAX_SEEDS) {
            int value = COEFFICIENTS[n][i];

            if (rank >= value) {
                rank -= value;
                position[i + 1] = seeds;
                seeds = 0;
                i--;
            } else {
                seeds++;
                n++;
            }
        }

        position[i + 1] = (byte) (MAX_SEEDS - n + seeds);

        return position;
    }


    /**
     * Returns the number of known positions with exactly the number
     * of specified seeds.
     *
     * @param numSeeds  number of seeds
     */
    public int exactCount(int numSeeds) {
        return exactCount[numSeeds];
    }


    /**
     * Returns the total number of known positions.
     */
    public int exactCount() {
        int count = 0;

        for (int seeds = 0; seeds <= MAX_SEEDS; seeds++)
            count += exactCount[seeds];

        return count;
    }


    /**
     * Returns the number of positions that exist with exactly the number
     * of specified seeds.
     *
     * @param numSeeds  number of seeds
     */
    public int numPositions(int numSeeds) {
        if (numSeeds >= 0 && numSeeds < MAX_SEEDS) {
            return OFFSETS[numSeeds + 1] - OFFSETS[numSeeds];
        } else if (numSeeds == MAX_SEEDS) {
            return SIZE - OFFSETS[numSeeds];
        }

        return 0;
    }


    /**
     * Returns the total number of positions that exist.
     */
    public int numPositions() {
        return SIZE;
    }


    /**
     * Returns the latest used cycle flag.
     */
    public int previousFlag() {
        return flag;
    }


    /**
     * Exports the generated endgame database to a suitable book file.
     *
     * @param path  file path
     */
    public void exportToBook(String path, int numSeeds) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");

        int size = 1 + OFFSETS[1 + numSeeds];
        byte[] book = new byte[size];

        // Write the book header information

        raf.writeChars("Oware Endgames 1.0\n");
        raf.writeChars("Date: " + (new Date().toString()) + "\n");
        raf.writeChars("Positions: " + size + "\n");
        raf.writeChars("Name: Aalina's Endgames\n");
        raf.writeChars("Author: Joan Sala Soler\n");
        raf.writeChars("License: GNU General Public License version 3\n");
        raf.writeChar('\n');

        // Write all the expanded nodes

        for (int node = 0; node < size; node++) {
            int seeds = nodeSeeds(node);
            int score = scores[node];
            int captures = score + (seeds - score) / 2;
            int type = types[node];
            int flag = flags[node];

            if (flag != 0 && type == EXACT_SCORE)
                type = CYCLE_SCORE;

            if (type != EXACT_SCORE)
                captures = 0;

            book[node] = (byte) ((captures << 2) | type);
        }

        raf.write(book);
        raf.close();
    }


    /**
     * Serializes this object to a file.
     *
     * @param path  file path
     */
    public void writeGraph(String path) throws IOException {
        FileOutputStream file = new FileOutputStream(path);
        ObjectOutputStream out = new ObjectOutputStream(file);

        out.writeObject(this);

        out.close();
        file.close();
    }


    /**
     * Unserializes this object from a file.
     *
     * @param path  file path
     */
    public static Solver readGraph(String path) throws Exception {
        FileInputStream file = new FileInputStream(path);
        ObjectInputStream in = new ObjectInputStream(file);

        Solver solver = (Solver) in.readObject();
        solver.stack = new DStack(SIZE);

        in.close();
        file.close();

        return solver;
    }


    /**
     * Builds an endgame tablebase in a progressive manner.
     *
     * @param argv  Command line arguments.
     */
    public static void main(String[] argv) throws Exception {
        String bookPath = "./res/oware-leaves.bin";
        String graphPath = "./res/endgames/book.db";
        Solver solver = new Solver();

        byte minSeeds = 0;
        byte maxSeeds = 15;
        long startTime = System.currentTimeMillis();

        // Solve all the non-cycle dependent nodes

        System.out.format(
            "Solving independent nodes%n" +
            "=================================================%n%n");

        for (byte seeds = minSeeds; seeds <= maxSeeds; seeds++) {
            long start = System.currentTimeMillis();

            System.out.format("Solving %d seeds nodes%n%n", seeds);

            // Expand nodes

            solver.expand(seeds);

            // Flag the strongly-connected components

            int flag = solver.previousFlag();
            int cycles = solver.markComponents(seeds);

            // Propagate scores

            solver.propagate(seeds);

            // Solve each component in topological order

            // if (cycles > 0) {
            //     for (int i = 0; i < cycles; i++) {
            //         solver.solveComponent(seeds, 1 + flag++);
            //         solver.propagate(seeds);
            //     }
            // }

            // Show positions statistics

            long end = System.currentTimeMillis();

            int nodes = solver.numPositions(seeds);
            int exact = solver.exactCount(seeds);

            double seconds = (end - start) / 1000.0D;
            double percent = 100.0D * exact / nodes;

            System.out.format(
                "Cycles:         %16d cycles%n" +
                "Nodes:          %16d nodes%n" +
                "Exact:          %16d nodes%n" +
                "Percent:        %16.3f %%%n" +
                "Time:           %16.3f seconds%n%n",
                cycles, nodes, exact, percent, seconds
            );
        }

        // Export to a book file format

        try {
            solver.exportToBook(bookPath, maxSeeds);
            // solver.writeGraph(graphPath);
        } catch (IOException e) {
            System.err.println(e);
        }

        // Show statistics about solved positions

        long endTime = System.currentTimeMillis();

        int total = solver.numPositions(maxSeeds);
        int exact = solver.exactCount();

        double seconds = (endTime - startTime) / 1000.0D;
        double percent = 100.0D * exact / total;

        System.out.format(
            "Statistics%n" +
            "=================================================%n%n" +
            "Time:           %16.3f seconds%n" +
            "Total:          %16d nodes%n" +
            "Exact:          %16d nodes%n" +
            "Percent:        %16.3f %%%n%n",
            seconds, total, exact, percent
        );
    }


    /* The following arrays are used to make computations faster */

    /** Nodes which lead to forced repetitions */
    private static final int[] FORCED_REPETITIONS = {
        19, 20, 31, 32, 42, 43, 52, 53, 61, 62, 69, 75 };

    /** Index identifiers for south player houses */
    private static final byte[] SOUTH_HOUSES = {
        5, 4, 3, 2, 1, 0 };

    /** Index identifiers for north player houses */
    private static final byte[] NORTH_HOUSES = {
        11, 10, 9, 8, 7, 6 };

    /** Used to make capturing seeds computations faster */
    private static final byte[][] HARVESTER = {
        {0}, {1, 0}, {2, 1, 0}, {3, 2, 1, 0},
        {4, 3, 2, 1, 0}, {5, 4, 3, 2, 1, 0},
        {6}, {7, 6}, {8, 7, 6}, {9, 8, 7, 6},
        {10, 9, 8, 7, 6}, {11, 10, 9, 8, 7, 6}
    };

    /** Used to determine the pit where a seeding lands. Helps in
        determining when a move could be a capture */
    private static final byte[][] REAPER = {
        {-1, -1, -1, -1, -1, -1,  6,  7,  8,  9, 10, 11,
         -1, -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1,
         -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1,
         -1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1,
         -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1,
         -1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1,
         -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1,
         -1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1,
         -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1,
         -1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,
          6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1,  6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,
          6,  7,  8,  9, 10, 11, -1, -1, -1, -1, -1,  6,
          7,  8,  9, 10, 11, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5,
         -1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1,
         -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1,
         -1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1,
         -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1,
         -1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1,
         -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1,
         -1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1,
         -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1, -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1,
         -1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,
          0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
        {-1,  0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,
          0,  1,  2,  3,  4,  5, -1, -1, -1, -1, -1,  0,
          1,  2,  3,  4,  5, -1, -1, -1, -1, -1, -1, -1,
         -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
    };

    /** Binomial coefficients used to compute hash codes */
    private static final int[][] COEFFICIENTS = {
        { 0X0000000F, 0X00000078, 0X000002A8, 0X00000BF4,
          0X00002D6C, 0X00009768, 0X0001C638, 0X0004E11A,
          0X000C7826, 0X001DED28, 0X004403B8, 0X00935D64 },
        { 0X0000000E, 0X00000069, 0X00000230, 0X0000094C,
          0X00002178, 0X000069FC, 0X00012ED0, 0X00031AE2,
          0X0007970C, 0X00117502, 0X00261690, 0X004F59AC },
        { 0X0000000D, 0X0000005B, 0X000001C7, 0X0000071C,
          0X0000182C, 0X00004884, 0X0000C4D4, 0X0001EC12,
          0X00047C2A, 0X0009DDF6, 0X0014A18E, 0X0029431C },
        { 0X0000000C, 0X0000004E, 0X0000016C, 0X00000555,
          0X00001110, 0X00003058, 0X00007C50, 0X0001273E,
          0X00029018, 0X000561CC, 0X000AC398, 0X0014A18E },
        { 0X0000000B, 0X00000042, 0X0000011E, 0X000003E9,
          0X00000BBB, 0X00001F48, 0X00004BF8, 0X0000AAEE,
          0X000168DA, 0X0002D1B4, 0X000561CC, 0X0009DDF6 },
        { 0X0000000A, 0X00000037, 0X000000DC, 0X000002CB,
          0X000007D2, 0X0000138D, 0X00002CB0, 0X00005EF6,
          0X0000BDEC, 0X000168DA, 0X00029018, 0X00047C2A },
        { 0X00000009, 0X0000002D, 0X000000A5, 0X000001EF,
          0X00000507, 0X00000BBB, 0X00001923, 0X00003246,
          0X00005EF6, 0X0000AAEE, 0X0001273E, 0X0001EC12 },
        { 0X00000008, 0X00000024, 0X00000078, 0X0000014A,
          0X00000318, 0X000006B4, 0X00000D68, 0X00001923,
          0X00002CB0, 0X00004BF8, 0X00007C50, 0X0000C4D4 },
        { 0X00000007, 0X0000001C, 0X00000054, 0X000000D2,
          0X000001CE, 0X0000039C, 0X000006B4, 0X00000BBB,
          0X0000138D, 0X00001F48, 0X00003058, 0X00004884 },
        { 0X00000006, 0X00000015, 0X00000038, 0X0000007E,
          0X000000FC, 0X000001CE, 0X00000318, 0X00000507,
          0X000007D2, 0X00000BBB, 0X00001110, 0X0000182C },
        { 0X00000005, 0X0000000F, 0X00000023, 0X00000046,
          0X0000007E, 0X000000D2, 0X0000014A, 0X000001EF,
          0X000002CB, 0X000003E9, 0X00000555, 0X0000071C },
        { 0X00000004, 0X0000000A, 0X00000014, 0X00000023,
          0X00000038, 0X00000054, 0X00000078, 0X000000A5,
          0X000000DC, 0X0000011E, 0X0000016C, 0X000001C7 },
        { 0X00000003, 0X00000006, 0X0000000A, 0X0000000F,
          0X00000015, 0X0000001C, 0X00000024, 0X0000002D,
          0X00000037, 0X00000042, 0X0000004E, 0X0000005B },
        { 0X00000002, 0X00000003, 0X00000004, 0X00000005,
          0X00000006, 0X00000007, 0X00000008, 0X00000009,
          0X0000000A, 0X0000000B, 0X0000000C, 0X0000000D },
        { 0X00000001, 0X00000001, 0X00000001, 0X00000001,
          0X00000001, 0X00000001, 0X00000001, 0X00000001,
          0X00000001, 0X00000001, 0X00000001, 0X00000001 }
    };

    /** Hash code offsets; i.e. number of positions with N seeds */
    private static final int[] OFFSETS = {
               0,       1,        13,       91,      455,     1820,
            6188,   18564,     50388,   125970,   293930,   646646,
         1352078, 2704156,   5200300,  9657700, 17383860
    };

}
