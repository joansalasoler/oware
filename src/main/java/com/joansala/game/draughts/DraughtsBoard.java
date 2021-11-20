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

import java.util.StringJoiner;
import com.joansala.engine.base.BaseBoard;
import com.joansala.util.bits.BitsetConverter;
import com.joansala.util.notation.CoordinateConverter;
import com.joansala.util.notation.DiagramConverter;
import static com.joansala.game.draughts.Draughts.*;
import static com.joansala.game.draughts.DraughtsGame.*;


/**
 * Represents a othello board.
 */
public class DraughtsBoard extends BaseBoard<long[]> {

    /** Bitboard converter */
    private static BitsetConverter bitset;

    /** Algebraic coordinates converter */
    private static CoordinateConverter algebraic;

    /** Piece placement converter */
    private static DiagramConverter fen;

    /** Conversion between paths and moves */
    private static DraughtsPaths encoder;

    /** Draw countdown clock */
    private int clock = 50;


    /**
     * Initialize notation converters.
     */
    static {
        encoder = new DraughtsPaths();
        bitset = new BitsetConverter(BITS);
        algebraic = new CoordinateConverter(COORDINATES);
        fen = new DiagramConverter(PIECES);
    }


    /**
     * Creates a new board for the start position.
     */
    public DraughtsBoard() {
        this(START_POSITION, SOUTH);
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     */
    public DraughtsBoard(long[] position, int turn) {
        super(position.clone(), turn);
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     * @param clock         Draw countdown clock
     */
    public DraughtsBoard(long[] position, int turn, int clock) {
        super(position.clone(), turn);
        this.clock = clock;
    }


    /**
     * Draw countdown clock.
     */
    public int clock() {
        return clock;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long[] position() {
        return position.clone();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int toMove(String notation) {
        int[] path = toPath(notation);
        return encoder.toMove(this, path);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toCoordinates(int move) {
        int[] path = encoder.toPath(this, move);
        return toAlgebraic(move, path);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toNotation(int[] moves) {
        StringJoiner joiner = new StringJoiner(" ");
        int[][] paths = encoder.toPaths(this, moves);

        for (int i = 0; i < moves.length; i++) {
            joiner.add(toAlgebraic(moves[i], paths[i]));
        }

        return joiner.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int[] toMoves(String notation) {
        if (notation == null || notation.isBlank()) {
            return new int[0];
        }

        String[] notations = notation.split(" ");
        int[][] paths = new int[notations.length][];

        for (int i = 0; i < paths.length; i++) {
            paths[i] = toPath(notations[i]);
        }

        return encoder.toMoves(this, paths);
    }


    /**
     * Move checkers path to coordinates string.
     */
    private String toAlgebraic(int move, int[] path) {
        int capture = (move >> 12);
        String delimiter = capture > 0 ? "x" : "-";
        StringJoiner notation = new StringJoiner(delimiter);

        for (int checker : path) {
            notation.add(algebraic.toCoordinate(checker));
        }

        return notation.toString();
    }


    /**
     * Move coordinates string to checkers path.
     */
    private int[] toPath(String notation) {
        String[] fields = notation.split("[-x]");
        int[] path = new int[fields.length];

        for (int i = 0; i < path.length; i++) {
            path[i] = algebraic.toIndex(fields[i]);
        }

        return path;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DraughtsBoard toBoard(String notation) {
        String[] fields = notation.split(" ");

        long[] position = toPosition(fen.toArray(fields[0]));
        int turn = toTurn(fields[1].charAt(0));
        int clock = Integer.parseInt(fields[2]);

        return new DraughtsBoard(position, turn, clock);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toDiagram() {
        StringJoiner notation = new StringJoiner(" ");

        notation.add(fen.toDiagram(toOccupants(position)));
        notation.add(String.valueOf(toPlayerSymbol(turn)));
        notation.add(String.valueOf(clock));

        return notation.toString();
    }


    /**
     * An array of piece symbols placed on the board.
     */
    private String[] toPieceSymbols(long[] position) {
        return fen.toSymbols(toOccupants(position));
    }


    /**
     * Bidimensional array of piece identifiers from bitboards.
     */
    private int[][] toOccupants(long[] position) {
        int[][] occupants = new int[BOARD_RANKS][BOARD_FILES];
        return bitset.toOccupants(occupants, position);
    }


    /**
     * Bitboards from a bidimensional array of piece identifiers.
     */
    private long[] toPosition(int[][] occupants) {
        long[] position = new long[PIECE_COUNT];
        return bitset.toPosition(position, occupants);
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
     * Replace blank squares by an empty symbol.
     */
    private static Object[] replaceEmpty(String[] symbols) {
        for (int i = 0; i < symbols.length; i++) {
            if (symbols[i].charAt(0) == DiagramConverter.EMPTY_SYMBOL) {
                symbols[i] = Character.toString(EMPTY_SYMBOL);
            }
        }

        return symbols;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format((
            "===============( %turn to move )===============%n" +
            "         1       2       3       4       5     %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "   |   | # |   | # |   | # |   | # |   | # | 5 %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            " 6 | # |   | # |   | # |   | # |   | # |   |   %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "   |   | # |   | # |   | # |   | # |   | # | 15%n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "16 | # |   | # |   | # |   | # |   | # |   |   %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "   |   | # |   | # |   | # |   | # |   | # | 25%n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "26 | # |   | # |   | # |   | # |   | # |   |   %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "   |   | # |   | # |   | # |   | # |   | # | 35%n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "36 | # |   | # |   | # |   | # |   | # |   |   %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "   |   | # |   | # |   | # |   | # |   | # | 45%n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "46 | # |   | # |   | # |   | # |   | # |   |   %n" +
            "   +---+---+---+---+---+---+---+---+---+---+   %n" +
            "     46      47      48      49      50        %n" +
            "===============================================").
            replaceAll("(#)", "%1s").
            replace("%turn", toPlayerName(turn)),
            replaceEmpty(toPieceSymbols(position))
        );
    }
}
