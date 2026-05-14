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
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

public class ScanDefinitionJsonTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void fromJsonRequiresNonEmptyName() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{\"name\":\"\",\"scanMapping\":[]}}");
		try
		{
			ScanDefinitionJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void fromJsonRequiresScanDefinitionField() throws Exception
	{
		JsonNode params = mapper.readTree("{}");
		try
		{
			ScanDefinitionJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
			assertTrue(ex.getMessage().contains("scanDefinition"));
		}
	}

	@Test
	public void fromJsonRejectsUnknownMergeStrategy() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{\"name\":\"x\",\"mergeStrategy\":\"NOT_A_REAL_STRATEGY\","
				+ "\"scanMapping\":[{\"source\":{\"location\":\"/a\"},\"target\":{\"location\":\"/b\"}}]}}");
		try
		{
			ScanDefinitionJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
			assertTrue(ex.getMessage().contains("mergeStrategy"));
		}
	}

	@Test
	public void fromJsonHydratesFullScanDefinition() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{"
				+ "\"name\":\"Photos backup\","
				+ "\"mergeStrategy\":\"TARGET\","
				+ "\"autoResolveConflicts\":true,"
				+ "\"includeNonCreationTimestamps\":false,"
				+ "\"includeAdvancedAttributes\":true,"
				+ "\"scanMapping\":["
				+ "  {\"source\":{\"location\":\"/src/photos\"},\"target\":{\"location\":\"/dst/photos\"},"
				+ "   \"exclusions\":[{\"location\":\"/src/photos/temp\"}]}"
				+ "]}}");

		ScanDefinition def = ScanDefinitionJson.fromJson(params);

		assertNull("id is omitted on create", def.getId());
		assertEquals("Photos backup", def.getName());
		assertEquals(AssuranceMergeStrategy.TARGET, def.getMergeStrategy());
		assertTrue(def.getAutoResolveConflicts());
		assertFalse(def.getIncludeNonCreationTimestamps());
		assertTrue(def.getIncludeAdvancedAttributes());

		Iterator<ScanMappingDefinition> mappings = def.getUnmodifiableScanMapping().iterator();
		assertTrue(mappings.hasNext());
		ScanMappingDefinition mapping = mappings.next();
		assertEquals(new File("/src/photos"), mapping.getSource());
		assertEquals(new File("/dst/photos"), mapping.getTarget());
		assertEquals(1, mapping.getUnmodifiableExclusions().size());
	}

	@Test
	public void fromJsonRejectsMappingWithoutSourceOrTarget() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{\"name\":\"x\",\"scanMapping\":[{\"source\":{\"location\":\"/a\"}}]}}");
		try
		{
			ScanDefinitionJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void toJsonOmitsScanMappingWhenIncludeMappingsFalse()
	{
		ScanDefinition def = new ScanDefinition();
		def.setId(7L);
		def.setName("p");
		def.setMergeStrategy(AssuranceMergeStrategy.SOURCE);

		ObjectNode flat = ScanDefinitionJson.toJson(def, false);

		assertEquals(7L, flat.get("id").asLong());
		assertFalse(flat.has("scanMapping"));
	}

	@Test
	public void toJsonIncludesScanMappingWithFileReferenceShape()
	{
		ScanDefinition def = new ScanDefinition();
		def.setId(7L);
		def.setName("p");
		def.setMergeStrategy(AssuranceMergeStrategy.SOURCE);
		ScanMappingDefinition mapping = new ScanMappingDefinition();
		mapping.setId(13L);
		mapping.setSource(new File("/src"));
		mapping.setTarget(new File("/dst"));
		mapping.addExclusion(new FileReference(new File("/src/excluded")));
		def.addMappingDefinition(mapping);

		ObjectNode full = ScanDefinitionJson.toJson(def, true);

		JsonNode mappings = full.get("scanMapping");
		assertNotNull(mappings);
		assertEquals(1, mappings.size());
		JsonNode entry = mappings.get(0);
		assertEquals(13L, entry.get("id").asLong());
		assertEquals("/src", entry.get("source").get("location").asText());
		assertEquals("/dst", entry.get("target").get("location").asText());
		JsonNode exclusions = entry.get("exclusions");
		assertEquals(1, exclusions.size());
		assertEquals("/src/excluded", exclusions.get(0).get("location").asText());
	}
}
