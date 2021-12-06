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
 * A text field that has a string as a value.
 */
public class StringOption extends UCIOption {

    /** Default option value */
    private String fallback;


    /**
     * Creates a new instance.
     *
     * @param fallback      Default value
     */
    public StringOption(String fallback) {
        super(Type.STRING_TYPE);
        this.fallback = fallback;
    }


    /**
     * Sets a new default value for this option.
     */
    protected void setFallback(String fallback) {
        this.fallback = fallback;
    }


    /**
     * {@inheritDoc}
     */
    public void accept(UCIService service, String token) {
        handle(service, token.trim());
    }


    /**
     * Handle this operation on a parsed value.
     */
    public void handle(UCIService service, String value) {
        // Optional operation
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s %s", super.toString(),
            DEFAULT, fallback);
    }
}
