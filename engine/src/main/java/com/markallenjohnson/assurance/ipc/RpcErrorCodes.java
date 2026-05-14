/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

/**
 * Standard JSON-RPC 2.0 error codes plus the Assurance-specific error code
 * range documented in {@code docs/ipc-contract.md} ({@code -32000}..{@code -32099}).
 */
public final class RpcErrorCodes
{
	// Standard JSON-RPC 2.0 reserved codes.
	public static final int PARSE_ERROR = -32700;
	public static final int INVALID_REQUEST = -32600;
	public static final int METHOD_NOT_FOUND = -32601;
	public static final int INVALID_PARAMS = -32602;
	public static final int INTERNAL_ERROR = -32603;

	// Assurance-specific codes (server-defined range).
	public static final int PROTOCOL_VERSION_MISMATCH = -32000;
	public static final int ENGINE_ERROR = -32001;
	public static final int ENTITY_NOT_FOUND = -32002;
	public static final int SCAN_ALREADY_RUNNING = -32003;
	public static final int FILESYSTEM_ERROR = -32004;
	public static final int DATABASE_ERROR = -32005;
	public static final int MERGE_ALREADY_RUNNING = -32006;
	public static final int RESULT_MERGE_ALREADY_RUNNING = -32007;
	public static final int RESTORE_ALREADY_RUNNING = -32008;

	private RpcErrorCodes()
	{
	}
}
