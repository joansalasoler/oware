package com.joansala.oware;

/*
 * Aalina oware engine.
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


import com.joansala.engine.*;
import com.joansala.engine.negamax.Negamax;
import com.google.inject.AbstractModule;


/**
 * Binds together the components of the Oware engine.
 */
public class OwareModule extends AbstractModule {
    @Override protected void configure() {
        bind(Game.class).to(OwareGame.class);
        bind(Board.class).to(OwareBoard.class);
        bind(Engine.class).to(Negamax.class);
        bind(Cache.class).to(OwareCache.class);
        bind(Leaves.class).to(OwareLeaves.class);
        bind(Roots.class).to(OwareRoots.class);
    }
}
