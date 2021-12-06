package com.joansala.game.chess;

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
import java.util.StringJoiner;
import com.joansala.engine.base.BaseBoard;
import com.joansala.except.IllegalMoveException;
import com.joansala.util.bits.BitsetConverter;
import com.joansala.util.notation.CoordinateConverter;
import com.joansala.util.notation.DiagramConverter;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.chess.Chess.*;
import static com.joansala.game.chess.ChessGame.*;


/**
 * Represents a chess board.
 */
public class ChessBoard extends BaseBoard<long[]> {

    /** Game instance where moves are traced */
    private static ChessGame game = new ChessGame();

    /** Bitboard converter */
    private static BitsetConverter bitset;

    /** Algebraic coordinates converter */
    private static CoordinateConverter algebraic;

    /** Piece placement converter */
    private static DiagramConverter fen;

    /** Fullmoves since the game started */
    private int fullmove = 1;

    /** Halfmoves since last advance */
    private int clock = 0;


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
    public ChessBoard() {
        this(START_POSITION, SOUTH);
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     */
    public ChessBoard(long[] position, int turn) {
        super(position.clone(), turn);
    }


    /**
     * Creates a new board instance.
     *
     * @param position      Position array
     * @param turn          Player to move
     * @param clock         Halfmove clock
     * @param fullmove      Fullmove number
     */
    public ChessBoard(long[] position, int turn, int clock, int fullmove) {
        super(position.clone(), turn);
        this.fullmove = fullmove;
        this.clock = clock;
    }


    /**
     * Plies since last advance.
     */
    public int clock() {
        return clock;
    }


    /**
     * Number of the full move.
     */
    public int fullmove() {
        return fullmove;
    }


    /**
     * Castling rights identifier.
     */
    private int castlings() {
        long flags = position[FLAGS];
        long active = flags & 1L;

        active |= (flags & (1L << 7)) >>> 6;
        active |= (flags & (1L << 56)) >>> 54;
        active |= (flags & (1L << 63)) >>> 60;

        return (int) active;
    }


    /**
     * Checker of the pawn that can be captured en-passant or zero
     * if no en-passant capture is possible.
     */
    private int enpassant() {
        long active = position[FLAGS] & ~CASTLE_MASK;
        return empty(active) ? 0 : first(active);
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
    public int toMove(String coordinate) {
        return toMove(position, coordinate);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toCoordinates(int move) {
        if (move <= 0) {
            throw new IllegalMoveException(
                "Not a valid move encoding");
        }

        StringBuilder notation = new StringBuilder();
        final int piece = (move >> 12) & 0x7;
        final int from = (move >> 6) & 0x3F;
        final int to = (move) & 0x3F;

        notation.append(algebraic.toCoordinate(from));
        notation.append(algebraic.toCoordinate(to));

        if (PROMOTION == (move & CASTLING)) {
            notation.append(fen.toSymbol(piece - KING));
        }

        return notation.toString();
    }


    /**
     * Encode a single move given a position and a coordinate.
     *
     * @param position      Position bitboards
     * @param coordinate    Move coordinates
     * @return              Encoded move
     */
    private int toMove(long[] position, String coordinate) {
        String origin = coordinate.substring(0, 2);
        String target = coordinate.substring(2, 4);

        int to = algebraic.toIndex(target);
        int from = algebraic.toIndex(origin);
        int capture = piece(position, to);
        int piece = piece(position, from);
        int move = (from << 6) | to;

        if (isCastling(piece, CASTLING | move)) {
            return CASTLING | move;
        }

        if (coordinate.length() > 4) {
            char symbol = coordinate.charAt(4);
            symbol = Character.toUpperCase(symbol);
            piece = KING + fen.toPiece(symbol);
            move |= PROMOTION;
        }

        if (capture > KING == false) {
            if (isEnPassant(piece, from, to)) {
                move |= ENPASSANT;
            }
        }

        return move | (piece << 12);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toNotation(int[] moves) {
        StringJoiner joiner = new StringJoiner(" ");

        for (int move : moves) {
            joiner.add(toCoordinates(move));
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

        String[] coordinates = notation.split(" ");
        int[] moves = new int[coordinates.length];

        synchronized (game) {
            game.setBoard(this);
            game.ensureCapacity(moves.length);

            for (int i = 0; i < coordinates.length; i++) {
                long[] state = game.state();
                String coordinate = coordinates[i];
                moves[i] = toMove(state, coordinate);
                game.makeMove(moves[i]);
            }
        }

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChessBoard toBoard(String notation) {
        String[] fields = notation.split(" ");

        long[] position = toPosition(fen.toArray(fields[0]));
        int turn = toTurn(fields[1].charAt(0));
        int enpassant = toEnPassantChecker(fields[3]);
        int clock = Integer.parseInt(fields[4]);
        int fullmove = Integer.parseInt(fields[5]);
        long castlings = toCastlingFlags(fields[2]);

        position[FLAGS] |= castlings;

        if (enpassant >= 0) {
            position[FLAGS] |= bit(enpassant);
        }

        return new ChessBoard(position, turn, clock, fullmove);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toDiagram() {
        StringJoiner notation = new StringJoiner(" ");

        notation.add(fen.toDiagram(toOccupants(position)));
        notation.add(String.valueOf(toPlayerSymbol(turn)));
        notation.add(toCastlingRights(castlings()));
        notation.add(toEnPassantCoordinate(enpassant()));
        notation.add(String.valueOf(clock));
        notation.add(String.valueOf(fullmove));

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
        long[] pieces = new long[PIECES.length];

        for (int piece = KING; piece < WHITE; piece++) {
            pieces[piece] = position[piece] & position[WHITE];
            pieces[piece + 6] = position[piece] & position[BLACK];
        }

        return bitset.toOccupants(occupants, pieces);
    }


    /**
     * Bitboards from a bidimensional array of piece identifiers.
     */
    private long[] toPosition(int[][] occupants) {
        long[] position = new long[STATE_SIZE];
        long[] bitboards = new long[PIECES.length];
        bitboards = bitset.toPosition(bitboards, occupants);

        for (int piece = KING; piece < WHITE; piece++) {
            long white = bitboards[piece];
            long black = bitboards[piece + 6];
            position[piece] |= white | black;
            position[WHITE] |= white;
            position[BLACK] |= black;
        }

        return position;
    }


    /**
     * Obtain the piece on the given bitboard index.
     */
    private int piece(long[] position, int index) {
        for (int piece = KING; piece <= PAWN; piece++) {
            if (contains(position[piece], bit(index))) {
                return piece;
            }
        }

        return -1;
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
     * Converts an en-passant target square to a coordinate.
     */
    private static String toEnPassantCoordinate(int checker) {
        return checker == 0 ? "-" : algebraic.toCoordinate(checker);
    }


    /**
     * Converts an en-passant coodinate to a target checker.
     */
    private static int toEnPassantChecker(String coordinate) {
        return "-".equals(coordinate) ? -1 : algebraic.toIndex(coordinate);
    }


    /**
     * Converts a castling identifier to a castling rights string.
     */
    private static String toCastlingRights(int index) {
        return CASTLINGS[index];
    }


    /**
     * Converts castling rights string to a castling identifier.
     */
    private static long toCastlingFlags(String rights) {
        char[] castlings = rights.toCharArray();
        Arrays.sort(castlings);
        rights = String.valueOf(castlings);

        long rooks = 0x00L;

        for (int index = 0; index < CASTLINGS.length; index++) {
            if (CASTLINGS[index].equals(rights)) {
                rooks |= index & 0x1L;
                rooks |= (index & 0x2L) << 6;
                rooks |= (index & 0x4L) << 54;
                rooks |= (index & 0x8L) << 60;
                break;
            }
        }

        return rooks;
    }


    /**
     * Checks if we are dealing with a castling move.
     *
     * @param piece         Piece to move
     * @param move          Move encoding
     */
    private boolean isCastling(int piece, int move) {
        return piece == KING && (
            move == WHITE_SHORT.move || move == WHITE_LONG.move ||
            move == BLACK_SHORT.move || move == BLACK_LONG.move
        );
    }


    /**
     * Checks if we are dealing with an en-passant capture.
     *
     * @param piece         Piece to move
     * @param move          Move encoding
     */
    private boolean isEnPassant(int piece, int from, int to) {
        return piece == PAWN && (to - from) % BOARD_FILES != 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format((
            "=========( %turn to move )=========%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "8 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "7 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "6 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "5 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "4 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "3 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "2 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "1 | # | # | # | # | # | # | # | # |%n" +
            "  +---+---+---+---+---+---+---+---+%n" +
            "    a   b   c   d   e   f   g   h%n" +
            "===================================").
            replaceAll("(#)", "%1s").
            replace("%turn", toPlayerName(turn)),
            toPieceSymbols(position)
        );
    }
}
