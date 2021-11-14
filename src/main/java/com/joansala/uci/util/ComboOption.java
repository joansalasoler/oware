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

import java.util.Set;
import com.joansala.uci.UCIOption;
import com.joansala.uci.UCIService;
import static com.joansala.uci.UCI.*;


/**
 * A combo box that can have different predefined strings as a value.
 */
public class ComboOption extends UCIOption {

    /** Default option value */
    private String fallback;

    /** Predefined values */
    private Set<String> values;


    /**
     * Creates a new instance.
     *
     * @param values        Predefined choices
     * @param fallback      Default value
     */
    public ComboOption(Set<String> values, String fallback) {
        super(Type.STRING_TYPE);
        this.fallback = fallback;
        this.values = values;
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
        handle(service, parse(token));
    }


    /**
     * Handle this operation on a parsed value.
     */
    public void handle(UCIService service, String value) {
        // Optional operation
    }


    /**
     * Converts a token to a string value.
     */
    private String parse(String token) {
        String value = token.trim();

        if (!values.contains(value)) {
            throw new IllegalArgumentException(
                "Invalid option value: " + value);
        }

        return value;
    }


    /**
     * Predefined options as a string.
     */
    private String vars() {
        StringBuilder vars = new StringBuilder();

        for (String value : values) {
            vars.append(" var ");
            vars.append(value);
        }

        return vars.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s %s%s", super.toString(),
            DEFAULT, fallback, vars());
    }
}
