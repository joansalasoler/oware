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


import java.io.File;
import java.util.Collection;
import com.sleepycat.je.*;
import com.sleepycat.persist.*;


/**
 * Store and retrieve nodes from a database.
 */
public class DOEStore implements AutoCloseable {

    /** Database entity store */
    private final EntityStore store;

    /** Database environment */
    private final Environment environment;

    /** Primary index for the store */
    private final PrimaryIndex<Long, DOENode> nodes;

    /** Current database transaction */
    private Transaction transaction;


    /**
     * Create a new store instance.
     */
    public DOEStore(String path) throws DatabaseException {
        File home = getHomeFolder(path);
        environment = openEnvironment(home);
        store = openStore(environment);
        nodes = store.getPrimaryIndex(Long.class, DOENode.class);
        transaction = newTransaction();
    }


    /**
     * Creates a database environment on the given folder.
     */
    private Environment openEnvironment(File home) throws DatabaseException {
        EnvironmentConfig config = new EnvironmentConfig();
        config.setTransactional(true);
        config.setAllowCreate(true);

        return new Environment(home, config);
    }


    /**
     * Opens the entity store on the given environment
     */
    private EntityStore openStore(Environment env) throws DatabaseException {
        StoreConfig config = new StoreConfig();
        config.setTransactional(true);
        config.setAllowCreate(true);

        return new EntityStore(env, "graph", config);
    }


    /**
     * Get the environment's home folder.
     */
    private File getHomeFolder(String path) {
        File home = new File(path);

        if (home.exists() == false){
            home.mkdirs();
        }

        return home;
    }


    /**
     * Creates a new database transaction.
     */
    private Transaction newTransaction() throws DatabaseException {
        return environment.beginTransaction(null, null);
    }


    /**
     * Handle a database exception.
     */
    private void handleException(DatabaseException e) {
        throw new RuntimeException(e);
    }


    /**
     * Collection view of the nodes on this store.
     */
    public Collection<DOENode> values() {
        return nodes.map().values();
    }


    /**
     * Check if the store contains a node.
     */
    public boolean contains(Long id) {
        try {
            return nodes.contains(id);
        } catch (DatabaseException e) {
            handleException(e);
        }

        return false;
    }


    /**
     * Get a node from the store.
     */
    public DOENode read(Long id) {
        try {
            if (id != null) {
                return nodes.get(id);
            }
        } catch (DatabaseException e) {
            handleException(e);
        }

        return null;
    }


    /**
     * Save a node to the store.
     */
    public void write(DOENode node) {
        try {
            nodes.putNoReturn(node);
        } catch (DatabaseException e) {
            handleException(e);
        }
    }


    /**
     * Commit current transaction.
     */
    public void commit() {
        try {
            transaction.commitSync();
            transaction = newTransaction();
        } catch (DatabaseException e) {
            handleException(e);
        }
    }


    /**
     * Rollback current transaction.
     */
    public void rollback() {
        try {
            transaction.abort();
            transaction = newTransaction();
        } catch (DatabaseException e) {
            handleException(e);
        }
    }


    /**
     * Close this store.
     */
    public void close() {
        try {
            transaction.abort();
            store.close();
            environment.close();
        } catch (DatabaseException e) {
            handleException(e);
        }
    }


    /**
     * Number of stored nodes.
     */
    public long count() {
        try {
            return nodes.count();
        } catch (DatabaseException e) {
            handleException(e);
        }

        return 0;
    }
}
