/**
 * Renderer-side typed wrapper around `window.assuranceapi.engineCall`.
 *
 * The preload bridge ferries a discriminated-union envelope across the
 * `contextBridge` boundary; this module unwraps it back into a Promise so
 * call sites read like ordinary async APIs:
 *
 *   const { scanDefinitions } = await engine.call('assurance.loadScanDefinitions');
 *
 * Rejections are always {@link EngineCallError} instances carrying the
 * JSON-RPC `code`, `message`, and optional `data`.
 */

import type {
    EngineCallEnvelope,
    EngineCallErrorWire,
    EngineNotificationEnvelope,
    EngineNotificationListener,
} from '../preload';
import type { MainMenuStateWire } from '../main-menu/mainMenuState';
import type ScanDefinition from '../model/ScanDefinition';
import type Scan from '../model/Scan';
import type ComparisonResult from '../model/ComparisonResult';
import type ApplicationConfiguration from '../model/ApplicationConfiguration';

export class EngineCallError extends Error {
    readonly code: number;
    readonly data: unknown;

    constructor(error: EngineCallErrorWire) {
        super(error.message);
        this.name = 'EngineCallError';
        this.code = error.code;
        this.data = error.data;
    }
}

interface AssuranceApiSurface {
    engineCall: (method: string, params: unknown) => Promise<EngineCallEnvelope>;
    onEngineNotification: (listener: EngineNotificationListener) => () => void;
    /**
     * Main-process (Electron) menu command handler.
     * Subscribes to a string command emitted by the Electron main process.
     */
    onMainMenuCommand?: (listener: (command: string) => void) => () => void;
    /**
     * Sends the current enablement state to the Electron main process so
     * menu items can be disabled when they have no focused target.
     */
    setMainMenuState?: (state: MainMenuStateWire) => void;
}

declare global {
    interface Window {
        assuranceapi: AssuranceApiSurface & {
            // selectPath is preserved here so the typing remains compatible
            // with the existing callback-style API.
            selectPath: (options: unknown, callback: (result: unknown) => void) => void;
        };
    }
}

function api(): AssuranceApiSurface {
    if (typeof window === 'undefined' || !window.assuranceapi) {
        throw new Error('assuranceapi bridge is not available in this context.');
    }
    return window.assuranceapi;
}

/**
 * Invokes a JSON-RPC method on the embedded Java engine. Resolves with the
 * `result` payload; rejects with {@link EngineCallError} on a JSON-RPC error
 * response or transport failure.
 */
export async function call<TResult = unknown>(method: string, params: unknown = {}): Promise<TResult> {
    const envelope = await api().engineCall(method, params);
    if (envelope.ok === true) {
        return envelope.result as TResult;
    }
    throw new EngineCallError(envelope.error);
}

/**
 * Loads all persisted scan definitions from the engine.
 *
 * Mirrors the {@code assurance.loadScanDefinitions} contract documented in
 * {@code docs/ipc-contract.md}: returns an array of {@link ScanDefinition}
 * values including {@code scanMapping} (source/target paths) for each entry.
 *
 * @throws {EngineCallError} on a JSON-RPC error response (typically
 *   {@code -32001} engine error) or transport failure.
 */
export async function loadScanDefinitions(): Promise<ScanDefinition[]> {
    const result = await call<{ scanDefinitions: ScanDefinition[] }>('assurance.loadScanDefinitions', {});
    return result.scanDefinitions ?? [];
}

/**
 * Persists a scan definition. Pass an object without `id` to create; include
 * `id` to update an existing definition. Resolves with the persisted entity
 * (including the engine-assigned `id` and the saved `scanMapping`).
 *
 * Mirrors the {@code assurance.saveScanDefinition} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32602} on validation failure (e.g. empty
 *   name, missing source/target), {@code -32001} on engine error, or transport
 *   failure.
 */
