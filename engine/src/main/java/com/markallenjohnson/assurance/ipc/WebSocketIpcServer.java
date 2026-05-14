/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loopback-only WebSocket server that hosts the JSON-RPC IPC channel between
 * the Java engine and the Electron UI. The server owns the transport (loopback
 * bind, ephemeral port, lifecycle hooks) and delegates all frame processing to
 * an {@link RpcDispatcher}.
 *
 * <p>Two transport-level concerns are handled here rather than inside the
 * dispatcher: closing the socket after a {@code hello} protocol-version
 * mismatch (SPEC §3 "Lifecycle handshake protocol") and recognising the
 * {@code shutdown} notification.
 */
public class WebSocketIpcServer extends WebSocketServer
{
	private static final Logger logger = LogManager.getLogger(WebSocketIpcServer.class);
	private static final int CLOSE_CODE_PROTOCOL_MISMATCH = CloseFrame.PROTOCOL_ERROR;

	private final CountDownLatch startupLatch = new CountDownLatch(1);
	private final RpcDispatcher dispatcher;
	private final ObjectMapper inspectionMapper = new ObjectMapper();

	public WebSocketIpcServer(int protocolVersion, String engineVersion)
	{
		this(buildDefaultDispatcher(protocolVersion, engineVersion, null));
	}

	public WebSocketIpcServer(int protocolVersion, String engineVersion, EngineFacade engineFacade)
	{
		this(buildDefaultDispatcher(protocolVersion, engineVersion, engineFacade));
	}

	public WebSocketIpcServer(RpcDispatcher dispatcher)
	{
		super(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
		this.dispatcher = dispatcher;
		setReuseAddr(true);
	}

	/**
	 * Builds the standard dispatcher with the lifecycle and platform-namespace
	 * methods that ship with this slice. {@code engineFacade} is optional; when
	 * absent only the lifecycle ({@code hello}) and liveness ({@code assurance.ping})
	 * methods are registered, which is sufficient for the smoke-test shape used
	 * by the Phase 1 integration tests. When present, the model-bound methods
	 * (e.g. {@code assurance.loadScanDefinitions}) are also registered.
	 */
	public static RpcDispatcher buildDefaultDispatcher(int protocolVersion, String engineVersion, EngineFacade engineFacade)
	{
		RpcDispatcher dispatcher = new RpcDispatcher();
		dispatcher.register("hello", new HelloHandler(protocolVersion, engineVersion));
		dispatcher.register("assurance.ping", new PingHandler());
		if (engineFacade != null)
		{
			dispatcher.register("assurance.loadScanDefinitions",
				new LoadScanDefinitionsHandler(engineFacade::loadScanDefinitions));
			dispatcher.register("assurance.saveScanDefinition",
				new SaveScanDefinitionHandler(engineFacade::saveScanDefinition));
			dispatcher.register("assurance.deleteScanDefinition",
				new DeleteScanDefinitionHandler(engineFacade::deleteScanDefinitionById));
			dispatcher.register("assurance.performScan",
				new PerformScanHandler(engineFacade::startScan));
			dispatcher.register("assurance.loadScans",
				new LoadScansHandler(engineFacade::loadScans));
			dispatcher.register("assurance.loadScanResults",
				new LoadScanResultsHandler(engineFacade::loadScanResults));
			dispatcher.register("assurance.loadComparisonResult",
				new LoadComparisonResultHandler(engineFacade::loadComparisonResult));
			dispatcher.register("assurance.mergeScanResult",
				new MergeScanResultHandler(engineFacade::startScanResultMerge));
			dispatcher.register("assurance.mergeScan",
				new MergeScanHandler(engineFacade::startWholeScanMerge));
			dispatcher.register("assurance.restoreDeletedItem",
				new RestoreDeletedItemHandler(engineFacade::startRestoreDeletedItem));
			dispatcher.register("assurance.loadApplicationConfiguration",
				new LoadApplicationConfigurationHandler(engineFacade::loadApplicationConfiguration));
			dispatcher.register("assurance.saveApplicationConfiguration",
				new SaveApplicationConfigurationHandler(engineFacade::saveApplicationConfiguration));
			dispatcher.register("assurance.deleteScan",
				new DeleteScanHandler(engineFacade::deleteScanById));
		}
		return dispatcher;
	}

	public RpcDispatcher getDispatcher()
	{
		return dispatcher;
	}

	/**
	 * Blocks until the server thread has bound to its ephemeral port (signalled
	 * via {@link #onStart()}) and then returns the OS-assigned port.
	 */
	public int awaitStart(long timeout, TimeUnit unit) throws InterruptedException, IOException
	{
		if (!startupLatch.await(timeout, unit))
		{
			throw new IOException("WebSocket IPC server did not start within " + unit.toMillis(timeout) + "ms");
		}
		return getPort();
	}

	@Override
	public void onStart()
	{
		logger.info("IPC WebSocket server bound to {}", getAddress());
		startupLatch.countDown();
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake)
	{
		logger.info("IPC client connected from {}", conn.getRemoteSocketAddress());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote)
	{
		logger.info("IPC client disconnected (code={}, remote={}, reason={})", code, remote, reason);
	}

	@Override
	public void onMessage(WebSocket conn, String message)
	{
		logger.debug("IPC frame received: {}", message);

		if (looksLikeShutdownNotification(message))
		{
			logger.info("Received shutdown notification from client; closing socket.");
			conn.close(CloseFrame.NORMAL);
			return;
		}

		String response = dispatcher.dispatch(message);
		if (response == null)
		{
			return;
		}
		conn.send(response);

		if (isProtocolMismatchResponse(response))
		{
			logger.warn("Closing connection following protocol version mismatch.");
			conn.close(CLOSE_CODE_PROTOCOL_MISMATCH, "protocol version mismatch");
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex)
	{
		logger.error("IPC error", ex);
	}

	private boolean looksLikeShutdownNotification(String message)
	{
		try
		{
			JsonNode root = inspectionMapper.readTree(message);
			if (root == null || !root.isObject())
			{
				return false;
			}
			JsonNode method = root.get("method");
			if (method == null || !method.isTextual())
			{
				return false;
			}
			if (!"shutdown".equals(method.asText()))
			{
				return false;
			}
			// Notifications carry no id (per JSON-RPC 2.0).
			return !root.has("id");
		}
		catch (JsonProcessingException ex)
		{
			return false;
		}
	}

	private boolean isProtocolMismatchResponse(String response)
	{
		try
		{
			JsonNode root = inspectionMapper.readTree(response);
			if (root == null || !root.isObject())
			{
				return false;
			}
			JsonNode error = root.get("error");
			if (error == null || !error.isObject())
			{
				return false;
			}
			JsonNode code = error.get("code");
			return code != null && code.canConvertToInt()
				&& code.intValue() == RpcErrorCodes.PROTOCOL_VERSION_MISMATCH;
		}
		catch (JsonProcessingException ex)
		{
			return false;
		}
	}
}
