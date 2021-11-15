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
import static com.joansala.uci.UCI.*;


/**
 * Instructs the service to start searching for a move.
 */
public class GoCommand implements UCICommand {

    /**
     * {@inheritDoc}
     */
    public String[] parameterNames() {
        return new String[] { DEPTH, MOVETIME, INFINITE, PONDER };
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
        long movetime = Engine.DEFAULT_MOVETIME;
        int depth = Engine.DEFAULT_DEPTH;
        boolean infinite = false;

        if (params.contains(DEPTH)) {
            String value = params.get(DEPTH);
            depth = Integer.parseInt(value);
        }

        if (params.contains(MOVETIME)) {
            String value = params.get(MOVETIME);
            movetime = Integer.parseInt(value);
        }

        if (params.contains(PONDER) ||
            params.contains(INFINITE)) {
            infinite = true;
            depth = Integer.MAX_VALUE;
            movetime = Integer.MAX_VALUE;
        }

        engine.setDepth(depth - 1);
        engine.setMoveTime(movetime);
        brain.startThinking(infinite);
    }
}
