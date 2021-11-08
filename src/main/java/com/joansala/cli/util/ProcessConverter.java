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

import java.nio.file.Paths;
import picocli.CommandLine.ITypeConverter;
import com.joansala.cli.MainCommand;
import com.joansala.engine.base.BaseModule;


/**
 * Convert a command string to a process instance.
 */
public class ProcessConverter implements ITypeConverter<Process> {

    /** Default command string */
    public static final String DEFAULT = "<default>";


    /**
     * Builds a command for the default module service.
     *
     * @return          Command descriptor
     */
    private String[] getDefaultCommand() {
        BaseModule module = MainCommand.getCurrentModule();

        String home = System.getProperty("java.home");
        String path = System.getProperty("java.class.path");
        String bin = Paths.get(home, "/bin", "/java").toString();

        String[] command = { bin, "-cp", path };
        String[] params = module.getServiceParameters();

        String[] result = new String[command.length + params.length];
        System.arraycopy(command, 0, result, 0, command.length);
        System.arraycopy(params, 0, result, command.length, params.length);

        return result;
    }


    /**
     * Builds a command descriptor given its path.
     *
     * @return          Command descriptor
     */
    private String[] getCommandFromPath(String path) {
        return DEFAULT.equals(path) ?
            getDefaultCommand() : path.split("\\s+");
    }


    /**
     * Convert a command path to a process.
     *
     * @param path      Program and its parameters
     * @return          A new process
     */
    @Override public Process convert(String path) throws Exception {
        String[] command = getCommandFromPath(path);
        ProcessBuilder builder = new ProcessBuilder(command);

        return builder.start();
    }
}
