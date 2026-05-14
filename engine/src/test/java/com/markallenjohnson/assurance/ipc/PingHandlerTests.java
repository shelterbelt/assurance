/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class PingHandlerTests
{
	@Test
	public void returnsPongAndIsoTimestamp() throws Exception
	{
		Clock fixed = Clock.fixed(Instant.parse("2026-05-05T20:30:40Z"), ZoneOffset.UTC);
		PingHandler handler = new PingHandler(fixed);

		JsonNode result = handler.invoke(null);

		assertTrue("Expected pong field", result.get("pong").asBoolean());
		assertEquals("2026-05-05T20:30:40Z", result.get("timestamp").asText());
	}
}
