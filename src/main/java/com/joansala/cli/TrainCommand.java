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

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import com.google.inject.Injector;
import picocli.CommandLine.*;

import com.joansala.cli.util.EngineType;
import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.engine.Engine;
import com.joansala.engine.doe.*;


/**
 * Automatic opening book builder.
 */
@Command(
  name = "train",
  description = "Automatic opening book builder",
  mixinStandardHelpOptions = true
)
public class TrainCommand implements Callable<Integer> {

    /** Class injector */
    private final Injector injector;

    /** Game board instance */
    private final Board rootBoard;

    /** Root game state */
    private final Game rootGame;

    @Option(
      names = "--path",
      description = "Database storage folder"
    )
    private String path = "book.db";

    @Option(
      names = "--bias",
      description = "Exploration bias factor"
    )
    private double bias = DOE.DEFAULT_BIAS;

    @Option(
      names = "--depth",
      description = "Expansion depth limit (plies)"
    )
    private int depth = Engine.DEFAULT_DEPTH;

    @Option(
      names = "--movetime",
      description = "Time limit per move (ms)"
    )
    private long moveTime = Engine.DEFAULT_MOVETIME;

    @Option(
      names = "--nodes",
      description = "Number of nodes to expand (nodes)"
    )
    private int nodeSize = 1000;

    @Option(
      names = "--threads",
      description = "Size of the evaluation thread pool"
    )
    private int poolSize = Runtime.getRuntime().availableProcessors();

    @Option(
      names = "--engine",
      description = "Evaluation engine to use (${COMPLETION-CANDIDATES})"
    )
    private EngineType engineType = EngineType.MONTECARLO;

    @Option(
      names = "--export",
      description = "Path to export the opening book"
    )
    private String exportPath = null;


    /**
     * Creates a new trainer.
     */
    @Inject public TrainCommand(Injector injector) {
        this.injector = injector;
        this.rootGame = injector.getInstance(Game.class);
        this.rootBoard = rootGame.getBoard();
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        final DOEStore store = new DOEStore(path);
        final DOEExporter exporter = new DOEExporter(store);
        final DOE trainer = new DOE(store, poolSize);

        trainer.setDepth(depth);
        trainer.setInfinity(rootGame.infinity());
        trainer.setExplorationBias(bias);

        System.out.format("%s%n", formatSetup());
        System.out.format("Expanding nodes%n%s%n", horizontalRule('-'));

        // Initialize the evaluation engines

        Queue<Evaluator> evaluators = new ArrayDeque<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            evaluators.add(new Evaluator());
        }

        // Report the best variation found so far

        trainer.attachConsumer(report -> {
            final int[] moves = report.getVariation();
            final int centis = report.getScore();
            final long count = store.count();

            String result = formatResult(moves, centis, count);
            System.out.format("= %s%n", result);
        });

        // Evaluate postions as they arrive

        trainer.trainEngine(nodeSize, rootGame, (moves) -> {
            Evaluator evaluator = evaluators.poll();

            final int score = evaluator.computeScore(moves);
            final int centis = rootGame.toCentiPawns(score);
            final long count = store.count();

            String result = formatResult(moves, centis, count);
            System.out.format("- %s%n", result);

            evaluators.offer(evaluator);

            return score;
        });

        // Export the book to the given path

        if (exportPath != null) {
            System.out.format("%nExporting book%n%s%n", horizontalRule('-'));
            System.out.format("Entries: %d%n", exporter.export(exportPath));
        }

        store.close();

        return 0;
    }


    /**
     * Obtain an evaluation engine instance.
     */
    private Engine getEngineInstance() {
        Class<Engine> type = engineType.getType();
        Engine engine = injector.getInstance(type);

        engine.setInfinity(rootGame.infinity());
        engine.setMoveTime(moveTime);

        return engine;
    }


    /**
     * Obtain a game instance.
     */
    private Game getGameInstance() {
        return injector.getInstance(Game.class);
    }


    /**
     * Formats the current engine setting into a string.
     *
     * @retun       A string
     */
    private String formatSetup() {
        return String.format(
            "Training setup%n" +
            "%s%n" +
            "Exploration bias:  %33.3f%n" +
            "Time per move:     %,33d ms%n" +
            "Depth limit:       %,33d plies%n" +
            "Thread pool size:  %,33d threads%n" +
            "Engine class:  %45s%n" +
            "Game class:    %45s%n",
            horizontalRule('-'),
            bias,
            moveTime,
            depth,
            poolSize,
            ellipsis(engineType.getType().getName(), 44),
            ellipsis(className(rootGame), 44)
        );
    }


    /**
     * Formats a computation result into a string.
     *
     * @retun       A string
     */
    private String formatResult(int[] moves, int score, long count) {
        String notation = rootBoard.toAlgebraic(moves);
        String result = String.format("%d %6d %s", count, score, notation);
        return ellipsis(result, 57);
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


    /**
     * Evaluates a game states using an engine.
     */
    private class Evaluator {
        private Game game;
        private Engine engine;

        private Evaluator() {
            game = getGameInstance();
            engine = getEngineInstance();
        }

        private void setMoves(int[] moves) {
            game.setBoard(rootBoard);
            game.ensureCapacity(moves.length);

            for (int move : moves) {
                game.makeMove(move);
            }
        }

        private int computeScore(int[] moves) {
            setMoves(moves);
            return engine.computeBestScore(game);
        }
    }
}
