package com.joansala.uci.util;

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

import com.joansala.uci.UCIOption;
import com.joansala.uci.UCIService;
import static com.joansala.uci.UCI.*;


/**
 * A checkbox that can either be true or false.
 */
public class CheckOption extends UCIOption {

    /** Default option value */
    private boolean fallback;


    /**
     * Creates a new instance.
     *
     * @param fallback      Default value
     */
    public CheckOption(boolean fallback) {
        super(Type.CHECK_TYPE);
        this.fallback = fallback;
    }


    /**
     * Sets a new default value for this option.
     */
    protected void setFallback(boolean fallback) {
        this.fallback = fallback;
    }


    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, String token) {
        handle(service, parse(token));
    }


    /**
     * Handle this operation on a parsed value.
     */
    public void handle(UCIService service, boolean value) {
        // Optional operation
    }


    /**
     * Converts a token to a boolean value.
     */
    private boolean parse(String token) {
        if (!token.equals(TRUE) && !token.equals(FALSE)) {
            throw new IllegalArgumentException(
                "Invalid option value: " + token);
        }

        return token.equals(TRUE);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s %b", super.toString(),
            DEFAULT, fallback);
    }
}
