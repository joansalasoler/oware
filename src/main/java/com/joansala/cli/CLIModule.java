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


import java.lang.reflect.Constructor;
import com.google.inject.AbstractModule;
import com.google.inject.Module;

import com.joansala.oware.OwareModule;


/**
 * Command line module.
 */
public class CLIModule extends AbstractModule {

    /** Current engine module */
    private Module module;

    /** Engine process instance */
    private Process process;

    /** Installable game modules */
    private Class<?>[] MODULES = { OwareModule.class };


    /**
     * Create a new process for the given command.
     *
     * @param argv      Command line parameters
     */
    public CLIModule(String[] argv) throws Exception {
        module = newModule(MODULES[0]);

        if (argv.length > 0) {
            process = newProcess(argv[0]);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override protected void configure() {
        install(module);

        if (process != null) {
            bind(Process.class).toInstance(process);
        }
    }


    /**
     * Instantiate a game module given its class.
     *
     * @param type      Game module class
     * @return          Game module instance
     */
    private static Module newModule(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        return (Module) constructor.newInstance();
    }


    /**
     * Instantiate a new process given its command.
     *
     * @param command   Program and its parameters
     * @return          New process
     */
    private static Process newProcess(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command.split("\\s+"));
        return builder.start();
    }
}
