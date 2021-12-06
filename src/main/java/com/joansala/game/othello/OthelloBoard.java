package com.joansala.game.othello;

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
import static com.joansala.game.othello.Othello.*;
import static com.joansala.game.othello.OthelloGame.*;


/**
 * Represents a othello board.
 */
public class OthelloBoard extends BaseBoard<long[]> {

    /** Bitboard converter */
    private static BitsetConverter bitset;

    /** Algebraic coordinates converter */
    private static CoordinateConverter algebraic;

    /** Piece placement converter */
    private static DiagramConverter fen;


    /**
     * Initialize notation converters.
     */
    static {
        bitset = new BitsetConverter(BITS);
        algebraic = new CoordinateConverter(COORDINATES);
        fen = new DiagramConverter(PIECES);
    }


    /**
     * Creates a new board for the start position.
     */
    public OthelloBoard() {
        this(START_POSITION, SOUTH);
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     */
    public OthelloBoard(long[] position, int turn) {
        super(position.clone(), turn);
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
    public OthelloBoard toBoard(String notation) {
        String[] fields = notation.split(" ");

        long[] position = toPosition(fen.toArray(fields[0]));
        int turn = toTurn(fields[1].charAt(0));

        return new OthelloBoard(position, turn);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toDiagram() {
        StringJoiner notation = new StringJoiner(" ");

        notation.add(fen.toDiagram(toOccupants(position)));
        notation.add(String.valueOf(toPlayerSymbol(turn)));

        return notation.toString();
    }


    /**
     * An array of piece symbols placed on the board.
     */
    private Object[] toPieceSymbols(long[] position) {
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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format((
            "=========( %turn to move )=========%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "1 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "2 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "3 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "4 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "5 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "6 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "7 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "8 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "    a   b   c   d   e   f   g   h%n" +
            "===================================").
            replaceAll("(#)", "%1s").
            replace("%turn", toPlayerName(turn)),
            toPieceSymbols(position)
        );
    }
}
