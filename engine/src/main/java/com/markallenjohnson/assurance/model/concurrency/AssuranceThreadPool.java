/*
 * Assurance
 *
 * Created by Mark Johnson
 *
 * Copyright (c) 2015 - 2023 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.markallenjohnson.assurance.model.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.stereotype.Component;

@Component("ThreadPool")
public class AssuranceThreadPool implements IAssuranceThreadPool {

	private static final Integer DEFAULT_NUMBER_OF_THREADS = 4;

	private Logger logger = LogManager.getLogger(AssuranceThreadPool.class);

	// Tracks every Future returned by submit() until await() drains them.
	// await() takes a snapshot of this list under the futures monitor, clears
	// it, then calls Future.get() on each one — which joins the worker thread
	// and surfaces any uncaught exception. If a worker submits more work
	// during its run, those entries appear in the list after the snapshot
	// is taken; await() loops back and drains them on the next pass.
	private final List<Future<?>> futures = new ArrayList<>();

	private ExecutorService threadPool = Executors.newFixedThreadPool(AssuranceThreadPool.DEFAULT_NUMBER_OF_THREADS);

    public void submit(Runnable task)
    {
    	Future<?> f = threadPool.submit(task);
    	synchronized (futures)
    	{
    		futures.add(f);
    	}
    }

    // register/unregister were part of the prior Phaser-based design. They
    // remain on the interface as no-ops so existing callers (test code that
    // pre-registered the main thread, worker classes that arriveAndDeregister
    // in their finally block) continue to compile and run. await() now relies
    // on the futures list rather than a phase counter, so neither side needs
    // to track party membership.
    public void register() { }

    public void unregister() { }

    public void await()
    {
    	while (true)
    	{
    		List<Future<?>> snapshot;
    		synchronized (futures)
    		{
    			if (futures.isEmpty())
    			{
    				logger.debug("All threads should be complete.");
    				return;
    			}
    			snapshot = new ArrayList<>(futures);
    			futures.clear();
    		}
    		for (Future<?> f : snapshot)
    		{
    			try
    			{
    				f.get();
    			}
    			catch (InterruptedException ex)
    			{
    				Thread.currentThread().interrupt();
    				return;
    			}
    			catch (ExecutionException ex)
    			{
    				logger.warn("Worker task failed", ex.getCause() != null ? ex.getCause() : ex);
    			}
    		}
    		// Loop: a worker may have submitted additional work via submit()
    		// during its run. Drain anything new before returning.
    	}
    }

    // NOTE: Spring recommends setter-based injection of properties, but this
    // particular case feels like it could be problematic.
    public void setNumberOfThreads(Integer numberOfThreads)
    {
    	if ((numberOfThreads == null) || (numberOfThreads < 2))
    	{
    		numberOfThreads = AssuranceThreadPool.DEFAULT_NUMBER_OF_THREADS;
    	}
    	this.threadPool = Executors.newFixedThreadPool(numberOfThreads);
    }

    public void shutdown()
    {
    	this.threadPool.shutdown();
    	try
    	{
    		if (!this.threadPool.awaitTermination(5, TimeUnit.SECONDS))
    		{
    			this.threadPool.shutdownNow();
    		}
    	}
    	catch (InterruptedException ex)
    	{
    		this.threadPool.shutdownNow();
    		Thread.currentThread().interrupt();
    	}
    	synchronized (futures)
    	{
    		futures.clear();
    	}
    }
}
