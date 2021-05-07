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

import picocli.CommandLine;
import picocli.CommandLine.*;


/**
 * Parent of all the command line tools.
 */
@Command(
  name = "aalina",
  version = "1.2.1",
  description = "Aalina is an Oware game engine",
  mixinStandardHelpOptions = true,
  subcommands = {
      BenchCommand.class,
      MatchCommand.class,
      PerftCommand.class,
      ServiceCommand.class,
      ShellCommand.class
  }
)
public final class MainCommand {
    public static void main(String[] args) throws Exception {
        CommandFactory factory = new CommandFactory();
        CommandLine c = new CommandLine(MainCommand.class, factory);
        System.exit(c.execute(args));
    }
}
