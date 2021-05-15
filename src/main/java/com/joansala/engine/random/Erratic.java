package com.joansala.engine.random;

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

import java.util.function.Consumer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.joansala.engine.*;


/**
 * An engine that plays randomly.
 */
public class Erratic implements Engine {

    /** Maximum depth allowed for a search */
    public static final int MAX_DEPTH = 1;

    /** Consumer of best moves */
    private Set<Consumer<Report>> consumers = new HashSet<>();

    /** Random number generator */
    private Random random = new Random();

    /** The maximum depth allowed for the current search */
    private int maxDepth = MAX_DEPTH;

    /** The maximum time allowed for the current search */
    private long moveTime = 0;

    /** The maximum possible score value */
    private int maxScore = Integer.MAX_VALUE;

    /** Contempt factor used to evaluaty draws */
    private int contempt = Game.DRAW_SCORE;


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
    public int getPonderMove(Game game) {
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
        this.contempt = score;
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
    public synchronized void newMatch() {}


    /**
     * {@inheritDoc}
     */
    @Override
    public void abortComputation() {}


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int computeBestMove(Game game) {
        if (game.hasEnded()) {
            return Game.NULL_MOVE;
        }

        int[] moves = game.legalMoves();
        int choice = random.nextInt(moves.length);

        return moves[choice];
    }
}
