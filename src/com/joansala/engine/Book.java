package com.joansala.engine;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.TreeMap;


/**
 * Abstract book implementation. Initializes a book from a file and
 * provides methods for reading its information.
 *
 * <p>This book implementation skeleton reads the book headers from a file
 * stored on disk after verifying that the book format is correct. Therefore,
 * after initialising the book using the default constructor the database
 * offset is positioned just after the last header read</p>
 *
 * <p>The database header must have the following format:</p>
 *
 * <pre>
 * signature version\n
 * field_name_1: field_value_1\n
 * field_name_2: field_value_2\n
 * ...
 * field_name_N: field_value_N\n
 * \n
 * </pre>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class Book {
    
    /** Book version, as provided by the database headers */
    private String version = null;
    
    private String signature = null;
    
    /** Contains descriptive information of the database */
    private final TreeMap<String, String> headers;
    
    /** The database file */
    private final RandomAccessFile database;
    
    
    /**
     * Initializes a new {@code Book} object wich will read entries from
     * a file stored on disk.
     *
     * @param file      The database file for the book
     * @param signature Signature that identifies the book format
     *
     * @throws FileNotFoundException  If the file could not be opened
     * @throws IOException  If an I/O exception occurred
     */
    public Book(File file, String signature) throws IOException {
        this.signature = signature;
        this.database = new RandomAccessFile(file, "r");
        this.headers = readHeaders();
    }
    
    
    /**
     * Returns the value for the database book format version.
     *
     * @return  Version of the book format or {@code null}
     */
    public String getVersion() {
        return version;
    }
    
    
    /**
     * Returns the random access file object associated with this book.
     */
    public RandomAccessFile getDatabase() {
        return database;
    }
    
    
    /**
     * Returns the value of the specified header field.
     *
     * @param name  Field name
     * @return      Value associated to the field name or {@code null}
     *              if the field doesn't exist
     */
    public String getField(String name) {
        return headers.get(name);
    }
    
    
    /**
     * Returns an iterator over the field names found in the book's
     * header information.
     *
     * @return  An iterator over the header fields
     */
    public Iterator<String> fields() {
        final Iterator<String> iter = headers.keySet().iterator();
        
        return new Iterator<String>() {
            
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
            public boolean hasNext() {
                return iter.hasNext();
            }
            
            public String next() {
                return iter.next();
            }
            
        };
    }
    
    
    /**
     * Reads the header information of the database.
     *
     * @return  Set containing all the header fields
     * @throws IOException  If the header is not valid
     */
    private TreeMap<String, String> readHeaders() throws IOException {
        TreeMap<String, String> headers = new TreeMap<String, String>();
        
        // Read header signature for the book
        
        for (int i = 0; i < signature.length(); i++) {
            if (database.readChar() != signature.charAt(i))
                throw new IOException("Invalid header signature");
        }
        
        // Read book format version information
        
        StringBuilder version = new StringBuilder();
        char character = database.readChar();
        
        while (character != '\n') {
            version.append(character);
            character = database.readChar();
        }
        
        if (version.length() > 0)
            this.version = version.toString();
        
        // Read all header fields until an empty line is found
        
        StringBuilder field = null;
        
        do {
            field = new StringBuilder();
            character = database.readChar();
            
            while (character != '\n') {
                field.append(character);
                character = database.readChar();
            }
            
            if (field.length() > 0) {
                int index = field.indexOf(":");
                
                if (index != -1) {
                    String name = field.substring(0, index).trim();
                    String value = field.substring(
                        index + 1, field.length()).trim();
                    headers.put(name, value);
                }
            }
        } while (field.length() > 0);
        
        return headers;
    }
    
    
    /**
     * Destructor for this {@code Book} instance.
     */
    @Override
    protected void finalize() throws Throwable {
        database.close();
        super.finalize();
    }

}

