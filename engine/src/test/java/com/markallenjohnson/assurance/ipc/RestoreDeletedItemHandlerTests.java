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

public class RestoreDeletedItemHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	private static EngineFacade.StartRestoreOutcome outcome(
		EngineFacade.StartRestoreStatus status, Long resultId, Long scanId) throws Exception
	{
		// StartRestoreOutcome is intentionally non-public-constructable;
		// reflectively build instances so the handler can be tested in
		// isolation without pulling in a Spring context or a mocking framework.
		Constructor<EngineFacade.StartRestoreOutcome> ctor =
			EngineFacade.StartRestoreOutcome.class.getDeclaredConstructor(
				EngineFacade.StartRestoreStatus.class, Long.class, Long.class);
		ctor.setAccessible(true);
		return ctor.newInstance(status, resultId, scanId);
	}

	@Test
	public void startedReturnsResultIdAndScanId() throws Exception
	{
		AtomicReference<Long> seenResultId = new AtomicReference<>();
		RestoreDeletedItemHandler handler = new RestoreDeletedItemHandler((resultId) -> {
			seenResultId.set(resultId);
			try { return outcome(EngineFacade.StartRestoreStatus.STARTED, 7L, 99L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"resultId\":7}"));
		assertEquals(7L, result.get("resultId").asLong());
		assertEquals(99L, result.get("scanId").asLong());
		assertEquals(Long.valueOf(7L), seenResultId.get());
	}

	@Test
	public void resultNotFoundMapsToEntityNotFound() throws Exception
	{
		RestoreDeletedItemHandler handler = new RestoreDeletedItemHandler((resultId) -> {
			try { return outcome(EngineFacade.StartRestoreStatus.RESULT_NOT_FOUND, resultId, null); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":42}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertEquals(42L, ex.getDataNode().get("resultId").asLong());
		}
	}

	@Test
	public void alreadyRunningMapsToRestoreAlreadyRunning() throws Exception
	{
		RestoreDeletedItemHandler handler = new RestoreDeletedItemHandler((resultId) -> {
			try { return outcome(EngineFacade.StartRestoreStatus.ALREADY_RUNNING, 7L, 314L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":7}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.RESTORE_ALREADY_RUNNING, ex.getCode());
			assertEquals(7L, ex.getDataNode().get("resultId").asLong());
			assertEquals(314L, ex.getDataNode().get("scanId").asLong());
		}
	}

	@Test
	public void invalidParamsAreReportedBeforeStarter() throws Exception
	{
		RestoreDeletedItemHandler handler = new RestoreDeletedItemHandler((resultId) -> {
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
			handler.invoke(mapper.readTree("{\"resultId\":\"x\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":null}"));
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
		RestoreDeletedItemHandler handler = new RestoreDeletedItemHandler((resultId) -> {
			throw new RuntimeException("boom");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("boom"));
		}
	}
}
