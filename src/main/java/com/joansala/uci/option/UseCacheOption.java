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

import com.joansala.engine.Cache;
import com.joansala.engine.Game;
import com.joansala.engine.base.BaseCache;
import com.joansala.uci.UCIService;
import com.joansala.uci.util.CheckOption;


/**
 * If enabled the engine will use a transpositions table.
 */
public class UseCacheOption extends CheckOption {

    /** Transpositions table provided by the game module */
    private Cache<Game> cache;

    /** Whether the game module provides a transpositions table */
    private boolean enabled = false;


    /**
     * Creates a new option instance.
     */
    public UseCacheOption() {
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
        Cache<Game> cache = service.getCache();
        enabled = (cache instanceof BaseCache == false);
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, boolean value) {
        service.setCache(enabled ? cache : null);
        service.debug("Cache is now " + (enabled ? "enabled" : "disabled"));
    }
}
