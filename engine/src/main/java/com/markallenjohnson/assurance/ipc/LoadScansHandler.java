/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.Scan;

/**
 * Handler for {@code assurance.loadScans}. Returns the persisted scan history
 * as a JSON-RPC result of the form {@code { "scans": [ ... ] }}, per
 * {@code docs/ipc-contract.md}.
 *
 * <p>Each element is the thin projection produced by
 * {@link ScanJson#toJson(Scan, boolean) ScanJson.toJson} with
 * {@code includeResultCount=true}: scalar identifiers, ISO 8601 timestamps, a
 * minimal {@code scanDef} reference, and a {@code resultCount} scalar. The
 * heavy {@code results} collection is intentionally omitted; UIs that need
 * the full results call {@code assurance.loadScanResults} for a specific
 * scan id.
 *
 * <p>The handler depends on a {@code Supplier<List<Scan>>} rather than
 * directly on {@link EngineFacade} so unit tests can substitute a plain
 * lambda. In production, {@link WebSocketIpcServer#buildDefaultDispatcher}
 * wires the supplier to {@link EngineFacade#loadScans()}, which carries the
 * {@code @Transactional(readOnly = true)} boundary required to keep the
 * persistence context open while {@link ScanJson} touches the lazy
 * {@code results} collection.
 */
public final class LoadScansHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(LoadScansHandler.class);

	private final Supplier<List<Scan>> source;

	public LoadScansHandler(Supplier<List<Scan>> source)
	{
		this.source = source;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		List<Scan> scans;
		try
		{
			scans = this.source.get();
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to load scans", ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to load scans: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		ArrayNode array = JsonNodeFactory.instance.arrayNode(scans == null ? 0 : scans.size());
		if (scans != null)
		{
			for (Scan scan : scans)
			{
				array.add(ScanJson.toJson(scan, true));
			}
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("scans", array);
		return result;
	}
}
