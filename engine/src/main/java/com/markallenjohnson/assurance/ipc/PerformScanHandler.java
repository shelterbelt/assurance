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

/**
 * Handler for {@code assurance.performScan}. Accepts
 * {@code { "scanDefinitionId": <int>, "merge": <bool> }} and returns
 * {@code { "scanId": <int> }} once the scan has been started, per
 * {@code docs/ipc-contract.md}.
 *
 * <p>The actual scan executes asynchronously on the engine; the response is
 * sent as soon as the new {@code Scan} record has been persisted (so the id
 * can be quoted) and the work has been submitted to the runner. Subsequent
 * progress is delivered via server-initiated
 * {@code assurance.scanStarted} / {@code assurance.scanProgress} /
 * {@code assurance.scanCompleted} / {@code assurance.scanFailed} notifications.
 *
 * <p>The handler delegates to a
 * {@link BiFunction BiFunction&lt;Long, Boolean, EngineFacade.StartScanOutcome&gt;}
 * so unit tests can substitute a plain lambda without bringing up a Spring
 * context. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the function to
 * {@link EngineFacade#startScan(Long, boolean)}.
 */
public final class PerformScanHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(PerformScanHandler.class);

	private final BiFunction<Long, Boolean, EngineFacade.StartScanOutcome> starter;

	public PerformScanHandler(BiFunction<Long, Boolean, EngineFacade.StartScanOutcome> starter)
	{
		this.starter = starter;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"performScan.params must be an object containing scanDefinitionId");
		}
		Long scanDefinitionId = parseScanDefinitionId(params);
		boolean merge = parseMergeFlag(params);

		EngineFacade.StartScanOutcome outcome;
		try
		{
			outcome = this.starter.apply(scanDefinitionId, merge);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to start scan for definition " + scanDefinitionId, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to start scan: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		switch (outcome.getStatus())
		{
			case STARTED:
				ObjectNode result = JsonNodeFactory.instance.objectNode();
				result.put("scanId", outcome.getScanId());
				return result;
			case DEFINITION_NOT_FOUND:
				ObjectNode notFoundData = JsonNodeFactory.instance.objectNode();
				notFoundData.put("scanDefinitionId", scanDefinitionId);
				throw new RpcException(
					RpcErrorCodes.ENTITY_NOT_FOUND,
					"No scan definition found with id " + scanDefinitionId,
					notFoundData);
			case ALREADY_RUNNING:
				ObjectNode runningData = JsonNodeFactory.instance.objectNode();
				runningData.put("scanDefinitionId", scanDefinitionId);
				if (outcome.getScanId() != null)
				{
					runningData.put("scanId", outcome.getScanId());
				}
				throw new RpcException(
					RpcErrorCodes.SCAN_ALREADY_RUNNING,
					"A scan is already running for definition " + scanDefinitionId,
					runningData);
			default:
				throw new RpcException(
					RpcErrorCodes.ENGINE_ERROR,
					"Unexpected start-scan status: " + outcome.getStatus());
		}
	}

	private static Long parseScanDefinitionId(JsonNode params) throws RpcException
	{
		JsonNode idNode = params.get("scanDefinitionId");
		if (idNode == null || idNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"performScan.params.scanDefinitionId is required");
		}
		if (!idNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"performScan.params.scanDefinitionId must be an integer");
		}
		return idNode.asLong();
	}

	private static boolean parseMergeFlag(JsonNode params) throws RpcException
	{
		JsonNode mergeNode = params.get("merge");
		if (mergeNode == null || mergeNode.isNull())
		{
			return false;
		}
		if (!mergeNode.isBoolean())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"performScan.params.merge must be a boolean when present");
		}
		return mergeNode.asBoolean();
	}
}
