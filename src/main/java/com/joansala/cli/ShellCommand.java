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
import java.util.logging.Logger;
import java.util.logging.Level;
import com.google.inject.Inject;
import org.jline.reader.*;
import org.jline.terminal.*;
import picocli.CommandLine.*;

import org.jline.builtins.Completers.TreeCompleter;
import static org.jline.builtins.Completers.TreeCompleter.node;

import com.joansala.engine.Board;
import com.joansala.cli.util.ProcessConverter;
import com.joansala.uci.UCIClient;
import static com.joansala.uci.UCI.*;


/**
 * Executes the Universal Chess Interface interpreter.
 */
@Command(
  name = "shell",
  description = "Runs an interactive UCI interpreter",
  mixinStandardHelpOptions = true
)
public class ShellCommand implements Callable<Integer> {

    /** UCI client instance */
    private UCIClient client;

    @Option(
      names = "--command",
      description = "Custom UCI engine command",
      converter = ProcessConverter.class,
      defaultValue = "<default>"
    )
    private Process service = null;


    @Option(
      names = "--debug",
      description = "Log debug messages."
    )
    private boolean debug = false;


    /**
     * Creates a new service.
     */
    @Inject public ShellCommand(UCIClient client) {
        this.client = client;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        configureLoggers();
        client.setService(service);
        runInterpreter();
        return 0;
    }


    /**
     * Runs the UCI interpreter while the client is running.
     */
    public void runInterpreter() throws IOException {
        LineReader reader = newLineReader();
        PrintWriter writer = reader.getTerminal().writer();
        Board board = null;

        printWelcome(writer);

        try {
            client.send(DEBUG, ON);
        } catch (Exception e) {
            writer.format("Warning: Cannot set debug mode%n");
        }

        while (client.isRunning()) {
            try {
                if (!client.getBoard().equals(board)) {
                    board = client.getBoard();
                    printBoard(writer);
                }

                client.send(nextCommand(reader));

                while (!client.isReady() || !client.isUCIReady()) {
                    printResponse(writer, client.receive());
                }

                if (client.hasTimeLimit()) {
                    while (client.isPondering() || client.isThinking()) {
                        printResponse(writer, client.receive());
                    }
                }
            } catch (Exception e) {
                writer.format("Error: %s%n", e.getMessage());
            }
        }
    }


    /**
     * Prints the current board of the UCI client.
     *
     * @param writer    Terminal writer
     */
    private void printBoard(PrintWriter writer) {
        writer.println(client.getBoard());
        writer.flush();
    }


    /**
     * Prints an engine message to a terminal writer.
     *
     * @param writer    Terminal writer
     * @param message   Engine message
     */
    private void printResponse(PrintWriter writer, String message) {
        writer.format("< %s%n", message);
        writer.flush();
    }


    /**
     * Prints a welcome message to a terminal writer.
     *
     * @param writer    Terminal writer
     */
    private void printWelcome(PrintWriter writer) {
        Package pack = UCIClient.class.getPackage();
        String version = pack.getImplementationVersion();
        writer.format("UCI Interpreter %s%n", version);
        writer.format("Type UCI commands to send them to the engine.%n%n");
    }


    /**
     * Reads the next command from the given terminal reader.
     *
     * @param reader    Terminal reader
     */
    private String nextCommand(LineReader reader) {
        String line = null;

        while (line == null || line.trim().isEmpty()) {
            line = reader.readLine("uci> ");
        }

        return line;
    }


    /**
     * Configure the application loggers.
     */
    private void configureLoggers() {
        Logger logger = Logger.getLogger("com.joansala.uci");
        logger.setLevel(debug ? Level.ALL : Level.OFF);
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
        return builder.terminal(terminal).completer(COMPLETER).build();
    }


    /**
     * Provides keyword completions for the shell.
     */
    private static final Completer COMPLETER = new TreeCompleter(
        node("debug", node("on", "off")),
        node("go", node("ponder", "infinite", "movetime", "depth")),
        node("position", node("fen"), node("startpos", node("moves"))),
        node("setoption", node("name")),
        node("ucinewgame"),
        node("isready"),
        node("moves"),
        node("ponderhit"),
        node("quit"),
        node("stop"),
        node("uci")
    );
}
