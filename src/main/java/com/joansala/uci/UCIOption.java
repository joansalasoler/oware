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

import java.util.function.BiConsumer;
import com.joansala.uci.UCIService;
import static com.joansala.uci.UCI.*;


/**
 * Abstract UCI option.
 */
public abstract class UCIOption implements BiConsumer<UCIService, String> {

    /** Option type */
    private final Type type;


    /**
     * Creates an option of the given type.
     */
    public UCIOption(Type type) {
        this.type = type;
    }


    /**
     * Checks if this option is available.
     */
    public boolean isEnabled() {
        return true;
    }


    /**
     * Type of this option.
     */
    public Type getType() {
        return type;
    }


    /**
     * Initializes the option.
     */
    public void initialize(UCIService service) {
        // Optional operation
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s %s", TYPE, getType());
    }


    /**
     * Predefined obtion types.
     */
    public static enum Type {
        CHECK_TYPE  (CHECK),
        SPIN_TYPE   (SPIN),
        COMBO_TYPE  (COMBO),
        BUTTON_TYPE (BUTTON),
        STRING_TYPE (STRING);

        /** Type name */
        private String name;


        /**
         * Creates a new named type.
         */
        private Type(String name) {
            this.name = name;
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name;
        }
    }
}
