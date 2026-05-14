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

public class MergeScanHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	private static EngineFacade.StartWholeScanMergeOutcome outcome(
		EngineFacade.StartWholeScanMergeStatus status, Long scanId) throws Exception
	{
		// StartWholeScanMergeOutcome is intentionally non-public-constructable;
		// reflectively build instances so the handler can be tested in
		// isolation without pulling in a Spring context or a mocking framework.
		Constructor<EngineFacade.StartWholeScanMergeOutcome> ctor =
			EngineFacade.StartWholeScanMergeOutcome.class.getDeclaredConstructor(
				EngineFacade.StartWholeScanMergeStatus.class, Long.class);
		ctor.setAccessible(true);
		return ctor.newInstance(status, scanId);
	}

	@Test
	public void startedReturnsScanId() throws Exception
	{
		AtomicReference<Long> seenScanId = new AtomicReference<>();
		MergeScanHandler handler = new MergeScanHandler((scanId) -> {
			seenScanId.set(scanId);
			try { return outcome(EngineFacade.StartWholeScanMergeStatus.STARTED, scanId); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"scanId\":42}"));
		assertEquals(42L, result.get("scanId").asLong());
		assertEquals(Long.valueOf(42L), seenScanId.get());
	}

	@Test
	public void scanNotFoundMapsToEntityNotFound() throws Exception
	{
		MergeScanHandler handler = new MergeScanHandler((scanId) -> {
			try { return outcome(EngineFacade.StartWholeScanMergeStatus.SCAN_NOT_FOUND, scanId); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":99}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertEquals(99L, ex.getDataNode().get("scanId").asLong());
		}
	}

	@Test
	public void alreadyRunningMapsToMergeAlreadyRunning() throws Exception
	{
		MergeScanHandler handler = new MergeScanHandler((scanId) -> {
			try { return outcome(EngineFacade.StartWholeScanMergeStatus.ALREADY_RUNNING, scanId); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":7}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.MERGE_ALREADY_RUNNING, ex.getCode());
			assertEquals(7L, ex.getDataNode().get("scanId").asLong());
		}
	}

	@Test
	public void invalidParamsAreReportedBeforeStarter() throws Exception
	{
		MergeScanHandler handler = new MergeScanHandler((scanId) -> {
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
			handler.invoke(mapper.readTree("{\"scanId\":\"x\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":null}"));
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
		MergeScanHandler handler = new MergeScanHandler((scanId) -> {
			throw new RuntimeException("boom");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"scanId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("boom"));
		}
	}
}
