package com.joansala.util.dump;

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
import java.util.Date;
import com.sleepycat.je.DatabaseException;

import com.joansala.tools.train.TrainGraph;
import com.joansala.tools.train.TrainNode;
import static com.joansala.tools.train.TrainNode.*;


/**
 *
 */
public class BookExporter {

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
    public void export(TrainGraph graph, String path) throws DatabaseException, IOException {
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
            TrainNode node = graph.get(hash);
            short[] scores = new short[6];

            if ((node.getFlag() & KNOWN) == 0) {
                continue;
            }

            if (node.numEdges() < 1) {
                continue;
            }

            for (int i = 0; i < 6; i++) {
                scores[i] = Short.MIN_VALUE;
            }

            for (int i = 0; i < node.numEdges(); i++) {
                TrainNode child = node.getChild(i);
                int move = node.getMove(i);
                int index = (move > 5) ? move - 6 : move;
                scores[index] = (short) -child.getScore();
            }

            raf.writeLong(hash);

            for (short score : scores) {
                raf.writeShort(score);
            }
        }

        raf.close();
    }
}
