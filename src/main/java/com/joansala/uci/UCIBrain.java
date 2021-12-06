package com.joansala.uci;

/*
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

import java.util.StringJoiner;
import java.util.function.Consumer;
import com.joansala.engine.*;
import static com.joansala.engine.Game.*;
import static com.joansala.uci.UCI.*;


/**
 * Search thread where UCI computations are performed.
 */
public class UCIBrain extends Thread {

    /** Search reports consumer */
    private Consumer<Report> consumer;

    /** UCI service */
    private UCIService service;

    /** Search engine instance */
    private Engine engine;

    /** Game instance */
    private Game game;

    /** Start state for computations */
    private Board board;

    /** State after moves are performed */
    private Board parser;

    /** Openings book instance */
    private Roots<Game> roots;

    /** Performed moves on the start state */
    private int[] moves;

    /** Last report that was sent */
    private String info = null;

    /** Stop searching only when stop is received */
    private boolean infinite = false;

    /** When the current search was initiated */
    private long startTime = 0L;

    /** Set to true while computing a move */
    private volatile boolean thinking = false;


    /**
     * Create a new brain for a given sercice.
     */
    public UCIBrain(UCIService service) {
        this.consumer = createSearchConsumer();
        this.engine = service.getEngine();
        this.game = service.getGame();
        this.service = service;
    }


    /**
     * Check if this brain is computing a move.
     *
     * @return      If a search is in progress
     */
    public boolean isThinking() {
        return thinking;
    }


    /**
     * Player that is currently to move.
     *
     * @return      Turn identifier for the player to move
     */
    public int getSearchTurn() {
        return board.turn();
    }


    /**
     * Time elapsed since the last {@code startThinking} was invoked.
     *
     * @return      Milliseconds spent searching
     */
    public long getSearchTime() {
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }


    /**
     * Sets a new state for future searches.
     *
     * @param board     Initial board state
     * @param moves     Moves performed on the board
     */
    public void setState(Board board, int[] moves) {
        synchronized (this) {
            this.board = board;
            this.moves = moves;
        }
    }


    /**
     * Instructs the brain to start searching.
     */
    public void startThinking(boolean infinite) {
        synchronized (this) {
            this.startTime = System.currentTimeMillis();
            this.infinite = infinite;
            this.thinking = true;
            this.notify();
        }
    }


    /**
     * Instructs the brain to stop searching.
     */
    public void stopThinking() {
        while (isThinking()) {
            engine.abortComputation();
            synch();
        }
    }


    /**
     * Waits for the brain to be ready for at most one second. The
     * brain is ready when it's thinking state is false.
     */
    public void synch() {
        try {
            for (int i = 0; isThinking() && i < 50; i++) {
                Thread.sleep(20);
            }
        } catch (InterruptedException e) {}
    }


    /**
     * The main bucle for the brain.
     */
    @Override public void run() {
        engine.attachConsumer(consumer);

        while (true) {
            synchronized (this) {
                try {
                    thinking = false;
                    this.wait();
                    setupGameState();
                    setupSearchEngine();
                    findBestMove();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        engine.detachConsumer(consumer);
    }


    /**
     * Setup the search state on the game.
     */
    private void setupGameState() {
        game.setBoard(board);
        game.ensureCapacity(moves.length);

        for (int move : moves) {
            game.makeMove(move);
        }

        parser = game.toBoard();
    }


    /**
     * Setup the search engine and roots.
     */
    private void setupSearchEngine() {
        roots = service.getRoots();

        if (engine instanceof HasCache) {
            HasCache e = (HasCache) engine;
            e.setCache(service.getCache());
        }

        if (engine instanceof HasLeaves) {
            HasLeaves e = (HasLeaves) engine;
            e.setLeaves(service.getLeaves());
        }
    }


    /**
     * Finds a best and ponder moves either from the engine or the
     * openings book and sends a message with the result.
     */
    private void findBestMove() {
        int move = NULL_MOVE;
        int ponder = NULL_MOVE;

        if (game.hasEnded()) {
            service.send(BESTMOVE, NULLMOVE);
            return;
        }

        // Pick a move from the book or engine

        if (infinite == false) {
            move = getBookMove(game);
        }

        if (move == NULL_MOVE) {
            move = engine.computeBestMove(game);
        } else {
            service.debug("A book move was chosen");
        }

        // Reply with a best move and a ponder move if available.
        // Notice move notations may be dependent on the game state.

        String mc = game.toBoard().toCoordinates(move);
        game.makeMove(move);

        if ((ponder = getPonderMove(game)) != NULL_MOVE) {
            String pc = game.toBoard().toCoordinates(ponder);
            service.send(BESTMOVE, mc, PONDER, pc);
        } else {
            service.send(BESTMOVE, mc);
        }
    }


    /**
     * Returns a move for pondering.
     *
     * @param game  A game object
     * @return      A move or {@code Game.NULL_MOVE}
     */
    private int getPonderMove(Game game) {
        int move = NULL_MOVE;

        if ((move = getBookPonderMove(game)) == NULL_MOVE) {
            move = engine.getPonderMove(game);
        }

        return move;
    }


    /**
     * Returns a move from the openings book.
     *
     * @param game  A game object
     * @return      A move or {@code Game.NULL_MOVE}
     */
    private int getBookMove(Game game) {
        try {
            return roots.pickBestMove(game);
        } catch (Exception e) {
            service.debug("Cannot get move from book");
            service.debug("Reason:", e.getMessage());
        }

        return NULL_MOVE;
    }


    /**
     * Returns a ponder move from the openings book.
     *
     * @param game  A game object
     * @return      A move or {@code Game.NULL_MOVE}
     */
    private int getBookPonderMove(Game game) {
        try {
            return roots.pickPonderMove(game);
        } catch (Exception e) {
            service.debug("Cannot get ponder move from book");
            service.debug("Reason:", e.getMessage());
        }

        return NULL_MOVE;
    }


    /**
     * Reports search information for the current state.
     *
     * @return      New consumer instance
     */
    private Consumer<Report> createSearchConsumer() {
        return (report) -> {
            if (report.getFlag() != Flag.EMPTY) {
                String message = getReportMessage(report);

                if (message.equals(info) == false) {
                    service.send(INFO, message);
                    info = message;
                }
            }
        };
    }


    /**
     * Formats a report as an information message.
     *
     * @param report    Search report
     * @return          Information string
     */
    private String getReportMessage(Report report) {
        StringJoiner message = new StringJoiner(" ");

        int flag = report.getFlag();
        int score = report.getScore();
        int depth = report.getDepth();
        int[] variation = report.getVariation();

        if (depth > 0) {
            message.add(DEPTH);
            message.add(String.valueOf(depth));
        }

        message.add(SCORE);
        message.add(CENTIPAWNS);
        message.add(String.valueOf(score));

        if (flag == Flag.LOWER) {
            message.add(LOWERBOUND);
        }

        if (flag == Flag.UPPER) {
            message.add(UPPERBOUND);
        }

        if (variation.length > 0) {
            message.add(VARIATION);
            message.add(parser.toNotation(variation));
        }

        return message.toString();
    }
}
