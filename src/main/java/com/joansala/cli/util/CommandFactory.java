package com.joansala.cli.util;

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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import picocli.CommandLine.IFactory;

import com.joansala.oware.OwareModule;


/**
 * Factory for instantiating the command line tools. This factory uses
 * Guice to inject the dependencies of the Picocli commands.
 */
public class CommandFactory implements IFactory {

    /** Module injector instance */
    private Injector injector;


    /**
     * Creates a new factory.
     */
    public CommandFactory() {
        injector = Guice.createInjector(new AbstractModule() {
            @Override protected void configure() {
                install(new OwareModule());
            }
        });
    }


    /**
     * Inject and instantiate the given type.
     *
     * @param type      Class type
     * @return          Class instance
     */
    @Override public <T> T create(Class<T> type) {
        return injector.getInstance(type);
    }
}
