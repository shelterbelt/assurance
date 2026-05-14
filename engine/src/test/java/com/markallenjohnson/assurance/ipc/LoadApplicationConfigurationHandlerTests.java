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

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;

public class LoadApplicationConfigurationHandlerTests
{
	@Test
	public void wrapsConfigurationInResultObject() throws Exception
	{
		ApplicationConfiguration config = new ApplicationConfiguration();
		config.setId(1L);
		config.setIgnoredFileNames(".DS_Store");
		config.setIgnoredFileExtensions("*.tmp");
		config.setNumberOfScanThreads(4);

		LoadApplicationConfigurationHandler handler = new LoadApplicationConfigurationHandler(() -> config);

		JsonNode result = handler.invoke(null);

		assertTrue("result should have configuration object", result.has("configuration"));
		JsonNode body = result.get("configuration");
		assertEquals(1L, body.get("id").asLong());
		assertEquals(".DS_Store", body.get("ignoredFileNames").asText());
		assertEquals(4, body.get("numberOfScanThreads").asInt());
		assertNotNull(body.get("ignoredFileNamesCollection"));
	}

	@Test
	public void runtimeFailureMapsToEngineErrorRpcException()
	{
		LoadApplicationConfigurationHandler handler = new LoadApplicationConfigurationHandler(() -> {
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
			assertTrue(ex.getMessage().contains("h2 down"));
		}
	}

	@Test
	public void nullConfigurationMapsToEngineErrorRpcException()
	{
		LoadApplicationConfigurationHandler handler = new LoadApplicationConfigurationHandler(() -> null);

		try
		{
			handler.invoke(null);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
		}
	}
}
