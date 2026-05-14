/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.io.File;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;
import com.markallenjohnson.assurance.model.entities.ScanMappingDefinition;
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

/**
 * Codec for {@link ScanDefinition} payloads on the JSON-RPC wire.
 *
 * <p>Centralises the field-by-field projection so every method that produces
 * or consumes a {@code ScanDefinition} (currently
 * {@code assurance.loadScanDefinitions}, {@code assurance.saveScanDefinition},
 * with more to come) renders the same shape. The wire shape mirrors the
 * persisted Java entity field set documented in {@code docs/ipc-contract.md}:
 *
 * <ul>
 *   <li>{@code id}, {@code name}, {@code mergeStrategy} (enum name),
 *       {@code autoResolveConflicts}, {@code includeNonCreationTimestamps},
 *       {@code includeAdvancedAttributes} as scalars on {@code ScanDefinition}.</li>
 *   <li>Optional {@code scanMapping}: array of {@link ScanMappingDefinition}
 *       projections, each with {@code source} / {@code target} as
 *       {@link FileReference} objects (carrying {@code location}) and
 *       {@code exclusions} as an array of {@code FileReference}.</li>
 * </ul>
 *
 * <p>Reverse-direction parsing is permissive: optional fields are tolerated
 * as missing or {@code null} so create-vs-update payloads (which omit
 * {@code id}) and the single-mapping UI (which sends a flat
 * {@code source}/{@code target} pair) round-trip without ceremony. Parse
 * errors raise {@link RpcException} with {@link RpcErrorCodes#INVALID_PARAMS}.
 */
final class ScanDefinitionJson
{
	private ScanDefinitionJson()
	{
	}

	static ObjectNode toJson(ScanDefinition def)
	{
		return toJson(def, true);
	}

	/**
	 * Projects a managed {@link ScanDefinition} into its wire shape.
	 *
	 * @param includeMappings when {@code true}, the {@code scanMapping} array is
	 *   populated by walking the (initialized) child collection. Most callers
	 *   pass {@code true} ({@code assurance.loadScanDefinitions},
	 *   {@code assurance.saveScanDefinition}); tests may pass {@code false} to
	 *   assert scalar-only projections.
	 */
	static ObjectNode toJson(ScanDefinition def, boolean includeMappings)
	{
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("id", def.getId());
		node.put("name", def.getName());
		AssuranceMergeStrategy strategy = def.getMergeStrategy();
		if (strategy != null)
		{
			node.put("mergeStrategy", strategy.name());
		}
		node.put("autoResolveConflicts", Boolean.TRUE.equals(def.getAutoResolveConflicts()));
		node.put("includeNonCreationTimestamps", Boolean.TRUE.equals(def.getIncludeNonCreationTimestamps()));
		node.put("includeAdvancedAttributes", Boolean.TRUE.equals(def.getIncludeAdvancedAttributes()));

		if (includeMappings)
		{
			ArrayNode mappings = node.putArray("scanMapping");
			Collection<ScanMappingDefinition> children = def.getUnmodifiableScanMapping();
			if (children != null)
			{
				for (ScanMappingDefinition child : children)
				{
					mappings.add(mappingToJson(child));
				}
			}
		}

		return node;
	}

	private static ObjectNode mappingToJson(ScanMappingDefinition mapping)
	{
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("id", mapping.getId());
		node.set("source", fileReferenceToJson(mapping.getSource()));
		node.set("target", fileReferenceToJson(mapping.getTarget()));
		ArrayNode exclusions = node.putArray("exclusions");
		Collection<FileReference> excl = mapping.getUnmodifiableExclusions();
		if (excl != null)
		{
			for (FileReference ref : excl)
			{
				exclusions.add(fileReferenceToJson(ref == null ? null : ref.getFile()));
			}
		}
		return node;
	}

	private static JsonNode fileReferenceToJson(File file)
	{
		if (file == null)
		{
			return JsonNodeFactory.instance.nullNode();
		}
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("location", file.getPath());
		return node;
	}

