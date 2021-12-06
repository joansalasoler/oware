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
import com.joansala.uci.util.SpinOption;


/**
 * Resizes the engine cache according to the user preferences.
 */
public class HashOption extends SpinOption {

    /** Whether the engine uses a cache */
    private boolean enabled = false;


    /**
     * Creates a new option instance.
     */
    public HashOption() {
        super(1, 1, 1);
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
        if (enabled) initializeRangeOfValues(cache);
    }


    /**
     * {@inheritDoc}
     */
    public void handle(UCIService service, int value) {
        Cache<Game> cache = service.getCache();
        cache.resize(value << 20); // Size in bytes
        long size = cache.size() >> 20; // Size in megabytes
        service.debug("Cache size is now " + size + " MB");
    }


    /**
     * Intitialize the range of option values according to the
     * available memory on the virtual machine.
     *
     * @param cache     Engine cache
     */
    private void initializeRangeOfValues(Cache<Game> cache) {
        long free = Runtime.getRuntime().freeMemory();
        int current = (int) (cache.size() >> 20);
        int available = (int) (free >> 20);

        setMaximum(available + current);
        setFallback(current);
        setMinimum(1);
    }
}
