package com.joansala.engine.base;

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

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.joansala.engine.*;


/**
 * An engine that always returns the null move.
 */
public class BaseEngine implements Engine {

    /** Maximum depth allowed for a search */
    public static final int MAX_DEPTH = 254;

    /** Search count-down timer */
    protected final Timer timer;

    /** Abort computation mutex */
    private ReentrantLock abortLock = new ReentrantLock();

    /** Consumer of best moves */
    protected Set<Consumer<Report>> consumers = new HashSet<>();

    /** The maximum depth allowed for the current search */
    protected int maxDepth = MAX_DEPTH;

    /** The maximum time allowed for the current search */
    protected long moveTime = DEFAULT_MOVETIME;

    /** The maximum possible score value */
    protected int maxScore = Integer.MAX_VALUE;

    /** Contempt factor used to evaluaty draws */
    protected int contempt = Game.DRAW_SCORE;

    /** This flag is set to true to abort a computation */
    private volatile boolean aborted = false;


    /**
     * Instantiates this engine.
     */
    protected BaseEngine() {
        timer = new Timer(true);
    }


    /**
     * Check if a computation was aborted.
     */
    protected boolean aborted() {
        return aborted;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getDepth() {
        return maxDepth;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long getMoveTime() {
        return moveTime;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getContempt() {
        return contempt;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getInfinity() {
        return maxScore;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getPonderMove(Game game) {
        return Game.NULL_MOVE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setDepth(int depth) {
        maxDepth = Math.min(depth, MAX_DEPTH);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setMoveTime(long delay) {
        moveTime = Math.max(delay, 1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setContempt(int score) {
        contempt = score;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setInfinity(int score) {
        maxScore = Math.max(score, 1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void attachConsumer(Consumer<Report> consumer) {
        consumers.add(consumer);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void detachConsumer(Consumer<Report> consumer) {
        consumers.remove(consumer);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void newMatch() {
        timer.purge();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void abortComputation() {
        try {
            abortLock.lock();
            aborted = true;

            synchronized (this) {
                aborted = false;
            }
        } finally {
            abortLock.unlock();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int computeBestScore(Game game) {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        return Game.NULL_MOVE;
    }


    /**
     * Schedules a timer task that sets the aborted flag to true
     * when the time per move of the engine is elapsed.
     */
    protected TimerTask scheduleCountDown() {
        if (abortLock.isLocked() == false) {
            aborted = false;
        }

        TimerTask countDown = new TimerTask() {
            @Override public void run() {
                aborted = true;
            }
        };

        timer.schedule(countDown, moveTime);

        return countDown;
    }
}
