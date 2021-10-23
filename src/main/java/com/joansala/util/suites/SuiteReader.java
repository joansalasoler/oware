package com.joansala.util.suites;

import java.io.InputStream;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.InputStreamReader;


/**
 * Reads game suites from an input stream.
 *
 * Each suite is a string of the form {@code <diagram> [moves <notation>]},
 * where {@code diagram} represents a board and {@code notation} a sequence
 * of moves performed on that board position. Blank lines or lines starting
 * with the character {@code #} are ignored.
 */
public class SuiteReader implements AutoCloseable {

    /** Board diagram index */
    private static int DIAGRAM_INDEX = 0;

    /** Moves notation index */
    private static int NOTATION_INDEX = 1;

    /** An empty string */
    private static String EMPTY_STRING = "";

    /** Input stream reader */
    private InputStreamReader input;

    /** Buffered reader */
    private BufferedReader reader;


    /**
     * Reads game suites from an input stream.
     */
    public SuiteReader(InputStream source) {
        input = new InputStreamReader(source);
        reader = new BufferedReader(input);
    }


    /**
     * Returns a stream of game suites.
     *
     * @return      A stream
     */
    public Stream<Suite> stream() {
        return reader.lines().filter(this::filter).map(this::map);
    }


    /**
     * Converts a line of text to a {@code Suite}.
     *
     * @param line      A string
     * @return          A new {@code Suite} instance
     */
    private Suite map(String line) {
        String[] parts = split(line);
        String diagram = value(parts, DIAGRAM_INDEX);
        String notation = value(parts, NOTATION_INDEX);

        return new Suite(diagram, notation);
    }


    /**
     * Returns if a string is blank or a comment.
     *
     * @param line      A string
     * @return          If string must be filtered
     */
    private boolean filter(String line) {
        return !line.isBlank() && !line.trim().startsWith("#");
    }


    /**
     * Returns the value at index or an empty string.
     *
     * @param parts     String array
     * @return          A string
     */
    private String value(String[] parts, int index) {
        return parts.length > index ? parts[index] : EMPTY_STRING;
    }


    /**
     * Split a suite text into its components.
     *
     * @param text      A string
     * @return          A string array
     */
    private String[] split(String text) {
        return text.trim().split("\\s+moves\\s+", 2);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        reader.close();
        input.close();
    }
}
