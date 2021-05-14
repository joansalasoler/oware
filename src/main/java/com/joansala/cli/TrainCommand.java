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
      names = "--movetime",
      description = "Time limit per move (ms)"
    )
    private long moveTime = Engine.DEFAULT_MOVETIME;

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
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        final DOEStore store = new DOEStore(path);
        final DOE trainer = new DOE(store, poolSize);

        final Game root = injector.getInstance(Game.class);
        final Board parser = injector.getInstance(Board.class);
        final Object position = root.position();
        final int turn = root.turn();

        trainer.setContempt(root.contempt());
        trainer.setInfinity(root.infinity());
        trainer.setExplorationBias(bias);

        // Report search information

        trainer.attachConsumer(report -> {
            int[] moves = report.getVariation();
            String notation = parser.toAlgebraic(moves);
            System.out.format("= %d %s%n", store.count(), notation);
        });

        // Evaluate postions as they arrive

        trainer.trainEngine(root, (moves) -> {
            Negamax engine = injector.getInstance(Negamax.class);
            Game game = injector.getInstance(Game.class);

            engine.setMoveTime(moveTime);
            game.setStart(position, turn);
            game.ensureCapacity(moves.length);

            for (int move : moves) {
                game.makeMove(move);
            }

            String notation = parser.toAlgebraic(moves);
            System.out.format("- %d %s%n", store.count(), notation);

            return engine.computeBestScore(game);
        });

        store.close();

        return 0;
    }
}
