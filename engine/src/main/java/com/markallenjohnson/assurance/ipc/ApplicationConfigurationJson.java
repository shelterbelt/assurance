/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;

/**
 * Codec for {@link ApplicationConfiguration} payloads on the JSON-RPC wire.
 *
 * <p>Centralises the field-by-field projection so {@code assurance.loadApplicationConfiguration}
 * and {@code assurance.saveApplicationConfiguration} render the same shape.
 * The wire shape mirrors the persisted Java entity field set documented in
 * {@code docs/ipc-contract.md}:
 *
 * <ul>
 *   <li>{@code id} (nullable for first-time saves before a row exists)</li>
 *   <li>{@code ignoredFileNames}, {@code ignoredFileExtensions}: comma-separated
 *       persisted scalars (always projected; empty string when null).</li>
 *   <li>{@code numberOfScanThreads}: integer scalar.</li>
 *   <li>{@code ignoredFileNamesCollection}, {@code ignoredFileExtensionsCollection}:
 *       parsed list views derived from the scalars on the Java side. The IPC
 *       layer projects them so the renderer can display ready-to-render lists
 *       without re-implementing the tokenizer.</li>
 * </ul>
 *
 * <p>Reverse-direction parsing accepts either the bare scalars or the list
 * views (the engine re-derives the lists from the scalars on save, so list
 * views on input are ignored). {@code id} is optional so first-time saves
 * round-trip without ceremony. Parse errors raise {@link RpcException} with
 * {@link RpcErrorCodes#INVALID_PARAMS}.
 */
final class ApplicationConfigurationJson
{
	private ApplicationConfigurationJson()
	{
	}

	static ObjectNode toJson(ApplicationConfiguration config)
	{
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		if (config.getId() != null)
		{
			node.put("id", config.getId());
		}
		node.put("ignoredFileNames", config.getIgnoredFileNames() == null ? "" : config.getIgnoredFileNames());
		node.put("ignoredFileExtensions", config.getIgnoredFileExtensions() == null ? "" : config.getIgnoredFileExtensions());
		if (config.getNumberOfScanThreads() != null)
		{
			node.put("numberOfScanThreads", config.getNumberOfScanThreads());
		}

		node.set("ignoredFileNamesCollection", listToArray(config.getIngnoredFileNames()));
		node.set("ignoredFileExtensionsCollection", listToArray(config.getIngnoredFileExtensions()));

		return node;
	}

	private static ArrayNode listToArray(List<String> values)
	{
		ArrayNode array = JsonNodeFactory.instance.arrayNode();
		if (values != null)
		{
			for (String value : values)
			{
				array.add(value);
			}
		}
		return array;
	}

	/**
	 * Builds a transient {@link ApplicationConfiguration} from a wire payload.
	 * The returned entity is detached; callers are expected to merge it into a
	 * persistence context.
	 */
	static ApplicationConfiguration fromJson(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"saveApplicationConfiguration.params must be an object containing configuration");
		}
		JsonNode body = params.get("configuration");
		if (body == null || !body.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"saveApplicationConfiguration.params.configuration is required and must be an object");
		}

		ApplicationConfiguration config = new ApplicationConfiguration();

		JsonNode idNode = body.get("id");
		if (idNode != null && !idNode.isNull())
		{
			if (!idNode.canConvertToLong())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"configuration.id must be an integer when present");
			}
			config.setId(idNode.asLong());
		}

		config.setIgnoredFileNames(readOptionalString(body, "ignoredFileNames"));
		config.setIgnoredFileExtensions(readOptionalString(body, "ignoredFileExtensions"));

		JsonNode threadsNode = body.get("numberOfScanThreads");
		if (threadsNode != null && !threadsNode.isNull())
		{
			if (!threadsNode.canConvertToInt())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"configuration.numberOfScanThreads must be an integer when present");
			}
			int value = threadsNode.asInt();
			if (value < 1)
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"configuration.numberOfScanThreads must be >= 1");
			}
			config.setNumberOfScanThreads(value);
		}

		return config;
	}

	private static String readOptionalString(JsonNode body, String field) throws RpcException
	{
		JsonNode node = body.get(field);
		if (node == null || node.isNull())
		{
			return "";
		}
		if (!node.isTextual())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"configuration." + field + " must be a string when present");
		}
		return node.asText();
	}
}
