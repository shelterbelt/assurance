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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeleteScanDefinitionHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void deletesExistingDefinitionAndReturnsTrue() throws Exception
	{
		AtomicReference<Long> seen = new AtomicReference<>();
		DeleteScanDefinitionHandler handler = new DeleteScanDefinitionHandler((id) -> {
			seen.set(id);
			return Boolean.TRUE;
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"scanDefinitionId\":42}"));
		assertEquals(Long.valueOf(42L), seen.get());
		assertTrue(result.get("deleted").asBoolean());
	}

	@Test
	public void missingDefinitionMapsToEntityNotFound() throws Exception
	{
		DeleteScanDefinitionHandler handler = new DeleteScanDefinitionHandler((id) -> Boolean.FALSE);

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":99}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertNotNull(ex.getDataNode());
			assertEquals(99L, ex.getDataNode().get("scanDefinitionId").asLong());
		}
	}

	@Test
	public void missingScanDefinitionIdParamMapsToInvalidParams() throws Exception
	{
		DeleteScanDefinitionHandler handler = new DeleteScanDefinitionHandler((id) -> Boolean.TRUE);

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
	public void nonIntegerScanDefinitionIdMapsToInvalidParams() throws Exception
	{
		DeleteScanDefinitionHandler handler = new DeleteScanDefinitionHandler((id) -> Boolean.TRUE);

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":\"not-a-number\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void runtimeFailureFromDeleterMapsToEngineError() throws Exception
	{
		DeleteScanDefinitionHandler handler = new DeleteScanDefinitionHandler((id) -> {
			throw new RuntimeException("constraint violation");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("constraint violation"));
		}
	}
}
