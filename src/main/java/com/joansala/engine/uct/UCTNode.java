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
 */
class UCTNode {

    /** Parent of this node */
    UCTNode parent = null;

    /** First child of this node */
    UCTNode child = null;

    /** First sibling of this node */
    UCTNode sibling = null;

    /** Unique hash code of the node */
    long hash = 0x00;

    /** Performed move to reach the node */
    int move = Game.NULL_MOVE;

    /** Current move generation cursor */
    int cursor = Game.NULL_MOVE;

    /** Initial move generation cursor */
    int reset = Game.NULL_MOVE;

    /** Whether it is fully expanded */
    boolean expanded = false;

    /** Whether it is an endgame position */
    boolean terminal = false;

    /** Number of played simulations */
    long count = 0;

    /** Average score of the simulations */
    double score = 0.0;


    /**
     * Adds a new child node to this parent.
     *
     * @param node      Child node
     */
    void pushChild(UCTNode node) {
        node.parent = this;
        node.sibling = child;
        child = node;
    }


    /**
     * Set the initial state of this node.
     *
     * @param game      Game state
     * @param move      Performed move
     */
    void setState(Game game, int move) {
        hash = game.hash();
        cursor = game.getCursor();
        terminal = game.hasEnded();
        this.reset = cursor;
        this.move = move;
    }


    /**
     * Increase the number of simulations.
     */
    void increaseCount() {
        count++;
    }


    /**
     * Sets the initial score of this node.
     *
     * @param value     Node score
     */
    void initScore(double value) {
        score = value;
        count = 1;
    }


    /**
     * Sets this node as terminal and fixes its score.
     *
     * @param value     Terminal score
     */
    void settleScore(double value) {
        terminal = true;
        score = value;
        count++;
    }


    /**
     * Update the average score with a new value.
     *
     * @param value     Simulation score
     */
    void updateScore(double value) {
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
    void proveScore(double score) {
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
    void detachChildren() {
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
    void detachFromTree() {
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
