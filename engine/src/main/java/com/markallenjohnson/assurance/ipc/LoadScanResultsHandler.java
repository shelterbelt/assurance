/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;

/**
 * Handler for {@code assurance.loadScanResults}. Returns the persisted
 * comparison results for a given scan as
 * {@code { "results": ComparisonResult[] }} per
 * {@code docs/ipc-contract.md}.
 *
 * <p>Each element is the projection produced by
 * {@link ComparisonResultJson#toJson(ComparisonResult) ComparisonResultJson.toJson}:
 * scalar identifiers, source/target {@code FileReference}s with optional
 * inline {@code fileAttributes}, the result {@code reason} and
 * {@code resolution} enums (as their Java enum names), and the
 * {@code resolutionError} string.
 *
 * <p>The handler depends on a {@code Function<Long, List<ComparisonResult>>}
 * rather than on {@link EngineFacade} directly so unit tests can substitute
 * a plain lambda. The function returns {@code null} when no scan exists
 * with the given id; the handler maps that to JSON-RPC {@code -32002}
 * (entity not found). In production
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the function to
 * {@link EngineFacade#loadScanResults(Long)}, which carries the
 * {@code @Transactional(readOnly = true)} boundary required to traverse
 * the lazy associations on each result.
 */
public final class LoadScanResultsHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(LoadScanResultsHandler.class);

	private final Function<Long, List<ComparisonResult>> source;

	public LoadScanResultsHandler(Function<Long, List<ComparisonResult>> source)
	{
		this.source = source;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		Long scanId = readScanId(params);

		List<ComparisonResult> results;
		try
		{
			results = this.source.apply(scanId);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to load scan results for scan " + scanId, ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to load scan results: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		if (results == null)
		{
			ObjectNode data = JsonNodeFactory.instance.objectNode();
			data.put("scanId", scanId);
			throw new RpcException(
				RpcErrorCodes.ENTITY_NOT_FOUND,
				"No scan found with id " + scanId,
				data);
		}

		ArrayNode array = JsonNodeFactory.instance.arrayNode(results.size());
		for (ComparisonResult result : results)
		{
			array.add(ComparisonResultJson.toJson(result));
		}

		ObjectNode payload = JsonNodeFactory.instance.objectNode();
		payload.set("results", array);
		return payload;
	}

	private static Long readScanId(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"loadScanResults.params must be an object containing scanId");
		}
		JsonNode scanIdNode = params.get("scanId");
		if (scanIdNode == null || scanIdNode.isNull())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"loadScanResults.params.scanId is required");
		}
		if (!scanIdNode.canConvertToLong())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"loadScanResults.params.scanId must be an integer");
		}
		return scanIdNode.asLong();
	}
}
