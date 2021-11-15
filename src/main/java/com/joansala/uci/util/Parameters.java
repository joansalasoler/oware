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

    /** Map of matched parameters */
    private Map<String, String> params;


    /**
     * Creates a new instance.
     *
     * @param args      Arguments string
     * @param names     Valid parameter names
     */
    public Parameters(String args, String[] names) {
        this.params = match(args, names);
    }


    /**
     * Check if a value with the given name exists.
     */
    public boolean contains(String name) {
        return params.containsKey(name);
    }


    /**
     * Value for a name or {@code null} if not present.
     */
    public String get(String name) {
        return params.get(name);
    }


    /**
     * {@inheritDoc}
     */
    private static Map<String, String> match(String args, String[] keywords) {
        Map<String, String> params = new LinkedHashMap<>();
        Pattern pattern = compilePattern(keywords);
        Scanner scanner = new Scanner(args);

        while (scanner.hasNext()) {
            boolean isKeyword = scanner.hasNext(pattern);
            String keyword = scanner.next();

            if (isKeyword == true) {
                String value = findValue(scanner, pattern);
                params.put(keyword, value);
            }
        }

        scanner.close();

        return params;
    }


    /**
     * Scan all the tokens while a pattern does not match. Returns
     * the matched tokens concatenated with spaces.
     */
    private static String findValue(Scanner scanner, Pattern pattern) {
        StringJoiner tokens = new StringJoiner(" ");

        while (scanner.hasNext() && !scanner.hasNext(pattern)) {
            tokens.add(scanner.next());
        }

        return tokens.toString();
    }


    /**
     * Compiles a pattern that matches any of the given keywords.
     */
    private static Pattern compilePattern(String[] keywords) {
        StringJoiner values = new StringJoiner("|");

        for (String keyword : keywords) {
            values.add(Pattern.quote(keyword));
        }

        return Pattern.compile(values.toString());
    }
}
