/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.io.File;
import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileAttributes;
import com.markallenjohnson.assurance.model.entities.FileReference;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

/**
 * Codec for {@link ComparisonResult} payloads on the JSON-RPC wire.
 *
 * <p>Mirrors the {@link ScanDefinitionJson} / {@link ScanJson} pattern. The
 * wire shape mirrors the persisted Java entity field set documented in
 * {@code docs/ipc-contract.md}:
 *
 * <ul>
 *   <li>{@code id} (Long).</li>
 *   <li>{@code source} / {@code target} as nested {@code FileReference}
 *       objects ({@code id}, {@code location}, optional inline
 *       {@code fileAttributes}). Either may be omitted entirely when the
 *       engine has no reference to project (e.g. a
 *       {@code SOURCE_DOES_NOT_EXIST} result has no source file
 *       reference).</li>
 *   <li>{@code reason} / {@code resolution} as Java enum names
 *       ({@link AssuranceResultReason} / {@link AssuranceResultResolution}).</li>
 *   <li>{@code resolutionError} as a string (empty when none was
 *       recorded).</li>
 * </ul>
 *
 * <p>Like the other codec helpers, this is one-way (server → client) for
 * now: clients reference results by {@code resultId}, never by sending a
 * full {@code ComparisonResult} payload back.
 */
final class ComparisonResultJson
{
	private ComparisonResultJson()
	{
	}

	static ObjectNode toJson(ComparisonResult result)
	{
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("id", result.getId());

		ObjectNode source = fileReferenceToJson(result.getSource());
		if (source != null)
		{
			node.set("source", source);
		}
		ObjectNode target = fileReferenceToJson(result.getTarget());
		if (target != null)
		{
			node.set("target", target);
		}

		AssuranceResultReason reason = result.getReason();
		if (reason != null)
		{
			node.put("reason", reason.name());
		}
		AssuranceResultResolution resolution = result.getResolution();
		if (resolution != null)
		{
			node.put("resolution", resolution.name());
		}
		node.put("resolutionError",
			result.getResolutionError() == null ? "" : result.getResolutionError());
		return node;
	}

	private static ObjectNode fileReferenceToJson(FileReference reference)
	{
		if (reference == null)
		{
			return null;
		}
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("id", reference.getId());
		File file = reference.getFile();
		if (file != null)
		{
			node.put("location", file.getPath());
		}
		FileAttributes attributes = reference.getFileAttributes();
		if (attributes != null)
		{
			node.set("fileAttributes", fileAttributesToJson(attributes));
		}
		return node;
	}

	private static ObjectNode fileAttributesToJson(FileAttributes attrs)
	{
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("id", attrs.getId());
		putOptionalString(node, "contentsHash", attrs.getContentsHash());
		putOptionalIso(node, "creationTime", attrs.getCreationTime());
		putOptionalBool(node, "isDirectory", attrs.getIsDirectory());
		putOptionalBool(node, "isOther", attrs.getIsOther());
		putOptionalBool(node, "isRegularFile", attrs.getIsRegularFile());
		putOptionalBool(node, "isSymbolicLink", attrs.getIsSymbolicLink());
		putOptionalIso(node, "lastAccessTime", attrs.getLastAccessTime());
		putOptionalIso(node, "lastModifiedTime", attrs.getLastModifiedTime());
		if (attrs.getSize() != null)
		{
			node.put("size", attrs.getSize());
		}
		putOptionalBool(node, "isArchive", attrs.getIsArchive());
		putOptionalBool(node, "isHidden", attrs.getIsHidden());
		putOptionalBool(node, "isReadOnly", attrs.getIsReadOnly());
		putOptionalBool(node, "isSystem", attrs.getIsSystem());
		putOptionalString(node, "groupName", attrs.getGroupName());
		putOptionalString(node, "owner", attrs.getOwner());
		putOptionalString(node, "permissions", attrs.getPermissions());
		putOptionalString(node, "fileOwner", attrs.getFileOwner());
		putOptionalString(node, "aclDescription", attrs.getAclDescription());
		putOptionalString(node, "userDefinedAttributesHash", attrs.getUserDefinedAttributesHash());
		return node;
	}

	private static void putOptionalString(ObjectNode node, String field, String value)
	{
		if (value != null)
		{
			node.put(field, value);
		}
	}

	private static void putOptionalBool(ObjectNode node, String field, Boolean value)
	{
		if (value != null)
		{
			node.put(field, value.booleanValue());
		}
	}

	private static void putOptionalIso(ObjectNode node, String field, Date date)
	{
		if (date == null)
		{
			return;
		}
		node.put(field, Instant.ofEpochMilli(date.getTime()).toString());
	}
}
