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
import com.markallenjohnson.assurance.model.enums.AssuranceMergeStrategy;

public class MergeScanResultHandlerTests
{
	private final ObjectMapper mapper = new ObjectMapper();

	private static EngineFacade.StartResultMergeOutcome outcome(
		EngineFacade.StartResultMergeStatus status, Long resultId, Long scanId) throws Exception
	{
		// StartResultMergeOutcome is intentionally non-public-constructable;
		// reflectively build instances so the handler can be tested in
		// isolation without pulling in a Spring context or a mocking framework.
		Constructor<EngineFacade.StartResultMergeOutcome> ctor =
			EngineFacade.StartResultMergeOutcome.class.getDeclaredConstructor(
				EngineFacade.StartResultMergeStatus.class, Long.class, Long.class);
		ctor.setAccessible(true);
		return ctor.newInstance(status, resultId, scanId);
	}

	@Test
	public void startedReturnsResultIdAndScanId() throws Exception
	{
		AtomicReference<Long> seenResultId = new AtomicReference<>();
		AtomicReference<AssuranceMergeStrategy> seenStrategy = new AtomicReference<>();
		MergeScanResultHandler handler = new MergeScanResultHandler((resultId, strategy) -> {
			seenResultId.set(resultId);
			seenStrategy.set(strategy);
			try { return outcome(EngineFacade.StartResultMergeStatus.STARTED, 7L, 99L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		JsonNode result = handler.invoke(mapper.readTree("{\"resultId\":7,\"strategy\":\"SOURCE\"}"));
		assertEquals(7L, result.get("resultId").asLong());
		assertEquals(99L, result.get("scanId").asLong());
		assertEquals(Long.valueOf(7L), seenResultId.get());
		assertEquals(AssuranceMergeStrategy.SOURCE, seenStrategy.get());
	}

	@Test
	public void allStrategyValuesAreAccepted() throws Exception
	{
		for (AssuranceMergeStrategy strategy : AssuranceMergeStrategy.values())
		{
			AtomicReference<AssuranceMergeStrategy> seenStrategy = new AtomicReference<>();
			MergeScanResultHandler handler = new MergeScanResultHandler((resultId, s) -> {
				seenStrategy.set(s);
				try { return outcome(EngineFacade.StartResultMergeStatus.STARTED, resultId, 1L); }
				catch (Exception ex) { throw new RuntimeException(ex); }
			});

			handler.invoke(mapper.readTree("{\"resultId\":1,\"strategy\":\"" + strategy.name() + "\"}"));
			assertEquals(strategy, seenStrategy.get());
		}
	}

	@Test
	public void resultNotFoundMapsToEntityNotFound() throws Exception
	{
		MergeScanResultHandler handler = new MergeScanResultHandler((resultId, strategy) -> {
			try { return outcome(EngineFacade.StartResultMergeStatus.RESULT_NOT_FOUND, null, null); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":42,\"strategy\":\"SOURCE\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENTITY_NOT_FOUND, ex.getCode());
			assertEquals(42L, ex.getDataNode().get("resultId").asLong());
		}
	}

	@Test
	public void alreadyRunningMapsToResultMergeAlreadyRunning() throws Exception
	{
		MergeScanResultHandler handler = new MergeScanResultHandler((resultId, strategy) -> {
			try { return outcome(EngineFacade.StartResultMergeStatus.ALREADY_RUNNING, 7L, 314L); }
			catch (Exception ex) { throw new RuntimeException(ex); }
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":7,\"strategy\":\"TARGET\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.RESULT_MERGE_ALREADY_RUNNING, ex.getCode());
			assertEquals(7L, ex.getDataNode().get("resultId").asLong());
			assertEquals(314L, ex.getDataNode().get("scanId").asLong());
		}
	}

	@Test
	public void invalidParamsAreReportedBeforeStarter() throws Exception
	{
		MergeScanResultHandler handler = new MergeScanResultHandler((resultId, strategy) -> {
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
			handler.invoke(mapper.readTree("{\"resultId\":\"x\",\"strategy\":\"SOURCE\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":1}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":1,\"strategy\":\"INVALID\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.INVALID_PARAMS, ex.getCode());
		}

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":1,\"strategy\":42}"));
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
		MergeScanResultHandler handler = new MergeScanResultHandler((resultId, strategy) -> {
			throw new RuntimeException("boom");
		});

		try
		{
			handler.invoke(mapper.readTree("{\"resultId\":1,\"strategy\":\"SOURCE\"}"));
			fail("Expected RpcException");
		}
		catch (RpcException ex)
		{
			assertEquals(RpcErrorCodes.ENGINE_ERROR, ex.getCode());
			assertTrue(ex.getMessage().contains("boom"));
		}
	}
}
