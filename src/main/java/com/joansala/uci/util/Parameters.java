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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.StringJoiner;
import java.util.Scanner;


/**
 * Matches a string against a set of keywords.
 */
public class Parameters {

    /** */
    private String params;


    /**
     * Creates a new instance.
     */
    public Parameters(String params) {
        this.params = params;
    }


    /**
     * {@inheritDoc}
     */
    public Map<String, String> match(String... keywords) {
        Map<String, String> matches = new LinkedHashMap<>();
        Pattern pattern = compilePattern(keywords);
        Scanner scanner = new Scanner(params);

        while (scanner.hasNext()) {
            boolean isKeyword = scanner.hasNext(pattern);
            String keyword = scanner.next();

            if (isKeyword == true) {
                String value = findValue(scanner, pattern);
                matches.put(keyword, value);
            }
        }

        scanner.close();

        return matches;
    }


    /**
     * Scan all the tokens while a pattern does not match. Returns
     * the matched tokens concatenated with spaces.
     */
    private String findValue(Scanner scanner, Pattern pattern) {
        StringJoiner tokens = new StringJoiner(" ");

        while (scanner.hasNext() && !scanner.hasNext(pattern)) {
            tokens.add(scanner.next());
        }

        return tokens.toString();
    }


    /**
     * Compiles a pattern that matches any of the given keywords.
     */
    private Pattern compilePattern(String[] keywords) {
        StringJoiner values = new StringJoiner("|");

        for (String keyword : keywords) {
            values.add(Pattern.quote(keyword));
        }

        return Pattern.compile(values.toString());
    }
}
