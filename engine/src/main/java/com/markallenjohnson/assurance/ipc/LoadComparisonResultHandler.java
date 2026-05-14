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
import com.markallenjohnson.assurance.model.entities.ComparisonResult;

/**
 * Handler for {@code assurance.loadComparisonResult}. Returns one persisted
 * row as {@code { "result": ComparisonResult }} using
 * {@link ComparisonResultJson#toJson(ComparisonResult)}.
 *
 * <p>The backing function returns {@code null} when no result exists with the
 * given id; the handler maps that to JSON-RPC {@code -32002}.
 */
public final class LoadComparisonResultHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(LoadComparisonResultHandler.class);

	private final Function<Long, ComparisonResult> source;

	public LoadComparisonResultHandler(Function<Long, ComparisonResult> source)
	{
		this.source = source;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		Long resultId = readResultId(params);

		ComparisonResult entity;
		try
		{
			entity = this.source.apply(resultId);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to load comparison result " + resultId, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to load comparison result: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		if (entity == null)
		{
			ObjectNode data = JsonNodeFactory.instance.objectNode();
			data.put("resultId", resultId);
			throw new RpcException(
				RpcErrorCodes.ENTITY_NOT_FOUND,
				"No comparison result found with id " + resultId,
				data);
		}

		ObjectNode payload = JsonNodeFactory.instance.objectNode();
		payload.set("result", ComparisonResultJson.toJson(entity));
		return payload;
	}

	private static Long readResultId(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"loadComparisonResult.params must be an object containing resultId");
		}
		JsonNode idNode = params.get("resultId");
		if (idNode == null || idNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"loadComparisonResult.params.resultId is required");
		}
		if (!idNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"loadComparisonResult.params.resultId must be an integer");
		}
		return idNode.asLong();
	}
}
