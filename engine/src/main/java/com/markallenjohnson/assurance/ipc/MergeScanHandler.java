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
 * Handler for {@code assurance.mergeScan}. Accepts {@code { "scanId": <int> }}
 * and returns {@code { "scanId": <int> }} once the whole-scan merge has been
 * started, per {@code docs/ipc-contract.md}.
 *
 * <p>The merge runs asynchronously on the engine using the merge strategy
 * stored on the scan's owning {@code ScanDefinition} (mirroring 1.x
 * "Scan and Merge" semantics — the wire never carries a strategy here).
 * Subsequent progress is delivered via server-initiated
 * {@code assurance.mergeStarted} / {@code assurance.mergeProgress} /
 * {@code assurance.mergeCompleted} / {@code assurance.mergeFailed}
 * notifications bound to the same {@code scanId}.
 *
 * <p>The handler delegates to a
 * {@link Function Function&lt;Long, EngineFacade.StartWholeScanMergeOutcome&gt;}
 * so unit tests can substitute a plain lambda without bringing up a Spring
 * context. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the function to
 * {@link EngineFacade#startWholeScanMerge(Long)}.
 */
public final class MergeScanHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(MergeScanHandler.class);

	private final Function<Long, EngineFacade.StartWholeScanMergeOutcome> starter;

	public MergeScanHandler(Function<Long, EngineFacade.StartWholeScanMergeOutcome> starter)
	{
		this.starter = starter;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScan.params must be an object containing scanId");
		}
		Long scanId = parseScanId(params);

		EngineFacade.StartWholeScanMergeOutcome outcome;
		try
		{
			outcome = this.starter.apply(scanId);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to start whole-scan merge for scan " + scanId, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to start whole-scan merge: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		switch (outcome.getStatus())
		{
			case STARTED:
				ObjectNode ok = JsonNodeFactory.instance.objectNode();
				ok.put("scanId", outcome.getScanId());
				return ok;
			case SCAN_NOT_FOUND:
				ObjectNode notFoundData = JsonNodeFactory.instance.objectNode();
				notFoundData.put("scanId", scanId);
				throw new RpcException(
					RpcErrorCodes.ENTITY_NOT_FOUND,
					"No scan found with id " + scanId,
					notFoundData);
			case ALREADY_RUNNING:
				ObjectNode runningData = JsonNodeFactory.instance.objectNode();
				runningData.put("scanId", scanId);
				throw new RpcException(
					RpcErrorCodes.MERGE_ALREADY_RUNNING,
					"A whole-scan merge is already running for scan " + scanId,
					runningData);
			default:
				throw new RpcException(
					RpcErrorCodes.ENGINE_ERROR,
					"Unexpected start-whole-scan-merge status: " + outcome.getStatus());
		}
	}

	private static Long parseScanId(JsonNode params) throws RpcException
	{
		JsonNode idNode = params.get("scanId");
		if (idNode == null || idNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScan.params.scanId is required");
		}
		if (!idNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"mergeScan.params.scanId must be an integer");
		}
		return idNode.asLong();
	}
}
