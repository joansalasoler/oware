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
import com.joansala.engine.base.BaseBoard;
import com.joansala.except.IllegalPositionException;
import com.joansala.util.notation.CoordinateConverter;
import com.joansala.util.hash.BinomialHash;
import static com.joansala.game.oware.OwareGame.*;
import static com.joansala.game.oware.Oware.*;


/**
 * Oware board representation.
 *
 * Encapsulates a game position and turn. It also provides methods to convert
 * the board and the moves performed on it between its textual and numeric
 * representations.
 */
public class OwareBoard extends BaseBoard<int[]> {

    /** Hash code generator */
    private static BinomialHash hasher;

    /** Algebraic coordinates converter */
    private static CoordinateConverter algebraic;


    /**
     * Initialize notation converters.
     */
    static {
        hasher = hashFunction();
        algebraic = new CoordinateConverter(HOUSES);
    }


    /**
     * Creates a new board for the start position.
     */
    public OwareBoard() {
        this(START_POSITION, SOUTH);
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     *
     * @throws GameEngineException
     */
    public OwareBoard(int[] position, int turn) {
        super(position.clone(), turn);
        validatePosition(position);
    }


    /**
     * Initialize the hash code generator.
     */
    public static BinomialHash hashFunction() {
        return new BinomialHash(SEED_COUNT, POSITION_SIZE);
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
    public int toMove(String notation) {
        return algebraic.toIndex(notation);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toCoordinates(int move) {
        return algebraic.toCoordinate(move);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toNotation(int[] moves) {
        StringBuilder builder = new StringBuilder();

        for (int move : moves) {
            builder.append(toCoordinates(move));
        }

        return builder.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] toMoves(String notation) {
        if (notation == null || notation.isBlank()) {
            return new int[0];
        }

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
        int[] position = new int[POSITION_SIZE];
        int turn = toTurn(parts[POSITION_SIZE].charAt(0));

        for (int i = 0; i < POSITION_SIZE; i++) {
            position[i] = Integer.parseInt(parts[i]);
        }

        return new OwareBoard(position, turn);
    }


    /**
     * Converts a binomial hash code to a board instance.
     *
     * @param hash      Unique position hash code
     * @return          New boardboard instance
     */
    public OwareBoard toBoard(long hash) {
        int[] position = hasher.unhash(hash & ~SOUTH_SIGN);
        int turn = (SOUTH_SIGN & hash) == 0L ? NORTH : SOUTH;
        return new OwareBoard(position, turn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toDiagram() {
        StringBuilder builder = new StringBuilder();

        for (int seeds : position) {
            builder.append(seeds);
            builder.append('-');
        }

        builder.append(toPlayerSymbol(turn));

        return builder.toString();
    }


    /**
     * Check is an array represents a valid oware position.
     *
     * @param position      Position array
     * @return              {@code true} if valid
     */
    private static boolean isPosition(int[] position) {
        int count = 0;

        if (position.length == POSITION_SIZE) {
            for (int i = 0; i < POSITION_SIZE; i++) {
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
     * Asserts a value represents a valid position for this game.
     *
     * @throws GameEngineException If not valid
     */
    private static void validatePosition(Object position) {
        if (position instanceof int[] == false) {
            throw new IllegalPositionException(
                "Game position is not an array");
        }

        if (isPosition((int[]) position) == false) {
            throw new IllegalPositionException(
                "Game position is not valid");
        }
    }


    /**
     * An array of piece symbols placed on the board.
     */
    private Object[] toPieceSymbols(int[] position) {
        return Arrays.stream(position).boxed().toArray(Object[]::new);
    }


    /**
     * Converts a turn identifier to a player notation.
     */
    private static char toPlayerSymbol(int turn) {
        return turn == SOUTH ? SOUTH_SYMBOL : NORTH_SYMBOL;
    }


    /**
     * Converts a turn identifier to a player name.
     */
    private static String toPlayerName(int turn) {
        return turn == SOUTH ? SOUTH_NAME : NORTH_NAME;
    }


    /**
     * Converts a player symbol to a turn identifier.
     */
    private static int toTurn(char symbol) {
        return symbol == SOUTH_SYMBOL ? SOUTH : NORTH;
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
            replace("%turn", toPlayerName(turn)),
            toPieceSymbols(position)
        ).replaceAll("\\s0", "  ");
    }
}
