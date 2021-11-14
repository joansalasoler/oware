package com.joansala.uci;

/*
 * Copyright (c) 2014-2021 Joan Sala Soler <contact@joansala.com>
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


/**
 * UCI commands and responses.
 */
public class UCI {

    // -------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------

    public static final String DEBUG =          "debug";
    public static final String GO =             "go";
    public static final String ISREADY =        "isready";
    public static final String PONDERHIT =      "ponderhit";
    public static final String POSITION =       "position";
    public static final String QUIT =           "quit";
    public static final String REGISTER =       "register";
    public static final String SETOPTION =      "setoption";
    public static final String STOP =           "stop";
    public static final String UCI =            "uci";
    public static final String UCINEWGAME =     "ucinewgame";

    // -------------------------------------------------------------------
    // Responses
    // -------------------------------------------------------------------

    public static final String BESTMOVE =       "bestmove";
    public static final String COPYPROTECTION = "copyprotection";
    public static final String ID =             "id";
    public static final String INFO =           "info";
    public static final String OPTION =         "option";
    public static final String READYOK =        "readyok";
    public static final String REGISTRATION =   "registration";
    public static final String UCIOK =          "uciok";

    // -------------------------------------------------------------------
    // Parameters
    // -------------------------------------------------------------------

    public static final String AUTHOR =         "author";
    public static final String CENTIPAWNS =     "cp";
    public static final String DEFAULT =        "default";
    public static final String DEPTH =          "depth";
    public static final String FEN =            "fen";
    public static final String MAX =            "max";
    public static final String MIN =            "min";
    public static final String MOVES =          "moves";
    public static final String MOVETIME =       "movetime";
    public static final String NAME =           "name";
    public static final String SCORE =          "score";
    public static final String TYPE =           "type";
    public static final String VALUE =          "value";
    public static final String VAR =            "var";

    // -------------------------------------------------------------------
    // Option types
    // -------------------------------------------------------------------

    public static final String BUTTON =         "button";
    public static final String CHECK =          "check";
    public static final String COMBO =          "combo";
    public static final String SPIN =           "spin";
    public static final String STRING =         "string";

    // -------------------------------------------------------------------
    // Keywords
    // -------------------------------------------------------------------

    public static final String FALSE =          "false";
    public static final String INFINITE =       "infinite";
    public static final String LOWERBOUND =     "lowerbound";
    public static final String NULLMOVE =       "0000";
    public static final String OFF =            "off";
    public static final String ON =             "on";
    public static final String PONDER =         "ponder";
    public static final String STARTPOS =       "startpos";
    public static final String TRUE =           "true";
    public static final String UPPERBOUND =     "upperbound";
    public static final String VARIATION =      "pv";

    // -------------------------------------------------------------------
    // Predefined options
    // -------------------------------------------------------------------

    public static final String DRAW_SEARCH =    "DrawSearch";
    public static final String HASH_SIZE =      "Hash";
    public static final String USE_CACHE =      "UseCache";
    public static final String USE_LEAVES =     "UseLeaves";
    public static final String USE_PONDER =     "Ponder";
    public static final String USE_ROOTS =      "OwnBook";

}
