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
 * Generic in-memory single-flight registry keyed by an opaque {@code Long}
 * id. Used by the IPC layer to enforce the "only one operation at a time
 * per resource" guards documented in {@code docs/ipc-contract.md} for
 * asynchronous methods (whole-scan merge, per-result merge, and restore).
 *
 * <p>The registry deliberately does not persist across restarts — if the
 * engine crashes mid-operation there is nothing in-flight to track on the
 * next launch. {@link ScanRunRegistry} predates this class and is preserved
 * as-is for the {@code performScan} flow that already depends on it.
 */
public final class ConcurrentRunRegistry
{
	private final ConcurrentMap<Long, Long> activeByKey = new ConcurrentHashMap<>();

	/**
	 * Attempts to claim the slot for {@code key}, recording {@code runId} as
	 * the operation that owns it. Returns {@code true} when the slot was
	 * previously empty (and is now claimed), {@code false} when an
	 * operation was already in flight against {@code key}.
	 */
	public boolean tryAcquire(Long key, Long runId)
	{
		if (key == null || runId == null)
		{
			throw new IllegalArgumentException("key and runId must not be null");
		}
		return activeByKey.putIfAbsent(key, runId) == null;
	}

	/**
	 * Releases the slot for {@code key}. Idempotent — releasing an
	 * already-empty slot is a no-op.
	 */
	public void release(Long key)
	{
		if (key != null)
		{
			activeByKey.remove(key);
		}
	}

	/**
	 * Returns the {@code runId} currently registered for {@code key}, or
	 * {@code null} if none.
	 */
	public Long currentRunId(Long key)
	{
		if (key == null)
		{
			return null;
		}
		return activeByKey.get(key);
	}
}
