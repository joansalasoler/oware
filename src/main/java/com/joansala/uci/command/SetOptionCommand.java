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
import com.joansala.uci.UCICommand;
import com.joansala.uci.UCIOption;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.Parameters;
import static com.joansala.uci.UCI.*;


/**
 * Sets the specified option to the given value.
 */
public class SetOptionCommand implements UCICommand {

    /**
     * {@inheritDoc}
     */
    public String[] parameterNames() {
        return new String[] { NAME, VALUE };
    }


    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, Parameters params) {
        if (service.isReady() == false) {
            throw new IllegalStateException(
                "Engine is not ready");
        }

        Map<String, UCIOption> options = service.getOptions();
        UCIOption option = options.get(params.get(NAME));
        option.accept(service, params.get(VALUE));
    }
}
