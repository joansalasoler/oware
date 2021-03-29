package com.joansala.tools.bench;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import com.joansala.engine.*;
import com.joansala.oware.*;


/**
 * Runs a simple benchmark from test suites provided on the standard
 * input. A test suite consists on a series of games played from the
 * start position. Each line of the standard input represents a single
 * game where lines starting with # are ignored.
 *
 * <p>The command line parameters accepted are:</p>
 *
 * <pre>
 *   --depth     Maximum allowed depth
 *   --movetime  Maximum allowed time per move
 *   --hashsize  Requested transposition table size in megabytes
 * </pre>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class Benchmark {

    /**
     * Game object used for the benchmark.
     *
     * {@inheritDoc}
     */
    private static class BenchGame extends OwareGame {

        public long nodesMade = 0;
        public long terminal = 0;
        public long evaluated = 0;


        /** {@inheritDoc} */
        @Override
        public void makeMove(int move) {
            super.makeMove(move);
            nodesMade++;
        }


        /** {@inheritDoc} */
        @Override
        public int outcome() {
            terminal++;
            evaluated++;
            return super.outcome();
        }


        /** {@inheritDoc} */
        @Override
        public int score() {
            evaluated++;
            return super.score();
        }

    }


    /**
     * Transposition table used for the benchmark.
     *
     * {@inheritDoc}
     */
    private static class BenchCache extends OwareCache {

        public long nodesStored = 0;
        public long nodesProbed = 0;
        public long nodesFound = 0;

        /** {@inheritDoc} */
        public BenchCache(long memory) {
            super(memory);
        }

        /** {@inheritDoc} */
        @Override
        public void store(Game g, int s, int m, int d, int f) {
            super.store(g, s, m, d, f);
            nodesStored++;
        }

        /** {@inheritDoc} */
        @Override
        public boolean find(Game game) {
            nodesProbed++;

            if (super.find(game)) {
                nodesFound++;
                return true;
            }

            return false;
        }

    }


    /**
     * This class cannot be instantiated.
     */
    private Benchmark() { }


    /**
     * Shows an usage notice on the standard output
     */
    private static void showUsage() {
        System.out.format(
            "Usage:%n%n" +
            "  Benchmark [parameters] <file>%n%n" +
            "Valid parameters are:%n%n" +
            "  -depth     <byte>  (plies)%n" +
            "  -movetime  <long>  (milliseconds)%n" +
            "  -hashsize  <int>   (MB)%n"
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
            Benchmark.showUsage();
            System.exit(1);
        } finally {
            if (scanner != null)
                scanner.close();
        }

        return suites;
    }


    /**
     * Main method for the class. Reads tests suites from the standard
     * output and performs a benchmark on the suites.
     *
     * @param argv  Command line arguments.
     */
    public static void main(String[] argv) {
        // Output separator

        char[] s = new char[76]; s[74] = '%'; s[75] = 'n';
        String separator = new String(s).replace("\0", "=");

        // Default configuration

        String path = null;
        int depth = Integer.MAX_VALUE;
        long movetime = 2000;
        int hashsize = 32;

        // Parse command line arguments

        try {
            int i = 0;

            for (i = 0; i < argv.length - 1; i++) {
                if ("-depth".equals(argv[i])) {
                    depth = Integer.parseInt(argv[++i]);
                } else if ("-movetime".equals(argv[i])) {
                    movetime = Long.parseLong(argv[++i]);
                } else if ("-hashsize".equals(argv[i])) {
                    hashsize = Integer.parseInt(argv[++i]);
                } else {
                    throw new IllegalArgumentException();
                }
            }

            path = argv[i];
        } catch (Exception e) {
            Benchmark.showUsage();
            System.exit(1);
        }

        // Load the test suites from a file and configure the engine

        List<String> suites = Benchmark.readSuites(path);
        BenchGame game = new BenchGame();
        BenchCache cache = new BenchCache(hashsize << 20);
        OwareBoard start = new OwareBoard();
        Negamax engine = new Negamax();

        engine.setInfinity(OwareGame.MAX_SCORE);
        engine.setMoveTime(movetime);
        engine.setDepth(depth);
        engine.setCache(cache);

        // Show configured options

        int maxdepth = engine.getDepth() + 1;
        long settime = engine.getMoveTime();
        long capacity = cache.size();

        System.out.format(
            "Settings%n" +
            separator +
            "Hash table size:    %,44d bytes%n" +
            "Time per move:      %,44d ms%n" +
            "Maximum depth:      %,44d plies%n%n",
            capacity, settime, maxdepth
        );

        // Run benchmark tests

        System.out.format("Running test suite%n" + separator);

        long totalTime = 0;
        long totalMoves = 0;
        long depthSum = 0;
        long depthMoves = 0;
        int size = suites.size();

        for (int i = 0; i < size; i++) {
            System.out.format("Running test:\t %44d/%d%n", i + 1, size);

            // Start a new game

            engine.newMatch();
            game.setStart(start.position(), start.turn());

            // Compute each move from the suite

            String suite = suites.get(i);
            int[] moves = start.toMoves(suite);

            game.ensureCapacity(suite.length());

            for (int suiteMove : moves) {
                // Compute a move for the position

                long startTime = System.currentTimeMillis();
                int move = engine.computeBestMove(game);
                long endTime = System.currentTimeMillis();

                // Append the results to the counters

                totalMoves++;
                totalTime += (endTime - startTime);

                // Get reached depth

                game.makeMove(move);

                if (cache.find(game)) {
                    depthSum += cache.getDepth() + 1;
                    depthMoves++;
                }

                game.unmakeMove();

                // Make the move found on the suite

                game.makeMove(suiteMove);
            }
        }

        System.out.println();

        // Show benchmark results

        double timeSeconds = totalTime / 1000.0;
        double nodesSecond = game.nodesMade / timeSeconds;
        double averageDepth = (double) depthSum / depthMoves;
        double hashSuccess = (double) cache.nodesFound / cache.nodesProbed;
        double averageNodes = (double) game.evaluated / totalMoves;
        double branching = Math.pow(averageNodes, 1.0D / averageDepth);

        System.out.format(
            "Results%n" +
            separator +
            "Total moves:          %,38d       moves%n" +
            "Total time:           %,42.3f   seconds%n" +
            "Searched nodes:       %,38d       nodes%n" +
            "Evaluated nodes:      %,38d       nodes%n" +
            "Terminal nodes:       %,38d       nodes%n" +
            "Branching factor (b): %,42.3f   nodes%n" +
            "Average depth (d):    %,42.3f   plies%n" +
            "O(b ^ d):             %,42.3f   nodes%n" +
            "Nodes per second:     %,42.3f   nps%n" +
            "Stored nodes:         %,38d       nodes%n" +
            "Probed nodes:         %,38d       nodes%n" +
            "Found nodes:          %,38d       nodes%n" +
            "Hash table success:   %,42.3f   %%%n",
            totalMoves, timeSeconds, game.nodesMade, game.evaluated,
            game.terminal, branching, averageDepth, averageNodes,
            nodesSecond, cache.nodesStored, cache.nodesProbed,
            cache.nodesFound, 100.0D * hashSuccess
        );
    }

}
