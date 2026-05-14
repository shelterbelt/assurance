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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

public class LoadScanResultsHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void emptyResultsForKnownScanProducesEmptyArray() throws Exception
	{
		AtomicReference<Long> seen = new AtomicReference<>();
		LoadScanResultsHandler handler = new LoadScanResultsHandler((id) -> {
			seen.set(id);
			return Collections.emptyList();
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"scanId\":42}"));

		assertEquals(Long.valueOf(42L), seen.get());
		assertNotNull(result.get("results"));
		assertTrue(result.get("results").isArray());
		assertEquals(0, result.get("results").size());
	}

	@Test
	public void projectsResultsViaCodec() throws Exception
	{
		List<ComparisonResult> results = new ArrayList<>();
		results.add(newResult(1L, "/data/source.txt", "/data/target.txt",
			AssuranceResultReason.FILE_DIRECTORY_MISMATCH));
		results.add(newResult(2L, "/data/missing.txt", null,
			AssuranceResultReason.TARGET_DOES_NOT_EXIST));

		LoadScanResultsHandler handler = new LoadScanResultsHandler((id) -> results);

		JsonNode result = handler.invoke(mapper.readTree("{\"scanId\":42}"));

		JsonNode array = result.get("results");
		assertEquals(2, array.size());

		assertEquals(1L, array.get(0).get("id").asLong());
		assertEquals("/data/source.txt", array.get(0).get("source").get("location").asText());
		assertEquals("/data/target.txt", array.get(0).get("target").get("location").asText());
		assertEquals("FILE_DIRECTORY_MISMATCH", array.get(0).get("reason").asText());

		assertEquals(2L, array.get(1).get("id").asLong());
		assertEquals("/data/missing.txt", array.get(1).get("source").get("location").asText());
		assertTrue("target must be omitted when null on the entity",
			!array.get(1).has("target"));
		assertEquals("TARGET_DOES_NOT_EXIST", array.get(1).get("reason").asText());
	}

	@Test
	public void unknownScanMapsToEntityNotFound() throws Exception
	{
		LoadScanResultsHandler handler = new LoadScanResultsHandler((id) -> null);

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":99}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertNotNull(ex.getDataNode());
			assertEquals(99L, ex.getDataNode().get("scanId").asLong());
		}
	}

	@Test
	public void missingScanIdMapsToInvalidParams() throws Exception
	{
		LoadScanResultsHandler handler = new LoadScanResultsHandler((id) -> Collections.emptyList());

		try
		{
			handler.invoke(mapper.readTree("{}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void nonIntegerScanIdMapsToInvalidParams() throws Exception
	{
		LoadScanResultsHandler handler = new LoadScanResultsHandler((id) -> Collections.emptyList());

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":\"forty-two\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void runtimeFailureMapsToEngineError() throws Exception
	{
		LoadScanResultsHandler handler = new LoadScanResultsHandler((id) -> {
			throw new RuntimeException("hibernate fail");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("hibernate fail"));
		}
	}

	private ComparisonResult newResult(long id, String source, String target, AssuranceResultReason reason)
	{
		ComparisonResult result = new ComparisonResult();
		result.setId(id);
		if (source != null)
		{
			result.setSource(new File(source));
		}
		if (target != null)
		{
			result.setTarget(new File(target));
		}
		result.setReason(reason);
		result.setResolution(AssuranceResultResolution.UNRESOLVED);
		return result;
	}
}
