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
import com.markallenjohnson.assurance.model.entities.ScanDefinition;

/**
 * Handler for {@code assurance.loadScanDefinitions}. Returns the persisted
 * scan definitions as a JSON-RPC result of the form
 * {@code { "scanDefinitions": [ ... ] }}, per
 * {@code docs/ipc-contract.md}.
 *
 * <p>Each element is a full {@link ScanDefinition} wire projection, including
 * {@code scanMapping} (source/target paths and exclusions), so clients can open
 * the definition editor without a second round-trip. Lazy associations are
 * initialised inside {@link EngineFacade#loadScanDefinitions()} before the
 * persistence context closes; this handler only maps already-loaded graphs to
 * JSON.
 *
 * <p>The handler depends on a {@code Supplier<List<ScanDefinition>>} rather
 * than directly on {@link EngineFacade} so unit tests can substitute a plain
 * lambda without bringing up a Spring context or a mocking framework. In
 * production, {@link WebSocketIpcServer#buildDefaultDispatcher} wires the
 * supplier to {@link EngineFacade#loadScanDefinitions()}, which carries the
 * {@code @Transactional} boundary.
 */
public final class LoadScanDefinitionsHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(LoadScanDefinitionsHandler.class);

	private final Supplier<List<ScanDefinition>> source;

	public LoadScanDefinitionsHandler(Supplier<List<ScanDefinition>> source)
	{
		this.source = source;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		List<ScanDefinition> definitions;
		try
		{
			definitions = this.source.get();
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to load scan definitions", ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to load scan definitions: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		ArrayNode array = JsonNodeFactory.instance.arrayNode(definitions == null ? 0 : definitions.size());
		if (definitions != null)
		{
			for (ScanDefinition def : definitions)
			{
				array.add(ScanDefinitionJson.toJson(def, true));
			}
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("scanDefinitions", array);
		return result;
	}
}
