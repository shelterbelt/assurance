/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handler for {@code assurance.deleteScan}. Accepts
 * {@code { "scanId": <int> }} and returns {@code { "deleted": true }} on
 * success, per {@code docs/ipc-contract.md}.
 *
 * <p>Mirrors {@link DeleteScanDefinitionHandler}: delegates to a
 * {@link Function Function&lt;Long, Boolean&gt;} so unit tests can substitute a
 * plain lambda without bringing up a Spring context. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the function to
 * {@link EngineFacade#deleteScanById(Long)}, which carries the
 * {@code @Transactional} boundary.
 *
 * <p>A {@code false} return from the deleter indicates the entity did not
 * exist; the handler maps that to JSON-RPC {@code -32002} (entity not found).
 * Any {@link RuntimeException} thrown by the deleter maps to {@code -32001}
 * (engine error).
 */
public final class DeleteScanHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(DeleteScanHandler.class);

	private final Function<Long, Boolean> deleter;

	public DeleteScanHandler(Function<Long, Boolean> deleter)
	{
		this.deleter = deleter;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		Long id = parseScanId(params);

		boolean deleted;
		try
		{
			Boolean outcome = this.deleter.apply(id);
			deleted = Boolean.TRUE.equals(outcome);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to delete scan " + id, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to delete scan: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		if (!deleted)
		{
			ObjectNode data = JsonNodeFactory.instance.objectNode();
			data.put("scanId", id);
			throw new RpcException(
				RpcErrorCodes.ENTITY_NOT_FOUND,
				"No scan found with id " + id,
				data);
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put("deleted", true);
		return result;
	}

	private static Long parseScanId(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"deleteScan.params must be an object containing scanId");
		}
		JsonNode idNode = params.get("scanId");
		if (idNode == null || idNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"deleteScan.params.scanId is required");
		}
		if (!idNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"deleteScan.params.scanId must be an integer");
		}
		return idNode.asLong();
	}
}
