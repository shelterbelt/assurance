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

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.springframework.stereotype.Component;

@Component("ThreadPool")
public class AssuranceThreadPool implements IAssuranceThreadPool {
	
	private static final Integer DEFAULT_NUMBER_OF_THREADS = 4;

	private Logger logger = LogManager.getLogger(AssuranceThreadPool.class);

	private Collection<Future<?>> futures = new LinkedList<>();
	
	private Phaser lock = new Phaser(0);
	
	private ExecutorService threadPool = Executors.newFixedThreadPool(AssuranceThreadPool.DEFAULT_NUMBER_OF_THREADS);

    public void submit(Runnable task)
    {
    	this.register();
    	this.futures.add(threadPool.submit(task));
    }
    
    public void register()
    {
    	this.lock.register();
    }
    
    public void unregister()
    {
    	this.lock.arriveAndDeregister();
    }
    
    public void await()
    {
    	if (lock.getRegisteredParties() > 0)
    	{
    		lock.arriveAndAwaitAdvance();
    	}
    	else
    	{
        	logger.debug("No threads are registered.");
    	}
    	logger.debug("All threads should be complete.");
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
}
