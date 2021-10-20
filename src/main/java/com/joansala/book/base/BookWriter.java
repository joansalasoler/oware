package com.joansala.book.base;

/*
 * Copyright (c) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;


/**
 * Writes a game book to a file.
 */
public class BookWriter implements Closeable {

    /** Book format signature */
    private String signature = "Aalina Book 1.0";

    /** Additional book information */
    private Map<String, String> headers;

    /** Book entries */
    private TreeSet<BookEntry> entries;

    /** File where the book is exported */
    private RandomAccessFile file;


    /**
     * Create a new writer for the given file path.
     *
     * @param path          Path to a file
     */
    public BookWriter(String path) throws IOException {
        file = new RandomAccessFile(path, "rw");
        headers = new HashMap<>();
        entries = new TreeSet<>();
    }


    /**
     * Sets the book signature identifier.
     *
     * @param signature     Signature string
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }


    /**
     * Sets the book headers as a map.
     *
     * @param headers       Book headers map
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }


    /**
     * Adds an entry to the book or overwrites it.
     *
     * @param entry     A book entry
     */
    public void writeEntry(BookEntry entry) {
        if (entries.contains(entry) == false) {
            entries.add(entry);
        } else {
            BookEntry e = entries.ceiling(entry);

            if (entry.getCount() > e.getCount()) {
                entries.remove(e);
                entries.add(entry);
            }
        }
    }


    /**
     * Writes this book data to its file.
     */
    public void save() throws IOException {
        file.setLength(0);
        this.writeSignature();
        this.writeHeaders();
        file.writeChar('\n');
        this.writeEntries();
    }


    /**
     * Writes the entries of the book to its file.
     */
    private void writeEntries() throws IOException {
        for (BookEntry entry : entries) {
            entry.writeData(file);
        }
    }


    /**
     * Writes the headers of the book to its file.
     */
    private void writeHeaders() throws IOException {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String line = String.format("%s: %s\n", key, value);
            file.write(line.getBytes("UTF-8"));
        }
    }


    /**
     * Writes the signature of the book to its file.
     */
    private void writeSignature() throws IOException {
        String line = String.format("%s\n", signature);
        file.write(line.getBytes("UTF-8"));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        file.close();
    }
}