	/**
	 * Builds a transient {@link ScanDefinition} from a wire payload. The
	 * returned entity is detached; callers are expected to merge it into a
	 * persistence context.
	 */
	static ScanDefinition fromJson(JsonNode params) throws RpcException
	{
		if (params == null || !params.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"saveScanDefinition.params must be an object containing scanDefinition");
		}
		JsonNode body = params.get("scanDefinition");
		if (body == null || !body.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"saveScanDefinition.params.scanDefinition is required and must be an object");
		}

		ScanDefinition def = new ScanDefinition();

		JsonNode idNode = body.get("id");
		if (idNode != null && !idNode.isNull())
		{
			if (!idNode.canConvertToLong())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"scanDefinition.id must be an integer when present");
			}
			def.setId(idNode.asLong());
		}

		JsonNode nameNode = body.get("name");
		if (nameNode == null || !nameNode.isTextual() || nameNode.asText().isEmpty())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"scanDefinition.name is required and must be a non-empty string");
		}
		def.setName(nameNode.asText());

		JsonNode strategyNode = body.get("mergeStrategy");
		if (strategyNode != null && !strategyNode.isNull())
		{
			if (!strategyNode.isTextual())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"scanDefinition.mergeStrategy must be a string when present");
			}
			AssuranceMergeStrategy strategy;
			try
			{
				strategy = AssuranceMergeStrategy.valueOf(strategyNode.asText());
			}
			catch (IllegalArgumentException ex)
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"scanDefinition.mergeStrategy must be one of SOURCE, TARGET, BOTH");
			}
			def.setMergeStrategy(strategy);
		}

		def.setAutoResolveConflicts(readOptionalBoolean(body, "autoResolveConflicts"));
		def.setIncludeNonCreationTimestamps(readOptionalBoolean(body, "includeNonCreationTimestamps"));
		def.setIncludeAdvancedAttributes(readOptionalBoolean(body, "includeAdvancedAttributes"));

		JsonNode mappings = body.get("scanMapping");
		if (mappings != null && !mappings.isNull())
		{
			if (!mappings.isArray())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"scanDefinition.scanMapping must be an array when present");
			}
			for (JsonNode mappingNode : mappings)
			{
				ScanMappingDefinition mapping = mappingFromJson(mappingNode);
				def.addMappingDefinition(mapping);
			}
		}

		return def;
	}

	private static Boolean readOptionalBoolean(JsonNode body, String field) throws RpcException
	{
		JsonNode node = body.get(field);
		if (node == null || node.isNull())
		{
			return Boolean.FALSE;
		}
		if (!node.isBoolean())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"scanDefinition." + field + " must be a boolean when present");
		}
		return node.asBoolean();
	}

	private static ScanMappingDefinition mappingFromJson(JsonNode node) throws RpcException
	{
		if (node == null || !node.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"scanMapping entries must be objects");
		}

		ScanMappingDefinition mapping = new ScanMappingDefinition();

		JsonNode idNode = node.get("id");
		if (idNode != null && !idNode.isNull())
		{
			if (!idNode.canConvertToLong())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"scanMapping.id must be an integer when present");
			}
			mapping.setId(idNode.asLong());
		}

		File source = readFileReference(node, "source");
		File target = readFileReference(node, "target");
		if (source == null || target == null)
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"scanMapping.source and scanMapping.target are required and must include a non-empty location");
		}
		mapping.setSource(source);
		mapping.setTarget(target);

		JsonNode exclusionsNode = node.get("exclusions");
		if (exclusionsNode != null && !exclusionsNode.isNull())
		{
			if (!exclusionsNode.isArray())
			{
				throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
					"scanMapping.exclusions must be an array when present");
			}
			for (JsonNode exclusion : exclusionsNode)
			{
				File exclusionFile = fileReferenceLocation(exclusion);
				if (exclusionFile != null)
				{
					mapping.addExclusion(new FileReference(exclusionFile));
				}
			}
		}

		return mapping;
	}

	private static File readFileReference(JsonNode container, String field) throws RpcException
	{
		JsonNode node = container.get(field);
		if (node == null || node.isNull())
		{
			return null;
		}
		if (!node.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"scanMapping." + field + " must be a FileReference object when present");
		}
		return fileReferenceLocation(node);
	}

	private static File fileReferenceLocation(JsonNode node) throws RpcException
	{
		if (node == null || !node.isObject())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"FileReference entries must be objects with a location");
		}
		JsonNode locationNode = node.get("location");
		if (locationNode == null || locationNode.isNull())
		{
			return null;
		}
		if (!locationNode.isTextual() || locationNode.asText().isEmpty())
		{
			throw new RpcException(RpcErrorCodes.INVALID_PARAMS,
				"FileReference.location must be a non-empty string when present");
		}
		return new File(locationNode.asText());
	}
}
