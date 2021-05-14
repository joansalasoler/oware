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


import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 * Executes tasks on a fixed size thread pool.
 */
public class DOEExecutor {

    /** Controls the number of concurrent tasks */
    private final Semaphore semaphore;

    /** Thread executor service */
    private final ExecutorService executor;


    /**
     * Create a new executor instance.
     */
    public DOEExecutor() {
        this(2 * Runtime.getRuntime().availableProcessors());
    }


    /**
     * Create a new executor with a fixed thread pool size.
     *
     * @param poolSize      Number of threads
     */
    public DOEExecutor(int poolSize) {
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.semaphore = new Semaphore(poolSize);
    }


    /**
     * Submit a task to the executor.
     *
     * @param task      Runnable
     */
    public void submit(Runnable task) {
        try {
            semaphore.acquire();

            executor.submit(() -> {
                task.run();
                semaphore.release();
            });
        } catch (InterruptedException e) {
            semaphore.release();
        }
    }


    /**
     * Stops this executor service.
     */
    public void shutdown() {
        try {
            executor.shutdown();

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
