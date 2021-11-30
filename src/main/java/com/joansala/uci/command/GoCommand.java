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

import com.joansala.engine.Engine;
import com.joansala.uci.UCIBrain;
import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.Parameters;
import com.joansala.uci.util.TimeManager;
import static com.joansala.engine.Game.*;
import static com.joansala.uci.UCI.*;


/**
 * Instructs the service to start searching for a move.
 */
public class GoCommand implements UCICommand {

    /**
     * {@inheritDoc}
     */
    public String[] parameterNames() {
        return new String[] {
            DEPTH, MOVETIME, INFINITE, PONDER, MOVESTOGO,
            BINC, BTIME, WINC, WTIME
        };
    }


    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, Parameters params) {
        if (service.isReady() == false) {
            throw new IllegalStateException(
                "Engine is not ready");
        }

        UCIBrain brain = service.getBrain();
        Engine engine = service.getEngine();
        TimeManager manager = service.getTimeManager();

        resetTimeManager(manager);
        int turn = brain.getSearchTurn();
        int depth = Engine.DEFAULT_DEPTH;
        long moveTime = Engine.DEFAULT_MOVETIME;
        boolean infinite = false;

        if (params.contains(DEPTH)) {
            String value = params.get(DEPTH);
            depth = Integer.parseInt(value);
        }

        if (params.contains(BTIME)) {
            String value = params.get(BTIME);
            long timeLeft = Long.parseLong(value);
            manager.setTimeLeft(NORTH, timeLeft);
        }

        if (params.contains(WTIME)) {
            String value = params.get(WTIME);
            long timeLeft = Long.parseLong(value);
            manager.setTimeLeft(SOUTH, timeLeft);
        }

        if (params.contains(BINC)) {
            String value = params.get(BINC);
            long timeInc = Long.parseLong(value);
            manager.setTimeIncrement(NORTH, timeInc);
        }

        if (params.contains(WINC)) {
            String value = params.get(WINC);
            long timeInc = Long.parseLong(value);
            manager.setTimeIncrement(SOUTH, timeInc);
        }

        if (params.contains(MOVESTOGO)) {
            String value = params.get(MOVESTOGO);
            int movesLeft = Integer.parseInt(value);
            manager.setMovesLeft(movesLeft);
        }

        if (params.contains(MOVETIME)) {
            String value = params.get(MOVETIME);
            moveTime = Integer.parseInt(value);
            manager.setFixedTimeActive(true);
            manager.setMoveTime(moveTime);
        }

        moveTime = manager.getMoveTimeAdvice(turn);

        if (params.contains(PONDER) ||
            params.contains(INFINITE)) {
            infinite = true;
            depth = Integer.MAX_VALUE;
            moveTime = Integer.MAX_VALUE;
        }

        engine.setDepth(depth - 1);
        engine.setMoveTime(moveTime);
        brain.startThinking(infinite);
    }


    /**
     * Reset the time manager to its default values.
     *
     * @param manager       Time manager instance
     */
    private void resetTimeManager(TimeManager manager) {
        long moveTime = Engine.DEFAULT_MOVETIME;

        manager.setTimeLeft(SOUTH, 0);
        manager.setTimeLeft(NORTH, 0);
        manager.setTimeIncrement(SOUTH, 0);
        manager.setTimeIncrement(NORTH, 0);
        manager.setFixedTimeActive(false);
        manager.setMoveTime(moveTime);
        manager.setMovesLeft(0);
    }
}
