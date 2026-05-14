/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;

/**
 * Handler for {@code assurance.loadApplicationConfiguration}. Returns the
 * persisted application configuration as
 * {@code { "configuration": ApplicationConfiguration }}, per
 * {@code docs/ipc-contract.md}.
 *
 * <p>The handler depends on a {@code Supplier<ApplicationConfiguration>}
 * rather than directly on {@link EngineFacade} so unit tests can substitute a
 * plain lambda without bringing up a Spring context or a mocking framework.
 * In production, {@link WebSocketIpcServer#buildDefaultDispatcher} wires the
 * supplier to {@link EngineFacade#loadApplicationConfiguration()}, which
 * carries the {@code @Transactional} boundary.
 */
public final class LoadApplicationConfigurationHandler implements RpcDispatcher.RpcMethod
{
	private static final Logger logger = LogManager.getLogger(LoadApplicationConfigurationHandler.class);

	private final Supplier<ApplicationConfiguration> source;

	public LoadApplicationConfigurationHandler(Supplier<ApplicationConfiguration> source)
	{
		this.source = source;
	}

	@Override
	public JsonNode invoke(JsonNode params) throws RpcException
	{
		ApplicationConfiguration config;
		try
		{
			config = this.source.get();
		}
		catch (RuntimeException ex)
		{
			logger.error("Failed to load application configuration", ex);
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Failed to load application configuration: " + ex.getMessage(),
				JsonNodeFactory.instance.textNode(ex.getClass().getSimpleName()),
				ex);
		}

		if (config == null)
		{
			throw new RpcException(
				RpcErrorCodes.ENGINE_ERROR,
				"Engine returned a null application configuration.");
		}

		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("configuration", ApplicationConfigurationJson.toJson(config));
		return result;
	}
}
