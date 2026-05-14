/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ConcurrentRunRegistryTests
{
	@Test
	public void firstAcquireSucceeds()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		assertTrue(registry.tryAcquire(7L, 100L));
		assertEquals(Long.valueOf(100L), registry.currentRunId(7L));
	}

	@Test
	public void secondAcquireForSameKeyFailsAndPreservesFirstRunId()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		registry.tryAcquire(7L, 100L);
		assertFalse(registry.tryAcquire(7L, 200L));
		assertEquals(Long.valueOf(100L), registry.currentRunId(7L));
	}

	@Test
	public void releaseAllowsReacquire()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		registry.tryAcquire(7L, 100L);
		registry.release(7L);
		assertNull(registry.currentRunId(7L));
		assertTrue(registry.tryAcquire(7L, 200L));
	}

	@Test
	public void releaseOfUnknownKeyIsNoop()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		registry.release(42L);
		assertNull(registry.currentRunId(42L));
	}

	@Test
	public void releaseOfNullKeyIsNoop()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		registry.release(null);
		assertNull(registry.currentRunId(null));
	}

	@Test
	public void distinctKeysCanRunConcurrently()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		assertTrue(registry.tryAcquire(1L, 11L));
		assertTrue(registry.tryAcquire(2L, 22L));
	}

	@Test
	public void nullArgumentsAreRejectedFromAcquire()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		try
		{
			registry.tryAcquire(null, 1L);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException ignored) { }
		try
		{
			registry.tryAcquire(1L, null);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException ignored) { }
	}

	@Test
	public void currentRunIdIsNullForNullKey()
	{
		ConcurrentRunRegistry registry = new ConcurrentRunRegistry();
		assertNull(registry.currentRunId(null));
	}
}
