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

import java.io.Console;
import java.io.PrintWriter;
import java.util.TimerTask;
import java.util.Timer;
import com.google.inject.Inject;

import com.joansala.engine.*;


/**
 * Command-line interface to run a match between an human player
 * and an UCI engine.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class UCIMatch {

    /** Start board for the game */
    private Board start = null;

    /** Current game object */
    private Game game = null;

    /** Current engine client object */
    private UCIClient client;

    /** Current console object */
    private Console console;

    /** Current console writer */
    private PrintWriter writer;

    /** Time limit per move (milliseconds) */
    private long moveTime = Engine.DEFAULT_MOVETIME;

    /** Depth limit per move (plies) */
    private int depth = Engine.DEFAULT_DEPTH;


    /**
     * Instantiates a new match object
     *
     * @param client   System console
     * @param start    Start board for the game
     * @param game     Game object where moves will be performed
     */
    @Inject public UCIMatch(UCIClient client, Board start, Game game) {
        this.console = System.console();
        this.writer = console.writer();
        this.client = client;
        this.start = start;
        this.game = game;
    }


    /**
     * Obtain this object's UCI client.
     */
    public UCIClient getUCIClient() {
        return client;
    }


    /**
     * Sets a depth limit for subsequent computations.
     *
     * @param depth         Plies
     */
    public synchronized void setDepth(int depth) {
        this.depth = Math.max(1, depth);
    }


    /**
     * Sets a time limit for subsequent computations.
     *
     * @param moveTime      Milliseconds
     */
    public synchronized void setMoveTime(long moveTime) {
        this.moveTime = Math.max(1, moveTime);
    }


    /**
     * Prints a welcome message to the console.
     */
    private void showWelcome() {
        Package pack = UCIMatch.class.getPackage();
        String version = pack.getImplementationVersion();
        writer.format("UCI Match %s%n", version);
    }


    /**
     * Prints the current engine identification to the console.
     */
    private void showEngineID() {
        String name = client.getName();
        writer.format("You are playing against %s%n", name);
    }


    /**
     * Initializes the engine in UCI mode.
     */
    private void initEngine() throws Exception {
        Timer timer = new Timer(true);

        timer.schedule(
            new TimerTask() {
                public void run() {
                    writer.println("The engine is not responding");
                    client.getService().destroy();
                    System.exit(1);
                }
            },
            16000
        );

        client.send("uci");

        while (!client.isUCIReady())
            client.receive();

        timer.cancel();
    }


    /**
     * Starts a new game and synchronizes this ui with the engine.
     */
    private void startNewGame() throws Exception {
        client.send("ucinewgame");
        client.send("isready");

        while (!client.isReady())
            client.receive();
    }


    /**
     * Stops the engine process.
     */
    private void quitEngine() throws Exception {
        client.send("quit");
    }


    /**
     * Requests a move to the human player.
     *
     * @return  a move
     */
    private int requestUserMove() {
        int move = Game.NULL_MOVE;

        while (move == Game.NULL_MOVE) {
            String request = console.readLine("Your move? ");

            if (request == null) {
                writer.println();
                continue;
            }

            request = request.trim();

            if (request.isEmpty())
                continue;

            try {
                move = start.toMove(request);
            } catch (Exception e) {
                writer.format("%s%n", e.getMessage());
            }
        }

        return move;
    }


    /**
     * Requests a move to the engine player.
     *
     * @return  a move
     */
    private int requestEngineMove(long moveTime) {
        int move = Game.NULL_MOVE;
        int ponder = Game.NULL_MOVE;

        try {
            if (client.isPondering()) {
                client.send("stop");

                while (client.isPondering())
                    client.receive();
            }

            client.send(getPositionCommand(game));
            client.send(getGoCommand(moveTime, depth));

            while (client.isThinking())
                client.receive();

            move = client.getBestMove();
            ponder = client.getPonderMove();

            int length = game.length();

            try {
                if (move != Game.NULL_MOVE)
                    performMove(game, move);

                if (ponder != Game.NULL_MOVE)
                    performMove(game, ponder);

                client.send(getPositionCommand(game));
                client.send("go ponder");
            } catch (Exception e) { }

            while (length < game.length())
                game.unmakeMove();
        } catch (Exception e) {
            writer.format("%s%n", e.getMessage());
        }

        if (move != Game.NULL_MOVE) {
            writer.format(
                "My move is: %s%n",
                start.toAlgebraic(move)
            );
        }

        return move;
    }


    /**
     * Returns a position command for the given game object.
     *
     * @return  position command
     */
    private String getPositionCommand(Game game) {
        String command = "position startpos";
        int[] moves = game.moves();

        if (moves.length > 0) {
            command = String.format(
                "position startpos moves %s",
                start.toAlgebraic(moves)
            );
        }

        return command;
    }


    /**
     * Returns a go command for the current parameters.
     *
     * @return  go command
     */
    private String getGoCommand(long moveTime, int depth) {
        return String.format("go movetime %d depth %d", moveTime, depth);
    }


    /**
     * Performs a move on the internal board. This method asserts that
     * the move is legal and ensures the game has enough capacity to
     * store it.
     *
     * @param game  Game object where a move must be performed
     * @param move  Move to perform on the game
     * @throws IllegalArgumentException  if the move cannot be
     *      performed on the provided game object
     */
    private void performMove(Game game, int move) {
        if (game.isLegal(move)) {
            game.ensureCapacity(1 + game.length());
            game.makeMove(move);
        } else {
            throw new IllegalArgumentException(
                "The provided move is not legal");
        }
    }


    /**
     * Ask the player who moves first.
     */
    private int requestUserTurn() {
        String reply = console.readLine("%nShall I move first? ");
        boolean isYes = reply != null && reply.matches("^\\s*y.*");

        return isYes ? Game.NORTH : Game.SOUTH;
    }


    /**
     * Runs a match against an engine process.
     *
     * @param turn      Human player turn
     */
    public void start() {
        try {
            showWelcome();
            initEngine();
            showEngineID();
            startNewGame();

            final int turn = requestUserTurn();

            while (!game.hasEnded()) {
                if (!client.isRunning()) {
                    throw new IllegalStateException(
                        "The engine is not running");
                }

                writer.format("%n%s%n%n", start.toBoard(game));

                int move = (turn == game.turn()) ?
                    requestUserMove() :
                    requestEngineMove(moveTime);

                try {
                    performMove(game, move);
                } catch (Exception e) {
                    writer.format("%s%n", e.getMessage());
                }
            }

            // End the game and quit

            game.endMatch();
            writer.format("%n%s%n%n", start.toBoard(game));

            if (turn == game.winner()) {
                writer.println("You won this match!");
            } else if (Game.DRAW == game.winner()) {
                writer.println("This match was drawn.");
            } else {
                writer.println("You lost this match.");
            }

            quitEngine();
        } catch (Exception e) {
            writer.format("%s%n", e.getMessage());
        }
    }
}
