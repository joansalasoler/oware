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


import java.io.Serializable;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import com.joansala.engine.Game;


/**
 * A game state on a search tree.
 */
@Entity
public class DOENode implements Serializable {

    /** Serialization version */
    static final long serialVersionUID = 1L;

    /** Unique identifier of the node */
    @PrimaryKey(sequence="ID")
    long id;

    /** Hash code of the node */
    long hash = 0x00;

    /** Parent of this node */
    Long parent = null;

    /** First child of this node */
    Long child = null;

    /** First sibling of this node */
    Long sibling = null;

    /** Current move generation cursor */
    int cursor = Game.NULL_MOVE;

    /** Performed move to reach the node */
    int move = Game.NULL_MOVE;

    /** Moves that lead to this node */
    int[] moves = null;

    /** Whether it is fully expanded */
    boolean expanded = false;

    /** Whether it is an endgame position */
    boolean terminal = false;

    /** Whether this node was evaluated */
    boolean evaluated = false;

    /** Number of played simulations */
    long count = 0;

    /** Average score of the simulations */
    double score = 0.0;


    /**
     * Create a new node.
     */
    DOENode() {}


    /**
     * Create a new node.
     *
     * @param game      Game state
     * @param move      Performed move
     */
    DOENode(Game game, int move) {
        hash = game.hash();
        cursor = game.getCursor();
        terminal = game.hasEnded();
        moves = game.moves();
        this.move = move;
    }


    /**
     * Generate the next move and return it.
     */
    int nextMove(Game game) {
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
    void pushChild(DOENode node) {
        node.parent = this.id;
        node.sibling = child;
        child = node.id;
    }


    /**
     * Update the average score with a new value.
     *
     * @param value     Simulation score
     */
    void updateScore(double value) {
        score += (value - score) / ++count;
    }
}
