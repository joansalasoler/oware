package com.joansala.util;

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

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import com.joansala.engine.Board;


/**
 * Reads game matches from an input stream.
 */
public class GameScanner implements Closeable, Iterator<GameState> {

    /** Board instance */
    private Board parser;

    /** Text scanner */
    private Scanner scanner;


    /**
     * Creates a scanner that reads from the standard input.
     */
    public GameScanner(Board board) {
        this(board, System.in);
    }


    /**
     * Creates a scanner that reads from an input stream.
     */
    public GameScanner(Board board, InputStream source) {
        scanner = new Scanner(source);
        parser = board;
    }


    /**
     * {@inheritDoc}
     */
    @Override public boolean hasNext() {
        return scanner.hasNextLine();
    }


    /**
     * {@inheritDoc}
     */
    @Override public GameState next() {
        String line = scanner.nextLine();
        String[] parts = line.trim().split("\\s", 2);
        Board board = parser.toBoard(parts[0]);
        int[] moves = new int[0];

        if (parts.length > 1) {
            moves = parser.toMoves(parts[1].trim());
        }

        return new GameState(board, moves);
    }


    /**
     * {@inheritDoc}
     */
    @Override public void close() throws IOException {
        scanner.close();
    }
}
