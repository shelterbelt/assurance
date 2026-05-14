/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.Scan;
import com.markallenjohnson.assurance.model.entities.ScanDefinition;

/**
 * Codec for {@link Scan} payloads on the JSON-RPC wire.
 *
 * <p>Centralises the field-by-field projection so every method that produces
 * a {@code Scan} (currently {@code assurance.loadScans}, with comparison-results
 * loaders to follow) renders the same shape. The wire shape mirrors the
 * persisted Java entity field set documented in {@code docs/ipc-contract.md}:
 *
 * <ul>
 *   <li>{@code id} (Long) — engine-assigned scan id.</li>
 *   <li>{@code scanStarted} / {@code whenCompleted} — ISO 8601 strings (UTC),
 *       per the contract's "Timestamps are ISO 8601 strings in UTC" rule.
 *       {@code whenCompleted} is {@code null} for an in-flight or aborted
 *       scan.</li>
 *   <li>{@code scanDef} — a thin reference object carrying just the
 *       definition's {@code id} and {@code name}. The full scan-definition
 *       graph (mappings, exclusions, merge strategy) is not included; UIs
 *       that need it call {@code assurance.loadScanDefinitions} or load the
 *       definition explicitly.</li>
 *   <li>{@code resultCount} — convenience scalar derived from the lazy
 *       {@code results} collection. Populated only when
 *       {@code includeResultCount} is requested and the collection is
 *       initialized; otherwise omitted.</li>
 * </ul>
 *
 * <p>Like {@link ScanDefinitionJson}, this codec is intentionally one-way
 * (server → client) for now. Inbound {@code Scan} payloads are not yet a
 * use case (clients reference scans by {@code scanId}).
 */
final class ScanJson
{
	private ScanJson()
	{
	}

	/**
	 * Projects a managed {@link Scan} into its wire shape, including a
	 * {@code resultCount} computed from the lazy {@code results} collection.
	 * Callers must ensure the persistence context is open (or the collection
	 * pre-initialized) when {@code includeResultCount} is {@code true}.
	 */
	static ObjectNode toJson(Scan scan, boolean includeResultCount)
	{
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("id", scan.getId());
		setIso(node, "scanStarted", scan.getScanStarted());
		setIso(node, "whenCompleted", scan.getScanCompleted());
		ScanDefinition def = scan.getScanDef();
		if (def != null)
		{
			ObjectNode defNode = JsonNodeFactory.instance.objectNode();
			defNode.put("id", def.getId());
			defNode.put("name", def.getName());
			node.set("scanDef", defNode);
		}
		if (includeResultCount)
		{
			Collection<ComparisonResult> results = scan.getUnmodifiableResults();
			node.put("resultCount", results == null ? 0 : results.size());
		}
		return node;
	}

	private static void setIso(ObjectNode node, String field, Date date)
	{
		if (date == null)
		{
			node.putNull(field);
			return;
		}
		node.put(field, Instant.ofEpochMilli(date.getTime()).toString());
	}
}
