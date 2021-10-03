package com.joansala.game.oware;

/*
 * Aalina oware engine.
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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
import com.joansala.engine.Board;
import static com.joansala.game.oware.OwareGame.*;
import static com.joansala.game.oware.Oware.*;


/**
 * Oware board representation.
 *
 * Encapsulates a game position and turn. It also provides methods to convert
 * the board and the moves performed on it between its textual and numeric
 * representations.
 */
public class OwareBoard implements Board {

    /** Player to move */
    private int turn;

    /** Board position state */
    private int[] position;


    /**
     * Creates a new board for the start position.
     */
    public OwareBoard() {
        this.turn = SOUTH;
        this.position = rootPosition();
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     *
     * @throws IllegalArgumentException
     */
    public OwareBoard(int[] position, int turn) {
        validateTurn(turn);
        validatePosition(position);
        this.position = position.clone();
        this.turn = turn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] position() {
        return position.clone();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int turn() {
        return this.turn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toAlgebraic(int move) {
        validateMove(move);
        return MOVES[move];
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toAlgebraic(int[] moves) {
        StringBuilder builder = new StringBuilder();

        for (int move : moves) {
            builder.append(toAlgebraic(move));
        }

        return builder.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int toMove(String notation) {
        int move = Arrays.binarySearch(MOVES, notation);
        validateMove(move);
        return move;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] toMoves(String notation) {
        String[] notations = notation.split("");
        int[] moves = new int[notations.length];

        for (int i = 0; i < notations.length; i++) {
            moves[i] = toMove(notations[i]);
        }

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public OwareBoard toBoard(String notation) {
        String[] parts = notation.split("-");
        int[] position = new int[2 + BOARD_SIZE];
        int turn = "S".equals(parts[14]) ? SOUTH : NORTH;

        for (int i = 0; i < 2 + BOARD_SIZE; i++) {
            position[i] = Integer.parseInt(parts[i]);
        }

        return new OwareBoard(position, turn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toNotation() {
        StringBuilder builder = new StringBuilder();

        for (int seeds : position) {
            builder.append(seeds);
            builder.append('-');
        }

        char player = (turn == SOUTH) ? 'S' : 'N';
        builder.append(player);

        return builder.toString();
    }


    /**
     * Array representation for the start position.
     *
     * @return      New position array
     */
    private static int[] rootPosition() {
        final int length = 2 + BOARD_SIZE;
        final int[] position = START_POSITION;
        return Arrays.copyOf(position, length);
    }


    /**
     * Check is an array represents a valid oware position.
     *
     * @param position      Position array
     * @return              {@code true} if valid
     */
    private static boolean isPosition(int[] position) {
        int count = 0;

        if (position.length == 2 + BOARD_SIZE) {
            for (int i = 0; i < 2 + BOARD_SIZE; i++) {
                if (position[i] >= 0) {
                    count += position[i];
                } else {
                    return false;
                }
            }
        }

        return (count == SEED_COUNT);
    }


    /**
     * Asserts a value represents a valid move for this game.
     *
     * @throws IllegalArgumentException If not valid
     */
    private static void validateMove(int move) {
        if (move < SOUTH_LEFT || move > NORTH_RIGHT) {
            throw new IllegalArgumentException(
                "Move is not a valid");
        }
    }


    /**
     * Asserts a value represents a valid turn for this game.
     *
     * @throws IllegalArgumentException If not valid
     */
    private static void validateTurn(int turn) {
        if (turn != SOUTH && turn != NORTH) {
            throw new IllegalArgumentException(
                "Game turn is not valid");
        }
    }


    /**
     * Asserts a value represents a valid position for this game.
     *
     * @throws IllegalArgumentException If not valid
     */
    private static void validatePosition(Object position) {
        if (position instanceof int[] == false) {
            throw new IllegalArgumentException(
                "Game position is not an array");
        }

        if (isPosition((int[]) position) == false) {
            throw new IllegalArgumentException(
                "Game position is not valid");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format((
            "============( %turn to move )============%n" +
            "        f    e    d    c    b    a%n" +
            "+----+----+----+----+----+----+----+----+%n" +
            "|    | 12 | 11 | 10 | #9 | #8 | #7 |    |%n" +
            "| 14 +----+----+----+----+----+----+ 13 |%n" +
            "|    | #1 | #2 | #3 | #4 | #5 | #6 |    |%n" +
            "+----+----+----+----+----+----+----+----+%n" +
            "        A    B    C    D    E    F%n" +
            "=========================================").
            replaceAll("#?(\\d+)", "%$1\\$2d").
            replace("%turn", turn == SOUTH ? "South" : "North"),
            Arrays.stream(position).boxed().toArray(Object[]::new)
        ).replaceAll("\\s0", "  ");
    }
}