export async function saveScanDefinition(scanDefinition: ScanDefinition): Promise<ScanDefinition> {
    const result = await call<{ scanDefinition: ScanDefinition }>(
        'assurance.saveScanDefinition',
        { scanDefinition },
    );
    return result.scanDefinition;
}

/**
 * Deletes a scan definition by id. Resolves with `true` on success.
 *
 * Mirrors the {@code assurance.deleteScanDefinition} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no definition exists with the
 *   given id, {@code -32602} if the id is missing or non-integer, or
 *   {@code -32001} on engine error.
 */
export async function deleteScanDefinition(scanDefinitionId: number): Promise<boolean> {
    const result = await call<{ deleted: boolean }>(
        'assurance.deleteScanDefinition',
        { scanDefinitionId },
    );
    return result.deleted === true;
}

/**
 * Loads the persisted scan history.
 *
 * Mirrors the {@code assurance.loadScans} contract documented in
 * {@code docs/ipc-contract.md}: each entry is a thin projection that
 * carries scalar identifiers, ISO 8601 timestamps, a minimal {@code scanDef}
 * reference, and a {@code resultCount} scalar. The full
 * {@code results} array is intentionally omitted; load it on demand via
 * {@link loadScanResults}.
 *
 * @throws {EngineCallError} {@code -32001} on engine error.
 */
export async function loadScans(): Promise<Scan[]> {
    const result = await call<{ scans: Scan[] }>('assurance.loadScans', {});
    return result.scans ?? [];
}

/**
 * Deletes a scan (and its results) by id. Resolves with `true` on success.
 *
 * Mirrors the {@code assurance.deleteScan} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no scan exists with the given
 *   id, {@code -32602} if the id is missing or non-integer, or {@code -32001}
 *   on engine error.
 */
export async function deleteScan(scanId: number): Promise<boolean> {
    const result = await call<{ deleted: boolean }>('assurance.deleteScan', { scanId });
    return result.deleted === true;
}

/**
 * Loads all comparison results for a given scan.
 *
 * Mirrors the {@code assurance.loadScanResults} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no scan exists with the
 *   given id, {@code -32602} on invalid params, or {@code -32001} on
 *   engine error.
 */
export async function loadScanResults(scanId: number): Promise<ComparisonResult[]> {
    const result = await call<{ results: ComparisonResult[] }>(
        'assurance.loadScanResults',
        { scanId },
    );
    return result.results ?? [];
}

/**
 * Loads a single comparison result by id (same wire shape as rows from
 * {@link loadScanResults}). Use after a per-row merge or restore to refresh
 * one row without reloading the full list.
 *
 * Mirrors the {@code assurance.loadComparisonResult} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no result exists with the
 *   given id, {@code -32602} on invalid params, or {@code -32001} on
 *   engine error.
 */
export async function loadComparisonResult(resultId: number): Promise<ComparisonResult> {
    const result = await call<{ result: ComparisonResult }>('assurance.loadComparisonResult', {
        resultId,
    });
    return result.result;
}

/**
 * Starts a scan asynchronously. Resolves with the new {@code scanId} as soon
 * as the scan record has been persisted. Progress is delivered via
 * server-initiated notifications; subscribe via {@link onScanNotification}.
 *
 * Mirrors the {@code assurance.performScan} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no definition exists with the
 *   given id, {@code -32003} if a scan is already running for that
 *   definition, {@code -32602} on invalid params, or {@code -32001} on
 *   engine error.
 */
export async function performScan(scanDefinitionId: number, merge = false): Promise<number> {
    const result = await call<{ scanId: number }>(
        'assurance.performScan',
        { scanDefinitionId, merge },
    );
    return result.scanId;
}

/**
 * Strategy values accepted by {@link mergeScanResult}, mirroring the Java
 * {@code AssuranceMergeStrategy} enum.
 */
export type MergeStrategy = 'SOURCE' | 'TARGET' | 'BOTH';

