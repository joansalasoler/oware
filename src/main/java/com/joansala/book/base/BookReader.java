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

import static com.joansala.book.base.BookEntry.*;


/**
 * Reads a game book from a file.
 */
public class BookReader implements Closeable {

    /** Book format signature */
    private final String signature;

    /** Additional book information */
    private final Map<String, String> headers;

    /** File from where the book is read */
    protected final RandomAccessFile file;

    /** File offset of the book entries */
    protected final long offset;

    /** Number of book entries */
    protected final long size;


    /**
     * Open a book for the given file path.
     */
     public BookReader(String path) throws IOException {
         file = new RandomAccessFile(path, "rw");
         signature = readSignature();
         headers = readHeaders();
         offset = file.getFilePointer();
         size = (file.length() - offset) / ENTRY_SIZE;
     }


    /**
     * Book format signature identifier.
     *
     * @return          Signature string
     */
    public String getSignature() {
        return signature;
    }


    /**
     * Map view of the book headers.
     *
     * @return          Headers map
     */
    public Map<String, String> getHeaders() {
        return headers;
    }


    /**
     * Reads a book entry for the given game state.
     *
     * @param parent    Game state parent hash
     * @param child     Game state child hash
     * @return          Book entry or {@code null}
     */
    public BookEntry readEntry(long parent, long child) throws IOException {
        BookEntry entry = null;

        if (seekEntry(parent, child) == true) {
            entry = new BookEntry();
            entry.readData(file);
        }

        return entry;
    }


    /**
     * Reads the book headers and returns them.
     *
     * @return          New book headers map
     */
    private Map<String, String> readHeaders() throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line = file.readLine();

        while (line != null && !line.trim().isEmpty()) {
            String[] parts = line.split("[:]", 2);
            headers.put(parts[0].trim(), parts[1].trim());
            line = file.readLine();
        }

        return headers;
    }


    /**
     * Reads the book signature and returns it.
     *
     * @return          Signature string
     */
    private String readSignature() throws IOException {
        return file.readLine();
    }


    /**
     * Seeks a hash code on the random access file.
     *
     * @param parent    Parent node hash code
     * @param child     Child node hash code
     * @return          Whether the hash was found
     */
    private boolean seekEntry(long parent, long child) throws IOException {
        long ceiling = size - 1;
        long floor = 0;

        while (floor <= ceiling) {
            long middle = (floor + ceiling) / 2;
            long position = offset + middle * ENTRY_SIZE;

            file.seek(position);
            long p = file.readLong();
            long c = file.readLong();

            if (p == parent && c == child) {
                file.seek(position);
                return true;
            } else if (p == parent && c < child) {
                floor = middle + 1;
            } else if (p == parent && c > child) {
                ceiling = middle - 1;
            } else if (p < parent) {
                floor = middle + 1;
            } else if (p > parent) {
                ceiling = middle - 1;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        file.close();
    }
}
