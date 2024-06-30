package com.joansala.cli.oware.egtb;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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
import picocli.CommandLine.*;
import com.joansala.game.oware.egtb.*;


/**
 * Endgames book builder.
 */
@Command(
  name = "solve",
  description = "Oware endgames book builder",
  mixinStandardHelpOptions = true
)
public class SolveCommand implements Callable<Integer> {

    @Option(
      names = "--path",
      description = "Database storage folder"
    )
    private String path = "egtb.db";


    @Option(
      names = "--max-pieces",
      description = "Maximum number of pieces on the board"
    )
    private int maxPieces = 15;


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        final EGTBStore store = new EGTBStore(path);
        final EGTBSolver solver = new EGTBSolver(store);

        // Ensures the store is properly closed

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down.");
            solver.abortComputation();
            store.close();
            System.out.println("Done.");
        }));

        System.out.format("%nBuilding endgames%n%s%n", horizontalRule('-'));
        solver.solve(maxPieces);

        return 0;
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
}