/**
 * Starts a single-result merge asynchronously. Resolves with the
 * {@code resultId} and parent {@code scanId} as soon as the merge has been
 * accepted; progress is delivered via server-initiated notifications.
 * Subscribe via {@link onResultMergeNotification}.
 *
 * Mirrors the {@code assurance.mergeScanResult} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no result exists with the
 *   given id, {@code -32007} if a merge is already running for that
 *   result, {@code -32602} on invalid params, or {@code -32001} on
 *   engine error.
 */
export async function mergeScanResult(
    resultId: number,
    strategy: MergeStrategy,
): Promise<{ resultId: number; scanId: number }> {
    return call<{ resultId: number; scanId: number }>(
        'assurance.mergeScanResult',
        { resultId, strategy },
    );
}

/**
 * Starts a whole-scan merge asynchronously. Resolves with the {@code scanId}
 * as soon as the merge has been accepted; progress is delivered via
 * server-initiated {@code assurance.merge*} notifications. Subscribe via
 * {@link onMergeNotification}.
 *
 * The merge strategy is taken from the scan's owning {@code ScanDefinition}
 * (mirroring 1.x semantics — the wire never carries a strategy here).
 *
 * Mirrors the {@code assurance.mergeScan} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no scan exists with the given
 *   id, {@code -32006} if a whole-scan merge is already running for that
 *   scan, {@code -32602} on invalid params, or {@code -32001} on engine
 *   error.
 */
export async function mergeScan(scanId: number): Promise<number> {
    const result = await call<{ scanId: number }>('assurance.mergeScan', { scanId });
    return result.scanId;
}

/**
 * Starts a single-result restore asynchronously. Resolves with the
 * {@code resultId} and parent {@code scanId} as soon as the restore has
 * been accepted; progress is delivered via server-initiated notifications.
 * Subscribe via {@link onRestoreNotification}.
 *
 * The strategy is chosen server-side from the result's
 * {@code AssuranceResultResolution} (mirroring 1.x semantics — the wire
 * never carries a strategy here).
 *
 * Mirrors the {@code assurance.restoreDeletedItem} contract documented in
 * {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32002} if no result exists with the
 *   given id, {@code -32008} if a restore is already running for that
 *   result, {@code -32602} on invalid params, or {@code -32001} on engine
 *   error.
 */
export async function restoreDeletedItem(
    resultId: number,
): Promise<{ resultId: number; scanId: number }> {
    return call<{ resultId: number; scanId: number }>(
        'assurance.restoreDeletedItem',
        { resultId },
    );
}

/**
 * Loads the persisted {@link ApplicationConfiguration} singleton. The engine
 * creates the default row on first call when no row exists yet (mirroring 1.x
 * semantics).
 *
 * Mirrors the {@code assurance.loadApplicationConfiguration} contract
 * documented in {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32001} on engine error.
 */
export async function loadApplicationConfiguration(): Promise<ApplicationConfiguration> {
    const result = await call<{ configuration: ApplicationConfiguration }>(
        'assurance.loadApplicationConfiguration',
        {},
    );
    return result.configuration;
}

/**
 * Persists application configuration. Pass an object without `id` to create
 * the singleton; include `id` to update. Resolves with the persisted entity
 * (including the parsed list views re-derived from the saved scalars).
 *
 * Mirrors the {@code assurance.saveApplicationConfiguration} contract
 * documented in {@code docs/ipc-contract.md}.
 *
 * @throws {EngineCallError} {@code -32602} on validation failure (e.g.
 *   {@code numberOfScanThreads < 1}, non-integer id), {@code -32001} on
 *   engine error.
 */
export async function saveApplicationConfiguration(
    configuration: ApplicationConfiguration,
): Promise<ApplicationConfiguration> {
    const result = await call<{ configuration: ApplicationConfiguration }>(
        'assurance.saveApplicationConfiguration',
        { configuration },
    );
    return result.configuration;
}

export interface ScanProgressPayload {
    /** Free-form name of the engine's current sub-phase. */
    phase?: string;
    /** Human-readable label for the file or step currently being processed. */
    currentItem?: string;
    /** Number of items processed so far in the current phase, when known. */
    itemsProcessed?: number;
    /** Total number of items expected in the current phase, when known. */
    itemsTotal?: number;
}

