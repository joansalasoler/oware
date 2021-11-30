package com.joansala.uci.util;

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

import java.util.Map;
import java.util.HashMap;
import com.joansala.engine.Engine;


/**
 * Search time manager.
 */
public class TimeManager {

    /** Maximum moves to plan ahead */
    private static int MAX_MOVES = 20;

    /** Minimum search time in milliseconds */
    private static long MIN_MOVETIME = 500;

    /** Set aside time for each ply in milliseconds */
    private static long TIME_OVERHEAD = 50;

    /** Current clock of each player */
    private Map<Integer, Clock> clocks = new HashMap<>();

    /** Moves left till the next time control */
    private int movesLeft = Integer.MAX_VALUE;

    /** Fixed maximum search time in milliseconds */
    private long moveTime = Engine.DEFAULT_MOVETIME;

    /** If a fixed move time was provided */
    private boolean fixedTimeActive = false;


    /**
     * Instructs this manager a new match will start.
     */
    public void newMatch() {
        moveTime = Engine.DEFAULT_MOVETIME;
        clocks.clear();
    }


    /**
     * Check if the fixed time per move is active.
     */
    public boolean isFixedTimeActive() {
        return fixedTimeActive;
    }


    /**
     * Fixed search time requested by a client.
     */
    public long getMoveTime() {
        return moveTime;
    }


    /**
     * Total time remaining until the next control.
     *
     * @param turn          Player turn identifier
     * @return              Milliseconds until next control
     */
    public long getTimeLeft(int turn) {
        Clock clock = clockInstance(turn);
        return clock.timeLeft;
    }


    /**
     * Number of plies to plan in advance.
     */
    private int getPlyHorizon() {
        int horizon = Math.min(MAX_MOVES, movesLeft);
        return 2 * (horizon <= 0 ? MAX_MOVES : horizon);
    }


    /**
     * Enable or disable the fixed time per move.
     *
     * @param active        True to activate
     */
    public void setFixedTimeActive(boolean active) {
        this.fixedTimeActive = active;
    }


    /**
     * Sets a fixed move time for the next move.
     *
     * @param milliseconds  Time in milliseconds
     */
    public void setMoveTime(long milliseconds) {
        this.moveTime = Math.max(1, milliseconds);
    }


    /**
     * Sets the moves remaining until the next control.
     *
     * @param fullmoves     Fullmoves until next control
     */
    public void setMovesLeft(int fullmoves) {
        this.movesLeft = Math.max(1, fullmoves);
    }


    /**
     * Sets the total time remaining until the next control.
     *
     * @param turn          Player turn identifier
     * @param milliseconds  Time in milliseconds
     */
    public void setTimeLeft(int turn, long milliseconds) {
        Clock clock = clockInstance(turn);
        clock.timeLeft = milliseconds;
    }


    /**
     * Sets the increment per move in milliseconds.
     *
     * @param turn          Player turn identifier
     * @param milliseconds  Time increment in milliseconds
     */
    public void setTimeIncrement(int turn, long milliseconds) {
        Clock clock = clockInstance(turn);
        clock.timeIncrement = milliseconds;
    }


    /**
     * Time a player should spend thinking on the next move.
     *
     * @param turn          Player to move
     * @return              Time per move in milliseconds
     */
    public long getMoveTimeAdvice(int turn) {
        long moveTime = this.moveTime;

        if (isFixedTimeActive() == false) {
            Clock clock = clockInstance(turn);

            int horizon = getPlyHorizon();
            long fallback = horizon * moveTime;
            long bonus = horizon * clock.timeIncrement;
            long left = clock.timeLeft <= 0 ? fallback : clock.timeLeft;
            long total = left + bonus - (horizon * TIME_OVERHEAD);

            moveTime = Math.max(MIN_MOVETIME, total / horizon);
        }

        return moveTime;
    }


    /**
     * Get a clock instance for a player.
     */
    private Clock clockInstance(int turn) {
        if (clocks.get(turn) == null) {
            clocks.put(turn, new Clock());
        }

        return clocks.get(turn);
    }


    /**
     * Clock data for a player.
     */
    private class Clock {
        long timeLeft = Long.MAX_VALUE;
        long timeIncrement = 0L;
    }
}
