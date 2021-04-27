package com.joansala.oware;

/*
 * Aalina oware engine.
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

import java.io.File;
import java.io.IOException;

import com.joansala.engine.negamax.Negamax;
import com.joansala.uci.UCIService;


/**
 * Universal Chess Interface service for the game of Oware. Reads UCI
 * commands from the standard input and writes replies to the standard
 * output.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class OwareService {

    /** Default number of seeds on board to probe for leaves */
    public static final int DEFAULT_LEAVES = 12;

    /** Default size of the hash table in megabytes */
    public static final int DEFAULT_HASH = 32;

    /** The default score to which draws are evaluated */
    public final static int DEFAULT_CONTEMPT = -9;


    /**
     * This class cannot be instantiated.
     */
    private OwareService() { }


    /**
     * Reads commands from the standard input and writes replies to the
     * standard output.
     *
     * @param argv  Command line arguments
     */
    public static void main(String[] argv) {
        // Opening book initialization

        OwareRoots roots = null;

        try {
            String bookPath = OwareService.class
                .getResource("/oware-book.bin").getFile();
            roots = new OwareRoots(new File(bookPath));
        } catch (Exception e) {
            System.err.println("Warning: Unable to open book file");
        }

        // Endgames book initialization

        OwareLeaves leaves = null;

        try {
            String leavesPath = OwareService.class
                .getResource("/oware-leaves.bin").getFile();
            leaves = new OwareLeaves(new File(leavesPath), DEFAULT_LEAVES);
        } catch (Exception e) {
            System.err.println("Warning: Unable to open endgames file");
        }

        // Initialize game objects

        OwareCache cache = new OwareCache(DEFAULT_HASH << 20);
        OwareBoard board = new OwareBoard();
        OwareGame game = new OwareGame();

        // Engine initialization

        Negamax engine = new Negamax();

        engine.setContempt(DEFAULT_CONTEMPT);
        engine.setInfinity(OwareGame.MAX_SCORE);
        engine.setLeaves(leaves);
        engine.setCache(cache);

        // Service initialization

        UCIService service = new UCIService(board, game, engine);

        service.setContempt(DEFAULT_CONTEMPT);
        service.setRoots(roots);
        service.setCache(cache);

        // Start the communication

        service.start();
    }

}
