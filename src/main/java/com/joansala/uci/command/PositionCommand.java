package com.joansala.uci.command;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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

import java.util.Map;
import com.joansala.engine.Board;
import com.joansala.uci.UCIBrain;
import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.Parameters;
import static com.joansala.uci.UCI.*;


/**
 * Sets a new game state given a position and a set of moves.
 */
public class PositionCommand implements UCICommand {
    public void accept(UCIService service, Parameters params) {
        if (service.isReady() == false) {
            throw new IllegalStateException(
                "Engine is not ready");
        }

        String[] keywords = { STARTPOS, FEN, MOVES };
        Map<String, String> values = params.match(keywords);
        UCIBrain brain = service.getBrain();
        Board board = service.getBoard();
        int[] moves = new int[0];

        if (values.containsKey(FEN)) {
            String diagram = values.get(FEN);
            board = board.toBoard(diagram);
        }

        if (values.containsKey(MOVES)) {
            String notation = values.get(MOVES);
            moves = board.toMoves(notation);
        }

        brain.setState(board, moves);
    }
}
