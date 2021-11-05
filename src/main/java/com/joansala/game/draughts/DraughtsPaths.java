package com.joansala.game.draughts;

/*
 * Aalina engine.
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

import java.util.Arrays;
import com.joansala.except.IllegalMoveException;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.draughts.Draughts.*;


/**
 * Conversion between move sequences and their traveled paths.
 */
public class DraughtsPaths {

    /** Game instance where moves are traced */
    private static DraughtsGame game = new DraughtsGame();


    /**
     * Move identifier to its coordinates path.
     */
    public int[] toPath(DraughtsBoard board, int move) {
        synchronized (game) {
            game.setBoard(board);
            return toPath(game, move);
        }
    }


    /**
     * Move coordinates path to a move identifier.
     */
    public int toMove(DraughtsBoard board, int[] path) {
        synchronized (game) {
            game.setBoard(board);
            return toMove(game, path);
        }
    }


    /**
     * Move sequence to their coordinates paths.
     */
    public int[][] toPaths(DraughtsBoard board, int[] moves) {
        int[][] paths = new int[moves.length][];

        synchronized (game) {
            game.setBoard(board);
            game.ensureCapacity(moves.length);

            for (int i = 0; i < moves.length; i++) {
                paths[i] = toPath(game, moves[i]);
                game.makeMove(moves[i]);
            }
        }

        return paths;
    }


    /**
     * Move path sequence to a move identifier sequence.
     */
    public int[] toMoves(DraughtsBoard board, int[][] paths) {
        int[] moves = new int[paths.length];

        synchronized (game) {
            game.setBoard(board);
            game.ensureCapacity(moves.length);

            for (int i = 0; i < moves.length; i++) {
                moves[i] = toMove(game, paths[i]);
                game.makeMove(moves[i]);
            }
        }

        return moves;
    }


    /**
     * Move identifier to its coordinates path.
     */
    private int[] toPath(DraughtsGame game, int move) {
        final int to = move & 0x3F;
        final int from = (move >> 6) & 0x3F;
        final int capture = (move >> 12);

        if (move < 0) {
            throw new IllegalMoveException(
                "Not a valid move encoding");
        }

        if (capture == 0) {
            int[] path = { from, to };
            return path;
        }

        int[] path = game.traceCaptures(move);

        return path;
    }


    /**
     * Move coordinates path to a move identifier.
     */
    private int toMove(DraughtsGame game, int[] path) {
        final int from = path[0];
        final int to = path[path.length - 1];
        final int code = (from << 6) | to;

        int[] moves = game.legalMoves();
        int[] matches = Arrays.stream(moves)
            .filter(m -> code == (m & 0xFFF))
            .toArray();

        if (matches.length == 1) {
            return matches[0];
        }

        for (int move : matches) {
            long rivals = game.rivals();
            int[] trace = game.traceCaptures(move);

            if (pathsEqual(path, trace, rivals)) {
                return move;
            }
        }

        throw new IllegalMoveException(
            "Not a valid move path");
    }


    /**
     * Traces a path as a bitboard that connects all the points.
     */
    public long toBitboard(int[] path) {
        int from = path[0];
        long bits = 0x00L;

        for (int i = 1; i < path.length; i++) {
            int to = path[i];
            int distance = Math.abs(to - from);
            int sense =  (to - from) / distance;
            int direction = (to - from) % NE == 0 ? NE : NW;

            for (int n = 1; n <= distance / direction; n++) {
                bits |= bit(from + n * sense * direction);
            }

            from = to;
        }

        return bits;
    }


    /**
     * Check if two paths capture the same rivals.
     */
    private boolean pathsEqual(int[] a, int[] b, long rivals) {
        final long ta = toBitboard(a);
        final long tb = toBitboard(b);
        return (ta & rivals) == (tb & rivals);
    }
}
