/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.model.concurrency;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.RejectedExecutionException;

import org.junit.Test;

/**
 * Locks down the {@link AssuranceThreadPool#shutdown()} contract that
 * {@code EngineFacade.runScan} relies on for per-scan thread-pool
 * lifecycle.
 *
 * <p>The legacy Swing UI sidestepped lifecycle by rebuilding its Spring
 * context (and therefore its {@code @Component("ThreadPool")} bean) on every
 * scan. The IPC layer keeps a single context for the engine's lifetime, so
 * reusing one pool across scans accumulates {@link java.util.concurrent.Phaser}
 * state and the second {@link AssuranceThreadPool#await() await()} blocks
 * indefinitely. The fix allocates a fresh pool per run and shuts it down
 * in {@code finally}; these tests verify the shutdown half of the contract.
 */
public class AssuranceThreadPoolTests
{
	@Test
	public void shutdownRejectsFurtherSubmissions()
	{
		AssuranceThreadPool pool = new AssuranceThreadPool();
		pool.shutdown();
		try
		{
			pool.submit(() -> { });
			fail("submit() after shutdown() should throw RejectedExecutionException");
		}
		catch (RejectedExecutionException expected)
		{
			// expected
		}
	}

	@Test
	public void shutdownIsIdempotent()
	{
		AssuranceThreadPool pool = new AssuranceThreadPool();
		pool.shutdown();
		pool.shutdown();
		// no exception, no hang
		assertTrue(true);
	}
}
