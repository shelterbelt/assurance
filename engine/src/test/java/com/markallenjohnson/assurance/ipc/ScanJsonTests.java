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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;

public class ScanJsonTests
{
	private static final long FIXED_STARTED_MILLIS = 1_700_000_000_000L; // 2023-11-14T22:13:20Z
	private static final long FIXED_COMPLETED_MILLIS = 1_700_000_300_000L; // +5 min

	@Test
	public void projectsRequiredScalarsAndIso8601Timestamps()
	{
		Scan scan = newScan(11L);
		scan.setScanCompleted(new Date(FIXED_COMPLETED_MILLIS));

		ObjectNode node = ScanJson.toJson(scan, false);

		assertEquals(11L, node.get("id").asLong());
		assertEquals(Instant.ofEpochMilli(scan.getScanStarted().getTime()).toString(),
			node.get("scanStarted").asText());
		assertEquals(Instant.ofEpochMilli(FIXED_COMPLETED_MILLIS).toString(),
			node.get("whenCompleted").asText());
	}

	@Test
	public void inFlightScanRendersWhenCompletedAsNull()
	{
		Scan scan = newScan(12L);
		// whenCompleted intentionally not set.

		ObjectNode node = ScanJson.toJson(scan, false);

		assertTrue(node.has("whenCompleted"));
		assertTrue(node.get("whenCompleted").isNull());
	}

	@Test
	public void includesThinScanDefReferenceWithIdAndName()
	{
		Scan scan = newScan(13L);
		ScanDefinition def = new ScanDefinition();
		def.setId(99L);
		def.setName("Daily backup");
		scan.setScanDef(def);

		ObjectNode node = ScanJson.toJson(scan, false);

		assertNotNull(node.get("scanDef"));
		assertTrue(node.get("scanDef").isObject());
		assertEquals(99L, node.get("scanDef").get("id").asLong());
		assertEquals("Daily backup", node.get("scanDef").get("name").asText());
	}

	@Test
	public void omitsScanDefWhenNull()
	{
		Scan scan = newScan(14L);
		// scanDef intentionally not set.

		ObjectNode node = ScanJson.toJson(scan, false);

		assertFalse("scanDef must be omitted when not set", node.has("scanDef"));
	}

	@Test
	public void resultCountIncludedOnlyWhenRequested()
	{
		Scan scan = newScan(15L);
		scan.addResult(new ComparisonResult());
		scan.addResult(new ComparisonResult());
		scan.addResult(new ComparisonResult());

		ObjectNode without = ScanJson.toJson(scan, false);
		assertNull("resultCount must be absent when not requested", without.get("resultCount"));

		ObjectNode with = ScanJson.toJson(scan, true);
		assertEquals(3, with.get("resultCount").asInt());
	}

	@Test
	public void resultCountIsZeroForEmptyScan()
	{
		Scan scan = newScan(16L);

		ObjectNode node = ScanJson.toJson(scan, true);

		assertEquals(0, node.get("resultCount").asInt());
	}

	private Scan newScan(long id)
	{
		Scan scan = new Scan();
		scan.setId(id);
		// Override the constructor-assigned scanStarted with a deterministic value.
		try
		{
			java.lang.reflect.Field field = Scan.class.getDeclaredField("scanStarted");
			field.setAccessible(true);
			field.set(scan, new Date(FIXED_STARTED_MILLIS));
		}
		catch (ReflectiveOperationException ex)
		{
			throw new AssertionError("Failed to seed scanStarted via reflection", ex);
		}
		return scan;
	}
}
