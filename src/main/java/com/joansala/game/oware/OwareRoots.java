package com.joansala.game.oware;

/*
 * Aalina oware engine.
 * Copyright (C) 2014-2024 Joan Sala Soler <contact@joansala.com>
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

import java.io.IOException;
import com.joansala.book.uct.UCTRoots;
import static com.joansala.game.oware.Oware.*;
import static com.joansala.game.oware.OwareGame.*;


/**
 * Opening book implementation for oware.
 */
public class OwareRoots extends UCTRoots {

    /** Default path of the book database */
    public static final String ROOTS_PATH = "/oware-roots.bin";


    /**
     * Creates an opening book instance.
     */
    public OwareRoots() throws IOException {
        this(ROOTS_PATH);
    }


    /**
     * Creates an opening book instance for the given database.
     *
     * @param path      Database path
     */
    public OwareRoots(String path) throws IOException {
        super(getResourcePath(path));
        setDisturbance(ROOT_DISTURBANCE);
        setThreshold(ROOT_THRESHOLD);
        setInfinity(MAX_SCORE);
    }


    /**
     * Obtain a path to the given resource file.
     */
    private static String getResourcePath(String path) {
        return OwareRoots.class.getResource(path).getFile();
    }
}
