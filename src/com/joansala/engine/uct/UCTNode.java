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

    /** Whether it is fully expanded */
    boolean expanded = false;

    /** Whether it is an endgame position */
    boolean terminal = false;

    /** Number of played simulations */
    int count = 0;

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
     * Init this node from a new game state.
     *
     * @param game      Game state
     * @param move      Performed move
     */
    void setState(Game game, int move) {
        hash = game.hash();
        cursor = game.getCursor();
        terminal = game.hasEnded();
        this.move = move;
        expanded = false;
    }


    /**
     * Sets the initial score of this node.
     *
     * @param value     Node score
     */
    void setFirstScore(double value) {
        score = value;
        count = 1;
    }


    /**
     * Sets this node as terminal and fixes its score.
     *
     * @param value     Terminal score
     */
    void setExactScore(double value) {
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
     * Increase the number of simulations.
     */
    void increaseCount() {
        count++;
    }


    /**
     * Detach this node from the tree. Removes all the refernces to the
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
