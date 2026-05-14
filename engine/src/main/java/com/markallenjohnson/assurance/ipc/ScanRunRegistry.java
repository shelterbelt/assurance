/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry of scans currently executing in the engine, keyed by the
 * id of the {@link com.markallenjohnson.assurance.model.entities.ScanDefinition}
 * they are running for.
 *
 * <p>The registry is used to enforce the
 * {@code -32003 SCAN_ALREADY_RUNNING} guard documented in
 * {@code docs/ipc-contract.md}: only one scan may run at a time per scan
 * definition. The registry deliberately does not persist across restarts —
 * if the engine crashes mid-scan there is no in-flight scan to track on the
 * next launch.
 */
public final class ScanRunRegistry
{
	private final ConcurrentMap<Long, Long> activeByDefinitionId = new ConcurrentHashMap<>();

	/**
	 * Attempts to claim the slot for {@code scanDefinitionId} with the given
	 * {@code scanId}. Returns {@code true} if the slot was previously empty
	 * and is now claimed, {@code false} if a scan is already running for that
	 * definition.
	 */
	public boolean tryAcquire(Long scanDefinitionId, Long scanId)
	{
		if (scanDefinitionId == null || scanId == null)
		{
			throw new IllegalArgumentException("scanDefinitionId and scanId must not be null");
		}
		return activeByDefinitionId.putIfAbsent(scanDefinitionId, scanId) == null;
	}

	/**
	 * Releases the slot for {@code scanDefinitionId}. Idempotent — releasing
	 * an already-empty slot is a no-op.
	 */
	public void release(Long scanDefinitionId)
	{
		if (scanDefinitionId != null)
		{
			activeByDefinitionId.remove(scanDefinitionId);
		}
	}

	/**
	 * Returns the {@code scanId} currently running for {@code scanDefinitionId},
	 * or {@code null} if none.
	 */
	public Long currentScanId(Long scanDefinitionId)
	{
		if (scanDefinitionId == null)
		{
			return null;
		}
		return activeByDefinitionId.get(scanDefinitionId);
	}
}
