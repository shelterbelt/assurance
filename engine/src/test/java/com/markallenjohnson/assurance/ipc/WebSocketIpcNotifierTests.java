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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WebSocketIpcNotifierTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	private static final class RecordingServer extends WebSocketIpcServer
	{
		private final List<String> broadcasts = new ArrayList<>();

		RecordingServer()
		{
			super(new RpcDispatcher());
		}

		@Override
		public void broadcast(String text)
		{
			broadcasts.add(text);
		}

		@Override
		public void broadcast(byte[] data)
		{
			throw new UnsupportedOperationException("byte broadcast not used by the notifier");
		}

		@Override
		public void broadcast(String text, Collection<org.java_websocket.WebSocket> clients)
		{
			throw new UnsupportedOperationException("scoped broadcast not used by the notifier");
		}

		List<String> getBroadcasts()
		{
			return broadcasts;
		}
	}

	@Test
	public void publishWritesJsonRpcNotificationEnvelope() throws Exception
	{
		RecordingServer server = new RecordingServer();
		WebSocketIpcNotifier notifier = new WebSocketIpcNotifier(server, mapper);

		ObjectNode params = JsonNodeFactory.instance.objectNode();
		params.put("scanId", 42);
		notifier.publish("assurance.scanStarted", params);

		assertEquals(1, server.getBroadcasts().size());
		JsonNode envelope = mapper.readTree(server.getBroadcasts().get(0));
		assertEquals("2.0", envelope.get("jsonrpc").asText());
		assertEquals("assurance.scanStarted", envelope.get("method").asText());
		assertEquals(42, envelope.get("params").get("scanId").asInt());
		assertFalse("notifications must not carry an id", envelope.has("id"));
	}

	@Test
	public void emptyMethodIsRejectedSilently()
	{
		RecordingServer server = new RecordingServer();
		WebSocketIpcNotifier notifier = new WebSocketIpcNotifier(server, mapper);

		notifier.publish("", JsonNodeFactory.instance.objectNode());
		notifier.publish(null, JsonNodeFactory.instance.objectNode());

		assertTrue("empty/null method must not produce broadcasts", server.getBroadcasts().isEmpty());
	}

	@Test
	public void nullParamsOmitsParamsField() throws Exception
	{
		RecordingServer server = new RecordingServer();
		WebSocketIpcNotifier notifier = new WebSocketIpcNotifier(server, mapper);

		notifier.publish("assurance.ping", null);

		JsonNode envelope = mapper.readTree(server.getBroadcasts().get(0));
		assertFalse(envelope.has("params"));
	}
}
