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

import java.util.List;
import java.util.LinkedList;

import com.joansala.engine.*;


/**
 * Obtains search information from a tree of nodes.
 */
public class DOEReport implements Report {

    /** Type of the collected score */
    private int flag = Flag.EXACT;

    /** Maximum search depth reached */
    private int depth = 0;

    /** Current evaluation of the game */
    private int score = 0;

    /** Moves of the principal variation */
    private int[] variation = {};


    /**
     * Create a new report.
     *
     * @param game      Game state
     * @param engine    Search engine
     * @param root      Root node
     */
    public DOEReport(DOE engine, Game game, DOENode root) {
        if (root != null && root.child != null) {
            collectReport(engine, game, root);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getDepth() {
        return depth;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlag() {
        return flag;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getVariation() {
        return variation;
    }


    /**
     * Collect this report data from the given game and tree.
     *
     * @param game      Game state
     * @param root      Root node
     */
    public void collectReport(DOE engine, Game game, DOENode root) {
        final DOENode bestChild = engine.pickBestChild(root);
        final List<Integer> moves = new LinkedList<>();

        DOENode child = bestChild;

        while ((child = nextNode(engine, child)) != null) {
            moves.add(child.move);
        }

        moves.add(0, bestChild.move);
        score = game.toCentiPawns((int) bestChild.score);
        variation = toArray(moves);
        depth = moves.size();
    }


    /**
     * Obtain the best child of an expanded node.
     *
     * @param engine    Search engine
     * @param node      Parent node
     * @param size      Minimum sample size
     *
     * @return          Best child or {@code null}
     */
    private DOENode nextNode(DOE engine, DOENode node) {
        return (node.child != null) ? engine.pickBestChild(node) : null;
    }


    /**
     * Convert an Integer list into an int[].
     *
     * @param values    List of values
     * @return          A new array
     */
    private int[] toArray(List<Integer> values) {
        return values.stream().mapToInt(i -> i).toArray();
    }
}
