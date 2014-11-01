package com.joansala.xo;

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

import com.joansala.engine.Negamax;
import com.joansala.engine.UCIService;


/**
 * Universal Chess Interface service for tic-tac-toe.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class XOService {
    
    
    /**
     * This class cannot be instantiated.
     */
    private XOService() { }
    
    
    /**
     * Reads commands from the standard input and writes replies to the
     * standard output.
     *
     * @param argv  Command line arguments
     */
    public static void main(String[] argv) {
        XOBoard board = new XOBoard();
        XOGame game = new XOGame();
        Negamax engine = new Negamax();
        UCIService service = new UCIService(board, game, engine);
        
        engine.setInfinity(XOGame.MAX_SCORE);
        service.start();
    }
    
}

