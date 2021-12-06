package com.joansala.util.bench;

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

import com.joansala.engine.Board;
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
    @Override public Game cast() {
        return game;
    }


    /** {@inheritDoc} */
    @Override public void makeMove(int move) {
        stats.visits.increment();
        game.makeMove(move);
    }


    /** {@inheritDoc} */
    @Override public int score() {
        stats.heuristic.increment();
        stats.depth.aggregate(game.length());
        return game.score();
    }


    /** {@inheritDoc} */
    @Override public int outcome() {
        stats.terminal.increment();
        stats.depth.aggregate(game.length());
        return game.outcome();
    }


    /** {@inheritDoc} */
    @Override public int length() {
        return game.length();
    }


    /** {@inheritDoc} */
    @Override public int[] moves() {
        return game.moves();
    }


    /** {@inheritDoc} */
    @Override public int turn() {
        return game.turn();
    }


    /** {@inheritDoc} */
    @Override public Board toBoard() {
        return game.toBoard();
    }


    /** {@inheritDoc} */
    @Override public Board getBoard() {
        return game.getBoard();
    }


    /** {@inheritDoc} */
    @Override public void setBoard(Board board) {
        game.setBoard(board);
    }


    /** {@inheritDoc} */
    @Override public void endMatch() {
        game.endMatch();
    }


    /** {@inheritDoc} */
    @Override public boolean hasEnded() {
        return game.hasEnded();
    }


    /** {@inheritDoc} */
    @Override public int winner() {
        return game.winner();
    }


    /** {@inheritDoc} */
    @Override public int contempt() {
        return game.contempt();
    }


    /** {@inheritDoc} */
    @Override public int infinity() {
        return game.infinity();
    }


    /** {@inheritDoc} */
    @Override public long hash() {
        return game.hash();
    }


    /** {@inheritDoc} */
    @Override public boolean isLegal(int move) {
        return game.isLegal(move);
    }


    /** {@inheritDoc} */
    @Override public void unmakeMove() {
        game.unmakeMove();
    }


    /** {@inheritDoc} */
    @Override public void unmakeMoves(int length) {
        game.unmakeMoves(length);
    }


    /** {@inheritDoc} */
    @Override public int nextMove() {
        return game.nextMove();
    }


    /** {@inheritDoc} */
    @Override public int[] legalMoves() {
        return game.legalMoves();
    }


    /** {@inheritDoc} */
    @Override public int toCentiPawns(int score) {
        return game.toCentiPawns(score);
    }


    /** {@inheritDoc} */
    @Override public void ensureCapacity(int minCapacity) {
        game.ensureCapacity(minCapacity);
    }


    /** {@inheritDoc} */
    @Override public int getCursor() {
        return game.getCursor();
    }


    /** {@inheritDoc} */
    @Override public void setCursor(int cursor) {
        game.setCursor(cursor);
    }
}
