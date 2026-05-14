/**
 * Main-side IPC contract tests.
 *
 * The two pure helpers in {@code src/ipc/main-handlers.ts} are the
 * Electron-main entry points the renderer calls into; here they're driven
 * with a fake {@code EngineRpc} (in for the WebSocket-backed engine) and a
 * fake {@link NotificationSink} (in for the {@code BrowserWindow}'s
 * {@code webContents}). Coverage tracks {@code docs/ipc-contract.md}: every
 * RPC method routes verbatim, every notification family forwards verbatim,
 * and the failure shapes match the JSON-RPC error envelope the contract
 * requires.
 */

import {
    FORWARDED_NOTIFICATIONS,
    handleEngineCall,
    registerNotificationForwarders,
    type NotificationSink,
} from '../main-handlers';
import { EngineRpcError } from '../../engine/rpc';

interface FakeRpc {
    call: jest.Mock<Promise<unknown>, [string, unknown]>;
    onNotification: jest.Mock<() => void, [string, (params: unknown) => void]>;
    emit(method: string, params: unknown): void;
    listenersFor(method: string): Array<(params: unknown) => void>;
    methodsSubscribed(): string[];
}

function makeFakeRpc(): FakeRpc {
    const listeners = new Map<string, Set<(params: unknown) => void>>();
    const call = jest.fn<Promise<unknown>, [string, unknown]>();
    const onNotification = jest.fn<() => void, [string, (params: unknown) => void]>(
        (method, listener) => {
            let bucket = listeners.get(method);
            if (!bucket) {
                bucket = new Set();
                listeners.set(method, bucket);
            }
            bucket.add(listener);
            return () => {
                const set = listeners.get(method);
                if (!set) return;
                set.delete(listener);
                if (set.size === 0) listeners.delete(method);
            };
        },
    );
    return {
        call,
        onNotification,
        emit(method, params) {
            const bucket = listeners.get(method);
            if (!bucket) return;
            for (const listener of Array.from(bucket)) listener(params);
        },
        listenersFor(method) {
            return Array.from(listeners.get(method) ?? []);
        },
        methodsSubscribed() {
            return Array.from(listeners.keys());
        },
    };
}

interface FakeSink extends NotificationSink {
    sent: Array<{ method: string; params: unknown }>;
    closed: boolean;
}

function makeFakeSink(): FakeSink {
    const sink: FakeSink = {
        sent: [],
        closed: false,
        send(envelope) {
            this.sent.push(envelope);
        },
        isClosed() {
            return this.closed;
        },
    };
    return sink;
}

