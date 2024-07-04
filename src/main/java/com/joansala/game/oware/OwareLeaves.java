package com.joansala.game.oware;

/*
 * Aalina oware engine.
 * Copyright (C) 2014-2024 Joan Sala Soler <contact@joansala.com>
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

import java.io.IOException;
import com.joansala.engine.Flag;
import com.joansala.engine.Game;
import com.joansala.engine.Leaves;
import com.joansala.engine.base.BaseBook;

import static com.joansala.engine.Game.DRAW_SCORE;
import static com.joansala.engine.Game.NORTH;
import static com.joansala.engine.base.BaseGame.MAX_SCORE;
import static com.joansala.game.oware.Oware.*;


/**
 * Implements an endgame database for oware.
 *
 * <p>All the positions on the database up to the specified number of
 * seeds are kept on main memory. To obtain the score of a position a
 * search must be performed first with the method {@code find}.</p>
 */
public class OwareLeaves extends BaseBook implements Leaves<Game> {

    /** Default path to the endgames book binary file */
    public static final String LEAVES_PATH = "oware-leaves.bin";

    /** Maximum number of seeds that a position can contain */
    public static final int MAX_SEEDS = 15;

    /** Default number of seeds of the endgames book */
    public static final int DEFAULT_SEEDS = 12;

    /** Used to calculate the remaining seeds for a hash */
    private static final int HASH_OFFSET = MAX_SEEDS - SEED_COUNT;

    /** Minimum number of seeds on the stores */
    private final int minStoreSeeds;

    /** Flag of the last found entry */
    private int flag = Flag.EMPTY;

    /** Score of the last found entry */
    private int score = Game.DRAW_SCORE;

    /** Captures of the last found entry */
    private int captures = 0;

    /** Placeholder for mirrored game states */
    private int[] mirror = new int[POSITION_SIZE];

    /** Entries of the database */
    private final byte[] data;


    /**
     * Create a new endgames book instance.
     */
    public OwareLeaves() throws IOException {
        this(LEAVES_PATH, DEFAULT_SEEDS);
    }


    /**
     * Create a new endgames book instance.
     *
     * @param path      book file path
     */
    public OwareLeaves(String path) throws IOException {
        this(path, DEFAULT_SEEDS);
    }


    /**
     * Instantiates a new endgames book object.
     *
     * @param path      book file path
     * @param seeds     maximum number of seeds
     */
    public OwareLeaves(String path, int seeds) throws IOException {
        super(path);

        if (seeds < 1 || seeds > MAX_SEEDS) {
            throw new IllegalArgumentException(
                "Incorrect number of seeds");
        }

        data = new byte[OFFSETS[seeds]];
        minStoreSeeds = SEED_COUNT - seeds;
        file.readFully(data);
        file.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getFlag() {
        return flag;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getScore() {
        return score;
    }


    /**
     * Expected captures.
     */
    public int getCaptures() {
        return captures;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean find(Game game) {
        return find((OwareGame) game);
    }


    /**
     * Searches the given game state on this database.
     *
     * If the game state is found updates the values of this class,
     * otherwise, no update is performed. For a game state to be found
     * the following conditions must be met:
     *
     * <ul>
     *  <li>Exist on the database.</li>
     *  <li>Contain a maximum of {@code MAX_SEEDS}.</li>
     *  <li>Its flag must be {@code Flag.EXACT} or, if the best outcome happens
     *      as a result of a position repetition, its flag be {@code Flag.LOWER}
     *      and the last performed move a capture.</li>
     * </ul>
     *
     * @param game      A game object
     * @return          If a score can be computed
     */
    public boolean find(OwareGame game) {
        int[] state = game.state();

        if (contains(state) == false) {
            return false;
        }

        if (game.turn() == NORTH) {
            state = rotatePosition(state);
        }

        final int hash = computeHash(state);
        final int entry = data[hash];
        final int flag = (entry & 0x03);

        if (flag != Flag.EXACT && !game.wasCapture()) {
            return false;
        }

        this.flag = flag;
        this.captures = (entry >> 2);

        final int south = state[SOUTH_STORE];
        final int seeds = south + captures;

        if (seeds > SEED_GOAL) {
            this.flag = Flag.EXACT;
            this.score = +MAX_SCORE * game.turn();
            return true;
        }

        if (seeds < SEED_GOAL) {
            this.flag = Flag.EXACT;
            this.score = -MAX_SCORE * game.turn();
            return true;
        }

        if (flag == Flag.EXACT) {
            this.score = DRAW_SCORE;
            return true;
        }

        return false;
    }


    /**
     * Check if this collection contains an entry for the given game
     * position. It does not check if the entry is valid.
     *
     * @param state         Game position
     */
    private boolean contains(int[] state) {
        final int south = state[SOUTH_STORE];
        final int north = state[NORTH_STORE];

        return (south + north >= minStoreSeeds);
    }


    /**
     * Computes a unique hash code for a game state.
     *
     * @param state         Game position
     */
    private int computeHash(int[] state) {
        int hash = 0x00;
        int n = HASH_OFFSET;

        n += state[SOUTH_STORE];
        n += state[NORTH_STORE];

        for (int i = NORTH_RIGHT; n < MAX_SEEDS && i >= 0; i--) {
            hash += COEFFICIENTS[n][i];
            n += state[i];
        }

        return hash;
    }


    /**
     * Computes a unique hash code for a game state.
     *
     * @param game          Game state
     */
    public int computeHash(Game game) {
        OwareGame g = (OwareGame) game;
        int[] state = g.state();

        if (game.turn() == NORTH) {
            state = rotatePosition(state);
        }

        return computeHash(state);
    }


    /**
     * Rotates the given position and returns the new representation.
     *
     * @param state         Game position
     * @return              A mirrored state array
     */
    private int[] rotatePosition(int[] state) {
        System.arraycopy(state, NORTH_LEFT, mirror, SOUTH_LEFT, NORTH_LEFT);
        System.arraycopy(state, SOUTH_LEFT, mirror, NORTH_LEFT, NORTH_LEFT);
        mirror[SOUTH_STORE] = state[NORTH_STORE];
        mirror[NORTH_STORE] = state[SOUTH_STORE];

        return mirror;
    }


    /** Number of positions with N or less seeds */
    private static final int[] OFFSETS = {
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
