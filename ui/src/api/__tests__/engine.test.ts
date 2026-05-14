/**
 * Renderer-side IPC contract tests.
 *
 * Each method declared in {@code docs/ipc-contract.md} has at least one test
 * that mocks {@code window.assuranceapi.engineCall} and verifies (a) the
 * method name and params shape sent on the wire, (b) the success-envelope
 * unwrap, and (c) the {@link EngineCallError} rejection on failure
 * envelopes. Notification subscribers are exercised against a mock
 * {@code onEngineNotification} that round-trips envelopes back to the
 * registered listener.
 */

import {
    engine,
    EngineCallError,
    onMergeNotification,
    onResultMergeNotification,
    onRestoreNotification,
    onScanNotification,
    type MergeNotificationParams,
    type ResultMergeNotificationParams,
    type RestoreNotificationParams,
    type ScanNotificationParams,
} from '../engine';
import type { EngineCallEnvelope, EngineNotificationEnvelope } from '../../preload';
import type ScanDefinition from '../../model/ScanDefinition';
import type Scan from '../../model/Scan';
import type ComparisonResult from '../../model/ComparisonResult';
import type ApplicationConfiguration from '../../model/ApplicationConfiguration';

interface MockApi {
    engineCall: jest.Mock<Promise<EngineCallEnvelope>, [string, unknown]>;
    onEngineNotification: jest.Mock<() => void, [(env: EngineNotificationEnvelope) => void]>;
    notifySubscribers(envelope: EngineNotificationEnvelope): void;
}

function installMockApi(): MockApi {
    const subscribers = new Set<(env: EngineNotificationEnvelope) => void>();

    const engineCall = jest.fn<Promise<EngineCallEnvelope>, [string, unknown]>();
    const onEngineNotification = jest.fn<() => void, [(env: EngineNotificationEnvelope) => void]>(
        (listener) => {
            subscribers.add(listener);
            return () => {
                subscribers.delete(listener);
            };
        },
    );

    (window as unknown as { assuranceapi: unknown }).assuranceapi = {
        engineCall,
        onEngineNotification,
        // selectPath isn't exercised here but the type requires it to be present.
        selectPath: jest.fn(),
    };

    return {
        engineCall,
        onEngineNotification,
        notifySubscribers(envelope) {
            for (const listener of Array.from(subscribers)) {
                listener(envelope);
            }
        },
    };
}

function ok<T>(result: T): EngineCallEnvelope {
    return { ok: true, result };
}

function err(code: number, message: string, data?: unknown): EngineCallEnvelope {
    return { ok: false, error: { code, message, data } };
}

