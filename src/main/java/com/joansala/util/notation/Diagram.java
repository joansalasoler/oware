package com.joansala.util.notation;

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

import java.util.List;
import java.util.LinkedList;
import java.util.StringJoiner;
import com.joansala.except.IllegalPieceException;


/**
 * Diagram position notation converter. This is based on the popular
 * Forsyth-Edwards Notation for Chess.
 */
public class Diagram {

    /** Identifier of an occupant that is not a piece */
    public static final int NULL_PIECE = -1;

    /** Default null piece symbol */
    public static final char EMPTY_SYMBOL = ' ';

    /** Indexed array of piece symbols */
    private final char[] pieces;


    /**
     * Creates a new diagram converter.
     *
     * @param pieces        Piece notations
     */
    public Diagram(char[] pieces) {
        this.pieces = pieces;
    }


    /**
     * Check if the given piece identifier is valid.
     */
    private boolean isPiece(int piece) {
        return piece >= 0 && piece < pieces.length;
    }


    /**
     * Converts a piece index to a piece notation.
     *
     * @param piece         A piece identifier
     * @return              Piece symbol
     *
     * @throws IllegalPieceException    If piece is not a valid
     */
    public char toSymbol(int piece) {
        if (piece == NULL_PIECE) {
            return EMPTY_SYMBOL;
        }

        if (piece < 0 || piece >= pieces.length) {
            throw new IllegalPieceException(
                "Not a valid piece: " + piece);
        }

        return pieces[piece];
    }


    /**
     * Converts a piece notation to a piece identifier.
     *
     * @param symbol        A piece symbol
     * @return              Piece identifier
     *
     * @throws IllegalPieceException    If symbol is not a valid
     */
    public int toPiece(char symbol) {
        for (int piece = 0; piece < pieces.length; piece++) {
            if (symbol == pieces[piece]) {
                return piece;
            }
        }

        throw new IllegalPieceException(
            "Not a valid piece symbol: " + symbol);
    }


    /**
     * Converts a placement notation to an array of pieces.
     *
     * @param notation      Piece placement notation
     * @return              Pieces on each rank and file
     *
     * @throws IllegalPieceException    If a piece symbol is not a valid
     */
    public int[][] toArray(String notation) {
        String[] placements = notation.split("/");
        int[][] occupants = new int[placements.length][];

        for (int i = 0; i < occupants.length; i++) {
            int rank = placements.length - i - 1;
            char[] symbols = placements[rank].toCharArray();
            occupants[i] = toPieces(symbols);
        }

        return occupants;
    }


    /**
     * Converts an array of pieces to a placement notation.
     *
     * @param occupants     Pieces on each rank and file
     * @return              Placement notation
     *
     * @throws IllegalPieceException    If a piece is not a valid
     */
    public String toNotation(int[][] occupants) {
        StringJoiner notation = new StringJoiner("/");

        for (int i = occupants.length - 1; i >= 0; i--) {
            String symbols = toPlacements(occupants[i]);
            notation.add(symbols);
        }

        return notation.toString();
    }


    /**
     * Converts an array of pieces to an array of piece notations.
     *
     * @param occupants     Pieces on each rank and file
     * @return              Piece notations array
     *
     * @throws IllegalPieceException    If a piece is not a valid
     */
    public String[] toSymbols(int[][] occupants) {
        List<String> symbols = new LinkedList<>();

        for (int i = occupants.length - 1; i >= 0; i--) {
            for (int piece : occupants[i]) {
                char symbol = toSymbol(piece);
                symbols.add(Character.toString(symbol));
            }
        }

        return toStringArray(symbols);
    }


    /**
     * Convert a pieces array to rank placement string.
     *
     * @param occupants     Pieces on the file
     * @return              Rank notation
     *
     * @throws IllegalPieceException    If a piece is not a valid
     */
    private String toPlacements(int[] occupants) {
        final StringBuilder builder = new StringBuilder();
        final int length = occupants.length;

        for (int i = 0; i < length; i++) {
            if (isPiece(occupants[i])) {
                char symbol = toSymbol(occupants[i]);
                builder.append(symbol);
                continue;
            }

            int n = i - 1;
            int count = 0;

            while (i < length && !isPiece(occupants[i])) {
                count++;
                i++;
            }

            i = n + count;
            builder.append(count);
        }

        return builder.toString();
    }


    /**
     * Converts an array of piece placement symbols to an array of
     * piece indices.
     *
     * @param symbols       Array of symbols
     * @return              Pieces array
     *
     * @throws IllegalPieceException    If a symbol is not a valid
     */
    private int[] toPieces(char[] symbols) {
        List<Integer> pieces = new LinkedList<>();

        for (char symbol : symbols) {
            if (!Character.isDigit(symbol)) {
                pieces.add(toPiece(symbol));
            } else {
                int empties = Character.digit(symbol, 10);

                for (int i = 0; i < empties; i++) {
                    pieces.add(NULL_PIECE);
                }
            }
        }

        return toIntArray(pieces);
    }


    /**
     * Converts a list of strings to an array of strings.
     *
     * @param values        List of values
     * @return              Array of strings
     */
    private String[] toStringArray(List<String> values) {
        return values.toArray(new String[values.size()]);
    }


    /**
     * Converts a list of integers to an array of primitives.
     *
     * @param values        List of values
     * @return              Array of integers
     */
    private int[] toIntArray(List<Integer> values) {
        int length = values.size();
        int[] array = new int[length];

        for (int i = 0; i < length; i++) {
            array[i] = values.get(i);
        }

        return array;
    }
}
