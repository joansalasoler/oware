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

import com.joansala.engine.Game;
import com.joansala.uci.UCIClient;
import com.joansala.uci.UCIMatch;


/**
 * Command-line interface to play oware.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class OwareMatch {
    
    
    /**
     * This class cannot be instantiated.
     */
    private OwareMatch() { }
    
    
    /**
     * Shows an usage notice on the standard output
     */
    private static void showUsage(Exception e) {
        System.out.format(
            "Exception:%n%n" +
            "  %s%n%n" +
            "Usage:%n%n" +
            "  Match [parameters] <command> [argument ...]%n%n" +
            "Valid parameters are:%n%n" +
            "  -turn      <string>  (south/north)%n" +
            "  -movetime  <int>     (milliseconds)%n",
            e.getMessage()
        );
    }
    
    
    /**
     * Runs a match between an human player and an engine.
     *
     * @param argv  Command line arguments.
     */
    public static void main(String[] argv) {
        String[] command = null;
        int turn = Game.SOUTH;
        int movetime = 2000;
        
        OwareBoard board = new OwareBoard();
        OwareGame game = new OwareGame();
        
        // Parse command line arguments
        
        try {
            if (argv.length < 1) {
                throw new IllegalArgumentException(
                    "No engine command was specified");
            } else {
                int i = 0;
                
                for (i = 0; i < argv.length; i++) {
                    if ("-turn".equals(argv[i])) {
                        String s = argv[++i];
                        
                        if ("south".equals(s)) {
                            turn = Game.SOUTH;
                        } else if ("north".equals(s)) {
                            turn = Game.NORTH;
                        } else {
                            throw new IllegalArgumentException(
                                "Not a valid turn argument");
                        }
                    } else if ("-movetime".equals(argv[i])) {
                        movetime = Integer.parseInt(argv[++i]);
                        
                        if (movetime < 1) {
                            throw new IllegalArgumentException(
                                "Not a valid time per move argument");
                        }
                    } else {
                        break;
                    }
                }
                
                if (argv.length > i) {
                    int length = argv.length - i;
                    command = new String[length];
                    System.arraycopy(argv, i, command, 0, length);
                } else {
                    throw new IllegalArgumentException(
                        "No engine command was specified");
                }
            }
        } catch (Exception e) {
            showUsage(e);
            System.exit(1);
        }
        
        // Initialize the engine process and run the ui
        
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process service = builder.start();
            
            UCIClient client = new UCIClient(service, board, game);
            UCIMatch match = new UCIMatch(client, board, new OwareGame());
            
            match.start(turn, movetime);
        } catch (Exception e) {
            showUsage(new Exception(
                "Cannot start engine process."));
        }
    }
    
}