describe('renderer-side IPC contract', () => {
    let api: MockApi;

    beforeEach(() => {
        api = installMockApi();
    });

    afterEach(() => {
        delete (window as unknown as { assuranceapi?: unknown }).assuranceapi;
    });

    describe('engine.call (assurance.ping + generic envelope)', () => {
        test('forwards method and params verbatim, unwraps the result', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ pong: true, timestamp: '2026-05-07T00:00:00Z' }));

            const result = await engine.call<{ pong: boolean; timestamp: string }>('assurance.ping', {});

            expect(api.engineCall).toHaveBeenCalledTimes(1);
            expect(api.engineCall).toHaveBeenCalledWith('assurance.ping', {});
            expect(result).toEqual({ pong: true, timestamp: '2026-05-07T00:00:00Z' });
        });

        test('rejects with EngineCallError carrying code/message/data', async () => {
            api.engineCall.mockResolvedValue(err(-32601, 'Method not found', { method: 'nope' }));

            await expect(engine.call('nope', {})).rejects.toBeInstanceOf(EngineCallError);
            await expect(engine.call('nope', {})).rejects.toMatchObject({
                code: -32601,
                message: 'Method not found',
                data: { method: 'nope' },
            });
        });

        test('throws when the assuranceapi bridge is unavailable', async () => {
            delete (window as unknown as { assuranceapi?: unknown }).assuranceapi;
            await expect(engine.call('assurance.ping', {})).rejects.toThrow(
                /assuranceapi bridge is not available/,
            );
        });

        test('default params is an empty object', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ pong: true }));
            await engine.call('assurance.ping');
            expect(api.engineCall).toHaveBeenCalledWith('assurance.ping', {});
        });
    });

    describe('assurance.loadScanDefinitions', () => {
        test('sends empty params and returns the scanDefinitions array', async () => {
            const defs: ScanDefinition[] = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }];
            api.engineCall.mockResolvedValueOnce(ok({ scanDefinitions: defs }));

            const result = await engine.loadScanDefinitions();

            expect(api.engineCall).toHaveBeenCalledWith('assurance.loadScanDefinitions', {});
            expect(result).toEqual(defs);
        });

        test('returns [] when scanDefinitions is missing from the envelope', async () => {
            api.engineCall.mockResolvedValueOnce(ok({}));
            await expect(engine.loadScanDefinitions()).resolves.toEqual([]);
        });

        test('-32001 engine error rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32001, 'Engine error'));
            await expect(engine.loadScanDefinitions()).rejects.toMatchObject({
                code: -32001,
                message: 'Engine error',
            });
        });
    });

    describe('assurance.saveScanDefinition', () => {
        test('wraps the scanDefinition in { scanDefinition } params and unwraps the saved entity', async () => {
            const incoming: ScanDefinition = { id: 0, name: 'New' };
            const saved: ScanDefinition = { id: 42, name: 'New' };
            api.engineCall.mockResolvedValueOnce(ok({ scanDefinition: saved }));

            const result = await engine.saveScanDefinition(incoming);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.saveScanDefinition', {
                scanDefinition: incoming,
            });
            expect(result).toEqual(saved);
        });

        test('-32602 invalid params rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32602, 'name is required'));
            await expect(
                engine.saveScanDefinition({ id: 0, name: '' }),
            ).rejects.toMatchObject({ code: -32602 });
        });
    });

    describe('assurance.deleteScanDefinition', () => {
        test('wraps the id in { scanDefinitionId } params and returns true on success', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ deleted: true }));

            const result = await engine.deleteScanDefinition(7);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.deleteScanDefinition', {
                scanDefinitionId: 7,
            });
            expect(result).toBe(true);
        });

        test('returns false when deleted is anything other than the literal true', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ deleted: false }));
            await expect(engine.deleteScanDefinition(7)).resolves.toBe(false);
        });

        test('-32002 not found rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32002, 'not found'));
            await expect(engine.deleteScanDefinition(99)).rejects.toMatchObject({ code: -32002 });
        });
    });

    describe('assurance.loadScans', () => {
        test('sends empty params and returns the scans array', async () => {
            const scans: Scan[] = [{ id: 10 } as unknown as Scan];
            api.engineCall.mockResolvedValueOnce(ok({ scans }));

            const result = await engine.loadScans();

            expect(api.engineCall).toHaveBeenCalledWith('assurance.loadScans', {});
            expect(result).toEqual(scans);
        });

        test('returns [] when scans is missing from the envelope', async () => {
            api.engineCall.mockResolvedValueOnce(ok({}));
            await expect(engine.loadScans()).resolves.toEqual([]);
        });

        test('-32001 engine error rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32001, 'boom'));
            await expect(engine.loadScans()).rejects.toMatchObject({ code: -32001 });
        });
    });

    describe('assurance.deleteScan', () => {
        test('wraps the id in { scanId } params and returns true on success', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ deleted: true }));

            const result = await engine.deleteScan(11);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.deleteScan', { scanId: 11 });
            expect(result).toBe(true);
        });

        test('-32002 not found rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32002, 'gone'));
            await expect(engine.deleteScan(99)).rejects.toMatchObject({ code: -32002 });
        });
    });

    describe('assurance.loadScanResults', () => {
        test('wraps the id in { scanId } params and returns the results array', async () => {
            const results: ComparisonResult[] = [{ id: 1 } as unknown as ComparisonResult];
            api.engineCall.mockResolvedValueOnce(ok({ results }));

            const result = await engine.loadScanResults(42);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.loadScanResults', { scanId: 42 });
            expect(result).toEqual(results);
        });

        test('returns [] when results is missing from the envelope', async () => {
            api.engineCall.mockResolvedValueOnce(ok({}));
            await expect(engine.loadScanResults(42)).resolves.toEqual([]);
        });

        test('-32002 not found rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32002, 'gone'));
            await expect(engine.loadScanResults(42)).rejects.toMatchObject({ code: -32002 });
        });
    });

    describe('assurance.loadComparisonResult', () => {
        test('wraps the id in { resultId } params and returns the result object', async () => {
            const row: ComparisonResult = { id: 9 } as unknown as ComparisonResult;
            api.engineCall.mockResolvedValueOnce(ok({ result: row }));

            const result = await engine.loadComparisonResult(9);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.loadComparisonResult', { resultId: 9 });
            expect(result).toEqual(row);
        });

        test('-32002 not found rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32002, 'gone'));
            await expect(engine.loadComparisonResult(99)).rejects.toMatchObject({ code: -32002 });
        });
    });

    describe('assurance.performScan', () => {
        test('wraps id+merge into { scanDefinitionId, merge } and unwraps scanId', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ scanId: 314 }));

            const result = await engine.performScan(7, true);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.performScan', {
                scanDefinitionId: 7,
                merge: true,
            });
            expect(result).toBe(314);
        });

        test('default merge is false', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ scanId: 1 }));
            await engine.performScan(7);
            expect(api.engineCall).toHaveBeenCalledWith('assurance.performScan', {
                scanDefinitionId: 7,
                merge: false,
            });
        });

        test('-32003 already-running rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32003, 'already running'));
            await expect(engine.performScan(7)).rejects.toMatchObject({ code: -32003 });
        });
    });

    describe('assurance.mergeScanResult', () => {
        test('wraps id+strategy into { resultId, strategy } and returns the parent scanId', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ resultId: 314, scanId: 42 }));

            const result = await engine.mergeScanResult(314, 'SOURCE');

            expect(api.engineCall).toHaveBeenCalledWith('assurance.mergeScanResult', {
                resultId: 314,
                strategy: 'SOURCE',
            });
            expect(result).toEqual({ resultId: 314, scanId: 42 });
        });

        test('-32007 already-running rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32007, 'in flight'));
            await expect(engine.mergeScanResult(1, 'TARGET')).rejects.toMatchObject({
                code: -32007,
            });
        });
    });

    describe('assurance.mergeScan', () => {
        test('wraps id into { scanId } and returns the scanId', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ scanId: 42 }));

            const result = await engine.mergeScan(42);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.mergeScan', { scanId: 42 });
            expect(result).toBe(42);
        });

        test('-32006 already-running rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32006, 'in flight'));
            await expect(engine.mergeScan(1)).rejects.toMatchObject({ code: -32006 });
        });
    });

    describe('assurance.restoreDeletedItem', () => {
        test('wraps id into { resultId } and returns both ids', async () => {
            api.engineCall.mockResolvedValueOnce(ok({ resultId: 314, scanId: 42 }));

            const result = await engine.restoreDeletedItem(314);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.restoreDeletedItem', {
                resultId: 314,
            });
            expect(result).toEqual({ resultId: 314, scanId: 42 });
        });

        test('-32008 already-running rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32008, 'in flight'));
            await expect(engine.restoreDeletedItem(1)).rejects.toMatchObject({ code: -32008 });
        });
    });

    describe('assurance.loadApplicationConfiguration', () => {
        test('sends empty params and returns the configuration', async () => {
            const cfg: ApplicationConfiguration = {
                id: 1,
                ignoredFileNames: '.DS_Store',
                ignoredFileExtensions: '',
                numberOfScanThreads: 4,
            };
            api.engineCall.mockResolvedValueOnce(ok({ configuration: cfg }));

            const result = await engine.loadApplicationConfiguration();

            expect(api.engineCall).toHaveBeenCalledWith('assurance.loadApplicationConfiguration', {});
            expect(result).toEqual(cfg);
        });

        test('-32001 engine error rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32001, 'oops'));
            await expect(engine.loadApplicationConfiguration()).rejects.toMatchObject({
                code: -32001,
            });
        });
    });

    describe('assurance.saveApplicationConfiguration', () => {
        test('wraps the configuration in { configuration } params and unwraps the saved entity', async () => {
            const incoming: ApplicationConfiguration = {
                id: 1,
                ignoredFileNames: 'a',
                ignoredFileExtensions: 'b',
                numberOfScanThreads: 8,
            };
            const saved: ApplicationConfiguration = { ...incoming, numberOfScanThreads: 8 };
            api.engineCall.mockResolvedValueOnce(ok({ configuration: saved }));

            const result = await engine.saveApplicationConfiguration(incoming);

            expect(api.engineCall).toHaveBeenCalledWith('assurance.saveApplicationConfiguration', {
                configuration: incoming,
            });
            expect(result).toEqual(saved);
        });

        test('-32602 invalid params rejects with EngineCallError', async () => {
            api.engineCall.mockResolvedValueOnce(err(-32602, 'numberOfScanThreads must be > 0'));
            await expect(
                engine.saveApplicationConfiguration({
                    id: 1,
                    ignoredFileNames: '',
                    ignoredFileExtensions: '',
                    numberOfScanThreads: 0,
                }),
            ).rejects.toMatchObject({ code: -32602 });
        });
    });

    describe('notification subscribers', () => {
        test('onScanNotification forwards in-family methods and ignores out-of-family', () => {
            const events: ScanNotificationParams[] = [];
            const off = onScanNotification((n) => events.push(n));

            for (const method of [
                'assurance.scanStarted',
                'assurance.scanProgress',
                'assurance.scanCompleted',
                'assurance.scanFailed',
            ]) {
                api.notifySubscribers({ method, params: { scanId: 1 } });
            }
            // Out-of-family — should not deliver.
            api.notifySubscribers({ method: 'assurance.mergeStarted', params: { scanId: 1 } });
            api.notifySubscribers({ method: 'assurance.somethingElse', params: {} });

            expect(events.map((e) => e.method)).toEqual([
                'assurance.scanStarted',
                'assurance.scanProgress',
                'assurance.scanCompleted',
                'assurance.scanFailed',
            ]);

            off();
            api.notifySubscribers({ method: 'assurance.scanStarted', params: { scanId: 2 } });
            expect(events.length).toBe(4);
        });

        test('onMergeNotification forwards in-family methods and ignores out-of-family', () => {
            const events: MergeNotificationParams[] = [];
            const off = onMergeNotification((n) => events.push(n));

            for (const method of [
                'assurance.mergeStarted',
                'assurance.mergeProgress',
                'assurance.mergeCompleted',
                'assurance.mergeFailed',
            ]) {
                api.notifySubscribers({ method, params: { scanId: 7 } });
            }
            api.notifySubscribers({ method: 'assurance.scanStarted', params: { scanId: 7 } });
            api.notifySubscribers({ method: 'assurance.resultMergeStarted', params: { resultId: 1, scanId: 7 } });

            expect(events.map((e) => e.method)).toEqual([
                'assurance.mergeStarted',
                'assurance.mergeProgress',
                'assurance.mergeCompleted',
                'assurance.mergeFailed',
            ]);

            off();
        });

        test('onResultMergeNotification forwards in-family methods and ignores out-of-family', () => {
            const events: ResultMergeNotificationParams[] = [];
            const off = onResultMergeNotification((n) => events.push(n));

            for (const method of [
                'assurance.resultMergeStarted',
                'assurance.resultMergeProgress',
                'assurance.resultMergeCompleted',
                'assurance.resultMergeFailed',
            ]) {
                api.notifySubscribers({ method, params: { resultId: 1, scanId: 2 } });
            }
            api.notifySubscribers({ method: 'assurance.mergeStarted', params: { scanId: 2 } });

            expect(events.map((e) => e.method)).toEqual([
                'assurance.resultMergeStarted',
                'assurance.resultMergeProgress',
                'assurance.resultMergeCompleted',
                'assurance.resultMergeFailed',
            ]);

            off();
        });

        test('onRestoreNotification forwards in-family methods and ignores out-of-family', () => {
            const events: RestoreNotificationParams[] = [];
            const off = onRestoreNotification((n) => events.push(n));

            for (const method of [
                'assurance.restoreStarted',
                'assurance.restoreProgress',
                'assurance.restoreCompleted',
                'assurance.restoreFailed',
            ]) {
                api.notifySubscribers({ method, params: { resultId: 1, scanId: 2 } });
            }
            api.notifySubscribers({ method: 'assurance.scanStarted', params: { scanId: 2 } });
            api.notifySubscribers({ method: 'assurance.mergeStarted', params: { scanId: 2 } });

            expect(events.map((e) => e.method)).toEqual([
                'assurance.restoreStarted',
                'assurance.restoreProgress',
                'assurance.restoreCompleted',
                'assurance.restoreFailed',
            ]);

            off();
        });

        test('notification listeners pass params through verbatim', () => {
            const events: ScanNotificationParams[] = [];
            onScanNotification((n) => events.push(n));

            const params = {
                scanId: 42,
                progress: { phase: 'comparing', itemsProcessed: 100, itemsTotal: 500 },
            };
            api.notifySubscribers({ method: 'assurance.scanProgress', params });

            expect(events).toHaveLength(1);
            expect(events[0]).toEqual({ method: 'assurance.scanProgress', params });
        });

        test('multiple subscribers across families coexist', () => {
            const scanEvents: string[] = [];
            const mergeEvents: string[] = [];
            const offScan = onScanNotification((n) => scanEvents.push(n.method));
            const offMerge = onMergeNotification((n) => mergeEvents.push(n.method));

            api.notifySubscribers({ method: 'assurance.scanStarted', params: { scanId: 1 } });
            api.notifySubscribers({ method: 'assurance.mergeStarted', params: { scanId: 1 } });

            expect(scanEvents).toEqual(['assurance.scanStarted']);
            expect(mergeEvents).toEqual(['assurance.mergeStarted']);

            offScan();
            offMerge();
        });
    });
});
