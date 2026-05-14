import { EventEmitter } from 'events';

import {
    EngineClient,
    EngineClientFatalError,
    newEngineClientForTest,
} from '../client';

/**
 * Bare-bones EventEmitter stand-in for a `ws` WebSocket. The post-handshake
 * listeners attached by {@link EngineClient.attachPostHandshakeListenersForTest}
 * only need {@code on('message'|'close'|'error', ...)} and a {@code close()}
 * method (which we don't actually invoke from the production close path —
 * the production path calls the underlying socket's close, but that's the
 * inbound 'close' event from the engine's side that we model directly here).
 *
 * {@code readyState = 1} matches {@code ws.WebSocket.OPEN} so
 * {@link EngineClient.sendShutdownNotification} doesn't short-circuit on
 * the readiness gate. {@code send} is captured per-test via {@link sentFrames}
 * so spec assertions can verify the wire shape.
 */
class FakeSocket extends EventEmitter {
    readyState = 1;
    sentFrames: string[] = [];
    /**
     * Optional hook fired synchronously after each {@link send}. Tests use
     * this to model the engine racing a close-frame back inside the same
     * tick as the outbound shutdown send.
     */
    onSend: ((frame: string) => void) | null = null;

    send(frame: string): void {
        this.sentFrames.push(frame);
        this.onSend?.(frame);
    }

    close(): void {
        this.readyState = 3;
        this.emit('close', 1000, Buffer.from(''));
    }
}

function newClientWithSocket(): { client: EngineClient; socket: FakeSocket; fatals: EngineClientFatalError[] } {
    const client = newEngineClientForTest();
    const socket = new FakeSocket();
    const fatals: EngineClientFatalError[] = [];
    client.on('fatal', (err: EngineClientFatalError) => fatals.push(err));
    client.attachPostHandshakeListenersForTest(socket as unknown as import('ws').WebSocket);
    return { client, socket, fatals };
}

test('unilateral post-handshake close emits a fatal error (engine died)', () => {
    const { socket, fatals } = newClientWithSocket();
    socket.emit('close', 1006, Buffer.from('connection lost'));
    expect(fatals.length).toBe(1);
    expect(fatals[0].reason).toBe('connection-closed');
    expect(fatals[0].message).toMatch(/Engine WebSocket closed unexpectedly \(code=1006/);
});

test('unilateral post-handshake error emits a fatal error', () => {
    const { socket, fatals } = newClientWithSocket();
    socket.emit('error', new Error('ECONNRESET'));
    expect(fatals.length).toBe(1);
    expect(fatals[0].reason).toBe('connection-error');
    expect(fatals[0].message).toMatch(/ECONNRESET/);
});

test('close() before the socket actually closes suppresses the fatal event', () => {
    // The production app-quit flow: the renderer initiates teardown via
    // close(), which marks the upcoming close as deliberate; the inbound
    // 'close' event from the underlying socket then arrives and must NOT
    // surface a user-facing dialog (which would block app.quit() on the
    // modal — the bug T5.3 was masking with its 15s SIGKILL fallback).
    const { client, socket, fatals } = newClientWithSocket();
    client.close();
    socket.emit('close', 1000, Buffer.from(''));
    expect(fatals.length).toBe(0);
});

test('sendShutdownNotification() suppresses the fatal event from the engine\'s reply close', () => {
    // The engine's WebSocketIpcServer replies to the JSON-RPC `shutdown`
    // notification by calling conn.close(CloseFrame.NORMAL); the close
    // frame races back to our 'close' listener before sendShutdownNotification
    // even returns. Setting deliberateShutdown BEFORE writing the frame
    // (rather than after) is what makes this safe.
    const { client, socket, fatals } = newClientWithSocket();
    socket.onSend = () => {
        socket.emit('close', 1000, Buffer.from(''));
    };

    client.sendShutdownNotification();

    expect(socket.sentFrames).toEqual([JSON.stringify({ jsonrpc: '2.0', method: 'shutdown' })]);
    expect(fatals.length).toBe(0);
});

test('post-deliberate-shutdown error events are suppressed (write-after-end during teardown)', () => {
    // ws can emit an error in the same tick as a close during a clean
    // teardown (e.g. a write that lost the race with the close handshake).
    // These are not actionable to the user; the close event is the
    // canonical signal and it's already being correctly suppressed.
    const { client, socket, fatals } = newClientWithSocket();
    client.close();
    socket.emit('error', new Error('write after end'));
    expect(fatals.length).toBe(0);
});

test('close() is idempotent and clears handshake state', () => {
    const { client, socket } = newClientWithSocket();
    expect(client.isConnected()).toBe(false); // handshakeInfo never populated by the test seam
    client.close();
    client.close();
    expect(client.isConnected()).toBe(false);
    // A subsequent unilateral close on the same already-closed socket must
    // remain quiet (the deliberateShutdown latch is set-once).
    const fatalsAfter: EngineClientFatalError[] = [];
    client.on('fatal', (err) => fatalsAfter.push(err));
    socket.emit('close', 1006, Buffer.from('would-have-been-fatal'));
    expect(fatalsAfter.length).toBe(0);
});

test('a fresh client (no deliberate shutdown) still surfaces fatals — no leak between instances', () => {
    // Defensive: the deliberateShutdown latch is per-instance, not global.
    // Make a second client and confirm the unilateral path still fires.
    const a = newClientWithSocket();
    a.client.close();
    a.socket.emit('close', 1000, Buffer.from(''));
    expect(a.fatals.length).toBe(0);

    const b = newClientWithSocket();
    b.socket.emit('close', 1006, Buffer.from('engine died'));
    expect(b.fatals.length).toBe(1);
});
