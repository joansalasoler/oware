package com.joansala.engine.doe;

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
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import com.joansala.book.base.BookEntry;
import com.joansala.book.base.BookWriter;


/**
 * Exports a DOE database to a more compact opening book format.
 */
public class DOEExporter {

    /** Default book signature */
    private String signature = "DOE Opening Book 1.0";

    /** Opening book headers */
    private Map<String, String> headers;

    /** Store to export */
    private DOEStore store;


    /**
     * Creates a exporter for the given store.
     *
     * @param store     DOE database store
     */
    public DOEExporter(DOEStore store) {
        this.headers = new HashMap<>();
        this.store = store;
    }


    /**
     * Exports the database nodes to a file.
     *
     * @param path      File were to save the book
     * @return          Number of nodes exported
     */
    public long export(String path) throws IOException {
        BookWriter writer = new BookWriter(path);
        long count = 0L;

        writer.setHeaders(headers);
        writer.setSignature(signature);

        for (DOENode node : store.values()) {
            if (node.evaluated && node.parent != null) {
                writer.writeEntry(toBookEntry(node));
                count++;
            }
        }

        headers.put("Date", String.valueOf(new Date()));
        headers.put("Entries", String.valueOf(count));

        writer.save();
        writer.close();

        return count;
    }


    /**
     * Converts a DOE node to a book entry.
     *
     * @param node      Tree node
     * @return          Book entry
     */
    private BookEntry toBookEntry(DOENode node) {
        DOENode parent = store.read(node.parent);
        BookEntry entry = new BookEntry();

        entry.setHash(node.hash);
        entry.setMove(node.move);
        entry.setScore(node.score);
        entry.setCount(node.count);
        entry.setParent(parent.hash);

        return entry;
    }
}
