package com.joansala.uci;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
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
import com.google.inject.Inject;

import com.joansala.engine.Board;


/**
 * Provides an interactive shell interface to an engine that supports the
 * UCI protocol. This implementation may be used for debugging purposes.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class UCIShell {

    /** Current engine client object */
    private final UCIClient client;

    /** Current console object */
    private final Console console;

    /** Current console writer */
    private final PrintWriter writer;


    /**
     * This class cannot be instantiated
     */
    @Inject public UCIShell(UCIClient client) {
        this.console = System.console();
        this.writer = console.writer();
        this.client = client;
    }


    /**
     * Prints a welcome message on the specified print writer.
     *
     * @param writer    a print writer
     */
    private void showWelcome() {
        Package pack = UCIShell.class.getPackage();
        String version = pack.getImplementationVersion();
        writer.format("UCI Interpreter %s%n", version);
        writer.format("Type UCI commands to send them to the engine.%n%n");
    }


    /**
     * Runs the UCI shell. In this mode the user introduces UCI
     * commands directly into the console.
     */
    public void start() {
        Board board = null;

        // Show a welcome message on the console

        showWelcome();

        // Obtain the current user account name

        String user = System.getProperty("user.name");
        if (user == null) user = "user";

        // Print the initial board

        if (client.isRunning()) {
            board = client.getBoard();
            writer.println(board);
            writer.println();
        }

        // Interpret commands while the engine is running

        while (client.isRunning()) {
            try {
                // Send all the command requests to the engine

                String request = console.readLine("%s> ", user);

                if (request == null) {
                    writer.println();
                    continue;
                }

                if (request.trim().isEmpty())
                    continue;

                client.send(request);

                // The engine stopped running after the request

                if (!client.isRunning())
                    break;

                // Wait for a response to an 'uci' command

                while (!client.isUCIReady()) {
                    String response = client.receive();
                    writer.format("< %s%n", response);
                }

                // Wait for a response to an 'isready' command

                while (!client.isReady()) {
                    String response = client.receive();
                    writer.format("< %s%n", response);
                }

                // Print the current board if it changed

                Board cboard = client.getBoard();

                if (!cboard.equals(board)) {
                    board = cboard;
                    writer.println();
                    writer.println(board);
                    writer.println();
                }

                // If the engine is ponding or thinking indefinitely
                // ask for new commands to the user

                if (!client.hasTimeLimit())
                    continue;

                // Wait for a reply to a ponder request

                while (client.isPondering()) {
                    String response = client.receive();
                    writer.format("< %s%n", response);
                }

                // Wait for a reply to a move request

                while (client.isThinking()) {
                    String response = client.receive();
                    writer.format("< %s%n", response);
                }
            } catch (Exception e) {
                writer.format("%s%n", e.getMessage());
            }
        }
    }


}
