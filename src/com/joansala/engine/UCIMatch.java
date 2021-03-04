package com.joansala.engine;

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

    /** Human playing turn */
    private int turn = Game.SOUTH;

    /** Human playing turn */
    private int movetime = 2000;


    /**
     * Instantiates a new match object
     *
     * @param client   System console
     * @param start    Start board for the game
     * @param game     Game object where moves will be performed
     */
    public UCIMatch(UCIClient client, Board start, Game game) {
        this.console = System.console();
        this.writer = console.writer();
        this.client = client;
        this.start = start;
        this.game = game;
    }


    /**
     * Prints a welcome message to the console.
     */
    private void showWelcome() {
        Package pack = UCIMatch.class.getPackage();
        String version = pack.getImplementationVersion();

        writer.format(
            "UCI Command-line Interface, version %s%n",
            version
        );
    }


    /**
     * Prints the current engine identification to the console.
     */
    private void showEngineID() {
        String name = client.getName();
        String author = client.getAuthor();

        writer.format(
            "%nYou will be playing with %s;%n" +
            "a game engine authored by %s.%n",
            name, author
        );
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
    private int requestEngineMove() {
        int move = Game.NULL_MOVE;
        int ponder = Game.NULL_MOVE;

        try {
            if (client.isPondering()) {
                client.send("stop");

                while (client.isPondering())
                    client.receive();
            }

            client.send(getPositionCommand(game));
            client.send(getGoCommand(movetime));

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
    private String getGoCommand(int movetime) {
        return String.format("go movetime %d", movetime);
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
     * Runs a match against an engine process.
     *
     * @param turn      Human player turn
     * @param movetime  Engine's time per move
     */
    public void start(int turn, int movetime) {
        try {
            this.turn = turn;
            this.movetime = movetime;

            showWelcome();
            initEngine();
            showEngineID();
            startNewGame();

            while (!game.hasEnded()) {
                if (!client.isRunning()) {
                    throw new IllegalStateException(
                        "The engine is not running");
                }

                writer.format("%n%s%n%n", start.toBoard(game));

                int move = (turn == game.turn()) ?
                    requestUserMove() :
                    requestEngineMove();

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
