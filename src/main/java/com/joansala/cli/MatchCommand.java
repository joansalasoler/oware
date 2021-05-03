package com.joansala.cli;

/*
 * Aalina oware engine.
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import org.jline.reader.*;
import org.jline.terminal.*;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.uci.UCIClient;

/**
 * Executes the user interface to play against an engine.
 */
@Command(
  name = "match",
  version = "1.2.1",
  description = "Play a match against the engine",
  mixinStandardHelpOptions = true
)
public final class MatchCommand implements Callable<Integer> {

    /** UCI client instance */
    private UCIClient client;

    /** Game being played */
    private Game game = null;

    /** Initial board of the game */
    private Board board = null;

    /** Turn of the human player */
    private int turn = Game.SOUTH;

    @Option(
      names = "--depth",
      description = "Depth limit per move (plies)"
    )
    private int depth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--movetime",
      description = "Time limit per move (ms)"
    )
    private long moveTime = Engine.DEFAULT_MOVETIME;

    @Option(
      names = "--command",
      description = "Custom UCI engine command",
      converter = ProcessConverter.class,
      defaultValue = "<default>"
    )
    private Process service = null;


    /**
     * Creates a new service.
     */
    @Inject public MatchCommand(UCIClient client, Game game) {
        this.board = client.getBoard();
        this.client = client;
        this.game = game;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        client.setService(service);
        runMatch();
        return 0;
    }


    /**
     * Runs a match against an engine process.
     */
    public void runMatch() throws Exception {
        LineReader reader = newLineReader();
        PrintWriter writer = reader.getTerminal().writer();

        try {
            startEngine();
            startNewGame();
            printWelcome(writer);
            printBoard(writer);

            turn = askForTurn(reader);

            while (client.isRunning() && !game.hasEnded()) {
                boolean isUserTurn = (turn == game.turn());

                try {
                    if (isUserTurn == true) {
                        int move = askForMove(reader);
                        makeMove(game, move);
                        printBoard(writer);
                    } else {
                        stopPondering();
                        int move = askForMove(writer);
                        makeMove(game, move);
                        startPondering(game);
                        printMove(writer, move);
                        printBoard(writer);
                    }
                } catch (UserInterruptException e) {
                    throw e;
                } catch (Exception e) {
                    writer.println(e.getMessage());
                    if (!isUserTurn) throw e;
                }
            }

            if (game.hasEnded()) {
                game.endMatch();
                printBoard(writer);
                printWinner(writer);
            }
        } catch (UserInterruptException e) {
            writer.println("Keyboard interrupt.");
        } catch (Exception e) {
            writer.println("Unhandled exception.");
        } finally {
            if (client.isRunning()) quitEngine();
            writer.println("Bye.");
            writer.flush();
        }
    }


    /**
     * Adds a move to the given game.
     *
     * @param game          Game state
     * @param move          Move to perform
     */
    private void makeMove(Game game, int move) {
        if (game.isLegal(move) == false) {
            throw new IllegalArgumentException(
                "Sorry, that's not a legal move.");
        }

        game.ensureCapacity(1 + game.length());
        game.makeMove(move);
    }


    /**
     * Asks the engine process to quit.
     */
    private void quitEngine() throws Exception {
        client.send("quit");
    }


    /**
     * Asks the engine process to start its UCI mode.
     */
    private void startEngine() throws Exception {
        client.send("uci");

        while (!client.isUCIReady()) {
            client.receive();
        }
    }


    /**
     * Asks the engine process to get ready for a new game.
     */
    private void startNewGame() throws Exception {
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
     */
    private void startThinking(Game game) throws Exception {
        client.send(toUCIPosition(game));
        client.send(toUCIGo(moveTime, depth));

        while (client.isThinking()) {
            client.receive();
        }
    }


    /**
     * Asks the engine process to start pondering a move.
     *
     * @param game      Game to ponder
     */
    private void startPondering(Game game) throws Exception {
        String position = toUCIPosition(game);
        int ponder = client.getPonderMove();

        if (ponder != Game.NULL_MOVE) {
            game.makeMove(ponder);
            position = toUCIPosition(game);
            game.unmakeMove();
        }

        client.send(position);
        client.send("go ponder");
    }


    /**
     * Asks the engine process to stop pondering a move.
     */
    private void stopPondering() throws Exception {
        if (client.isPondering()) {
            client.send("stop");

            while (client.isPondering()) {
                client.receive();
            }
        }
    }


    /**
     * Asks the user which move to perform.
     *
     * @param reader    Terminal reader
     * @return          Move identifier
     */
    private int askForMove(LineReader reader) throws Exception {
        String notation = reader.readLine("Your move? ");
        return board.toMove(notation.trim());
    }


    /**
     * Asks the engine which move to perform.
     *
     * @param writer    Terminal writer
     * @return          Move identifier
     */
    private int askForMove(PrintWriter writer) throws Exception {
        startThinking(game);
        return client.getBestMove();
    }


    /**
     * Asks the user if the computer should move first.
     *
     * @param reader    Terminal reader
     * @return          Turn of the user
     */
    private int askForTurn(LineReader reader) throws Exception {
        String reply = reader.readLine("Shall I move first? ");
        boolean isYes = reply != null && reply.matches("^\\s*y.*");
        return isYes ? Game.NORTH : Game.SOUTH;
    }


    /**
     * Prints a welcome message to the console.
     */
    private void printWelcome(PrintWriter writer) {
        String name = client.getName();
        Package pack = UCIClient.class.getPackage();
        String version = pack.getImplementationVersion();
        writer.format("UCI Match %s%n", version);
        writer.format("Playing against %s%n", name);
    }


    /**
     * Prints the current game board to the console.
     *
     * @param writer    Terminal writer
     */
    private void printBoard(PrintWriter writer) {
        writer.format("%n%s%n%n", board.toBoard(game));
        writer.flush();
    }


    /**
     * Prints an engine move to the console.
     *
     * @param writer    Terminal writer
     */
    private void printMove(PrintWriter writer, int move) {
        String notation = board.toAlgebraic(move);
        writer.format("My move is: %s%n", notation);
        writer.flush();
    }


    /**
     * Prints a welcome message to the console.
     */
    private void printWinner(PrintWriter writer) {
        final int winner = game.winner();

        if (winner == Game.DRAW) {
            writer.println("This match was drawn.");
        } else if (winner == turn) {
            writer.println("You won this match!");
        } else {
            writer.println("You lost this match.");
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
        String moves = board.toAlgebraic(game.moves());
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


    /**
     * Creates a new terminal instance.
     *
     * @return          New terminal
     */
    private static Terminal newTerminal() throws IOException {
        return TerminalBuilder.builder().build();
    }


    /**
     * Creates a new terminal reader instance.
     *
     * @return          New reader
     */
    private static LineReader newLineReader() throws IOException {
        Terminal terminal = newTerminal();
        LineReaderBuilder builder = LineReaderBuilder.builder();
        return builder.terminal(terminal).build();
    }
}
