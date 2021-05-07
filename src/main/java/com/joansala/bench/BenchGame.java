package com.joansala.bench;

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

import com.joansala.engine.Game;


/**
 * A decorated game that accumulates statistics.
 */
public final class BenchGame implements Game {

    /** Decorated game instance */
    private Game game;

    /** Statistics accumulator */
    private BenchStats stats;


    /**
     * Decorates a game object.
     */
    public BenchGame(BenchStats stats, Game game) {
        this.stats = stats;
        this.game = game;
    }


    /** {@inheritDoc} */
    public Game cast() {
        return game;
    }


    /** {@inheritDoc} */
    public void makeMove(int move) {
        stats.visits.increment();
        game.makeMove(move);
    }


    /** {@inheritDoc} */
    public int score() {
        stats.heuristic.increment();
        stats.depth.aggregate(game.length());
        return game.score();
    }


    /** {@inheritDoc} */
    public int outcome() {
        stats.terminal.increment();
        stats.depth.aggregate(game.length());
        return game.outcome();
    }


    /** {@inheritDoc} */
    public int length() {
        return game.length();
    }


    /** {@inheritDoc} */
    public int[] moves() {
        return game.moves();
    }


    /** {@inheritDoc} */
    public int turn() {
        return game.turn();
    }


    /** {@inheritDoc} */
    public Object position() {
        return game.position();
    }


    /** {@inheritDoc} */
    public void setStart(Object position, int turn) {
        game.setStart(position, turn);
    }


    /** {@inheritDoc} */
    public void endMatch() {
        game.endMatch();
    }


    /** {@inheritDoc} */
    public boolean hasEnded() {
        return game.hasEnded();
    }


    /** {@inheritDoc} */
    public int winner() {
        return game.winner();
    }


    /** {@inheritDoc} */
    public int contempt() {
        return game.contempt();
    }


    /** {@inheritDoc} */
    public int infinity() {
        return game.infinity();
    }


    /** {@inheritDoc} */
    public long hash() {
        return game.hash();
    }


    /** {@inheritDoc} */
    public boolean isLegal(int move) {
        return game.isLegal(move);
    }


    /** {@inheritDoc} */
    public void unmakeMove() {
        game.unmakeMove();
    }


    /** {@inheritDoc} */
    public int nextMove() {
        return game.nextMove();
    }


    /** {@inheritDoc} */
    public int[] legalMoves() {
        return game.legalMoves();
    }


    /** {@inheritDoc} */
    public int toCentiPawns(int score) {
        return game.toCentiPawns(score);
    }


    /** {@inheritDoc} */
    public void ensureCapacity(int minCapacity) {
        game.ensureCapacity(minCapacity);
    }


    /** {@inheritDoc} */
    public int getCursor() {
        return game.getCursor();
    }


    /** {@inheritDoc} */
    public void setCursor(int cursor) {
        game.setCursor(cursor);
    }
}
