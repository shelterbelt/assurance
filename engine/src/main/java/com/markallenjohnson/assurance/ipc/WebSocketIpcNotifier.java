/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Production {@link IpcNotifier} that broadcasts JSON-RPC notifications over
 * a {@link WebSocketIpcServer}. Frames carry the standard JSON-RPC 2.0
 * envelope with no {@code id}.
 *
 * <p>Since Assurance is a single-client desktop application the broadcast
 * fanout is effectively a unicast; the choice to use {@code broadcast} rather
 * than tracking a single connection avoids any reconnection bookkeeping and
 * defers gracefully to whatever clients happen to be attached (this is
 * convenient during dev when a smoke script and the Electron client may be
 * connected concurrently).
 *
 * <p>Serialisation failures and broadcast errors are logged and swallowed:
 * notifications are best-effort by design.
 */
public final class WebSocketIpcNotifier implements IpcNotifier
{
	private static final Logger logger = LogManager.getLogger(WebSocketIpcNotifier.class);

	private final WebSocketIpcServer server;
	private final ObjectMapper mapper;

	public WebSocketIpcNotifier(WebSocketIpcServer server)
	{
		this(server, new ObjectMapper());
	}

	WebSocketIpcNotifier(WebSocketIpcServer server, ObjectMapper mapper)
	{
		this.server = server;
		this.mapper = mapper;
	}

	@Override
	public void publish(String method, JsonNode params)
	{
		if (method == null || method.isEmpty())
		{
			logger.warn("Refusing to publish notification with empty method");
			return;
		}

		ObjectNode envelope = JsonNodeFactory.instance.objectNode();
		envelope.put("jsonrpc", "2.0");
		envelope.put("method", method);
		if (params != null)
		{
			envelope.set("params", params);
		}

		String frame;
		try
		{
			frame = mapper.writeValueAsString(envelope);
		}
		catch (JsonProcessingException ex)
		{
			logger.error("Failed to serialise IPC notification {}", method, ex);
			return;
		}

		try
		{
			server.broadcast(frame);
		}
		catch (RuntimeException ex)
		{
			logger.warn("Failed to broadcast IPC notification {}: {}", method, ex.toString());
		}
	}
}
