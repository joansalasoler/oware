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


import com.joansala.engine.Game;


/**
 * A game state on a search tree.
 *
 * The search tree is stored as a left-child right-sibling binary tree
 * for efficiency. To iterate the children of a node one must first check
 * the first child and from there recursively iterate their siblings.
 */
public class UCTNode {

    /** Parent of this node */
    private UCTNode parent = null;

    /** First child of this node */
    private UCTNode child = null;

    /** First sibling of this node */
    private UCTNode sibling = null;

    /** Hash code of the node */
    private long hash = 0x00;

    /** Player to move on this node */
    private int turn = Game.SOUTH;

    /** Performed move to reach the node */
    private int move = Game.NULL_MOVE;

    /** Current move generation cursor */
    private int cursor = Game.NULL_MOVE;

    /** Initial move generation cursor */
    private int reset = Game.NULL_MOVE;

    /** Whether it is fully expanded */
    private boolean expanded = false;

    /** Whether it is an endgame position */
    private boolean terminal = false;

    /** Number of played simulations */
    private long count = 0;

    /** Average score of the simulations */
    private double score = 0.0;


    /**
     * Create a new node.
     *
     * @param game      Game state
     * @param move      Performed move
     */
    protected UCTNode(Game game, int move) {
        hash = game.hash();
        cursor = game.getCursor();
        terminal = game.hasEnded();
        this.turn = game.turn();
        this.reset = cursor;
        this.move = move;
    }


    /**
     * Parent of this node if it is known.
     */
    public UCTNode parent() {
        return parent;
    }


    /**
     * Current first sibling of this node if it is known.
     */
    public UCTNode sibling() {
        return sibling;
    }


    /**
     * Current first child of this node if expanded.
     */
    public UCTNode child() {
        return child;
    }


    /**
     * Player to move on this node.
     */
    public int turn() {
        return turn;
    }


    /**
     * Performed move to reach the node.
     */
    public int move() {
        return move;
    }


    /**
     * Hash code of the node.
     */
    public long hash() {
        return hash;
    }


    /**
     * How many times this branch was expanded.
     */
    public long count() {
        return count;
    }


    /**
     * Average score of this node's branch.
     */
    public double score() {
        return score;
    }


    /**
     * Whether the exact score is known.
     */
    public boolean terminal() {
        return terminal;
    }


    /**
     * Whether all this node's childs were added to the tree.
     */
    public boolean expanded() {
        return expanded;
    }


    /**
     * Advance to the next move and return it.
     *
     * @param game      State of this node
     */
    protected int nextMove(Game game) {
        final int move;

        if (expanded) {
            move = Game.NULL_MOVE;
        } else {
            game.setCursor(cursor);
            move = game.nextMove();
            cursor = game.getCursor();
            expanded = (move == Game.NULL_MOVE);
        }

        return move;
    }


    /**
     * Adds a new child node to this parent.
     *
     * @param node      Child node
     */
    protected void pushChild(UCTNode node) {
        node.parent = this;
        node.sibling = child;
        child = node;
    }


    /**
     * Increase the number of simulations.
     */
    protected void increaseCount() {
        count++;
    }


    /**
     * Sets the initial score of this node.
     *
     * @param value     Node score
     */
    protected void initScore(double value) {
        score = value;
        count = 1;
    }


    /**
     * Sets this node as terminal and fixes its score.
     *
     * @param value     Terminal score
     */
    protected void settleScore(double value) {
        terminal = true;
        score = value;
        count++;
    }


    /**
     * Update the average score with a new value.
     *
     * @param value     Simulation score
     */
    protected void updateScore(double value) {
        score += (value - score) / ++count;
    }


    /**
     * Proves the terminal score of a node.
     *
     * If all children are terminal and their scores equal to the given
     * value sets the terminal score of the node; otherwise, updates the
     * average score of the node with the new value.
     *
     * @param value     Terminal score
     */
    protected void proveScore(double score) {
        UCTNode child = this.child;

        do {
            if (!child.terminal || child.score != -score) {
                updateScore(score);
                return;
            }
        } while ((child = child.sibling) != null);

        settleScore(score);
    }


    /**
     * Detach all the children of this node. Removes all the references
     * to this node from its children and resets the move cursor; so the
     * children subtrees can then be garbage collected.
     */
    protected void detachChildren() {
        if (child == null) {
            return;
        }

        UCTNode node = child;

        while ((node = node.sibling) != null) {
            node.parent = null;
        }

        cursor = reset;
        expanded = false;
        child.parent = null;
        child = null;
    }


    /**
     * Detach this node from the tree. Removes all the references to the
     * node found on the tree; so the node can be garbage collected.
     */
    protected void detachFromTree() {
        if (parent == null) {
            return;
        }

        if (parent.child == this) {
            parent.child = null;
            parent = null;
            return;
        }

        UCTNode node = parent.child;

        while ((node = node.sibling) != null) {
            if (node.sibling == this) {
                node.sibling = null;
                parent = null;
                break;
            }
        }
    }
}
