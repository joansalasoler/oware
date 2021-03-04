package com.joansala.tools;

/*
 * Copyright (C) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import com.joansala.engine.Game;
import com.joansala.oware.*;


/**
 * Runs a perft from test suites provided on the standard input.
 *
 * <p>The command line parameters accepted are:</p>
 *
 * <pre>
 *   --depth     Recursion depth
 * </pre>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class Perft {

    private static long[][] results;


    /**
     * This class cannot be instantiated.
     */
    private Perft() { }


    /**
     * Shows an usage notice on the standard output
     */
    private static void showUsage() {
        System.out.format(
            "Usage:%n%n" +
            "  Perft [parameters] <file>%n%n" +
            "Valid parameters are:%n%n" +
            "  -depth     <byte>  (plies)%n"
        );
    }


    /**
     * Reads test suites from a file.
     *
     * @param path  file path
     * @return      a list of suites
     */
    private static List<String> readSuites(String path) {
        List<String> suites = new ArrayList<String>();
        Scanner scanner = null;

        try {
            scanner = new Scanner(new File(path));

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.length() > 0 && line.charAt(0) != '#')
                    suites.add(line);
            }
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.err.println();
            showUsage();
            System.exit(1);
        } finally {
            if (scanner != null)
                scanner.close();
        }

        return suites;
    }


    /**
     *
     */
    private static long[][] test(OwareGame game, int depth) {
        results = new long[1 + depth][4];
        search(game, Game.NULL_MOVE, depth);
        return results;
    }


    /**
     *
     */
    private static void search(OwareGame game, int move, int depth) {
        if (move != Game.NULL_MOVE) {
            results[depth][0] += 1L;
            results[depth][1] += game.wasCapture() ? 1L : 0L;
            results[depth][2] += game.isRepetition() ? 1L : 0L;
            results[depth][3] += game.hasEnded() ? 1L : 0L;
        }

        if (depth == 0) {
            return;
        }

        int cmove = Game.NULL_MOVE;

        while ((cmove = game.nextMove()) != Game.NULL_MOVE) {
            game.makeMove(cmove);
            search(game, cmove, depth - 1);
            game.unmakeMove();
        }
    }


    /**
     * Main method for the class. Reads tests suites from the standard
     * output and performs a benchmark on the suites.
     *
     * @param argv  Command line arguments.
     */
    public static void main(String[] argv) throws Exception {
        // Output separator

        char[] s = new char[76]; s[74] = '%'; s[75] = 'n';
        String separator = new String(s).replace("\0", "=");

        // Default configuration

        String path = null;
        int depth = 5;

        // Parse command line arguments

        try {
            int i = 0;

            for (i = 0; i < argv.length - 1; i++) {
                if ("-depth".equals(argv[i])) {
                    depth = Integer.parseInt(argv[++i]);
                } else {
                    throw new IllegalArgumentException();
                }
            }

            path = argv[i];
        } catch (Exception e) {
            showUsage();
            System.exit(1);
        }

        // Load the test suites from a file and configure the engine

        List<String> suites = readSuites(path);
        OwareGame game = new OwareGame();
        OwareBoard start = new OwareBoard();

        // Run benchmark tests

        long totalTime = 0;
        long totalNodes = 0;

        System.out.format(
            " # " +
            "       Nodes " +
            "    Captures " +
            " Repetitions " +
            "    Endgames%n" +
            separator
        );

        for (int i = 0; i < suites.size(); i++) {
            String suite = suites.get(i);
            OwareBoard board = start.toBoard(suite);
            game.setStart(board.position(), board.turn());

            long startTime = System.currentTimeMillis();
            long[][] results = test(game, depth);
            long endTime = System.currentTimeMillis();

            totalTime += endTime - startTime;
            totalNodes += results[depth][0];

            for (int d = depth - 1; d >= 0; d--) {
                System.out.format(
                    "%2d %12d %12d %12d %12d%n",
                    depth - d, results[d][0], results[d][1],
                    results[d][2], results[d][3]
                );

                totalNodes += results[d][0];
            }

            System.out.format(separator);
        }

        System.out.println();

        // Show benchmark results

        double timeSeconds = totalTime / 1000.0;
        double nodesSecond = totalNodes / timeSeconds;

        System.out.format(
            "Results%n" +
            separator +
            "Total nodes:          %,38d       nodes%n" +
            "Total time:           %,42.3f   seconds%n" +
            "Nodes per second:     %,42.3f   nps%n",
            totalNodes, timeSeconds, nodesSecond
        );
    }
}
