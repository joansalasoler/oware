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
import com.google.inject.Injector;
import picocli.CommandLine.*;

import com.joansala.cli.util.EngineType;
import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import com.joansala.engine.Roots;
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

    /** Dependency injector */
    private Injector injector;


    @Option(
      names = "--engine",
      description = "Custom engine (${COMPLETION-CANDIDATES})"
    )
    private EngineType engineType = null;


    /**
     * Creates a new service.
     */
    @Inject public ServiceCommand(Injector injector) {
        this.injector = injector;
    }


    /**
     * {@inheritDoc}
     */
    @Override public Integer call() throws Exception {
        UCIService service = getServiceInstance(injector);
        service.start();
        return 0;
    }


    /**
     * Obtain an UCI service instance.
     */
    private UCIService getServiceInstance(Injector injector) {
        return (engineType instanceof EngineType == false) ?
            injector.getInstance(UCIService.class) :
            createService(injector, engineType);
    }


    /**
     * Creates a new UCI service for the given engine type.
     */
    private UCIService createService(Injector injector, EngineType engineType) {
        Game game = injector.getInstance(Game.class);
        Engine engine = injector.getInstance(engineType.getType());
        UCIService service = new UCIService(game, engine);

        try {
            Roots<?> roots = injector.getInstance(Roots.class);
            service.setRoots(roots);
        } catch (Exception e) {}

        return service;
    }
}
