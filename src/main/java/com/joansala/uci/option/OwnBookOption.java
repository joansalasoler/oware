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

import com.joansala.engine.Game;
import com.joansala.engine.Roots;
import com.joansala.book.base.BaseRoots;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.CheckOption;


/**
 * If enabled the service will use and openings book to select moves.
 */
public class OwnBookOption extends CheckOption {

    /** Openings book provided by the game module */
    private Roots<Game> roots;

    /** Whether the game module provides an openings book */
    private boolean enabled = false;


    /**
     * Creates a new option instance.
     */
    public OwnBookOption() {
        super(true);
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() {
        return enabled;
    }


    /**
     * {@inheritDoc}
     */
    public void initialize(UCIService service) {
        this.roots = service.getRoots();
        this.enabled = (roots instanceof BaseRoots == false);
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, boolean enabled) {
        service.setRoots(enabled ? roots : null);
        service.debug("Roots are now " + (enabled ? "enabled" : "disabled"));
    }
}
