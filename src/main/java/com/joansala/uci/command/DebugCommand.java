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

import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.Parameters;
import static com.joansala.uci.UCI.*;


/**
 * Switches the service debug mode on and off.
 */
public class DebugCommand implements UCICommand {

    /**
     * {@inheritDoc}
     */
    public String[] parameterNames() {
        return new String[] { ON, OFF };
    }


    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, Parameters params) {
        boolean active = (false == service.getDebug());
        active = params.contains(ON) ? true : active;
        active = params.contains(OFF) ? false : active;
        service.setDebug(active);
    }
}
