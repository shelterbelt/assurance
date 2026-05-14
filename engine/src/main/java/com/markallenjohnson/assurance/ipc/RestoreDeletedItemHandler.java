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
 * Handler for {@code assurance.restoreDeletedItem}. Accepts
 * {@code { "resultId": <int> }} and returns
 * {@code { "resultId": <int>, "scanId": <int> }} once the restore has been
 * started, per {@code docs/ipc-contract.md}.
 *
 * <p>The restore runs asynchronously on the engine. The strategy is chosen
 * server-side from the result's {@code AssuranceResultResolution}; the wire
 * never carries one. Subsequent progress is delivered via server-initiated
 * {@code assurance.restoreStarted} / {@code assurance.restoreProgress} /
 * {@code assurance.restoreCompleted} / {@code assurance.restoreFailed}
 * notifications.
 *
 * <p>The handler delegates to a
 * {@link Function Function&lt;Long, EngineFacade.StartRestoreOutcome&gt;} so
 * unit tests can substitute a plain lambda without bringing up a Spring
 * context. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the function to
 * {@link EngineFacade#startRestoreDeletedItem(Long)}.
 */
public final class RestoreDeletedItemHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(RestoreDeletedItemHandler.class);

	private final Function<Long, EngineFacade.StartRestoreOutcome> starter;

	public RestoreDeletedItemHandler(Function<Long, EngineFacade.StartRestoreOutcome> starter)
	{
		this.starter = starter;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"restoreDeletedItem.params must be an object containing resultId");
		}
		Long resultId = parseResultId(params);

		EngineFacade.StartRestoreOutcome outcome;
		try
		{
			outcome = this.starter.apply(resultId);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to start restore for result " + resultId, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to start restore: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		switch (outcome.getStatus())
		{
			case STARTED:
				ObjectNode ok = JsonNodeFactory.instance.objectNode();
				ok.put("resultId", outcome.getResultId());
				ok.put("scanId", outcome.getScanId());
				return ok;
			case RESULT_NOT_FOUND:
				ObjectNode notFoundData = JsonNodeFactory.instance.objectNode();
				notFoundData.put("resultId", resultId);
				throw new RpcException(
					RpcErrorCodes.ENTITY_NOT_FOUND,
					"No comparison result found with id " + resultId,
					notFoundData);
			case ALREADY_RUNNING:
				ObjectNode runningData = JsonNodeFactory.instance.objectNode();
				runningData.put("resultId", resultId);
				if (outcome.getScanId() != null)
				{
					runningData.put("scanId", outcome.getScanId());
				}
				throw new RpcException(
					RpcErrorCodes.RESTORE_ALREADY_RUNNING,
					"A restore is already running for result " + resultId,
					runningData);
			default:
				throw new RpcException(
					RpcErrorCodes.ENGINE_ERROR,
					"Unexpected start-restore status: " + outcome.getStatus());
		}
	}

	private static Long parseResultId(JsonNode params) throws RpcException
	{
		JsonNode idNode = params.get("resultId");
		if (idNode == null || idNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"restoreDeletedItem.params.resultId is required");
		}
		if (!idNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"restoreDeletedItem.params.resultId must be an integer");
		}
		return idNode.asLong();
	}
}
