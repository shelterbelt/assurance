/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Thrown by an {@link RpcDispatcher.RpcMethod} to surface an application-level
 * JSON-RPC error to the client. The {@code code} and {@code message} are
 * propagated verbatim into the response envelope, and {@code data} is included
 * when non-null. Use the constants in {@link RpcErrorCodes} to choose a code.
 */
public class RpcException extends Exception
{
	private static final long serialVersionUID = 1L;

	private final int code;
	private final transient JsonNode data;

	public RpcException(int code, String message)
	{
		this(code, message, null, null);
	}

	public RpcException(int code, String message, JsonNode data)
	{
		this(code, message, data, null);
	}

	public RpcException(int code, String message, JsonNode data, Throwable cause)
	{
		super(message, cause);
		this.code = code;
		this.data = data;
	}

	public int getCode()
	{
		return code;
	}

	public JsonNode getDataNode()
	{
		return data;
	}
}
