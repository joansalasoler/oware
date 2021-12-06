package com.joansala.game.oware;

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


import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import com.google.inject.Provides;

import com.joansala.cli.*;
import com.joansala.engine.*;
import com.joansala.engine.base.BaseModule;
import com.joansala.engine.base.BaseLeaves;
import com.joansala.engine.negamax.Negamax;
import com.joansala.book.base.BaseRoots;


/**
 * Binds together the components of the Oware engine.
 */
public class OwareModule extends BaseModule {

    /** Shared endgames book instance */
    private static Leaves<Game> leaves;

    /** Shared openings book instance */
    private static Roots<Game> roots;


    /**
     * Command line interface.
     */
    @Command(
      name = "oware",
      version = "2.0.0",
      description = "Oware (Abapa) is a mancala board game"
    )
    private static class OwareCommand extends MainCommand {

        @Option(names = "--roots", description = "Openings book path")
        private static String roots = OwareRoots.ROOTS_PATH;

        @Option(names = "--leaves", description = "Endgames book path")
        private static String leaves = OwareLeaves.LEAVES_PATH;
    }


    /**
     * Game module configuration.
     */
    @Override protected void configure() {
        bind(Game.class).to(OwareGame.class);
        bind(Board.class).to(OwareBoard.class);
        bind(Engine.class).to(Negamax.class);
        bind(Cache.class).to(OwareCache.class);
    }


    /**
     * Openings book provider.
     */
    @Provides @SuppressWarnings("rawtypes")
    public static Roots provideRoots() {
        if (roots instanceof Roots == false) {
            String path = OwareCommand.roots;

            try {
                roots = new OwareRoots(path);
            } catch (Exception e) {
                logger.warning("Cannot open openings book: " + path);
                roots = new BaseRoots();
            }
        }

        return roots;
    }


    /**
     * Endgames book provider.
     */
    @Provides @SuppressWarnings("rawtypes")
    public static Leaves provideLeaves() {
        if (leaves instanceof Leaves == false) {
            String path = OwareCommand.leaves;

            try {
                leaves = new OwareLeaves(path);
            } catch (Exception e) {
                logger.warning("Cannot open endgames book: " + path);
                leaves = new BaseLeaves();
            }
        }

        return leaves;
    }


    /**
     * Exectues the command line interface.
     *
     * @param args      Command line parameters
     */
    public static void main(String[] args) throws Exception {
        BaseModule module = new OwareModule();
        OwareCommand main = new OwareCommand();
        System.exit(main.execute(module, args));
    }
}