export type ScanNotificationParams =
    | { method: 'assurance.scanStarted'; params: { scanId: number; startedAt?: string } }
    | { method: 'assurance.scanProgress'; params: { scanId: number; progress: ScanProgressPayload } }
    | {
          method: 'assurance.scanCompleted';
          params: { scanId: number; resultCount: number; completedAt?: string };
      }
    | {
          method: 'assurance.scanFailed';
          params: {
              scanId: number;
              failedAt?: string;
              error: { code: number; message: string };
          };
      };

export type ScanNotificationListener = (notification: ScanNotificationParams) => void;

const SCAN_NOTIFICATION_METHODS = new Set<string>([
    'assurance.scanStarted',
    'assurance.scanProgress',
    'assurance.scanCompleted',
    'assurance.scanFailed',
]);

/**
 * Subscribes to {@code assurance.scan*} server-initiated notifications.
 * Returns an unsubscribe function.
 *
 * Notifications outside the scan family are silently ignored. Notification
 * payloads from the engine are passed through verbatim — this helper just
 * narrows the type for callers.
 */
export function onScanNotification(listener: ScanNotificationListener): () => void {
    return api().onEngineNotification((envelope: EngineNotificationEnvelope) => {
        if (!SCAN_NOTIFICATION_METHODS.has(envelope.method)) {
            return;
        }
        listener({
            method: envelope.method,
            params: envelope.params,
        } as ScanNotificationParams);
    });
}

export interface ResultMergeProgressPayload {
    /** Free-form name of the engine's current sub-phase. */
    phase?: string;
    /** Human-readable label for the file or step currently being processed. */
    currentItem?: string;
    /** Bytes processed so far in the current phase, when known. */
    bytesProcessed?: number;
    /** Total bytes expected in the current phase, when known. */
    bytesTotal?: number;
}

export type ResultMergeNotificationParams =
    | {
          method: 'assurance.resultMergeStarted';
          params: { resultId: number; scanId: number; startedAt?: string };
      }
    | {
          method: 'assurance.resultMergeProgress';
          params: { resultId: number; scanId: number; progress: ResultMergeProgressPayload };
      }
    | {
          method: 'assurance.resultMergeCompleted';
          params: { resultId: number; scanId: number; completedAt?: string };
      }
    | {
          method: 'assurance.resultMergeFailed';
          params: {
              resultId: number;
              scanId: number;
              failedAt?: string;
              error: { code: number; message: string };
          };
      };

export type ResultMergeNotificationListener = (
    notification: ResultMergeNotificationParams,
) => void;

const RESULT_MERGE_NOTIFICATION_METHODS = new Set<string>([
    'assurance.resultMergeStarted',
    'assurance.resultMergeProgress',
    'assurance.resultMergeCompleted',
    'assurance.resultMergeFailed',
]);

/**
 * Subscribes to {@code assurance.resultMerge*} server-initiated notifications.
 * Returns an unsubscribe function.
 *
 * Notifications outside the result-merge family are silently ignored.
 * Notification payloads from the engine are passed through verbatim — this
 * helper just narrows the type for callers.
 */
export function onResultMergeNotification(
    listener: ResultMergeNotificationListener,
): () => void {
    return api().onEngineNotification((envelope: EngineNotificationEnvelope) => {
        if (!RESULT_MERGE_NOTIFICATION_METHODS.has(envelope.method)) {
            return;
        }
        listener({
            method: envelope.method,
            params: envelope.params,
        } as ResultMergeNotificationParams);
    });
}

export interface MergeProgressPayload {
    /** Free-form name of the engine's current sub-phase. */
    phase?: string;
    /** Human-readable label for the file or step currently being processed. */
    currentItem?: string;
    /** Number of items processed so far in the current phase, when known. */
    itemsProcessed?: number;
    /** Total number of items expected in the current phase, when known. */
    itemsTotal?: number;
}

