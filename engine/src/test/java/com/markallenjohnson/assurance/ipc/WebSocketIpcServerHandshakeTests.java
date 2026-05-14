/*
 * Assurance
 *
 * Copyright (c) 2015 - 2026 Mark Johnson
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 */

package com.markallenjohnson.assurance.ipc;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.After;
import org.junit.Test;

/**
 * End-to-end tests that spin up the engine's WebSocket IPC server on an
 * ephemeral loopback port and drive a real WebSocket client through the
 * lifecycle handshake plus the JSON-RPC dispatcher.
 */
public class WebSocketIpcServerHandshakeTests
{
	private static final int CLIENT_TIMEOUT_MS = 5_000;
	private static final int SERVER_PROTOCOL_VERSION = 1;
	private static final String SERVER_ENGINE_VERSION = "2.0.0-test";

	private WebSocketIpcServer server;

	@After
	public void tearDown() throws Exception
	{
		if (server != null)
		{
			server.stop(1_000);
			server = null;
		}
	}

	@Test
	public void respondsWithWelcomeOnMatchingProtocolVersion() throws Exception
	{
		int port = startServer(SERVER_PROTOCOL_VERSION);
		TestClient client = TestClient.connect(port);
		try
		{
			String hello = "{\"jsonrpc\":\"2.0\",\"method\":\"hello\","
				+ "\"params\":{\"uiVersion\":\"2.0.0\",\"protocolVersion\":1},\"id\":1}";
			client.send(hello);

			String reply = client.awaitMessage();
			assertNotNull("Server must reply to hello", reply);
			assertTrue("Reply should contain matching id: " + reply, reply.contains("\"id\":1"));
			assertTrue("Reply should be a result envelope: " + reply, reply.contains("\"result\""));
			assertTrue("Reply should advertise engineVersion: " + reply,
				reply.contains("\"engineVersion\":\"" + SERVER_ENGINE_VERSION + "\""));
			assertTrue("Reply should advertise protocolVersion: " + reply,
				reply.contains("\"protocolVersion\":1"));
		}
		finally
		{
			client.close();
		}
	}

	@Test
	public void respondsWithErrorAndClosesOnProtocolMismatch() throws Exception
	{
		int port = startServer(SERVER_PROTOCOL_VERSION);
		TestClient client = TestClient.connect(port);
		try
		{
			String hello = "{\"jsonrpc\":\"2.0\",\"method\":\"hello\","
				+ "\"params\":{\"uiVersion\":\"2.0.0\",\"protocolVersion\":99},\"id\":1}";
			client.send(hello);

			String reply = client.awaitMessage();
			assertNotNull("Server must reply on mismatch", reply);
			assertTrue("Reply should be an error envelope: " + reply, reply.contains("\"error\""));
			assertTrue("Reply should carry mismatch code: " + reply, reply.contains("\"code\":-32000"));
			assertTrue("Server must close the connection on mismatch", client.awaitClose());
		}
		finally
		{
			client.close();
		}
	}

	@Test
	public void respondsWithInvalidParamsOnHelloMissingProtocolVersion() throws Exception
	{
		int port = startServer(SERVER_PROTOCOL_VERSION);
		TestClient client = TestClient.connect(port);
		try
		{
			String hello = "{\"jsonrpc\":\"2.0\",\"method\":\"hello\","
				+ "\"params\":{\"uiVersion\":\"2.0.0\"},\"id\":1}";
			client.send(hello);

			String reply = client.awaitMessage();
			assertNotNull(reply);
			assertTrue("Reply should be invalid params: " + reply, reply.contains("\"code\":-32602"));
			assertTrue("Reply should carry the request id: " + reply, reply.contains("\"id\":1"));
		}
		finally
		{
			client.close();
		}
	}

	@Test
	public void respondsWithParseErrorOnMalformedJson() throws Exception
	{
		int port = startServer(SERVER_PROTOCOL_VERSION);
		TestClient client = TestClient.connect(port);
		try
		{
			client.send("{not json");

			String reply = client.awaitMessage();
			assertNotNull(reply);
			assertTrue("Reply should be a parse error: " + reply, reply.contains("\"code\":-32700"));
		}
		finally
		{
			client.close();
		}
	}

	@Test
	public void answersAssurancePing() throws Exception
	{
		int port = startServer(SERVER_PROTOCOL_VERSION);
		TestClient client = TestClient.connect(port);
		try
		{
			client.send("{\"jsonrpc\":\"2.0\",\"method\":\"assurance.ping\",\"params\":{},\"id\":7}");

			String reply = client.awaitMessage();
			assertNotNull(reply);
			assertTrue("Reply should carry result: " + reply, reply.contains("\"result\""));
			assertTrue("Reply should carry pong: " + reply, reply.contains("\"pong\":true"));
			assertTrue("Reply should carry timestamp: " + reply, reply.contains("\"timestamp\""));
			assertTrue("Reply should preserve id: " + reply, reply.contains("\"id\":7"));
		}
		finally
		{
			client.close();
		}
	}

	@Test
	public void unknownMethodReturnsMethodNotFound() throws Exception
	{
		int port = startServer(SERVER_PROTOCOL_VERSION);
		TestClient client = TestClient.connect(port);
		try
		{
			client.send("{\"jsonrpc\":\"2.0\",\"method\":\"assurance.notARealMethod\",\"id\":9}");

			String reply = client.awaitMessage();
			assertNotNull(reply);
			assertTrue("Reply should be method-not-found: " + reply, reply.contains("\"code\":-32601"));
			assertTrue("Reply should preserve id: " + reply, reply.contains("\"id\":9"));
		}
		finally
		{
			client.close();
		}
	}

	private int startServer(int protocolVersion) throws Exception
	{
		server = new WebSocketIpcServer(protocolVersion, SERVER_ENGINE_VERSION);
		server.start();
		return server.awaitStart(5, TimeUnit.SECONDS);
	}

	private static final class TestClient extends WebSocketClient
	{
		private final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();
		private final CountDownLatch closeLatch = new CountDownLatch(1);
		private final AtomicReference<Exception> error = new AtomicReference<>();

		private TestClient(URI uri)
		{
			super(uri);
		}

		static TestClient connect(int port) throws Exception
		{
			TestClient client = new TestClient(URI.create("ws://127.0.0.1:" + port));
			if (!client.connectBlocking(CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS))
			{
				fail("Test WebSocket client failed to connect within " + CLIENT_TIMEOUT_MS + "ms");
			}
			return client;
		}

		String awaitMessage() throws InterruptedException
		{
			return messages.poll(CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		}

		boolean awaitClose() throws InterruptedException
		{
			return closeLatch.await(CLIENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		}

		@Override
		public void onOpen(ServerHandshake handshakedata)
		{
		}

		@Override
		public void onMessage(String message)
		{
			messages.offer(message);
		}

		@Override
		public void onClose(int code, String reason, boolean remote)
		{
			closeLatch.countDown();
		}

		@Override
		public void onError(Exception ex)
		{
			error.set(ex);
		}
	}
}
