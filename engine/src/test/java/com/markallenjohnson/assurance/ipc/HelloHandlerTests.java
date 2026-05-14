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
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HelloHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void returnsWelcomeOnMatchingProtocolVersion() throws Exception
	{
		HelloHandler handler = new HelloHandler(1, "2.0.0");
		JsonNode params = mapper.readTree("{\"uiVersion\":\"2.0.0\",\"protocolVersion\":1}");

		JsonNode result = handler.invoke(params);

		assertEquals("2.0.0", result.get("engineVersion").asText());
		assertEquals(1, result.get("protocolVersion").asInt());
	}

	@Test
	public void throwsRpcExceptionOnVersionMismatch() throws Exception
	{
		HelloHandler handler = new HelloHandler(1, "2.0.0");
		JsonNode params = mapper.readTree("{\"uiVersion\":\"2.0.0\",\"protocolVersion\":99}");

		try
		{
			handler.invoke(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.PROTOCOL_VERSION_MISMATCH, ex.getCode());
			JsonNode data = ex.getDataNode();
			assertNotNull(data);
			assertEquals(1, data.get("engineProtocolVersion").asInt());
			assertEquals(99, data.get("clientProtocolVersion").asInt());
		}
	}

	@Test
	public void throwsInvalidParamsWhenProtocolVersionMissing() throws Exception
	{
		HelloHandler handler = new HelloHandler(1, "2.0.0");
		JsonNode params = mapper.readTree("{\"uiVersion\":\"2.0.0\"}");

		try
		{
			handler.invoke(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void throwsInvalidParamsWhenParamsAbsent()
	{
		HelloHandler handler = new HelloHandler(1, "2.0.0");

		try
		{
			handler.invoke(null);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}
}