export type MergeNotificationParams =
    | {
          method: 'assurance.mergeStarted';
          params: { scanId: number; startedAt?: string };
      }
    | {
          method: 'assurance.mergeProgress';
          params: { scanId: number; progress: MergeProgressPayload };
      }
    | {
          method: 'assurance.mergeCompleted';
          params: { scanId: number; itemsMerged: number; completedAt?: string };
      }
    | {
          method: 'assurance.mergeFailed';
          params: {
              scanId: number;
              failedAt?: string;
              error: { code: number; message: string };
          };
      };

export type MergeNotificationListener = (notification: MergeNotificationParams) => void;

const MERGE_NOTIFICATION_METHODS = new Set<string>([
    'assurance.mergeStarted',
    'assurance.mergeProgress',
    'assurance.mergeCompleted',
    'assurance.mergeFailed',
]);

/**
 * Subscribes to {@code assurance.merge*} server-initiated notifications.
 * Returns an unsubscribe function.
 *
 * Notifications outside the whole-scan-merge family are silently ignored.
 * Notification payloads from the engine are passed through verbatim — this
 * helper just narrows the type for callers.
 */
export function onMergeNotification(listener: MergeNotificationListener): () => void {
    return api().onEngineNotification((envelope: EngineNotificationEnvelope) => {
        if (!MERGE_NOTIFICATION_METHODS.has(envelope.method)) {
            return;
        }
        listener({
            method: envelope.method,
            params: envelope.params,
        } as MergeNotificationParams);
    });
}

export interface RestoreProgressPayload {
    /** Free-form name of the engine's current sub-phase. */
    phase?: string;
    /** Human-readable label for the file or step currently being processed. */
    currentItem?: string;
    /** Bytes processed so far in the current phase, when known. */
    bytesProcessed?: number;
    /** Total bytes expected in the current phase, when known. */
    bytesTotal?: number;
}

export type RestoreNotificationParams =
    | {
          method: 'assurance.restoreStarted';
          params: { resultId: number; scanId: number; startedAt?: string };
      }
    | {
          method: 'assurance.restoreProgress';
          params: { resultId: number; scanId: number; progress: RestoreProgressPayload };
      }
    | {
          method: 'assurance.restoreCompleted';
          params: { resultId: number; scanId: number; completedAt?: string };
      }
    | {
          method: 'assurance.restoreFailed';
          params: {
              resultId: number;
              scanId: number;
              failedAt?: string;
              error: { code: number; message: string };
          };
      };

export type RestoreNotificationListener = (notification: RestoreNotificationParams) => void;

const RESTORE_NOTIFICATION_METHODS = new Set<string>([
    'assurance.restoreStarted',
    'assurance.restoreProgress',
    'assurance.restoreCompleted',
    'assurance.restoreFailed',
]);

/**
 * Subscribes to {@code assurance.restore*} server-initiated notifications.
 * Returns an unsubscribe function.
 *
 * Notifications outside the restore family are silently ignored.
 * Notification payloads from the engine are passed through verbatim — this
 * helper just narrows the type for callers.
 */
export function onRestoreNotification(listener: RestoreNotificationListener): () => void {
    return api().onEngineNotification((envelope: EngineNotificationEnvelope) => {
        if (!RESTORE_NOTIFICATION_METHODS.has(envelope.method)) {
            return;
        }
        listener({
            method: envelope.method,
            params: envelope.params,
        } as RestoreNotificationParams);
    });
}

export const engine = {
    call,
    loadScanDefinitions,
    saveScanDefinition,
    deleteScanDefinition,
    loadScans,
    deleteScan,
    loadScanResults,
    loadComparisonResult,
    performScan,
    mergeScanResult,
    mergeScan,
    restoreDeletedItem,
    loadApplicationConfiguration,
    saveApplicationConfiguration,
    onScanNotification,
    onResultMergeNotification,
    onMergeNotification,
    onRestoreNotification,
};
