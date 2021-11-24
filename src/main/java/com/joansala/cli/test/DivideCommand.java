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


/**
 * Count leafs of each child node.
 */
@Command(
  name = "divide",
  description = "Count leafs of each child node",
  mixinStandardHelpOptions = true
)
public class DivideCommand implements Callable<Integer> {

    /** Statistics accumulator */
    private BenchStats stats;

    /** Game board instance */
    private Board parser;

    /** Game instance */
    private Game game;

    @Option(
      names = "--depth",
      description = "Depth to count (plies)"
    )
    private int depth = 1;

    @Option(
      names = "--file",
      description = "Benchmark suite file."
    )
    private File file;


    /**
     * Creates a new service.
     */
    @Inject public DivideCommand(Game game) {
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

        if (file instanceof File == false) {
            String diagram = parser.toDiagram();
            System.out.format("%n%s%n", ellipsis(diagram, 59));
            System.out.format("%s%n", horizontalRule('-'));

            game.setBoard(parser);
            game.ensureCapacity(depth);
            benchmark(game, depth - 1);
        } else {
            InputStream input = new FileInputStream(file);

            try (SuiteReader reader = new SuiteReader(input)) {
                reader.stream().forEach((suite) -> {
                    String format = formatSuite(suite);
                    System.out.format("%n%s%n", ellipsis(format, 59));
                    System.out.format("%s%n", horizontalRule('-'));

                    Board board = parser.toBoard(suite.diagram());
                    int[] moves = board.toMoves(suite.notation());

                    game.setBoard(board);
                    game.ensureCapacity(moves.length + depth);

                    for (int move : moves) {
                        if (game.hasEnded() == false) {
                            game.makeMove(move);
                        }
                    }

                    benchmark(game, depth - 1);
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }


    /**
     * Benchmark a game state to the given depth.
     *
     * @param engine    Engine instance
     * @param game      Game instance
     */
    private void benchmark(Game game, int depth) {
        stats.visits.clear();

        for (int move : game.legalMoves()) {
            stats.terminal.clear();
            game.makeMove(move);
            expand(game, depth);
            String notation = parser.toCoordinates(move);
            System.out.format("%s%n", formatResults(notation));
            game.unmakeMove();
        }

        System.out.format("%n%s", formatStats());
    }


    /**
     * Expands the game tree to the given depth.
     */
    private void expand(Game game, int depth) {
        int[] moves = game.legalMoves();
        int count = moves.length;

        if (depth <= 1 || game.hasEnded()) {
            stats.terminal.increment(count);
            stats.visits.increment(count);
        } else {
            for (int move : moves) {
                game.makeMove(move);
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
            depth
        );
    }


    /**
     * Formats the current results into a string.
     *
     * @retun       A string
     */
    private String formatResults(String notation) {
        return String.format(
            "%s: %d",
            notation,
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
            "Leaf nodes count:         %,26d nodes%n",
            horizontalRule('-'),
            stats.visits.count()
        );
    }


    /**
     * String representation of a game suite.
     */
    private static String formatSuite(Suite suite) {
        StringJoiner joiner = new StringJoiner(" ");

        if (!suite.notation().isBlank()) {
            joiner.add(suite.notation());
        }

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
