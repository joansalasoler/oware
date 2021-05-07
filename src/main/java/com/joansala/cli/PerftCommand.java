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
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.*;
import com.joansala.bench.*;
import com.joansala.util.GameScanner;
import static com.joansala.engine.Game.NULL_MOVE;


/**
 * Executes the user interface to play against an engine.
 */
@Command(
  name = "perft",
  version = "1.2.1",
  description = "Count leaf nodes of a certain depth",
  mixinStandardHelpOptions = true
)
public final class PerftCommand implements Callable<Integer> {

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


    /**
     * Creates a new service.
     */
    @Inject public PerftCommand(Board board, Game game) {
        this.stats = new BenchStats();
        this.parser = board;
        this.game = game;
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

        try (GameScanner scanner = new GameScanner(parser)) {
            scanner.forEachRemaining((suite) -> {
                System.out.format("%nGame: %s%n", ellipsis(suite, 53));
                System.out.format("%s%n", horizontalRule('-'));

                final Board board = suite.board();
                final int[] moves = suite.moves();

                game.ensureCapacity(moves.length);
                game.setStart(board.position(), board.turn());

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
            throw e;
        }
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
