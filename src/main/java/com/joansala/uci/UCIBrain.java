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

import java.util.function.Consumer;
import com.joansala.engine.*;
import static com.joansala.engine.Game.*;


/**
 * Search thread where UCI computations are performed.
 */
public class UCIBrain extends Thread {

    /** UCI service */
    private UCIService service;

    /** Set to true while computing a move */
    private volatile boolean thinking = false;


    /**
     * Create a new brain for a given sercice.
     */
    public UCIBrain(UCIService service) {
        this.service = service;
    }


    /**
     * The main bucle for the brain.
     */
    @Override public void run() {
        Consumer<Report> consumer = createSearchConsumer();
        service.engine.attachConsumer(consumer);

        while (true) {
            synchronized (this) {
                try {
                    service.lastInfo = null;
                    thinking = false;
                    this.wait();
                    findBestMove();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        service.engine.detachConsumer(consumer);
    }


    /**
     * Returns if the brain is currently computing a move.
     *
     * @return {@code true} if there is a move computation in progress
     */
    public boolean isThinking() {
        return thinking;
    }


    /**
     * Instructs the brain to start a new move calculation
     */
    public void startThinking() {
        synchronized (this) {
            thinking = true;
            this.notify();
        }
    }


    /**
     * Instructs the brain to abort any move calculations
     */
    public void stopThinking() {
        while (isThinking()) {
            service.engine.abortComputation();
            service.synchBrain();
        }
    }


    /**
     * Performs all the necessary computations to find a best move
     * for the current game.
     */
    private void findBestMove() {
        int bestMove = NULL_MOVE;

        // If the game has ended return a null move

        if (service.game.hasEnded()) {
            service.output("bestmove 0000");
            return;
        }

        // Use the book to find a move

        if (service.ownBook && !service.infinite) {
            bestMove = getBookMove(service.game, false);
        }

        // Enable or disable the endgames database

        if (service.engine instanceof HasLeaves) {
            HasLeaves e = (HasLeaves) service.engine;
            e.setLeaves(service.useLeaves ? service.leaves : null);
        }

        // Use the engine to compute a move

        if (bestMove == NULL_MOVE) {
            bestMove = service.engine.computeBestMove(service.game);
        } else {
            service.output("info string A book move was chosen");
        }

        // Show the computed best and ponder moves

        Board board = service.game.toBoard();
        StringBuilder response = new StringBuilder();

        response.append("bestmove ");
        response.append(board.toCoordinates(bestMove));

        service.performMove(service.game, bestMove);
        int ponderMove = getPonderMove(service.game);

        if (ponderMove != NULL_MOVE) {
            response.append(" ponder ");
            response.append(service.game.toBoard().toCoordinates(ponderMove));
        }

        service.output(response.toString());
    }


    /**
     * Prints information for an engine's search.
     *
     * @param report    Search report
     * @return          Information string
     */
    private String formatReport(Report report) {
        StringBuilder response = new StringBuilder();

        int flag = report.getFlag();
        int score = report.getScore();
        int depth = report.getDepth();
        int[] variation = report.getVariation();

        response.append("info");

        if (depth > 0) {
            response.append(" depth ");
            response.append(depth);
        }

        response.append(" score cp ");
        response.append(score);

        if (flag == Flag.LOWER) {
            response.append(" lowerbound");
        }

        if (flag == Flag.UPPER) {
            response.append(" upperbound");
        }

        if (variation.length > 0) {
            Board board = service.game.toBoard();
            response.append(" pv ");
            response.append(board.toNotation(variation));
        }

        return response.toString();
    }


    /**
     * A consumer that prints search information for the current state.
     *
     * @return      New search consumer instance
     */
    private Consumer<Report> createSearchConsumer() {
        return (report) -> {
            if (report.getFlag() != Flag.EMPTY) {
                String info = formatReport(report);

                if (!info.equals(service.lastInfo)) {
                    service.lastInfo = info;
                    service.output(info);
                }
            }
        };
    }


    /**
     * Returns a move from the openings book.
     *
     * @param game  A game object
     * @return      A move or {@code Game.NULL_MOVE}
     */
    private int getBookMove(Game game, boolean ponder) {
        int move = NULL_MOVE;

        if (service.roots instanceof Roots == false) {
            return move;
        }

        try {
            move = (ponder == false) ?
                   service.roots.pickBestMove(game) :
                   service.roots.pickPonderMove(game);
        } catch (Exception e) {
            service.showError("Cannot select book move");
            service.showError(e.getMessage());
        }

        return move;
    }


    /**
     * Returns a move for pondering.
     *
     * @param game  A game object
     * @return      A move or {@code Game.NULL_MOVE}
     */
    private int getPonderMove(Game game) {
        int move = NULL_MOVE;

        if (service.ownBook == true) {
            move = getBookMove(game, true);
        }

        if (move == NULL_MOVE) {
            move = service.engine.getPonderMove(game);
        }

        return move;
    }
}
