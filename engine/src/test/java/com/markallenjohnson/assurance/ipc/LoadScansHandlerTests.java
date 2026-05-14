/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;

public class LoadScansHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void emptyResultProducesEmptyScansArray() throws Exception
	{
		LoadScansHandler handler = new LoadScansHandler(Collections::emptyList);

		JsonNode result = handler.invoke(mapper.readTree("{}"));

		assertNotNull(result.get("scans"));
		assertTrue(result.get("scans").isArray());
		assertEquals(0, result.get("scans").size());
	}

	@Test
	public void nullSupplierResultProducesEmptyScansArray() throws Exception
	{
		LoadScansHandler handler = new LoadScansHandler(() -> null);

		JsonNode result = handler.invoke(mapper.readTree("{}"));

		assertNotNull(result.get("scans"));
		assertTrue(result.get("scans").isArray());
		assertEquals(0, result.get("scans").size());
	}

	@Test
	public void projectsScansWithThinScanDefAndResultCount() throws Exception
	{
		List<Scan> scans = new ArrayList<>();
		scans.add(scanWith(1L, "Daily backup", 2));
		scans.add(scanWith(2L, "Photo archive", 0));

		LoadScansHandler handler = new LoadScansHandler(() -> scans);

		JsonNode result = handler.invoke(mapper.readTree("{}"));

		JsonNode array = result.get("scans");
		assertEquals(2, array.size());

		assertEquals(1L, array.get(0).get("id").asLong());
		assertEquals("Daily backup", array.get(0).get("scanDef").get("name").asText());
		assertEquals(2, array.get(0).get("resultCount").asInt());
		assertTrue(array.get(0).has("scanStarted"));
		assertTrue(array.get(0).has("whenCompleted"));

		assertEquals(2L, array.get(1).get("id").asLong());
		assertEquals("Photo archive", array.get(1).get("scanDef").get("name").asText());
		assertEquals(0, array.get(1).get("resultCount").asInt());
	}

	@Test
	public void runtimeFailureMapsToEngineError()
	{
		LoadScansHandler handler = new LoadScansHandler(() -> {
			throw new RuntimeException("db unavailable");
		});

		try
		{
			handler.invoke(mapper.readTree("{}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("db unavailable"));
		}
		catch (Exception ex)
		{
			fail("Expected RpcException, got " + ex.getClass().getSimpleName());
		}
	}

	private Scan scanWith(long scanId, String defName, int resultCount)
	{
		Scan scan = new Scan();
		scan.setId(scanId);
		scan.setScanCompleted(new Date());
		ScanDefinition def = new ScanDefinition();
		def.setId(scanId * 10);
		def.setName(defName);
		scan.setScanDef(def);
		for (int i = 0; i < resultCount; i++)
		{
			scan.addResult(new ComparisonResult());
		}
		return scan;
	}
}
