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

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

public class LoadComparisonResultHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void projectsSingleResultViaCodec() throws Exception
	{
		ComparisonResult entity = new ComparisonResult();
		entity.setId(77L);
		entity.setSource(new File("/tmp/a.txt"));
		entity.setTarget(new File("/tmp/b.txt"));
		entity.setReason(AssuranceResultReason.COMPARE_FAILED);
		entity.setResolution(AssuranceResultResolution.REPLACE_TARGET);

		LoadComparisonResultHandler handler = new LoadComparisonResultHandler((id) -> {
			assertEquals(Long.valueOf(77L), id);
			return entity;
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"resultId\":77}"));

		assertNotNull(result.get("result"));
		assertEquals(77L, result.get("result").get("id").asLong());
		assertEquals("/tmp/a.txt", result.get("result").get("source").get("location").asText());
		assertEquals("/tmp/b.txt", result.get("result").get("target").get("location").asText());
		assertEquals("COMPARE_FAILED", result.get("result").get("reason").asText());
		assertEquals("REPLACE_TARGET", result.get("result").get("resolution").asText());
	}

	@Test
	public void unknownResultMapsToEntityNotFound() throws Exception
	{
		LoadComparisonResultHandler handler = new LoadComparisonResultHandler((id) -> null);

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":99}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertNotNull(ex.getDataNode());
			assertEquals(99L, ex.getDataNode().get("resultId").asLong());
		}
	}

	@Test
	public void missingResultIdMapsToInvalidParams() throws Exception
	{
		LoadComparisonResultHandler handler = new LoadComparisonResultHandler((id) -> new ComparisonResult());

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
	public void nonIntegerResultIdMapsToInvalidParams() throws Exception
	{
		LoadComparisonResultHandler handler = new LoadComparisonResultHandler((id) -> new ComparisonResult());

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":\"x\"}"));
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
		LoadComparisonResultHandler handler = new LoadComparisonResultHandler((id) -> {
			throw new RuntimeException("db fail");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("db fail"));
		}
	}
}
