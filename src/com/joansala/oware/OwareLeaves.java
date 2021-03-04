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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.joansala.engine.Book;
import com.joansala.engine.Game;
import com.joansala.engine.Leaves;
import static com.joansala.oware.Oware.*;


/**
 * Implements an endgame database for oware.
 *
 * <p>All the positions on the database up to the specified number of
 * seeds are kept on main memory. To obtain the score of a position a
 * search must be performed first with the method {@code find}.</p>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class OwareLeaves extends Book implements Leaves {

    /** Header signature for the book format */
    public static final String SIGNATURE = "Oware Endgames ";

    /** Maximum number of seeds that a position can contain */
    public static final int MAX_SEEDS = 15;

    /** Used to calculate the remaining seeds for a hash */
    private static final int OFFSET = MAX_SEEDS - SEED_COUNT;

    /** Exact score flag */
    private static final int EXACT_SCORE = 3;

    /** Exact cycle score flag */
    private static final int CYCLE_SCORE = 1;

    /** Minimum number of captured seeds */
    private final int minCaptures;

    /** Keeps in memory all the database leaves */
    private final byte[] data;

    /** Last found position score */
    private int score = Game.DRAW_SCORE;


    /**
     * Instantiates a new endgames book object.
     *
     * @param file      book file
     * @param seeds     maximum number of seeds
     */
    public OwareLeaves(File file, int seeds) throws IOException {
        super(file, SIGNATURE);

        if (seeds < 1 || seeds > MAX_SEEDS) {
            throw new IllegalArgumentException(
                "Incorrect number of seeds");
        }

        data = new byte[1 + LENGTHS[seeds]];
        minCaptures = SEED_COUNT - seeds;

        RandomAccessFile database = getDatabase();
        database.readFully(data);
        database.close();
    }


    /**
     * Returns the exact score value for the last position found
     * from south's perspective.
     *
     * @return  the stored score value or zero
     */
    public int getScore() {
        return this.score;
    }


    /**
     * Search a position provided by a {@code OwareGame} object and sets
     * it as the current position on the endgames book.
     *
     * @param game  A game object
     * @return      {@code true} if an exact score for the position
     *              could be found; {@code false} otherwise
     */
    public boolean find(Game game) {
        final OwareGame oware = (OwareGame) game;
        final int captures = oware.southStore()
                           + oware.northStore();

        // Check if this database may contain the position

        if (captures < minCaptures) {
            return false;
        }

        // Check the game state from the player perspective

        return (game.turn() == Game.SOUTH) ?
            findSouth(oware, captures) :
            findNorth(oware, captures);
    }


    /**
     * Search a position from the south player perspective.
     *
     * @see OwareLeaves#find(Game)
     * @param game      An oware game object
     * @param captures  Number of captured seeds
     * @return          {@code true} if an exact score for the position
     *                  could be found; {@code false} otherwise
     */
    private boolean findSouth(OwareGame game, int captures) {
        // Obtain precomputed data for the position

        final int index = southIndex(game, captures);
        final int cdata = data[index];
        final int flag = (cdata & 0x03);

        // Return false if the score cannot be known

        if (flag == CYCLE_SCORE && !game.wasCapture())
            return false;

        // Compute the total number of captured seeds

        final int seeds = (cdata >> 2) + game.southStore();

        // Return the final score of the position

        if (seeds == 24) {
            score = Game.DRAW_SCORE;
        } else if (seeds > 24) {
            score = (flag == CYCLE_SCORE) ?
                +(captures << 4) :
                +OwareGame.MAX_SCORE;
        } else {
            score = (flag == CYCLE_SCORE) ?
                -(captures << 4) :
                -OwareGame.MAX_SCORE;
        }

        return true;
    }


    /**
     * Search a position from the south player perspective.
     *
     * @see OwareLeaves#find(Game)
     * @param game      An oware game object
     * @param captures  Number of captured seeds
     * @return          {@code true} if an exact score for the position
     *                  could be found; {@code false} otherwise
     */
    private boolean findNorth(OwareGame game, int captures) {
        // Obtain precomputed data for the position

        final int index = northIndex(game, captures);
        final int cdata = data[index];
        final int flag = (cdata & 0x03);

        // Return false if the score cannot be known

        if (flag == CYCLE_SCORE && !game.wasCapture()) {
            return false;
        }

        // Compute the total number of captured seeds

        final int seeds = (cdata >> 2) + game.northStore();

        // Return the final score of the position

        if (seeds == 24) {
            score = Game.DRAW_SCORE;
        } else if (seeds > 24) {
            score = (flag == CYCLE_SCORE) ?
                -(captures << 4) :
                -OwareGame.MAX_SCORE;
        } else {
            score = (flag == CYCLE_SCORE) ?
                +(captures << 4) :
                +OwareGame.MAX_SCORE;
        }

        return true;
    }


    /**
     * Computes the hash code for the current game position from
     * south's player perspective.
     *
     * @param game      Game object
     * @param captures  Number of captured seeds
     * @return          Unique hash code
     */
    private int southIndex(OwareGame game, int captures) {
        final int[] state = game.state();

        int n = OFFSET + captures;
        int rank = 0;

        for (int i = 11; n < MAX_SEEDS && i >= 0; i--) {
            rank += COEFFICIENTS[n][i];
            n += state[i];
        }

        return rank;
    }


    /**
     * Computes the hash code for the current game position from
     * north's player perspective.
     *
     * @param game      Game object
     * @param captures  Number of captured seeds
     * @return          Unique hash code
     */
    private int northIndex(OwareGame game, int captures) {
        final int[] state = game.state();

        int n = OFFSET + captures;
        int rank = 0;

        for (int i = 5; n < MAX_SEEDS && i >= 0; i--) {
            rank += COEFFICIENTS[n][i + 6];
            n += state[i];
        }

        for (int i = 11; n < MAX_SEEDS && i >= 6; i--) {
            rank += COEFFICIENTS[n][i - 6];
            n += state[i];
        }

        return rank;
    }


    /** Number of positions with N or less seeds */
    private static final int[] LENGTHS = {
                1,        13,       91,      455,
             1820,      6188,    18564,    50388,
           125970,    293930,   646646,   1352078,
          2704156,   5200300,  9657700,  17383860
    };

    /** Binomial coefficients used to compute hash codes */
    private static final int[][] COEFFICIENTS = {
        { 0X0000000F, 0X00000078, 0X000002A8, 0X00000BF4,
          0X00002D6C, 0X00009768, 0X0001C638, 0X0004E11A,
          0X000C7826, 0X001DED28, 0X004403B8, 0X00935D64 },
        { 0X0000000E, 0X00000069, 0X00000230, 0X0000094C,
          0X00002178, 0X000069FC, 0X00012ED0, 0X00031AE2,
          0X0007970C, 0X00117502, 0X00261690, 0X004F59AC },
        { 0X0000000D, 0X0000005B, 0X000001C7, 0X0000071C,
          0X0000182C, 0X00004884, 0X0000C4D4, 0X0001EC12,
          0X00047C2A, 0X0009DDF6, 0X0014A18E, 0X0029431C },
        { 0X0000000C, 0X0000004E, 0X0000016C, 0X00000555,
          0X00001110, 0X00003058, 0X00007C50, 0X0001273E,
          0X00029018, 0X000561CC, 0X000AC398, 0X0014A18E },
        { 0X0000000B, 0X00000042, 0X0000011E, 0X000003E9,
          0X00000BBB, 0X00001F48, 0X00004BF8, 0X0000AAEE,
          0X000168DA, 0X0002D1B4, 0X000561CC, 0X0009DDF6 },
        { 0X0000000A, 0X00000037, 0X000000DC, 0X000002CB,
          0X000007D2, 0X0000138D, 0X00002CB0, 0X00005EF6,
          0X0000BDEC, 0X000168DA, 0X00029018, 0X00047C2A },
        { 0X00000009, 0X0000002D, 0X000000A5, 0X000001EF,
          0X00000507, 0X00000BBB, 0X00001923, 0X00003246,
          0X00005EF6, 0X0000AAEE, 0X0001273E, 0X0001EC12 },
        { 0X00000008, 0X00000024, 0X00000078, 0X0000014A,
          0X00000318, 0X000006B4, 0X00000D68, 0X00001923,
          0X00002CB0, 0X00004BF8, 0X00007C50, 0X0000C4D4 },
        { 0X00000007, 0X0000001C, 0X00000054, 0X000000D2,
          0X000001CE, 0X0000039C, 0X000006B4, 0X00000BBB,
          0X0000138D, 0X00001F48, 0X00003058, 0X00004884 },
        { 0X00000006, 0X00000015, 0X00000038, 0X0000007E,
          0X000000FC, 0X000001CE, 0X00000318, 0X00000507,
          0X000007D2, 0X00000BBB, 0X00001110, 0X0000182C },
        { 0X00000005, 0X0000000F, 0X00000023, 0X00000046,
          0X0000007E, 0X000000D2, 0X0000014A, 0X000001EF,
          0X000002CB, 0X000003E9, 0X00000555, 0X0000071C },
        { 0X00000004, 0X0000000A, 0X00000014, 0X00000023,
          0X00000038, 0X00000054, 0X00000078, 0X000000A5,
          0X000000DC, 0X0000011E, 0X0000016C, 0X000001C7 },
        { 0X00000003, 0X00000006, 0X0000000A, 0X0000000F,
          0X00000015, 0X0000001C, 0X00000024, 0X0000002D,
          0X00000037, 0X00000042, 0X0000004E, 0X0000005B },
        { 0X00000002, 0X00000003, 0X00000004, 0X00000005,
          0X00000006, 0X00000007, 0X00000008, 0X00000009,
          0X0000000A, 0X0000000B, 0X0000000C, 0X0000000D },
        { 0X00000001, 0X00000001, 0X00000001, 0X00000001,
          0X00000001, 0X00000001, 0X00000001, 0X00000001,
          0X00000001, 0X00000001, 0X00000001, 0X00000001 }
    };
}
