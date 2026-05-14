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

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RpcDispatcherTests
{
	private RpcDispatcher dispatcher;
	private ObjectMapper mapper;

	@Before
	public void setUp()
	{
		dispatcher = new RpcDispatcher();
		mapper = new ObjectMapper();
		dispatcher.register("echo", params -> {
			ObjectNode result = JsonNodeFactory.instance.objectNode();
			result.set("echoed", params == null || params.isMissingNode() ? mapper.nullNode() : params);
			return result;
		});
		dispatcher.register("boom", params -> {
			throw new RpcException(RpcErrorCodes.ENGINE_ERROR, "kaboom");
		});
		dispatcher.register("crash", params -> {
			throw new RuntimeException("uncaught");
		});
	}

	@Test
	public void dispatchesRequestAndReturnsResultEnvelope() throws JsonProcessingException
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"echo\",\"params\":{\"hello\":\"world\"},\"id\":42}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals("2.0", envelope.get("jsonrpc").asText());
		assertEquals(42, envelope.get("id").asInt());
		assertFalse(envelope.has("error"));
		assertEquals("world", envelope.get("result").get("echoed").get("hello").asText());
	}

	@Test
	public void notificationsReturnNull()
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"echo\",\"params\":{}}");
		assertNull(response);
	}

	@Test
	public void unknownMethodReturnsMethodNotFound() throws JsonProcessingException
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"nope\",\"id\":1}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals(1, envelope.get("id").asInt());
		JsonNode error = envelope.get("error");
		assertNotNull(error);
		assertEquals(RpcErrorCodes.METHOD_NOT_FOUND, error.get("code").asInt());
	}

	@Test
	public void unknownNotificationIsSilentlyDropped()
	{
		assertNull(dispatcher.dispatch("{\"jsonrpc\":\"2.0\",\"method\":\"nope\"}"));
	}

	@Test
	public void parseErrorReturnedForMalformedJson() throws JsonProcessingException
	{
		String response = dispatcher.dispatch("{not json");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals(RpcErrorCodes.PARSE_ERROR, envelope.get("error").get("code").asInt());
		assertTrue("id should be null on parse error", envelope.get("id").isNull());
	}

	@Test
	public void invalidRequestForMissingJsonRpcVersion() throws JsonProcessingException
	{
		String response = dispatcher.dispatch("{\"method\":\"echo\",\"id\":1}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals(RpcErrorCodes.INVALID_REQUEST, envelope.get("error").get("code").asInt());
		assertEquals(1, envelope.get("id").asInt());
	}

	@Test
	public void invalidRequestForMissingMethod() throws JsonProcessingException
	{
		String response = dispatcher.dispatch("{\"jsonrpc\":\"2.0\",\"id\":1}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals(RpcErrorCodes.INVALID_REQUEST, envelope.get("error").get("code").asInt());
	}

	@Test
	public void rpcExceptionPropagatesCodeAndMessage() throws JsonProcessingException
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"boom\",\"id\":3}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		JsonNode error = envelope.get("error");
		assertEquals(RpcErrorCodes.ENGINE_ERROR, error.get("code").asInt());
		assertEquals("kaboom", error.get("message").asText());
		assertEquals(3, envelope.get("id").asInt());
	}

	@Test
	public void uncheckedExceptionMapsToInternalError() throws JsonProcessingException
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"crash\",\"id\":4}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals(RpcErrorCodes.INTERNAL_ERROR, envelope.get("error").get("code").asInt());
	}

	@Test
	public void preservesStringIdInResponse() throws JsonProcessingException
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"echo\",\"params\":{},\"id\":\"abc\"}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertEquals("abc", envelope.get("id").asText());
	}

	@Test
	public void preservesNullIdInResponse() throws JsonProcessingException
	{
		String response = dispatcher.dispatch(
			"{\"jsonrpc\":\"2.0\",\"method\":\"echo\",\"params\":{},\"id\":null}");
		assertNotNull(response);
		JsonNode envelope = mapper.readTree(response);
		assertTrue(envelope.get("id").isNull());
	}
}
