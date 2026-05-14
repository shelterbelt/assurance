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

public class ScanRunRegistryTests
{
	@Test
	public void firstAcquireSucceeds()
	{
		ScanRunRegistry registry = new ScanRunRegistry();
		assertTrue(registry.tryAcquire(7L, 100L));
		assertEquals(Long.valueOf(100L), registry.currentScanId(7L));
	}

	@Test
	public void secondAcquireForSameDefinitionFailsAndPreservesFirstScanId()
	{
		ScanRunRegistry registry = new ScanRunRegistry();
		registry.tryAcquire(7L, 100L);
		assertFalse(registry.tryAcquire(7L, 200L));
		assertEquals(Long.valueOf(100L), registry.currentScanId(7L));
	}

	@Test
	public void releaseAllowsReacquire()
	{
		ScanRunRegistry registry = new ScanRunRegistry();
		registry.tryAcquire(7L, 100L);
		registry.release(7L);
		assertNull(registry.currentScanId(7L));
		assertTrue(registry.tryAcquire(7L, 200L));
	}

	@Test
	public void releaseOfUnknownDefinitionIsNoop()
	{
		ScanRunRegistry registry = new ScanRunRegistry();
		registry.release(42L);
		assertNull(registry.currentScanId(42L));
	}

	@Test
	public void distinctDefinitionsCanRunConcurrently()
	{
		ScanRunRegistry registry = new ScanRunRegistry();
		assertTrue(registry.tryAcquire(1L, 11L));
		assertTrue(registry.tryAcquire(2L, 22L));
	}

	@Test
	public void nullArgumentsAreRejectedFromAcquire()
	{
		ScanRunRegistry registry = new ScanRunRegistry();
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
}
