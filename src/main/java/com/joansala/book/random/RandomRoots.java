package com.joansala.book.random;

/*
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import static com.joansala.engine.Game.*;


/**
 * An opening book that picks moves more or less randomly.
 */
public class RandomRoots implements Roots<Game> {

    /** Minimum allowed heuristic score in centipawns */
    private static final int MIN_CENTIPAWNS = -150;

    /** Random number generator */
    private final Random random = new Random();

    /** If no more book moves can be found */
    private boolean outOfBook = false;


    /**
     * {@inheritDoc}
     */
    @Override
    public void newMatch() {
        outOfBook = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int pickBestMove(Game game) throws IOException {
        if (outOfBook == false) {
            List<Integer> moves = moveChoices(game);

            if ((outOfBook = moves.isEmpty()) == false) {
                return pickRandomMove(moves);
            }
        }

        return NULL_MOVE;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int pickPonderMove(Game game) throws IOException {
        return pickBestMove(game);
    }


    /**
     * Picks a random element from a list of moves.
     *
     * @param entries       List of moves
     * @return              An entry on the list
     */
    private int pickRandomMove(List<Integer> moves) {
        return moves.get(random.nextInt(moves.size()));
    }


    /**
     * Obtain the list of moves for which their heuristic scores are
     * greater or equal to zero.
     *
     * @param game          Game state
     * @return              List of moves
     */
    private List<Integer> moveChoices(Game game) {
        List<Integer> choices = new LinkedList<>();
        int[] moves = game.legalMoves();
        int cursor = game.getCursor();

        game.ensureCapacity(1 + game.length());

        for (int move : moves) {
            game.makeMove(move);
            int score = game.score();
            int centis = game.toCentiPawns(score);

            if (centis >= MIN_CENTIPAWNS) {
                choices.add(move);
            }

            game.unmakeMove();
        }

        game.setCursor(cursor);

        return choices;
    }
}
