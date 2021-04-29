package com.joansala.cli;

/*
 * Aalina oware engine.
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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

import com.google.inject.Guice;
import com.google.inject.Injector;

import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.uci.UCIMatch;

/**
 * Command line interface to play against an engine.
 */
public final class CLIMatch {

    /** Turn of the human player */
    private static int turn = Game.SOUTH;

    /** Time per move of the engine */
    private static long movetime = Engine.DEFAULT_MOVETIME;


    /**
     * Run a match against an UCI service.
     */
    public static void main(String[] argv) throws Exception {
        CLIModule module = new CLIModule(argv);
        Injector injector = Guice.createInjector(module);
        UCIMatch match = injector.getInstance(UCIMatch.class);
        match.start(turn, movetime);
    }
}
