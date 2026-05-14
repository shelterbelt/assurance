/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;

/**
 * Handler for {@code assurance.saveScanDefinition}. Accepts
 * {@code { "scanDefinition": ScanDefinition }} and returns the persisted
 * entity (with {@code id} populated for create) as
 * {@code { "scanDefinition": ScanDefinition }}, per
 * {@code docs/ipc-contract.md}.
 *
 * <p>The handler delegates to a {@link UnaryOperator UnaryOperator&lt;ScanDefinition&gt;}
 * so unit tests can substitute a plain lambda without bringing up a Spring
 * context or a mocking framework. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the operator to
 * {@link EngineFacade#saveScanDefinition(ScanDefinition)}, which carries the
 * {@code @Transactional} boundary.
 */
public final class SaveScanDefinitionHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(SaveScanDefinitionHandler.class);

	private final UnaryOperator<ScanDefinition> persister;

	public SaveScanDefinitionHandler(UnaryOperator<ScanDefinition> persister)
	{
		this.persister = persister;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		ScanDefinition incoming = ScanDefinitionJson.fromJson(params);

		ScanDefinition saved;
		try
		{
			saved = this.persister.apply(incoming);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to save scan definition", ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to save scan definition: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		if (saved == null)
		{
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Engine returned a null scan definition after save.");
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("scanDefinition", ScanDefinitionJson.toJson(saved, true));
		return result;
	}
}
