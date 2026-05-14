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
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;

/**
 * Handler for {@code assurance.saveApplicationConfiguration}. Accepts
 * {@code { "configuration": ApplicationConfiguration }} and returns the
 * persisted entity (with {@code id} populated and the parsed list views
 * re-derived) as {@code { "configuration": ApplicationConfiguration }}, per
 * {@code docs/ipc-contract.md}.
 *
 * <p>The handler delegates to a {@link UnaryOperator UnaryOperator&lt;ApplicationConfiguration&gt;}
 * so unit tests can substitute a plain lambda without bringing up a Spring
 * context or a mocking framework. In production,
 * {@link WebSocketIpcServer#buildDefaultDispatcher} wires the operator to
 * {@link EngineFacade#saveApplicationConfiguration(ApplicationConfiguration)},
 * which carries the {@code @Transactional} boundary.
 */
public final class SaveApplicationConfigurationHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(SaveApplicationConfigurationHandler.class);

	private final UnaryOperator<ApplicationConfiguration> persister;

	public SaveApplicationConfigurationHandler(UnaryOperator<ApplicationConfiguration> persister)
	{
		this.persister = persister;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		ApplicationConfiguration incoming = ApplicationConfigurationJson.fromJson(params);

		ApplicationConfiguration saved;
		try
		{
			saved = this.persister.apply(incoming);
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to save application configuration", ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to save application configuration: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		if (saved == null)
		{
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Engine returned a null application configuration after save.");
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("configuration", ApplicationConfigurationJson.toJson(saved));
		return result;
	}
}
