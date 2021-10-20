package com.joansala.book.base;

/*
 * Copyright (C) 2021 Joan Sala Soler <contact@joansala.com>
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import static com.joansala.engine.Game.*;


/**
 * A book entry.
 */
public class BookEntry implements Comparable<BookEntry> {

    /** Size of each entry in bytes */
    static final int ENTRY_SIZE = 36;

    /** Parent state hash */
    private long parent = 0x00L;

    /** Game state hash */
    private long hash = 0x00L;

    /** Move that lead to the game state */
    private int move = NULL_MOVE;

    /** Average score of the game state */
    private double score = 0.0;

    /** How many scores were averaged */
    private long count = 1L;


    /**
     * Obtains the hash of the game state parent.
     */
    public long getParent() {
        return parent;
    }


    /**
     * Obtains the hash of the game state.
     */
    public long getHash() {
        return hash;
    }


    /**
     * Obtains the move that lead to the game state.
     */
    public int getMove() {
        return move;
    }


    /**
     * Obtains the average score of the game state.
     */
    public double getScore() {
        return score;
    }


    /**
     * Obtains how many scores were averaged.
     */
    public long getCount() {
        return count;
    }


    /**
     * Sets the hash of the game state parent.
     */
    public void setParent(long hash) {
        this.parent = hash;
    }


    /**
     * Sets the hash of the game state.
     */
    public void setHash(long hash) {
        this.hash = hash;
    }


    /**
     * Sets the move that lead to the game state.
     */
    public void setMove(int move) {
        this.move = move;
    }


    /**
     * Sets the average score of the game state.
     */
    public void setScore(double score) {
        this.score = score;
    }


    /**
     * Sets how many scores were averaged.
     */
    public void setCount(long count) {
        this.count = count;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(hash, parent);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(BookEntry o) {
        int value = Long.compare(parent, o.parent);
        return value == 0 ? Long.compare(hash, o.hash) : value;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof BookEntry == false) {
            return false;
        }

        BookEntry e = (BookEntry) o;

        return Objects.equals(parent, e.parent) &&
               Objects.equals(hash, e.hash);
    }


    /**
     * Reads this book entry data from a binary stream.
     *
     * @param input     Binary stream
     * @return          Book entry
     */
    void readData(DataInput input) throws IOException {
        parent = input.readLong();
        hash = input.readLong();
        move = input.readInt();
        score = input.readDouble();
        count = input.readLong();
    }


    /**
     * Writes this book entry to a binary stream.
     *
     * @param output    Binary stream
     */
    void writeData(DataOutput output) throws IOException {
        output.writeLong(parent);
        output.writeLong(hash);
        output.writeInt(move);
        output.writeDouble(score);
        output.writeLong(count);
    }
}
