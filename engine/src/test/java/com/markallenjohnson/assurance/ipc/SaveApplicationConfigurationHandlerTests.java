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
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;

public class SaveApplicationConfigurationHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void roundTripsCreatePayloadAndPopulatesId() throws Exception
	{
		AtomicReference<Long> incomingIdAtPersistTime = new AtomicReference<>();
		AtomicReference<ApplicationConfiguration> seen = new AtomicReference<>();
		SaveApplicationConfigurationHandler handler = new SaveApplicationConfigurationHandler((cfg) -> {
			incomingIdAtPersistTime.set(cfg.getId());
			seen.set(cfg);
			cfg.setId(99L);
			// Mirror ModelDelegate.saveApplicationConfiguration's post-flush
			// initialize() so the codec can derive list views.
			cfg.initialize();
			return cfg;
		});

		JsonNode params = mapper.readTree(
			"{\"configuration\":{"
				+ "\"ignoredFileNames\":\".DS_Store\","
				+ "\"ignoredFileExtensions\":\"*.tmp\","
				+ "\"numberOfScanThreads\":4"
				+ "}}");

		JsonNode result = handler.invoke(params);

		assertNotNull(result.get("configuration"));
		assertEquals(99L, result.get("configuration").get("id").asLong());
		assertEquals(".DS_Store", result.get("configuration").get("ignoredFileNames").asText());
		assertEquals(4, result.get("configuration").get("numberOfScanThreads").asInt());

		ApplicationConfiguration incoming = seen.get();
		assertNotNull(incoming);
		assertNull("id should be absent on create payload", incomingIdAtPersistTime.get());
		assertEquals(".DS_Store", incoming.getIgnoredFileNames());
		assertEquals(Integer.valueOf(4), incoming.getNumberOfScanThreads());
	}

	@Test
	public void roundTripsUpdatePayloadPreservingId() throws Exception
	{
		AtomicReference<ApplicationConfiguration> seen = new AtomicReference<>();
		SaveApplicationConfigurationHandler handler = new SaveApplicationConfigurationHandler((cfg) -> {
			seen.set(cfg);
			cfg.initialize();
			return cfg;
		});

		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"id\":7,\"ignoredFileNames\":\"\",\"ignoredFileExtensions\":\"\",\"numberOfScanThreads\":2}}");

		JsonNode result = handler.invoke(params);

		assertEquals(7L, result.get("configuration").get("id").asLong());
		assertEquals(Long.valueOf(7L), seen.get().getId());
		assertEquals(Integer.valueOf(2), seen.get().getNumberOfScanThreads());
	}

	@Test
	public void invalidParamsAreReportedBeforePersistenceIsAttempted()
	{
		AtomicReference<Boolean> persisterCalled = new AtomicReference<>(Boolean.FALSE);
		SaveApplicationConfigurationHandler handler = new SaveApplicationConfigurationHandler((cfg) -> {
			persisterCalled.set(Boolean.TRUE);
			return cfg;
		});

		try
		{
			handler.invoke(mapper.readTree("{\"configuration\":{\"numberOfScanThreads\":0}}"));
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
		SaveApplicationConfigurationHandler handler = new SaveApplicationConfigurationHandler((cfg) -> {
			throw new RuntimeException("constraint violation");
		});

		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"ignoredFileNames\":\"\",\"ignoredFileExtensions\":\"\",\"numberOfScanThreads\":4}}");

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
		SaveApplicationConfigurationHandler handler = new SaveApplicationConfigurationHandler((cfg) -> null);

		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"ignoredFileNames\":\"\",\"ignoredFileExtensions\":\"\",\"numberOfScanThreads\":4}}");

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