describe('main-side IPC contract', () => {
    describe('handleEngineCall', () => {
        test('forwards method+params to rpc.call verbatim', async () => {
            const rpc = makeFakeRpc();
            rpc.call.mockResolvedValueOnce({ pong: true });

            await handleEngineCall('assurance.ping', { foo: 'bar' }, rpc);

            expect(rpc.call).toHaveBeenCalledTimes(1);
            expect(rpc.call).toHaveBeenCalledWith('assurance.ping', { foo: 'bar' });
        });

        test('wraps a successful result in { ok: true, result }', async () => {
            const rpc = makeFakeRpc();
            rpc.call.mockResolvedValueOnce({ scanDefinitions: [] });

            const envelope = await handleEngineCall('assurance.loadScanDefinitions', {}, rpc);

            expect(envelope).toEqual({ ok: true, result: { scanDefinitions: [] } });
        });

        test('wraps an EngineRpcError into { ok: false, error: { code, message, data } }', async () => {
            const rpc = makeFakeRpc();
            rpc.call.mockRejectedValueOnce(
                new EngineRpcError({ code: -32602, message: 'bad params', data: { field: 'name' } }),
            );

            const envelope = await handleEngineCall('assurance.saveScanDefinition', {}, rpc);

            expect(envelope).toEqual({
                ok: false,
                error: { code: -32602, message: 'bad params', data: { field: 'name' } },
            });
        });

        test('wraps a generic Error into { ok: false, error: { code: -32603, message } }', async () => {
            const rpc = makeFakeRpc();
            rpc.call.mockRejectedValueOnce(new Error('socket dropped'));

            const envelope = await handleEngineCall('assurance.ping', {}, rpc);

            expect(envelope).toEqual({
                ok: false,
                error: { code: -32603, message: 'socket dropped' },
            });
        });

        test('wraps a non-Error rejection into { ok: false, error: { code: -32603, message: String(reason) } }', async () => {
            const rpc = makeFakeRpc();
            rpc.call.mockRejectedValueOnce('string-only failure');

            const envelope = await handleEngineCall('assurance.ping', {}, rpc);

            expect(envelope).toEqual({
                ok: false,
                error: { code: -32603, message: 'string-only failure' },
            });
        });

        test('rejects an empty method name with -32600 without invoking rpc.call', async () => {
            const rpc = makeFakeRpc();

            const envelope = await handleEngineCall('', {}, rpc);

            expect(envelope).toEqual({
                ok: false,
                error: {
                    code: -32600,
                    message: 'Invalid request: method must be a non-empty string',
                },
            });
            expect(rpc.call).not.toHaveBeenCalled();
        });

        test('rejects a non-string method with -32600 without invoking rpc.call', async () => {
            const rpc = makeFakeRpc();

            const envelope = await handleEngineCall(42, {}, rpc);

            expect(envelope).toMatchObject({
                ok: false,
                error: { code: -32600 },
            });
            expect(rpc.call).not.toHaveBeenCalled();
        });
    });

    describe('per-method routing (every method documented in docs/ipc-contract.md)', () => {
        const cases: Array<{ method: string; params: unknown; result: unknown }> = [
            { method: 'assurance.ping', params: {}, result: { pong: true, timestamp: '2026-05-07T00:00:00Z' } },
            { method: 'assurance.loadScanDefinitions', params: {}, result: { scanDefinitions: [] } },
            {
                method: 'assurance.saveScanDefinition',
                params: { scanDefinition: { id: 0, name: 'X' } },
                result: { scanDefinition: { id: 7, name: 'X' } },
            },
            {
                method: 'assurance.deleteScanDefinition',
                params: { scanDefinitionId: 1 },
                result: { deleted: true },
            },
            {
                method: 'assurance.performScan',
                params: { scanDefinitionId: 7, merge: false },
                result: { scanId: 314 },
            },
            { method: 'assurance.mergeScan', params: { scanId: 7 }, result: { scanId: 7 } },
            {
                method: 'assurance.mergeScanResult',
                params: { resultId: 5, strategy: 'SOURCE' },
                result: { resultId: 5, scanId: 7 },
            },
            {
                method: 'assurance.restoreDeletedItem',
                params: { resultId: 5 },
                result: { resultId: 5, scanId: 7 },
            },
            { method: 'assurance.loadScans', params: {}, result: { scans: [] } },
            {
                method: 'assurance.loadScanResults',
                params: { scanId: 7 },
                result: { results: [] },
            },
            {
                method: 'assurance.loadComparisonResult',
                params: { resultId: 5 },
                result: { result: { id: 5 } },
            },
            { method: 'assurance.deleteScan', params: { scanId: 7 }, result: { deleted: true } },
            {
                method: 'assurance.loadApplicationConfiguration',
                params: {},
                result: { configuration: { id: 1 } },
            },
            {
                method: 'assurance.saveApplicationConfiguration',
                params: { configuration: { id: 1, numberOfScanThreads: 4 } },
                result: { configuration: { id: 1, numberOfScanThreads: 4 } },
            },
        ];

        for (const { method, params, result } of cases) {
            test(`${method} routes through rpc.call(method, params) and rewraps the result`, async () => {
                const rpc = makeFakeRpc();
                rpc.call.mockResolvedValueOnce(result);

                const envelope = await handleEngineCall(method, params, rpc);

                expect(rpc.call).toHaveBeenCalledWith(method, params);
                expect(envelope).toEqual({ ok: true, result });
            });
        }
    });

    describe('FORWARDED_NOTIFICATIONS coverage', () => {
        test('includes every notification family documented in docs/ipc-contract.md', () => {
            // The contract documents 4 families × 4 lifecycle states = 16 notifications.
            const expected = [
                'assurance.scanStarted',
                'assurance.scanProgress',
                'assurance.scanCompleted',
                'assurance.scanFailed',
                'assurance.mergeStarted',
                'assurance.mergeProgress',
                'assurance.mergeCompleted',
                'assurance.mergeFailed',
                'assurance.resultMergeStarted',
                'assurance.resultMergeProgress',
                'assurance.resultMergeCompleted',
                'assurance.resultMergeFailed',
                'assurance.restoreStarted',
                'assurance.restoreProgress',
                'assurance.restoreCompleted',
                'assurance.restoreFailed',
            ];
            // Order isn't part of the contract; membership is.
            expect(new Set(FORWARDED_NOTIFICATIONS)).toEqual(new Set(expected));
            expect(FORWARDED_NOTIFICATIONS).toHaveLength(16);
        });
    });

    describe('registerNotificationForwarders', () => {
        test('subscribes once per method in FORWARDED_NOTIFICATIONS', () => {
            const rpc = makeFakeRpc();
            const sink = makeFakeSink();

            registerNotificationForwarders(rpc, sink);

            expect(rpc.onNotification).toHaveBeenCalledTimes(FORWARDED_NOTIFICATIONS.length);
            expect(new Set(rpc.methodsSubscribed())).toEqual(new Set(FORWARDED_NOTIFICATIONS));
            for (const method of FORWARDED_NOTIFICATIONS) {
                expect(rpc.listenersFor(method)).toHaveLength(1);
            }
        });

        test('forwards a notification with { method, params } verbatim to the sink', () => {
            const rpc = makeFakeRpc();
            const sink = makeFakeSink();
            registerNotificationForwarders(rpc, sink);

            const params = {
                scanId: 42,
                progress: { phase: 'comparing', itemsProcessed: 100, itemsTotal: 500 },
            };
            rpc.emit('assurance.scanProgress', params);

            expect(sink.sent).toEqual([{ method: 'assurance.scanProgress', params }]);
        });

        test('forwards every method in FORWARDED_NOTIFICATIONS — one event per method', () => {
            const rpc = makeFakeRpc();
            const sink = makeFakeSink();
            registerNotificationForwarders(rpc, sink);

            for (const method of FORWARDED_NOTIFICATIONS) {
                rpc.emit(method, { tag: method });
            }

            expect(sink.sent.map((envelope) => envelope.method)).toEqual([
                ...FORWARDED_NOTIFICATIONS,
            ]);
            expect(sink.sent[0]).toEqual({
                method: FORWARDED_NOTIFICATIONS[0],
                params: { tag: FORWARDED_NOTIFICATIONS[0] },
            });
        });

        test('skips send when the sink reports closed', () => {
            const rpc = makeFakeRpc();
            const sink = makeFakeSink();
            registerNotificationForwarders(rpc, sink);

            sink.closed = true;
            rpc.emit('assurance.scanStarted', { scanId: 1 });

            expect(sink.sent).toEqual([]);
        });

        test('teardown unsubscribes every forwarder', () => {
            const rpc = makeFakeRpc();
            const sink = makeFakeSink();
            const tearDown = registerNotificationForwarders(rpc, sink);

            tearDown();

            for (const method of FORWARDED_NOTIFICATIONS) {
                expect(rpc.listenersFor(method)).toHaveLength(0);
            }
            // After teardown, emissions are silently dropped.
            rpc.emit('assurance.scanStarted', { scanId: 1 });
            expect(sink.sent).toEqual([]);
        });

        test('honors the optional methods override (no FORWARDED_NOTIFICATIONS subscription leakage)', () => {
            const rpc = makeFakeRpc();
            const sink = makeFakeSink();

            registerNotificationForwarders(rpc, sink, ['assurance.scanStarted']);

            expect(rpc.methodsSubscribed()).toEqual(['assurance.scanStarted']);
            rpc.emit('assurance.scanProgress', { scanId: 1 });
            expect(sink.sent).toEqual([]);
        });

        test('a sink that reads its target window lazily survives a close+reopen cycle', () => {
            // Regression for the "Attempted to register a second handler for
            // 'selectPath'" crash on macOS dock-click reopen: the original
            // index.ts reregistered every IPC handler and forwarder in
            // createWindow(), which (a) crashed ipcMain.handle on the second
            // call and (b) leaked one EngineRpc subscriber per window-open
            // because each captured a BrowserWindow reference. The fix
            // moves IPC + forwarder registration to app-lifetime
            // (registerAppLifetimeIPC in index.ts) and routes the sink
            // through a mutable currentMainWindow read lazily, so the same
            // forwarder set transparently retargets the new window.
            //
            // Production-shape model: a single forwarder set installed once,
            // then the underlying "window" reference cycles open → closed
            // (sink reports closed) → reopened on a fresh target.
            const rpc = makeFakeRpc();
            type Window = { sent: Array<{ method: string; params: unknown }>; destroyed: boolean };
            let currentWindow: Window | null = null;
            const sink: NotificationSink = {
                send: (envelope) => {
                    if (currentWindow && !currentWindow.destroyed) {
                        currentWindow.sent.push(envelope);
                    }
                },
                isClosed: () => currentWindow === null || currentWindow.destroyed,
            };

            const tearDown = registerNotificationForwarders(rpc, sink);
            // forwarders subscribe exactly once, NOT once-per-window:
            for (const method of FORWARDED_NOTIFICATIONS) {
                expect(rpc.listenersFor(method)).toHaveLength(1);
            }

            const win1: Window = { sent: [], destroyed: false };
            currentWindow = win1;
            rpc.emit('assurance.scanStarted', { scanId: 1 });
            expect(win1.sent).toEqual([{ method: 'assurance.scanStarted', params: { scanId: 1 } }]);

            // User closes the window (without quitting the app).
            win1.destroyed = true;
            currentWindow = null;
            rpc.emit('assurance.scanProgress', { scanId: 1, progress: { phase: 'comparing' } });
            // Dropped: no live window, no leak to the destroyed one.
            expect(win1.sent).toHaveLength(1);

            // User reopens (macOS dock-click → app.on('activate') → createWindow()).
            const win2: Window = { sent: [], destroyed: false };
            currentWindow = win2;
            rpc.emit('assurance.scanCompleted', { scanId: 1, results: 5 });
            // The same long-lived forwarder routes the new event to the new
            // window — and the destroyed window is untouched.
            expect(win2.sent).toEqual([
                { method: 'assurance.scanCompleted', params: { scanId: 1, results: 5 } },
            ]);
            expect(win1.sent).toHaveLength(1);
            // Subscription count is still 1, not 2: no leak per reopen.
            for (const method of FORWARDED_NOTIFICATIONS) {
                expect(rpc.listenersFor(method)).toHaveLength(1);
            }

            tearDown();
        });
    });
});
