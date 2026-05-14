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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

public class SaveScanDefinitionHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void roundTripsCreatePayloadAndPopulatesId() throws Exception
	{
		AtomicReference<Long> incomingIdAtPersistTime = new AtomicReference<>();
		AtomicReference<ScanDefinition> seen = new AtomicReference<>();
		SaveScanDefinitionHandler handler = new SaveScanDefinitionHandler((def) -> {
			incomingIdAtPersistTime.set(def.getId());
			seen.set(def);
			def.setId(42L);
			return def;
		});

		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{"
				+ "\"name\":\"Photos backup\","
				+ "\"mergeStrategy\":\"SOURCE\","
				+ "\"scanMapping\":["
				+ "  {\"source\":{\"location\":\"/src\"},\"target\":{\"location\":\"/dst\"}}"
				+ "]}}");

		JsonNode result = handler.invoke(params);
		assertNotNull(result.get("scanDefinition"));
		assertEquals(42L, result.get("scanDefinition").get("id").asLong());
		assertEquals("Photos backup", result.get("scanDefinition").get("name").asText());
		assertEquals("SOURCE", result.get("scanDefinition").get("mergeStrategy").asText());

		ScanDefinition incoming = seen.get();
		assertNotNull(incoming);
		assertNull("id should be absent on create payload", incomingIdAtPersistTime.get());
		assertEquals("Photos backup", incoming.getName());
		assertEquals(AssuranceMergeStrategy.SOURCE, incoming.getMergeStrategy());
		assertEquals(1, incoming.getUnmodifiableScanMapping().size());
	}

	@Test
	public void roundTripsUpdatePayloadPreservingId() throws Exception
	{
		AtomicReference<ScanDefinition> seen = new AtomicReference<>();
		SaveScanDefinitionHandler handler = new SaveScanDefinitionHandler((def) -> {
			seen.set(def);
			return def;
		});

		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{\"id\":7,\"name\":\"x\","
				+ "\"scanMapping\":[{\"source\":{\"location\":\"/a\"},\"target\":{\"location\":\"/b\"}}]}}");

		JsonNode result = handler.invoke(params);
		assertEquals(7L, result.get("scanDefinition").get("id").asLong());
		assertEquals(Long.valueOf(7L), seen.get().getId());
	}

	@Test
	public void invalidParamsAreReportedBeforePersistenceIsAttempted()
	{
		AtomicReference<Boolean> persisterCalled = new AtomicReference<>(Boolean.FALSE);
		SaveScanDefinitionHandler handler = new SaveScanDefinitionHandler((def) -> {
			persisterCalled.set(Boolean.TRUE);
			return def;
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinition\":{\"name\":\"\"}}"));
			fail("Expected RpcException");
		}
		catch (Exception ex)
		{
			assertTrue("Expected RpcException, got " + ex.getClass(), ex instanceof RpcException);
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ((RpcException) ex).getCode());
		}

		assertEquals(Boolean.FALSE, persisterCalled.get());
	}

	@Test
	public void runtimeFailureFromPersisterMapsToEngineError() throws Exception
	{
		SaveScanDefinitionHandler handler = new SaveScanDefinitionHandler((def) -> {
			throw new RuntimeException("constraint violation");
		});

		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{\"name\":\"x\","
				+ "\"scanMapping\":[{\"source\":{\"location\":\"/a\"},\"target\":{\"location\":\"/b\"}}]}}");

		try
		{
			handler.invoke(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("constraint violation"));
		}
	}

	@Test
	public void nullPersistedResultMapsToEngineError() throws Exception
	{
		SaveScanDefinitionHandler handler = new SaveScanDefinitionHandler((def) -> null);

		JsonNode params = mapper.readTree(
			"{\"scanDefinition\":{\"name\":\"x\","
				+ "\"scanMapping\":[{\"source\":{\"location\":\"/a\"},\"target\":{\"location\":\"/b\"}}]}}");

		try
		{
			handler.invoke(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
		}
	}
}
