package com.joansala.cli.test;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.StringJoiner;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.*;
import com.joansala.util.bench.*;
import com.joansala.util.suites.Suite;
import com.joansala.util.suites.SuiteReader;
import static com.joansala.engine.Game.NULL_MOVE;


/**
 * Count leaf nodes of a certain depth.
 */
@Command(
  name = "perft",
  description = "Count leaf nodes of a certain depth",
  mixinStandardHelpOptions = true
)
public class PerftCommand implements Callable<Integer> {

    /** Statistics accumulator */
    private BenchStats stats;

    /** Game board instance */
    private Board parser;

    /** Game instance */
    private Game game;

    @Option(
      names = "--depth",
      description = "Depth limit per move (plies)"
    )
    private int maxDepth = Engine.DEFAULT_DEPTH;


    @Option(
      names = "--file",
      description = "Benchmark suite file."
    )
    private File file;


    /**
     * Creates a new service.
     */
    @Inject public PerftCommand(Game game) {
        this.game = game;
        this.stats = new BenchStats();
        this.parser = game.getBoard();
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        runBenchmark();
        return 0;
    }


    /**
     * Runs the benchmark on each position of the benchmark suites.
     */
    public void runBenchmark() throws IOException {
        System.out.format("%s", formatSetup());
        InputStream input = getInputStream();

        try (SuiteReader reader = new SuiteReader(input)) {
            reader.stream().forEach((suite) -> {
                String format = formatSuite(suite);
                System.out.format("%n%s%n", ellipsis(format, 59));
                System.out.format("%s%n", horizontalRule('-'));

                Board board = parser.toBoard(suite.diagram());
                int[] moves = board.toMoves(suite.notation());

                game.ensureCapacity(moves.length);
                game.setBoard(board);

                for (int move : moves) {
                    if (game.hasEnded() == false) {
                        game.makeMove(move);
                    }
                }

                final int cursor = game.getCursor();

                for (int depth = 1; depth <= maxDepth; depth++) {
                    benchmark(game, depth);
                    game.setCursor(cursor);
                    System.out.format("%s%n", formatResults());
                }
            });

            System.out.format("%n%s", formatStats());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    /**
     * Obtain the input stream from which to read positions. That is,
     * either from standard input or the specified file.
     */
    private InputStream getInputStream() throws IOException {
        InputStream input = System.in;

        if (file instanceof File) {
            input = new FileInputStream(file);
        }

        return input;
    }


    /**
     * Benchmark a game state to the given depth.
     *
     * @param engine    Engine instance
     * @param game      Game instance
     */
    private void benchmark(Game game, int depth) {
        stats.terminal.clear();
        stats.depth.clear();
        stats.depth.aggregate(depth);
        stats.watch.start();
        expand(game, depth);
        stats.watch.stop();
    }


    /**
     * Expands the game tree to the given depth.
     */
    private void expand(Game game, int depth) {
        boolean terminal = game.hasEnded();

        if (depth == 0) {
            stats.terminal.test(terminal);
        } else if (terminal == false) {
            int move = NULL_MOVE;

            while ((move = game.nextMove()) != NULL_MOVE) {
                game.makeMove(move);
                stats.visits.increment();
                expand(game, depth - 1);
                game.unmakeMove();
            }
        }
    }


    /**
     * Formats the current engine setting into a string.
     *
     * @retun       A string
     */
    private String formatSetup() {
        return String.format(
            "Benchmark setup%n" +
            "%s%n" +
            "Game class:    %45s%n" +
            "Depth limit:   %,39d plies%n",
            horizontalRule('-'),
            ellipsis(className(game), 44),
            maxDepth
        );
    }


    /**
     * Formats the current results into a string.
     *
     * @retun       A string
     */
    private String formatResults() {
        return String.format(
            "%,4.0f %,27d %,27d",
            stats.depth.average(),
            stats.terminal.count(),
            stats.terminal.success()
        );
    }


    /**
     * Formats the current statistics into a string.
     *
     * @retun       A string
     */
    private String formatStats() {
        return String.format(
            "Benchmark results%n" +
            "%s%n" +
            "Node visits per second:   %,28.0f nps%n" +
            "Node visits count:          %,26d nodes%n",
            horizontalRule('-'),
            stats.visitsPerSecond(),
            stats.visits.count()
        );
    }


    /**
     * String representation of a game suite.
     */
    private static String formatSuite(Suite suite) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(suite.notation());
        joiner.add(suite.diagram());
        return joiner.toString();
    }


    /**
     * String representation truncated to 60 characters.
     *
     * @param o         An object
     * @param size      Maximum string size
     * @return          A string
     */
    private static String ellipsis(Object o, int size) {
        final String v = String.valueOf(o);
        return v.replaceAll("(?<=^.{" + size + "}).*$", "…");
    }


    /**
     * Returns an horizontal rule of exactly 60 characters.
     *
     * @param c         Rule character
     * @return          A new string
     */
    private static String horizontalRule(char c) {
        return new String(new char[60]).replace('\0', c);
    }


    /**
     * Class name of an object.
     *
     * @param o         An object
     * @return          Class name
     */
    private static String className(Object o) {
        return o == null ? "-" : o.getClass().getName();
    }
}
