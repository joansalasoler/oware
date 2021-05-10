package com.joansala.uci;

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

import com.google.inject.Inject;
import com.joansala.engine.*;


/**
 * A simple UCI player.
 */
public class UCIPlayer {

    /** UCI client instance */
    private UCIClient client;

    /** Game board */
    private Board parser;

    /** Depth limit per move (plies) */
    private int depth = Engine.DEFAULT_DEPTH;

    /** Time limit per move (ms) */
    private long moveTime = Engine.DEFAULT_MOVETIME;


    /**
     * Create a new UCI player.
     */
    @Inject public UCIPlayer(UCIClient client) {
        this.parser = client.getBoard();
        this.client = client;
    }


    /**
     * Checks if the player process is alive.
     */
    public boolean isRunning() {
        return client.isRunning();
    }


    /**
     * UCI client of this player.
     */
    public UCIClient getClient() {
        return client;
    }


    /**
     * Sets the engine service.
     */
    public void setService(Process service) {
        client.setService(service);
    }


    /**
     * Depth limit for this player in plies.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }


    /**
     * Time limit for this player in milliseconds.
     */
    public void setMoveTime(long moveTime) {
        this.moveTime = moveTime;
    }


    /**
     * Asks the engine process to quit.
     */
    public void quitEngine() throws Exception {
        client.send("quit");
    }


    /**
     * Asks the engine process to start its UCI mode.
     */
    public void startEngine() throws Exception {
        client.send("uci");

        while (!client.isUCIReady()) {
            client.receive();
        }
    }


    /**
     * Asks the engine process to get ready for a new game.
     */
    public void startNewGame() throws Exception {
        client.send("ucinewgame");
        client.send("isready");

        while (!client.isReady()) {
            client.receive();
        }
    }


    /**
     * Asks the engine process to think for a move.
     *
     * @param game      Game to ponder
     * @return          Best move found
     */
    public int startThinking(Game game) throws Exception {
        client.send(toUCIPosition(game));
        client.send(toUCIGo(moveTime, depth));

        while (client.isThinking()) {
            client.receive();
        }

        return client.getBestMove();
    }


    /**
     * Asks the engine process to start pondering a move.
     *
     * @param game      Game to ponder
     */
    public void startPondering(Game game) throws Exception {
        String position = toUCIPosition(game);
        int ponder = client.getPonderMove();

        if (ponder != Game.NULL_MOVE) {
            if (game.isLegal(ponder)) {
                int cursor = game.getCursor();
                game.ensureCapacity(1 + game.length());
                game.makeMove(ponder);
                position = toUCIPosition(game);
                game.unmakeMove();
                game.setCursor(cursor);
            }
        }

        client.send(position);
        client.send("go ponder");
    }


    /**
     * Asks the engine process to stop pondering a move.
     */
    public void stopPondering() throws Exception {
        if (client.isPondering()) {
            client.send("stop");

            while (client.isPondering()) {
                client.receive();
            }
        }
    }


    /**
     * Format a UCI position command for the given game state.
     *
     * @param game      Game state
     * @return          UCI command string
     */
    private String toUCIPosition(Game game) {
        String command = "position startpos";
        String moves = parser.toAlgebraic(game.moves());
        String params = moves.isEmpty() ? moves : " moves " + moves;
        return String.format("%s%s", command, params);
    }


    /**
     * Format a UCI go command for the given parameters.
     *
     * @param moveTime  Time limit
     * @param depth     Depth limit
     * @return          UCI command string
     */
    private String toUCIGo(long moveTime, int depth) {
        String command = "go movetime %d depth %d";
        return String.format(command, moveTime, depth);
    }
}
