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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

public class LoadScanDefinitionsHandlerTests
{
	@Test
	public void wrapsListInScanDefinitionsResultObject() throws Exception
	{
		ScanDefinition def = new ScanDefinition();
		def.setId(7L);
		def.setName("Photos backup");
		def.setMergeStrategy(AssuranceMergeStrategy.SOURCE);
		def.setAutoResolveConflicts(Boolean.TRUE);
		def.setIncludeNonCreationTimestamps(Boolean.FALSE);
		def.setIncludeAdvancedAttributes(Boolean.TRUE);

		LoadScanDefinitionsHandler handler = new LoadScanDefinitionsHandler(() -> Arrays.asList(def));

		JsonNode result = handler.invoke(null);

		assertTrue("result should have scanDefinitions array", result.has("scanDefinitions"));
		JsonNode array = result.get("scanDefinitions");
		assertTrue("scanDefinitions should be an array", array.isArray());
		assertEquals(1, array.size());

		JsonNode entry = array.get(0);
		assertEquals(7, entry.get("id").asLong());
		assertEquals("Photos backup", entry.get("name").asText());
		assertEquals("SOURCE", entry.get("mergeStrategy").asText());
		assertTrue(entry.get("autoResolveConflicts").asBoolean());
		assertFalse(entry.get("includeNonCreationTimestamps").asBoolean());
		assertTrue(entry.get("includeAdvancedAttributes").asBoolean());
		assertTrue("scanMapping must be present for editor consumers",
			entry.has("scanMapping"));
		JsonNode mappings = entry.get("scanMapping");
		assertTrue(mappings.isArray());
		assertEquals(0, mappings.size());
	}

	@Test
	public void includesScanMappingWithSourceAndTargetLocations() throws Exception
	{
		ScanDefinition def = new ScanDefinition();
		def.setId(8L);
		def.setName("With paths");
		def.setMergeStrategy(AssuranceMergeStrategy.TARGET);

		ScanMappingDefinition mapping = new ScanMappingDefinition();
		mapping.setSource(new File("/var/source"));
		mapping.setTarget(new File("/var/target"));
		def.addMappingDefinition(mapping);

		LoadScanDefinitionsHandler handler = new LoadScanDefinitionsHandler(() -> Arrays.asList(def));

		JsonNode entry = handler.invoke(null).get("scanDefinitions").get(0);
		JsonNode mapArr = entry.get("scanMapping");
		assertEquals(1, mapArr.size());
		JsonNode first = mapArr.get(0);
		assertEquals("/var/source", first.get("source").get("location").asText());
		assertEquals("/var/target", first.get("target").get("location").asText());
	}

	@Test
	public void emptyListProducesEmptyArray() throws Exception
	{
		LoadScanDefinitionsHandler handler = new LoadScanDefinitionsHandler(Collections::emptyList);

		JsonNode result = handler.invoke(null);

		assertTrue(result.has("scanDefinitions"));
		assertEquals(0, result.get("scanDefinitions").size());
	}

	@Test
	public void nullListProducesEmptyArray() throws Exception
	{
		LoadScanDefinitionsHandler handler = new LoadScanDefinitionsHandler(() -> null);

		JsonNode result = handler.invoke(null);

		assertTrue(result.has("scanDefinitions"));
		assertEquals(0, result.get("scanDefinitions").size());
	}

	@Test
	public void runtimeFailureMapsToEngineErrorRpcException()
	{
		LoadScanDefinitionsHandler handler = new LoadScanDefinitionsHandler(() -> {
			throw new RuntimeException("h2 down");
		});

		try
		{
			handler.invoke(null);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue("message should include the underlying cause",
				ex.getMessage().contains("h2 down"));
		}
	}

	@Test
	public void nullableMergeStrategyOmittedFromOutput() throws Exception
	{
		ScanDefinition def = new ScanDefinition();
		def.setId(11L);
		def.setName("Strategy not set");
		def.setMergeStrategy(null);
		def.setAutoResolveConflicts(null);
		def.setIncludeNonCreationTimestamps(null);
		def.setIncludeAdvancedAttributes(null);

		List<ScanDefinition> list = Arrays.asList(def);
		LoadScanDefinitionsHandler handler = new LoadScanDefinitionsHandler(() -> list);

		JsonNode entry = handler.invoke(null).get("scanDefinitions").get(0);

		assertEquals(11, entry.get("id").asLong());
		assertEquals("Strategy not set", entry.get("name").asText());
		assertFalse("mergeStrategy must be omitted when unset",
			entry.has("mergeStrategy"));
		assertFalse("null booleans coerce to explicit false",
			entry.get("autoResolveConflicts").asBoolean());
		assertFalse(entry.get("includeNonCreationTimestamps").asBoolean());
		assertFalse(entry.get("includeAdvancedAttributes").asBoolean());
		assertTrue(entry.has("scanMapping"));
		assertEquals(0, entry.get("scanMapping").size());
	}
}
