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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import com.google.inject.Injector;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.engine.Engine;
import com.joansala.engine.negamax.Negamax;
import com.joansala.engine.doe.*;


/**
 * Executes the Universal Chess Interface service.
 */
@Command(
  name = "train",
  version = "1.2.1",
  description = "Automatic opening book builder",
  mixinStandardHelpOptions = true
)
public class TrainCommand implements Callable<Integer> {

    /** Class injector */
    private final Injector injector;

    /** Game board instance */
    private final Board parser;

    /** Root game state */
    private final Game root;

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
    private long nodeSize = 1000;

    @Option(
      names = "--threads",
      description = "Size of the evaluation thread pool"
    )
    private int poolSize = Runtime.getRuntime().availableProcessors();


    /**
     * Creates a new trainer.
     */
    @Inject public TrainCommand(Injector injector) {
        this.injector = injector;
        this.root = injector.getInstance(Game.class);
        this.parser = injector.getInstance(Board.class);
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        final DOEStore store = new DOEStore(path);
        final DOE trainer = new DOE(store, poolSize);
        final Object position = root.position();
        final int turn = root.turn();

        trainer.setDepth(depth);
        trainer.setContempt(root.contempt());
        trainer.setInfinity(root.infinity());
        trainer.setExplorationBias(bias);

        System.out.format("%s%n", formatSetup());
        System.out.format("Expanding nodes%n%s%n", horizontalRule('-'));

        // Report best variation found so far

        trainer.attachConsumer(report -> {
            final long count = store.count();
            final int centis = report.getScore();
            final int[] moves = report.getVariation();
            System.out.format("= %s%n", fromatResult(moves, centis, count));
        });

        // Evaluate postions as they arrive

        AtomicInteger nodeCount = new AtomicInteger();

        trainer.trainEngine(root, (moves) -> {
            if (nodeSize < nodeCount.incrementAndGet()) {
                trainer.abortComputation();
            }

            Negamax engine = injector.getInstance(Negamax.class);
            Game game = injector.getInstance(Game.class);

            game.setStart(position, turn);
            game.ensureCapacity(moves.length);
            engine.setMoveTime(moveTime);

            for (int move : moves) {
                game.makeMove(move);
            }

            final long count = store.count();
            final int score = engine.computeBestScore(game);
            final int centis = game.toCentiPawns(score);
            System.out.format("- %s%n", fromatResult(moves, centis, count));

            return score;
        });

        store.close();

        return 0;
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
            "Game class:    %45s%n",
            horizontalRule('-'),
            bias,
            moveTime,
            depth,
            poolSize,
            ellipsis(className(root), 44)
        );
    }


    /**
     * Formats a computation result into a string.
     *
     * @retun       A string
     */
    private String fromatResult(int[] moves, int score, long count) {
        String notation = parser.toAlgebraic(moves);
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
}
