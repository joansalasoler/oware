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

import com.joansala.uci.UCIService;


/**
 * Executes the Universal Chess Interface service.
 */
@Command(
  name = "service",
  description = "Starts the engine in UCI mode",
  mixinStandardHelpOptions = true
)
public class ServiceCommand implements Callable<Integer> {

    /** Injected UCI service instance */
    private UCIService service;


    /**
     * Creates a new service.
     */
    @Inject public ServiceCommand(UCIService service) {
        this.service = service;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        service.start();
        return 0;
    }
}
