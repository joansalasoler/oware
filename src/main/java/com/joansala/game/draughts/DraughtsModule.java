package com.joansala.game.draughts;

/*
 * Aalina engine.
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


import com.google.inject.Module;
import com.google.inject.AbstractModule;
import picocli.CommandLine.Command;

import com.joansala.cli.*;
import com.joansala.engine.*;
import com.joansala.engine.mcts.Montecarlo;


/**
 * Binds together the components of the Draughts engine.
 */
public class DraughtsModule extends AbstractModule {

    /**
     * Command line interface.
     */
    @Command(
      name = "draughts",
      version = "1.0.0",
      description = "An international draughts game engine"
    )
    private static class DraughtsCommand extends MainCommand {}


    /**
     * Game module configuration.
     */
    @Override protected void configure() {
        bind(Game.class).to(DraughtsGame.class);
        bind(Board.class).to(DraughtsBoard.class);
        bind(Engine.class).to(Montecarlo.class);
    }


    /**
     * Exectues the command line interface.
     *
     * @param args      Command line parameters
     */
    public static void main(String[] args) throws Exception {
        Module module = new DraughtsModule();
        DraughtsCommand main = new DraughtsCommand();
        System.exit(main.execute(module, args));
    }
}
