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
class Node {

    /** Parent of this node */
    Node parent = null;

    /** First child of this node */
    Node child = null;

    /** First sibling of this node */
    Node sibling = null;

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
     * Detach this node from the tree.
     */
    void detachNode() {
        this.sibling = null;
        parent.child = null;
        parent = null;
        System.gc();
    }


    /**
     * Init this node from a new game state.
     *
     * @param game      Game state
     */
    void updateState(Game game) {
        hash = game.hash();
        cursor = game.getCursor();
        terminal = game.hasEnded();
    }


    /**
     * Sets a new parent for this node.
     *
     * @param parent    Parent node
     */
    void updateParent(Node parent) {
        this.parent = parent;
        this.sibling = parent.child;
        parent.child = this;
    }


    /**
     * Update this node's score with the result of a simulation.
     *
     * @param value     Simulation score
     */
    void updateScore(double value) {
        score += (value - score) / ++count;
    }


    /**
     * Sets the terminal score of this node.
     *
     * @param value     Terminal score
     */
    void setScore(double value) {
        score = value;
        count++;
    }


    /**
     * Increase the number of simulations.
     */
    void increaseCount() {
        count++;
    }
}
