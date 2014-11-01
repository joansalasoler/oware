package com.joansala.oware;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
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

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;

import com.joansala.engine.UCIClient;
import com.joansala.engine.UCIShell;


/**
 * Provides an interactive shell interface to an oware engine.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class OwareShell {
    
    
    /**
     * This class cannot be instantiated
     */
    private OwareShell() { }
    
    
    /**
     * Shows an usage notice on the standard output
     */
    private static void showUsage(Exception e) {
        System.out.format(
            "Exception:%n%n" +
            "  %s%n%n" +
            "Usage:%n%n" +
            "  UCIShell <command> [argument ...]%n%n",
            e.getMessage()
        );
    }
    
    
    /**
     * Command-line interface to the UCI protocol service.
     *
     * @param argv  Command line arguments
     */
    public static void main(String[] argv) throws Exception {
        OwareBoard board = new OwareBoard();
        OwareGame game = new OwareGame();
        
        try {
            ProcessBuilder builder = new ProcessBuilder(argv);
            Process service = builder.start();
            
            UCIClient client = new UCIClient(service, board, game);
            UCIShell shell = new UCIShell(client);
            
            shell.start();
        } catch (Exception e) {
            showUsage(new Exception(
                "Cannot start engine process."));
        }
    }
    
}
