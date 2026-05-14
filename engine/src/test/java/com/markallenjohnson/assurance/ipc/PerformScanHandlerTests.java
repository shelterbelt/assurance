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
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PerformScanHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	private static EngineFacade.StartScanOutcome outcome(EngineFacade.StartScanStatus status, Long scanId) throws Exception
	{
		// StartScanOutcome is intentionally non-public-constructable; reflectively
		// build instances so the handler can be tested in isolation without
		// pulling in a Spring context or a mocking framework.
		Constructor<EngineFacade.StartScanOutcome> ctor = EngineFacade.StartScanOutcome.class
			.getDeclaredConstructor(EngineFacade.StartScanStatus.class, Long.class);
		ctor.setAccessible(true);
		return ctor.newInstance(status, scanId);
	}

	@Test
	public void startedReturnsScanIdResult() throws Exception
	{
		AtomicReference<Long> seenDefId = new AtomicReference<>();
		AtomicReference<Boolean> seenMerge = new AtomicReference<>();
		PerformScanHandler handler = new PerformScanHandler((defId, merge) -> {
			seenDefId.set(defId);
			seenMerge.set(merge);
			try { return outcome(EngineFacade.StartScanStatus.STARTED, 99L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"scanDefinitionId\":7,\"merge\":true}"));
		assertEquals(99L, result.get("scanId").asLong());
		assertEquals(Long.valueOf(7L), seenDefId.get());
		assertEquals(Boolean.TRUE, seenMerge.get());
	}

	@Test
	public void mergeFlagDefaultsToFalseWhenOmitted() throws Exception
	{
		AtomicReference<Boolean> seenMerge = new AtomicReference<>();
		PerformScanHandler handler = new PerformScanHandler((defId, merge) -> {
			seenMerge.set(merge);
			try { return outcome(EngineFacade.StartScanStatus.STARTED, 1L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		handler.invoke(mapper.readTree("{\"scanDefinitionId\":1}"));
		assertEquals(Boolean.FALSE, seenMerge.get());
	}

	@Test
	public void definitionNotFoundMapsToEntityNotFound() throws Exception
	{
		PerformScanHandler handler = new PerformScanHandler((defId, merge) -> {
			try { return outcome(EngineFacade.StartScanStatus.DEFINITION_NOT_FOUND, null); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":42}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertEquals(42L, ex.getDataNode().get("scanDefinitionId").asLong());
		}
	}

	@Test
	public void alreadyRunningMapsToScanAlreadyRunning() throws Exception
	{
		PerformScanHandler handler = new PerformScanHandler((defId, merge) -> {
			try { return outcome(EngineFacade.StartScanStatus.ALREADY_RUNNING, 314L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":7}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.SCAN_ALREADY_RUNNING, ex.getCode());
			assertEquals(7L, ex.getDataNode().get("scanDefinitionId").asLong());
			assertEquals(314L, ex.getDataNode().get("scanId").asLong());
		}
	}

	@Test
	public void invalidParamsAreReportedBeforeStarter() throws Exception
	{
		PerformScanHandler handler = new PerformScanHandler((defId, merge) -> {
			throw new AssertionError("starter must not be invoked for invalid params");
		});

		try
		{
			handler.invoke(mapper.readTree("{}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":\"x\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":1,\"merge\":\"yes\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}
	}

	@Test
	public void runtimeFailureFromStarterMapsToEngineError() throws Exception
	{
		PerformScanHandler handler = new PerformScanHandler((defId, merge) -> {
			throw new RuntimeException("boom");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanDefinitionId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("boom"));
		}
	}
}
