package com.joansala.tools.train;

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

import java.io.Serializable;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;


/**
 * Represents a node of a game state space.
 */
@Entity
public class TrainNode implements Serializable {

    /** Known node flag */
    public static final int KNOWN = 0x01;

    /** Propagated node flag */
    public static final int PROPAGATED = 0x02;

    /** Class version identifier */
    private static final long serialVersionUID = 13L;

    /** Graph to which this node pertains */
    private transient TrainGraph graph;

    /** Unique identifier for this node */
    @PrimaryKey
    private long hash = 0;

    /** Priority for the opponent */
    private float epo = 0.0F;

    /** Priority for the book player */
    private float epb = 0.0F;

    /** This node flag */
    private int flag = 0;

    /** Score for the node */
    private int score = Integer.MIN_VALUE;

    /** Number of childs of this node */
    private int numEdges = 0;

    /** Moves that lead to each of the childs */
    private int[] moves = new int[6];

    /** Immediate descendents of this node */
    private long[] childs = new long[6];


    /**
     * Use a {@code TrainGraph} object to instantiate a node.
     */
    protected TrainNode() {}


    /**
     * Sets the graph object to which this node pertains.
     *
     * @param graph  a graph object
     */
    protected void setGraph(TrainGraph graph) {
        this.graph = graph;
    }


    /**
     * Sets the unique identifier for this node.
     *
     * @param hash  unique identifier
     */
    protected void setHash(long hash) {
        this.hash = hash;
    }


    /**
     * Sets the score value of this node
     *
     * @param score score value
     */
    public void setScore(int score) {
        this.score = score;
    }


    /**
     * Sets the book player priority value of this node.
     *
     * @param priority  priority value
     */
    public void setBPriority(float priority) {
        this.epb = priority;
    }


    /**
     * Sets the opponent priority value of this node.
     *
     * @param priority  priority value
     */
    public void setOPriority(float priority) {
        this.epo = priority;
    }


    /**
     * Sets this node flag
     *
     * @param flag  flag for this node
     */
    public void setFlag(int flag) {
        this.flag = flag;
    }


    /**
     * Returns the unique identifier of this node
     *
     * @return  hash value
     */
    public long getHash() {
        return hash;
    }


    /**
     * Returns the score for this node
     *
     * @return  score value
     */
    public int getScore() {
        return score;
    }


    /**
     * Returns the book player priority for this node
     *
     * @return  priority value
     */
    public float getBPriority() {
        return epb;
    }


    /**
     * Returns the opponent priority for this node
     *
     * @return  priority value
     */
    public float getOPriority() {
        return epo;
    }


    /**
     * Returns this node flag value
     *
     * @return flag value
     */
    public int getFlag() {
        return flag;
    }


    /**
     * Returns the move for an specified edge.
     *
     * @param edge  edge identifier
     * @return      move performed
     */
    public int getMove(int edge) {
        return moves[edge];
    }


    /**
     * Returns the number of childs of the node.
     *
     * @return  number of edges for this node
     */
    public int numEdges() {
        return numEdges;
    }


    /**
     * Returns the child node linked to an specified edge.
     *
     * @param edge  edge identifier
     * @return      linked node
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public TrainNode getChild(int edge) throws DatabaseException {
        return graph.get(childs[edge]);
    }


    /**
     * Adds a new edge to this node.
     *
     * Each edge has a move value and a linked child node that pertains
     * to the same graph as this node. If the graph already contains a
     * node with the specified identifier that node is returned, otherwise
     * a new node is added to the graph.
     *
     * @param hash  child node identifier for the edge
     * @param move  move that leads to the edge
     * @return      linked node object for the edge
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public TrainNode addEdge(long hash, int move) throws DatabaseException {
        moves[numEdges] = move;
        childs[numEdges] = hash;
        numEdges++;

        return graph.add(hash);
    }


    /**
     * Returns an array containing all the childs of this node.
     *
     * @return  nodes array or {@code null} if node childs
     *          were added to this node
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public TrainNode[] childs() throws DatabaseException {
        if (numEdges == 0)
            return null;

        TrainNode[] nodes = new TrainNode[numEdges];

        for (int i = 0; i < numEdges; i++)
            nodes[i] = graph.get(childs[i]);

        return nodes;
    }
}
