/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.time.Clock;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@code assurance.ping}. Returns
 * {@code { "pong": true, "timestamp": "<iso8601>" }} per
 * {@code docs/ipc-contract.md}.
 */
public final class PingHandler implements RpcDispatcher.RpcMethod
{
	private final Clock clock;

	public PingHandler()
	{
		this(Clock.systemUTC());
	}

	public PingHandler(Clock clock)
	{
		this.clock = clock;
	}

	@Override
	public JsonNode invoke(JsonNode params)
	{
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put("pong", true);
		result.put("timestamp", Instant.now(clock).toString());
		return result;
	}
}
