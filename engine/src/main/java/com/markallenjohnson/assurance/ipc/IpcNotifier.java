/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Server-initiated JSON-RPC notification publisher.
 *
 * <p>Server-side asynchronous operations (currently {@code performScan} and
 * future merge/restore flows) need to push progress updates to the renderer
 * without an inbound request to correlate to. Per JSON-RPC 2.0 those are
 * <i>notifications</i>: a frame with {@code method} and {@code params} but
 * <b>no</b> {@code id}. The handler builds the {@code params} {@link JsonNode}
 * and hands it to a publisher, which is responsible for serialising the
 * envelope and broadcasting it to all connected clients.
 *
 * <p>Decoupling from the {@link WebSocketIpcServer} as an interface lets
 * unit tests substitute a recording fake without bringing up a server. The
 * production binding is {@link WebSocketIpcNotifier}.
 */
public interface IpcNotifier
{
	/**
	 * Publishes a JSON-RPC 2.0 notification with the given {@code method} and
	 * {@code params}. Any I/O failure is the publisher's concern (typically
	 * logged and dropped) — handlers should treat publication as best-effort.
	 *
	 * @param method JSON-RPC method name (e.g. {@code "assurance.scanProgress"})
	 * @param params {@code params} object carried verbatim into the envelope; may
	 *   be {@code null} (then the notification has no {@code params} field)
	 */
	void publish(String method, JsonNode params);
}
