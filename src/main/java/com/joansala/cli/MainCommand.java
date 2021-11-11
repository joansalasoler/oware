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
import com.joansala.engine.base.BaseModule;
import com.joansala.cli.util.CommandFactory;
import com.joansala.cli.book.BookCommand;
import com.joansala.cli.test.TestCommand;


/**
 * Parent of all the command line tools.
 */
@Command(
  name = "main",
  mixinStandardHelpOptions = true,
  subcommands = {
      BookCommand.class,
      MatchCommand.class,
      ServiceCommand.class,
      ShellCommand.class,
      TestCommand.class
  }
)
public class MainCommand {

    /** Current module in execution */
    private static BaseModule module;


    /**
     * Current module in execution.
     */
    public static final BaseModule getCurrentModule() {
        return module;
    }


    /**
     * Execute a command line interface for a module.
     *
     * @param module        Game module
     * @param args          Command line arguments
     * @return              Exit code
     */
    public int execute(BaseModule module, String[] args) {
        MainCommand.module = module;
        CommandFactory factory = new CommandFactory(module);
        CommandLine main = new CommandLine(this, factory);
        main.setCaseInsensitiveEnumValuesAllowed(true);
        return main.execute(args);
    }
}
