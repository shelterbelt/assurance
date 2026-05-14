/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON-RPC 2.0 request dispatcher. Owns the method-name registry and produces
 * encoded response envelopes for inbound request frames. The dispatcher is
 * transport-agnostic — a transport (e.g. {@link WebSocketIpcServer}) feeds it
 * raw inbound text and writes back the returned response text.
 *
 * <p>Notifications (frames without an {@code id}) do not produce a response;
 * the dispatcher returns {@code null} for those.
 *
 * <p>Method handlers receive the parsed {@code params} subtree (or
 * {@link MissingNode} when {@code params} is absent) and return a
 * {@link JsonNode} representing the {@code result}. Handlers signal an
 * application-level error by throwing {@link RpcException}.
 */
public class RpcDispatcher
{
	private static final Logger logger = LogManager.getLogger(RpcDispatcher.class);

	public static final String JSONRPC_VERSION = "2.0";

	private final ObjectMapper mapper = new ObjectMapper();
	private final Map<String, RpcMethod> methods = new LinkedHashMap<>();

	@FunctionalInterface
	public interface RpcMethod
	{
		JsonNode invoke(JsonNode params) throws RpcException;
	}

	public void register(String methodName, RpcMethod handler)
	{
		methods.put(methodName, handler);
	}

	public boolean isRegistered(String methodName)
	{
		return methods.containsKey(methodName);
	}

	/**
	 * Dispatches a single inbound frame. Returns the encoded JSON-RPC response,
	 * or {@code null} if the frame was a valid notification (no response is
	 * sent for notifications, per the JSON-RPC 2.0 spec).
	 */
	public String dispatch(String inboundText)
	{
		JsonNode root;
		try
		{
			root = mapper.readTree(inboundText);
		}
		catch (JsonProcessingException ex)
		{
			logger.warn("Parse error on inbound frame: {}", ex.getOriginalMessage());
			return encodeError(null, RpcErrorCodes.PARSE_ERROR, "Parse error", null);
		}

		if (root == null || !root.isObject())
		{
			return encodeError(null, RpcErrorCodes.INVALID_REQUEST, "Invalid request", null);
		}

		JsonNode jsonrpcNode = root.get("jsonrpc");
		if (jsonrpcNode == null || !JSONRPC_VERSION.equals(jsonrpcNode.asText()))
		{
			return encodeError(extractIdNode(root), RpcErrorCodes.INVALID_REQUEST,
				"Invalid request: missing or unsupported jsonrpc version", null);
		}

		JsonNode methodNode = root.get("method");
		if (methodNode == null || !methodNode.isTextual())
		{
			return encodeError(extractIdNode(root), RpcErrorCodes.INVALID_REQUEST,
				"Invalid request: missing method", null);
		}
		String methodName = methodNode.asText();

		JsonNode params = root.get("params");
		if (params == null)
		{
			params = MissingNode.getInstance();
		}

		boolean isNotification = !root.hasNonNull("id") && !root.has("id");

		RpcMethod handler = methods.get(methodName);
		if (handler == null)
		{
			if (isNotification)
			{
				logger.warn("Notification for unknown method '{}' ignored.", methodName);
				return null;
			}
			return encodeError(extractIdNode(root), RpcErrorCodes.METHOD_NOT_FOUND,
				"Method not found: " + methodName, null);
		}

		try
		{
			JsonNode result = handler.invoke(params);
			if (isNotification)
			{
				return null;
			}
			return encodeResult(extractIdNode(root), result == null ? mapper.nullNode() : result);
		}
		catch (RpcException rpcEx)
		{
			logger.debug("RPC handler for '{}' raised: {}", methodName, rpcEx.getMessage());
			return encodeError(extractIdNode(root), rpcEx.getCode(), rpcEx.getMessage(), rpcEx.getDataNode());
		}
		catch (RuntimeException ex)
		{
			logger.error("Unhandled exception dispatching '{}'", methodName, ex);
			return encodeError(extractIdNode(root), RpcErrorCodes.INTERNAL_ERROR,
				"Internal error", mapper.getNodeFactory().textNode(ex.getClass().getSimpleName()));
		}
	}

	String encodeResult(JsonNode id, JsonNode result)
	{
		ObjectNode envelope = JsonNodeFactory.instance.objectNode();
		envelope.put("jsonrpc", JSONRPC_VERSION);
		envelope.set("result", result);
		envelope.set("id", id);
		try
		{
			return mapper.writeValueAsString(envelope);
		}
		catch (JsonProcessingException ex)
		{
			// Fall through to a hand-rolled error envelope.
			logger.error("Failed to encode result envelope", ex);
			return encodeError(id, RpcErrorCodes.INTERNAL_ERROR, "Failed to encode response", null);
		}
	}

	String encodeError(JsonNode id, int code, String message, JsonNode data)
	{
		ObjectNode envelope = JsonNodeFactory.instance.objectNode();
		envelope.put("jsonrpc", JSONRPC_VERSION);
		ObjectNode error = envelope.putObject("error");
		error.put("code", code);
		error.put("message", message);
		if (data != null && !data.isMissingNode())
		{
			error.set("data", data);
		}
		envelope.set("id", id == null ? JsonNodeFactory.instance.nullNode() : id);
		try
		{
			return mapper.writeValueAsString(envelope);
		}
		catch (JsonProcessingException ex)
		{
			logger.error("Failed to encode error envelope", ex);
			return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":" + RpcErrorCodes.INTERNAL_ERROR
				+ ",\"message\":\"Failed to encode response\"},\"id\":null}";
		}
	}

	public ObjectMapper getMapper()
	{
		return mapper;
	}

	private static JsonNode extractIdNode(JsonNode root)
	{
		JsonNode id = root.get("id");
		return id == null ? JsonNodeFactory.instance.nullNode() : id;
	}
}
