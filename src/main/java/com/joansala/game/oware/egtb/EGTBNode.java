package com.joansala.game.oware.egtb;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Flag;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import static com.joansala.engine.Game.*;


/**
 * A game state on a search tree.
 */
@Entity(version = 1)
public class EGTBNode implements Comparable<EGTBNode>, Serializable {

    /** Serialization version */
    static final long serialVersionUID = 1L;

    /** Hash code of the node */
    @PrimaryKey()
    long hash = 0x00;

    /** Score type */
    int flag = Flag.EMPTY;

    /** Score of this node */
    int score = DRAW_SCORE;

    /** Seed on the position */
    int seeds = 0;

    /** Score of this node */
    int depth = 0;

    /** If this node's score is known */
    boolean known = false;


    /**
     * Create a new node.
     */
    EGTBNode() {}


    /**
     * Create a new node.
     *
     * @param game      Game state
     * @param move      Performed move
     */
    EGTBNode(long hash) {
        this.hash = hash;
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(EGTBNode o) {
        return Long.compare(hash, o.hash);
    }
}
