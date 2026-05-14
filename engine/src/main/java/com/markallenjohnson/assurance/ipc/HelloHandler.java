/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Stateless handler for the lifecycle {@code hello} request. Validates the
 * client's reported {@code protocolVersion} against the engine's value and
 * either returns the {@code welcome} payload or raises a structured protocol
 * mismatch error.
 *
 * <p>This handler intentionally does not handle connection close; the
 * {@link WebSocketIpcServer} closes the socket itself after observing a
 * mismatch error response.
 */
public final class HelloHandler implements RpcDispatcher.RpcMethod
{
	private final int engineProtocolVersion;
	private final String engineVersion;

	public HelloHandler(int engineProtocolVersion, String engineVersion)
	{
		this.engineProtocolVersion = engineProtocolVersion;
		this.engineVersion = engineVersion;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS, "hello requires a params object");
		}

		JsonNode pvNode = params.get("protocolVersion");
		if (pvNode == null || !pvNode.canConvertToInt())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"hello.params.protocolVersion is required and must be an integer");
		}
		int clientProtocolVersion = pvNode.intValue();

		if (clientProtocolVersion != this.engineProtocolVersion)
		{
			ObjectNode data = JsonNodeFactory.instance.objectNode();
			data.put("engineProtocolVersion", this.engineProtocolVersion);
			data.put("clientProtocolVersion", clientProtocolVersion);
			throw new RpcException(
				RpcErrorCodes.PROTOCOL_VERSION_MISMATCH,
				String.format("Protocol version mismatch: engine=%d, client=%d",
					this.engineProtocolVersion, clientProtocolVersion),
				data);
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put("engineVersion", this.engineVersion);
		result.put("protocolVersion", this.engineProtocolVersion);
		return result;
	}
}
