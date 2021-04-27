package com.joansala.oware;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joansala.engine.Board;
import com.joansala.engine.Game;

import static com.joansala.oware.OwareGame.*;
import static com.joansala.oware.Oware.*;


/**
 * Represents a valid mancala board for an oware game. A valid position
 * contains a maximum of 48 seeds, distributed in 12 pits plus the
 * captured seeds by each of the two players.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class OwareBoard implements Board {

    /** Position notation format pattern */
    private static Pattern boardPattern = Pattern.compile(
        "((?:[1-4]?[0-9]-){14})(S|N)");

    /** Moves notation format pattern */
    private static Pattern movesPattern = Pattern.compile(
        "([A-F]([a-f][A-F])*[a-f]?)|([a-f]([A-F][a-f])*[A-F]?)");

    /** Stores seeds distribution on the board and captured seeds */
    private int[] position;

    /** Indicates which player must move on the current position */
    private int turn;


    /**
     * Instantiates a new {@code OwareBoard} object with the default
     * position and turn for an oware game.
     */
    public OwareBoard() {
        this.turn = SOUTH;
        this.position = startPosition();
    }


    /**
     * Instantiates a new {@code OwareBoard} object containing the specified
     * position and turn identified by a hash code. The unique hash
     * identifier for a board object can be obtained with the method
     * {@code uniqueHash()}.
     *
     * @throws IllegalArgumentException  if the hash code does not
     *      represent a valid position and turn
     */
    public OwareBoard(long hash) {
        this.turn = (hash & SOUTH_SIGN) != 0 ? SOUTH : NORTH;
        this.position = unrankPosition(hash & 0x7FFFFFFFFFFL);
    }


    /**
     * Instantiates a new {@code OwareBoard} object containing the specified
     * position and turn.
     *
     * <p>Turn must be either {@code Game.SOUTH} or {@code Game.NORTH}.
     * A position is represented by an int array where the array indices
     * 0 to 5 represent the number of seeds on each of the south player
     * pits, indices 6 to 11 the number of seeds on north player's pits
     * and indices 12 and 13 are the number of seed captured by each
     * player.</p>
     *
     * @param position  The position of the board
     * @param turn      The player that is to move
     *
     * @throws IllegalArgumentException  if the {@code postion} or
     *      {@code turn} parameters are not valid
     */
    public OwareBoard(int[] position, int turn) {
        if (isValidTurn(turn) == false) {
            throw new IllegalArgumentException(
                "Game turn is not a valid");
        }

        if (isValidPosition(position) == false) {
            throw new IllegalArgumentException(
                "Position representation is not valid");
        }

        this.turn = turn;
        this.position = Arrays.copyOf(position, position.length);
    }


    /**
     * Returns a copy of the position array stored on the current object
     *
     * @return   The position array
     */
    public int[] position() {
        return Arrays.copyOf(position, position.length);
    }


    /**
     * Returns which player is to move for the board position
     *
     * @return   The player to move
     */
    public int turn() {
        return this.turn;
    }


    /**
     * Returns true if the turn parameter is a valid player identifier.
     * A valid identifier must be either {@code Game.SOUTH} or
     * {@code Game.NORTH}.
     *
     * @param turn  A player identifier
     * @return      {@code true} if turn is valid
     */
    private static boolean isValidTurn(int turn) {
        return turn == SOUTH || turn == NORTH;
    }


    /**
     * Returns true if the array is a valid representation of a board
     * position. A valid position contains exactly fourty eight seeds
     * distributed in fourteen houses.
     *
     * @param position  An array representation of a position
     * @return          {@code true} if position is valid
     */
    private static boolean isValidPosition(int[] position) {
        if (position == null) {
            return false;
        }

        if (position.length != 2 + BOARD_SIZE) {
            return false;
        }

        int seeds = 0;

        for (int i = 0; i < 2 + BOARD_SIZE; i++) {
            seeds += position[i];

            if (position[i] < 0) {
                return false;
            }
        }

        return (seeds == SEED_COUNT);
    }


    /**
     * Returns an array representation of the default start position
     * for an oware game.
     *
     * @return  Array representation of the position
     */
    private static int[] startPosition() {
        int[] position = new int[2 + BOARD_SIZE];
        System.arraycopy(START_POSITION, 0, position, 0, 2 + BOARD_SIZE);

        return position;
    }


    /**
     * Given a valid position representation array returns an unique
     * 43 bits identifier for the position.
     *
     * @param position  An array representation of a position
     * @throws IllegalArgumentException  if postion is not valid
     */
    public static long rankPosition(int[] position) {
        // Check for a valid position representation

        if (isValidPosition(position) == false)
            throw new IllegalArgumentException(
                "Position representation is not valid");

        // Rank the position array

        long rank = 0x00L;
        int n = position[NORTH_STORE];

        for (int i = SOUTH_STORE; n < SEED_COUNT && i >= 0; i--) {
            rank += COEFFICIENTS[n][i];
            n += position[i];
        }

        return rank;
    }


    /**
     * Returns a position array parsed from the unique binomial rank
     * number of the position. The rank number can be obtained with the
     * method {@code rankPosition}.
     *
     * @return  A valid position representation array
     */
    public static int[] unrankPosition(long rank) {
        int[] position = new int[2 + BOARD_SIZE];

        int n = 0;
        int seeds = 0;
        int i = BOARD_SIZE;

        while (i >= 0 && n < SEED_COUNT) {
            long value = COEFFICIENTS[n][i];

            if (rank >= value) {
                rank -= value;
                position[i + 1] = seeds;
                seeds = 0;
                i--;
            } else {
                seeds++;
                n++;
            }
        }

        position[i + 1] = SEED_COUNT - n + seeds;

        return position;
    }


    /**
     * Converts the current state of an oware game object to a board
     * representation.
     *
     * @param game  A game object
     * @throws IllegalArgumentException If the game is not valid
     */
    public OwareBoard toBoard(Game game) {
        if (!(game instanceof OwareGame)) {
            throw new IllegalArgumentException(
                "Not a valid game object");
        }

        return new OwareBoard((int[]) game.position(), game.turn());
    }


    /**
     * Converts a board notation to a board object.
     *
     * <p>A board notation is composed of 14 decimal numbers in the range
     * [0-48] and a letter in the range (S|N) separated by minus signs. The
     * first twelve bytes represent south and north houses from the left
     * most house on south to the right most house on north. The next two
     * bytes represent south and north stores respectively and the letter
     * represents the player to move on the position.</p>
     *
     * @param notation  A valid board notation
     * @throws IllegalArgumentException If the board notation is not valid
     */
    public OwareBoard toBoard(String notation) {
        Matcher matcher = boardPattern.matcher(notation);

        if (matcher.matches() == false) {
            throw new IllegalArgumentException(
                "Position notation is not valid");
        }

        int turn = SOUTH;
        int[] position = new int[2 + BOARD_SIZE];

        if ("N".equals(matcher.group(2))) {
            turn = NORTH;
        }

        String[] houses = matcher.group(1).split("-");

        for (int i = 0; i < 2 + BOARD_SIZE; i++) {
            position[i] = Integer.parseInt(houses[i]);
        }

        return new OwareBoard(position, turn);
    }


    /**
     * Converts this board object to its equivalent notation.
     *
     * @return      String representation of this board
     */
    public String toNotation() {
        StringBuilder sb = new StringBuilder();

        for (int house : position) {
            sb.append(house);
            sb.append('-');
        }

        sb.append((turn == SOUTH) ? 'S' : 'N');

        return sb.toString();
    }


    /**
     * Converts an integer representation of one or more moves to their
     * algebraic representation.
     *
     * @param moves Moves array
     * @return      Moves notation
     * @throws IllegalArgumentException if a move is not valid
     */
    public String toAlgebraic(int[] moves) {
        StringBuilder sb = new StringBuilder();

        for (int move : moves) {
            sb.append(toCharacter(move));
        }

        return sb.toString();
    }


    /**
     * Converts an integer representation of a move to its algebraic
     * representation.
     *
     * @param move  A move representation
     * @return      A move notation
     * @throws IllegalArgumentException if the move is not valid
     */
    public String toAlgebraic(int move) {
        return String.valueOf(toCharacter(move));
    }


    /**
     * Converts an integer representation of a move to its algebraic
     * representation.
     *
     * @param move  A move representation
     * @return      A move notation
     * @throws IllegalArgumentException if the move is not valid
     */
    private static char toCharacter(int move) {
        if (move < 0 || move > BOARD_SIZE - 1) {
            throw new IllegalArgumentException(
                "Not a valid move representation");
        }

        return (char) (move < NORTH_LEFT ?
            move + 'A' : move + 'a' - NORTH_LEFT);
    }


    /**
     * Converts an alphabetic move notation to its integer array moves
     * representation. A move notation is a sequence of chars in the
     * range [A-F] for south and [a-f] for north where each char
     * represents the house from where the move is performed.
     *
     * @param notation  Move notation
     * @return          Array representation of the moves
     * @throws IllegalArgumentException If the notation does not
     *                  represent a valid move sequence
     */
    public int[] toMoves(String notation) {
        Matcher matcher = movesPattern.matcher(notation);

        if (matcher.matches() == false) {
            throw new IllegalArgumentException(
                "Moves notation is not valid");
        }

        int[] moves = new int[notation.length()];

        for (int i = 0; i < notation.length(); i++) {
            char move = notation.charAt(i);
            moves[i] = toMove(move);
        }

        return moves;
    }


    /**
     * Converts a move form its algebraic notation to a numeric
     * representation.
     *
     * @param notation  Algebraic notation of the move
     * @return          Numeric representation of the move
     * @throws IllegalArgumentException  If the notation is not valid
     */
    public int toMove(String notation) {
        if (notation.length() != 1) {
            throw new IllegalArgumentException(
                "Not a valid move notation");
        }

        return toMove(notation.charAt(0));
    }


    /**
     * Converts a move from its alphabetic notation to a numeric
     * representation.
     *
     * @param notation  Algebraic notation of the move
     * @return          Numeric representation of the move
     * @throws IllegalArgumentException  If the notation is not valid
     */
    private static int toMove(char notation) {
        int move = NULL_MOVE;

        if (notation >= 'a' && notation <= 'f') {
            move = notation - 'a' + NORTH_LEFT;
        } else if (notation >= 'A' && notation <= 'F') {
            move = notation - 'A';
        } else {
            throw new IllegalArgumentException(
                "Not a valid move notation");
        }

        return move;
    }


    /**
     * Returns an unique hash identifier for the current board. The
     * produced hash code is a 44 bit number which uniquely identifies
     * a position and turn.
     *
     * @return  The hash code value for this object
     */
    public long hash() {
        long hash = (turn == SOUTH) ? SOUTH_SIGN : NORTH_SIGN;
        int n = position[NORTH_STORE];

        for (int i = SOUTH_STORE; n < SEED_COUNT && i >= 0; i--) {
            hash += COEFFICIENTS[n][i];
            n += position[i];
        }

        return hash;
    }


    /**
     * Returns a hash code for this board. The hash produced is not
     * unique.
     *
     * @return   a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(position) * turn;
    }


    /**
     * Compares this {@code OwareBoard} to the specified object. The result
     * is {@code true} if the argument is not {@code null} and is a
     * {@code OwareBoard} object that represents the same position and turn
     * as this object.
     *
     * @param obj  The object to compare with this <code>OwareBoard</code>
     * @return     {@code null} if the object represents the same board
     *             as this {@code OwareBoard} object. {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof OwareBoard)) {
            return false;
        }

        final OwareBoard board = (OwareBoard) obj;

        if (board.turn != this.turn) {
            return false;
        }

        return Arrays.equals(board.position, this.position);
    }


    /**
     * Returns an human readable string representation of this object.
     *
     * @return   A formatted string
     */
    @Override
    public String toString() {
        return String.format(
            "+----+----+----+----+----+----+----+----+%n" +
            "|    | %2d | %2d | %2d | %2d | %2d | %2d |    |%n" +
            "| %2d +----+----+----+----+----+----+ %2d +%n" +
            "|    | %2d | %2d | %2d | %2d | %2d | %2d |    |%n" +
            "+----+----+----+----+----+----+----+----+%n" +
            "%s to move",
            position[11], position[10], position[9], position[8],
            position[7], position[6], position[13], position[12],
            position[0], position[1], position[2], position[3],
            position[4], position[5],
            turn == SOUTH ? "South" : "North"
        );
    }

}
