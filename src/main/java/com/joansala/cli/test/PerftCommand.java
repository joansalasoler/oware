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
      description = "Maximum depth limit per move (plies)"
    )
    private int maxDepth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--min-depth",
      description = "Minimum depth limit per move (plies)"
    )
    private int minDepth = 1;

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

        if (file instanceof File == false) {
            String diagram = parser.toDiagram();
            System.out.format("%n%s%n", ellipsis(diagram, 59));
            System.out.format("%s%n", horizontalRule('-'));
            System.out.format("%s%n", formatHeader());

            game.setBoard(parser);
            benchmark(game, minDepth, maxDepth);
        } else {
            InputStream input = new FileInputStream(file);

            try (SuiteReader reader = new SuiteReader(input)) {
                reader.stream().forEach((suite) -> {
                    String format = formatSuite(suite);
                    System.out.format("%n%s%n", ellipsis(format, 59));
                    System.out.format("%s%n", horizontalRule('-'));
                    System.out.format("%s%n", formatHeader());

                    Board board = parser.toBoard(suite.diagram());
                    int[] moves = board.toMoves(suite.notation());

                    game.ensureCapacity(moves.length);
                    game.setBoard(board);

                    for (int move : moves) {
                        if (game.hasEnded() == false) {
                            game.makeMove(move);
                        }
                    }

                    benchmark(game, minDepth, maxDepth);
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.format("%n%s", formatStats());
    }


    /**
     * Run multiple benchmark iterations and print the results.
     *
     * @param minDepth  Minimum depth
     * @param maxDepth  Maximum depth
     */
    private void benchmark(Game game, int minDepth, int maxDepth) {
        final int cursor = game.getCursor();

        for (int depth = minDepth; depth <= maxDepth; depth++) {
            if (benchmark(game, depth) == 0L) break;
            System.out.format("%s%n", formatResults());
            game.setCursor(cursor);
        }
    }


    /**
     * Benchmark a game state to the given depth.
     *
     * @param engine    Engine instance
     * @param game      Game instance
     */
    private long benchmark(Game game, int depth) {
        stats.terminal.clear();
        stats.depth.clear();
        stats.depth.aggregate(depth);
        stats.watch.start();
        expand(game, depth);
        stats.watch.stop();
        return stats.terminal.count();
    }


    /**
     * Expands the game tree to the given depth.
     */
    private void expand(Game game, int depth) {
        boolean endgame = game.hasEnded();

        if (depth == 0) {
            stats.terminal.test(endgame);
        } else if (endgame == false) {
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
            "%,5.0f %,24d %,18d %,10.2f",
            stats.depth.average(),
            stats.terminal.count(),
            stats.terminal.success(),
            stats.visitsPerSecond() / 1000.0D
        );
    }


    /**
     * Obtain a formatted header for the results.
     */
    private String formatHeader() {
        return String.format(
            "%5s %24s %18s %10s",
            "depth", "leaves", "endgames", "kn/s"
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
