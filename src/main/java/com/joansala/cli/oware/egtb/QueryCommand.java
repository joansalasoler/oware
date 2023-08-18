package com.joansala.cli.oware.egtb;

/*
 * Copyright (c) 2023 Joan Sala Soler <contact@joansala.com>
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
import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import com.joansala.game.oware.OwareLeaves;
import com.joansala.util.suites.SuiteReader;


/**
 * Query an endgames book database.
 */
@Command(
  name = "query",
  description = "Query an endgames book database",
  mixinStandardHelpOptions = true
)
public class QueryCommand implements Callable<Integer> {

    /** Game board instance */
    private Board parser;

    /** Game instance */
    private Game game;

    @Option(
      names = "--suite",
      description = "Game suite file."
    )
    private File suiteFile;

    @Option(
      names = "--leaves",
      description = "Exported leaves book path",
      required = true
    )
    private String leavesPath = null;


    /**
     * Create a new instance.
     */
    @Inject public QueryCommand(Game game) {
        this.game = game;
        this.parser = game.getBoard();
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        InputStream input = new FileInputStream(suiteFile);

        try (OwareLeaves leaves = new OwareLeaves(leavesPath)) {
            try (SuiteReader reader = new SuiteReader(input)) {
                reader.stream().forEach((suite) -> {
                    Board board = parser.toBoard(suite.diagram());
                    int[] moves = board.toMoves(suite.notation());

                    game.setBoard(board);
                    game.ensureCapacity(1 + moves.length);

                    for (int move : moves) {
                        if (game.hasEnded() == false) {
                            game.makeMove(move);
                        }
                    }

                    Board state = game.toBoard();

                    for (int move : game.legalMoves()) {
                        game.makeMove(move);
                        printBookEntry(move, state, leaves);
                        game.unmakeMove();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return 0;
    }



    /**
     * Show the values of a book entry on the standard output.
     *
     * @param game      Game sate for the entry
     * @param leaves    Endgames book
     */
    public void printBookEntry(int move, Board board, OwareLeaves leaves) {
        if (leaves.find(game)) {
            System.out.format(
                "move = %s, score = %d, captures = %d, flag = %s%n",
                board.toCoordinates(move),
                leaves.getScore(),
                leaves.getCaptures(),
                leaves.getFlag()
            );
        } else {
            System.out.format(
                "move = %s, not found%n",
                board.toCoordinates(move)
            );
        }
    }
}
