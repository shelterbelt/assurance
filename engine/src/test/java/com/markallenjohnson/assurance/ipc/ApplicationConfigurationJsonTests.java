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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.markallenjohnson.assurance.model.entities.ApplicationConfiguration;

public class ApplicationConfigurationJsonTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void toJsonProjectsAllScalarFieldsAndDerivedListViews()
	{
		ApplicationConfiguration config = new ApplicationConfiguration();
		config.setId(1L);
		config.setIgnoredFileNames(".DS_Store, Thumbs.db");
		config.setIgnoredFileExtensions("*.tmp, *.bak");
		config.setNumberOfScanThreads(8);

		ObjectNode node = ApplicationConfigurationJson.toJson(config);

		assertEquals(1L, node.get("id").asLong());
		assertEquals(".DS_Store, Thumbs.db", node.get("ignoredFileNames").asText());
		assertEquals("*.tmp, *.bak", node.get("ignoredFileExtensions").asText());
		assertEquals(8, node.get("numberOfScanThreads").asInt());

		assertTrue(node.get("ignoredFileNamesCollection").isArray());
		assertEquals(2, node.get("ignoredFileNamesCollection").size());
		assertEquals(".ds_store", node.get("ignoredFileNamesCollection").get(0).asText());
		assertEquals("thumbs.db", node.get("ignoredFileNamesCollection").get(1).asText());

		assertTrue(node.get("ignoredFileExtensionsCollection").isArray());
		assertEquals(2, node.get("ignoredFileExtensionsCollection").size());
		assertEquals("tmp", node.get("ignoredFileExtensionsCollection").get(0).asText());
		assertEquals("bak", node.get("ignoredFileExtensionsCollection").get(1).asText());
	}

	@Test
	public void toJsonOmitsIdAndThreadsWhenAbsent()
	{
		ApplicationConfiguration config = new ApplicationConfiguration();
		config.setIgnoredFileNames("");
		config.setIgnoredFileExtensions("");

		ObjectNode node = ApplicationConfigurationJson.toJson(config);

		assertFalse("id must be omitted when null", node.has("id"));
		assertFalse("numberOfScanThreads must be omitted when null", node.has("numberOfScanThreads"));
		assertEquals("", node.get("ignoredFileNames").asText());
		assertEquals("", node.get("ignoredFileExtensions").asText());
	}

	@Test
	public void toJsonCoercesNullStringsToEmpty()
	{
		ApplicationConfiguration config = new ApplicationConfiguration();
		// Deliberately do not call any setIgnoredFile*; both scalars are null.
		ObjectNode node = ApplicationConfigurationJson.toJson(config);
		assertEquals("", node.get("ignoredFileNames").asText());
		assertEquals("", node.get("ignoredFileExtensions").asText());
	}

	@Test
	public void fromJsonRoundTripsCreatePayload() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{"
				+ "\"ignoredFileNames\":\".DS_Store\","
				+ "\"ignoredFileExtensions\":\"*.tmp\","
				+ "\"numberOfScanThreads\":4"
				+ "}}");

		ApplicationConfiguration parsed = ApplicationConfigurationJson.fromJson(params);

		assertNull("id should be absent on create payload", parsed.getId());
		assertEquals(".DS_Store", parsed.getIgnoredFileNames());
		assertEquals("*.tmp", parsed.getIgnoredFileExtensions());
		assertEquals(Integer.valueOf(4), parsed.getNumberOfScanThreads());
	}

	@Test
	public void fromJsonRoundTripsUpdatePayloadPreservingId() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"id\":7,\"ignoredFileNames\":\"\",\"ignoredFileExtensions\":\"\",\"numberOfScanThreads\":2}}");

		ApplicationConfiguration parsed = ApplicationConfigurationJson.fromJson(params);

		assertEquals(Long.valueOf(7L), parsed.getId());
		assertEquals(Integer.valueOf(2), parsed.getNumberOfScanThreads());
	}

	@Test
	public void fromJsonTreatsMissingScalarsAsEmptyStrings() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"numberOfScanThreads\":4}}");

		ApplicationConfiguration parsed = ApplicationConfigurationJson.fromJson(params);

		assertEquals("", parsed.getIgnoredFileNames());
		assertEquals("", parsed.getIgnoredFileExtensions());
	}

	@Test
	public void fromJsonTreatsExplicitNullScalarsAsEmptyStrings() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"ignoredFileNames\":null,\"ignoredFileExtensions\":null}}");

		ApplicationConfiguration parsed = ApplicationConfigurationJson.fromJson(params);

		assertEquals("", parsed.getIgnoredFileNames());
		assertEquals("", parsed.getIgnoredFileExtensions());
	}

	@Test
	public void fromJsonRequiresConfigurationField() throws Exception
	{
		try
		{
			ApplicationConfigurationJson.fromJson(mapper.readTree("{}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void fromJsonRejectsNonObjectParams() throws Exception
	{
		try
		{
			ApplicationConfigurationJson.fromJson(mapper.readTree("[]"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void fromJsonRejectsNonIntegerThreadCount() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"numberOfScanThreads\":\"four\"}}");
		try
		{
			ApplicationConfigurationJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void fromJsonRejectsZeroThreadCount() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"numberOfScanThreads\":0}}");
		try
		{
			ApplicationConfigurationJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void fromJsonRejectsNonIntegerId() throws Exception
	{
		JsonNode params = mapper.readTree(
			"{\"configuration\":{\"id\":\"notanint\"}}");
		try
		{
			ApplicationConfigurationJson.fromJson(params);
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}
}
