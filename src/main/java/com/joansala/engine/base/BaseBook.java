package com.joansala.engine.base;

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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;


/**
 * Base book implementation.
 */
public class BaseBook implements AutoCloseable {

    /** Book format signature */
    private final String signature;

    /** Additional book information */
    private final Map<String, String> headers;

    /** File from where the book is read */
    protected final RandomAccessFile file;

    /** File offset of the book entries */
    protected final long offset;


    /**
     * Open a book for the given file path.
     */
     public BaseBook(String path) throws IOException {
         file = new RandomAccessFile(path, "rw");
         signature = readSignature();
         headers = readHeaders();
         offset = file.getFilePointer();
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
     * Reads the book signature and returns it.
     *
     * @return          Signature string
     */
    protected String readSignature() throws IOException {
        return file.readLine();
    }


    /**
     * Reads the book headers and returns them.
     *
     * @return          New book headers map
     */
    protected Map<String, String> readHeaders() throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line = file.readLine();

        while (line != null && !line.trim().isEmpty()) {
            String[] parts = line.split("[:]", 2);
            headers.put(parts[0], parts[1]);
            line = file.readLine();
        }

        return headers;
    }


    /**
     * {@inheritDoc}
     */
    @Override public void close() throws IOException {
        file.close();
    }
}
