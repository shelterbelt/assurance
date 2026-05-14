/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.function.BiFunction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

/**
 * Handler for {@code assurance.mergeScanResult}. Accepts
 * {@code { "resultId": <int>, "strategy": "SOURCE" | "TARGET" | "BOTH" }}
 * and returns {@code { "resultId": <int>, "scanId": <int> }} once the merge
 * has been started, per {@code docs/ipc-contract.md}.
 *
 * <p>The merge runs asynchronously on the engine. Subsequent progress is
 * delivered via server-initiated
 * {@code assurance.resultMergeStarted} / {@code assurance.resultMergeProgress}
 * / {@code assurance.resultMergeCompleted} /
 * {@code assurance.resultMergeFailed} notifications.
 *
 * <p>The handler delegates to a
 * {@link BiFunction BiFunction&lt;Long, AssuranceMergeStrategy, EngineFacade.StartResultMergeOutcome&gt;}
 * so unit tests can substitute a plain lambda without bringing up a Spring
 * context. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the function to
 * {@link EngineFacade#startScanResultMerge(Long, AssuranceMergeStrategy)}.
 */
public final class MergeScanResultHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(MergeScanResultHandler.class);

	private final BiFunction<Long, AssuranceMergeStrategy, EngineFacade.StartResultMergeOutcome> starter;

	public MergeScanResultHandler(
		BiFunction<Long, AssuranceMergeStrategy, EngineFacade.StartResultMergeOutcome> starter)
	{
		this.starter = starter;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScanResult.params must be an object containing resultId and strategy");
		}
		Long resultId = parseResultId(params);
		AssuranceMergeStrategy strategy = parseStrategy(params);

		EngineFacade.StartResultMergeOutcome outcome;
		try
		{
			outcome = this.starter.apply(resultId, strategy);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to start result merge for result " + resultId, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to start result merge: " + ex.getMessage(),
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
					RpcErrorCodes.RESULT_MERGE_ALREADY_RUNNING,
					"A merge is already running for result " + resultId,
					runningData);
			default:
				throw new RpcException(
					RpcErrorCodes.ENGINE_ERROR,
					"Unexpected start-result-merge status: " + outcome.getStatus());
		}
	}

	private static Long parseResultId(JsonNode params) throws RpcException
	{
		JsonNode idNode = params.get("resultId");
		if (idNode == null || idNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScanResult.params.resultId is required");
		}
		if (!idNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScanResult.params.resultId must be an integer");
		}
		return idNode.asLong();
	}

	private static AssuranceMergeStrategy parseStrategy(JsonNode params) throws RpcException
	{
		JsonNode strategyNode = params.get("strategy");
		if (strategyNode == null || strategyNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScanResult.params.strategy is required");
		}
		if (!strategyNode.isTextual())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScanResult.params.strategy must be a string");
		}
		String raw = strategyNode.asText();
		try
		{
			return AssuranceMergeStrategy.valueOf(raw);
		}
		catch (IllegalArgumentException ex)
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScanResult.params.strategy must be one of SOURCE | TARGET | BOTH; got: " + raw);
		}
	}
}
