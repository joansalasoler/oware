package com.joansala.uci.option;

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
import com.joansala.engine.Game;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.CheckOption;


/**
 * If enabled the engine will have a strong preference for draws.
 */
public class DrawSearchOption extends CheckOption {

    /**
     * Creates a new option instance.
     */
    public DrawSearchOption() {
        super(false);
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, boolean active) {
        Game game = service.getGame();
        Engine engine = service.getEngine();
        engine.setContempt(active ? game.infinity() : game.contempt());
        service.debug("Contempt is now " + engine.getContempt());
    }
}
