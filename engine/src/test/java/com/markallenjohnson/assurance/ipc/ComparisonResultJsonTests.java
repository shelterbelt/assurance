/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.Instant;
import java.util.Date;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ComparisonResult;
import com.markallenjohnson.assurance.model.entities.FileAttributes;
import com.markallenjohnson.assurance.model.enums.AssuranceResultReason;
import com.markallenjohnson.assurance.model.enums.AssuranceResultResolution;

public class ComparisonResultJsonTests
{
	@Test
	public void projectsScalarFieldsAndEnumNames()
	{
		ComparisonResult result = newResult(11L);
		result.setReason(AssuranceResultReason.FILE_DIRECTORY_MISMATCH);
		result.setResolution(AssuranceResultResolution.UNRESOLVED);
		result.setResolutionError("");

		ObjectNode node = ComparisonResultJson.toJson(result);

		assertEquals(11L, node.get("id").asLong());
		assertEquals("FILE_DIRECTORY_MISMATCH", node.get("reason").asText());
		assertEquals("UNRESOLVED", node.get("resolution").asText());
		assertEquals("", node.get("resolutionError").asText());
	}

	@Test
	public void omitsSourceWhenNullAndIncludesTargetWhenPresent()
	{
		ComparisonResult result = newResult(12L);
		result.setTarget(new File("/data/target.txt"));

		ObjectNode node = ComparisonResultJson.toJson(result);

		assertFalse("source must be omitted when not set", node.has("source"));
		assertNotNull(node.get("target"));
		assertEquals("/data/target.txt", node.get("target").get("location").asText());
	}

	@Test
	public void includesFileReferenceLocationsForSourceAndTarget()
	{
		ComparisonResult result = newResult(13L);
		result.setSource(new File("/data/source.txt"));
		result.setTarget(new File("/data/target.txt"));

		ObjectNode node = ComparisonResultJson.toJson(result);

		assertEquals("/data/source.txt", node.get("source").get("location").asText());
		assertEquals("/data/target.txt", node.get("target").get("location").asText());
	}

	@Test
	public void inlineFileAttributesProjected()
	{
		ComparisonResult result = newResult(14L);
		// setSource(File) constructs a FileReference wrapping the path and
		// installs a FileAttributes via the protected setSource(FileReference)
		// codepath. We then mutate the resulting attributes object to seed
		// deterministic values for projection assertions (the constructor's
		// own attempt to capture from disk silently produces null fields when
		// the path does not exist, which is fine for a unit test).
		result.setSource(new File("/data/source.txt"));
		FileAttributes attrs = result.getSource().getFileAttributes();
		assertNotNull("setSource(File) should attach a FileAttributes", attrs);
		Date when = new Date(1_700_000_000_000L);
		attrs.setCreationTime(when);
		attrs.setSize(4096L);
		attrs.setIsDirectory(Boolean.FALSE);
		attrs.setIsReadOnly(Boolean.TRUE);
		attrs.setOwner("alice");

		ObjectNode node = ComparisonResultJson.toJson(result);

		JsonNode projected = node.get("source").get("fileAttributes");
		assertNotNull(projected);
		assertEquals(Instant.ofEpochMilli(when.getTime()).toString(),
			projected.get("creationTime").asText());
		assertEquals(4096L, projected.get("size").asLong());
		assertFalse(projected.get("isDirectory").asBoolean());
		assertTrue(projected.get("isReadOnly").asBoolean());
		assertEquals("alice", projected.get("owner").asText());
	}

	@Test
	public void absentFileAttributesNotIncluded()
	{
		ComparisonResult result = newResult(15L);
		result.setSource(new File("/data/file.txt"));
		// setSource(File) installs a FileAttributes via the protected
		// setSource(FileReference) codepath; deliberately clear it so we can
		// assert that the codec omits the field rather than rendering an
		// empty object.
		result.getSource().setFileAttributes(null);

		ObjectNode node = ComparisonResultJson.toJson(result);

		assertFalse("fileAttributes must be omitted when null on the entity",
			node.get("source").has("fileAttributes"));
	}

	@Test
	public void resolutionErrorRendersAsEmptyStringWhenNull()
	{
		ComparisonResult result = newResult(16L);
		result.setResolutionError(null);

		ObjectNode node = ComparisonResultJson.toJson(result);

		assertEquals("", node.get("resolutionError").asText());
	}

	private ComparisonResult newResult(long id)
	{
		ComparisonResult result = new ComparisonResult();
		result.setId(id);
		return result;
	}
}
