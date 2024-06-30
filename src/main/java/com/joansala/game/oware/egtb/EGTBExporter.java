package com.joansala.game.oware.egtb;

/*
 * Copyright (C) 2023-2024 Joan Sala Soler <contact@joansala.com>
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
import java.util.Date;


/**
 * Exports an EGTB database to a more compact format.
 */
public class EGTBExporter {

    /** Default book signature */
    private String signature = "Oware Endgames 2.1";

    /** Store to export */
    private EGTBStore store;


    /**
     * Creates a exporter for the given store.
     *
     * @param store     EGTB database store
     */
    public EGTBExporter(EGTBStore store) {
        this.store = store;
    }


    /**
     * Exports the database nodes to a file.
     *
     * @param path      File were to save the book
     *
     * @return          Number of nodes exported
     */
    public long export(String path) throws IOException {
        RandomAccessFile file = new RandomAccessFile(path, "rw");
        String date = String.valueOf(new Date());
        int size = (int) store.count();
        byte[] data = new byte[1 + size];

        file.writeChars(String.format("%s\n", signature));
        file.writeChars(String.format("Date: %s\n", date));
        file.writeChars(String.format("Entries: %d\n", size));
        file.writeChar('\n');

        for (int hash = 1; hash < size; hash++) {
            EGTBNode node = store.read((long) hash);
            int score = (1 + node.seeds + node.score) / 2;
            data[(int) hash] = (byte) ((score << 2) | node.flag);
        }

        file.write(data);
        file.close();

        return (long) size;
    }
}
