package com.joansala.tools.trainer;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.SortedSet;

import com.sleepycat.collections.StoredSortedKeySet;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.DatabaseException;

import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;


/**
 * Graph implementation that uses BerkeleyDB to store its nodes.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class BGraph {

    /** Name for the entity store */
    private static String storeName = "book";

    /** Database environment */
    private Environment env;

    /** Database entity store */
    private EntityStore store;

    /** Primary index for the store */
    private PrimaryIndex<Long, BNode> nodes;


    /**
     * Instantiates a new directed graph object.
     *
     * @param path          directory where the database is stored
     * @throws Exception    if the database cannot be accessed
     */
    public BGraph(String path) throws DatabaseException, FileNotFoundException {
        // Configure the environment

        EnvironmentConfig envConfig = new EnvironmentConfig();
        StoreConfig storeConfig = new StoreConfig();

        envConfig.setAllowCreate(true);
        envConfig.setTransactional(false);

        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(false);
        storeConfig.setDeferredWrite(true);

        // Open the environment and the entity store

        env = new Environment(new File(path), envConfig);
        store = new EntityStore(env, storeName, storeConfig);

        // Retrieve the primary index object

        nodes = store.getPrimaryIndex(Long.class, BNode.class);
    }


    /**
     * Returns the node object to which the specified identifier is
     * linked, or {@code null} if this graph does not contain a node
     * with such identifier.
     *
     * @param hash  node identifier
     * @return      a node object or {@code null}
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public BNode get(long hash) throws DatabaseException {
        BNode node = nodes.get(hash);

        if (node != null)
            node.setGraph(this);

        return node;
    }


    /**
     * Ensures this graph contains a node with the specified identifier.
     *
     * <p>If the graph already contained a node with the identifier that
     * node is returned, otherwise a new node object is added to the
     * graph and returned.</p>
     *
     * @param hash  node identifier
     * @return      a node object
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public BNode add(long hash) throws DatabaseException {
        BNode node = null;

        if (nodes.contains(hash)) {
            node = nodes.get(hash);
            node.setGraph(this);
        } else {
            node = new BNode();
            node.setGraph(this);
            node.setHash(hash);
            nodes.putNoReturn(node);
        }

        return node;
    }


    /**
     * Updates a node with a new value. Must be called each time a node
     * is modified to reflect the new values on the database.
     *
     * @param node  a node already stored on the database
     *
     * @throws IllegalArgumentException if the node was not already
     *          stored on the database
     * @throws DatabaseException  if a database failure occurs
     */
    public void update(BNode node) throws DatabaseException {
        long hash = node.getHash();

        if (nodes.contains(hash)) {
            nodes.putNoReturn(node);
        } else {
            throw new IllegalArgumentException(
                "Node not found on the database");
        }
    }


    /**
     * Returns a sorted view of the keys in this graph.
     *
     * @return  sorted view of the keys
     * @throws DatabaseException  if a database failure occurs
     */
    public SortedSet<Long> keys() throws DatabaseException {
        return new StoredSortedKeySet<>(
            nodes.getDatabase(), nodes.getKeyBinding(), false);
    }


    /**
     * Returns {@code true} if this graph contains a node with the
     * specified identifier.
     *
     * @param hash  node identifier
     * @return      {@code true} if the graph contains a node with
     *              the identifier, {@code false} otherwise
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public boolean contains(long hash) throws DatabaseException {
        return nodes.contains(hash);
    }


    /**
     * Returns the number of nodes stored in this graph.
     *
     * @return  number of nodes
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public long size() throws DatabaseException {
        return nodes.count();
    }


    /**
     * Flushes all the modification performed to the graph to the
     * physical storage.
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public void sync() throws DatabaseException {
        store.sync();
    }


    /**
     * Flushes all the modifications to the database and closes it
     *
     * @throws DatabaseException  if a database failure occurs
     */
    public void close() throws DatabaseException {
        store.close();
        env.close();
    }

}
