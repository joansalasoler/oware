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

import java.util.concurrent.Callable;
import com.google.inject.Inject;
import picocli.CommandLine.*;

import com.joansala.uci.UCIClient;
import com.joansala.uci.UCIShell;


/**
 * Executes the Universal Chess Interface interpreter.
 */
@Command(
  name = "shell",
  description = "Runs an interactive UCI interpreter",
  mixinStandardHelpOptions = true
)
public class ShellCommand implements Callable<Integer> {

    /** Injected UCI interpreter instance */
    private UCIShell shell;

    /** UCI client instance */
    private UCIClient client;

    @Option(
      names = "--command",
      description = "Custom UCI engine command",
      converter = ProcessConverter.class,
      defaultValue = "<default>"
    )
    private Process service = null;


    /**
     * Creates a new service.
     */
    @Inject public ShellCommand(UCIShell shell) {
        this.client = shell.getUCIClient();
        this.shell = shell;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        client.setService(service);
        shell.start();
        return 0;
    }
}
