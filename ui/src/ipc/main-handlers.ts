/**
 * Pure helpers powering the Electron main process's renderer-facing IPC
 * surface. Extracted from {@code index.ts} so contract tests can drive them
 * without booting Electron.
 *
 * - {@link handleEngineCall} wraps a single {@code engineRpc.call} into the
 *   discriminated-union envelope the preload bridge ferries across the
 *   {@code contextBridge} boundary (see {@code preload.ts}).
 * - {@link registerNotificationForwarders} bridges {@code engineRpc}'s
 *   server-initiated notification fan-out onto a {@link NotificationSink}
 *   abstraction (a {@code BrowserWindow}'s {@code webContents} in production).
 * - {@link FORWARDED_NOTIFICATIONS} is the canonical list of methods the
 *   renderer subscribes to via {@code window.assuranceapi.onEngineNotification}.
 */

import { EngineRpcError } from '../engine/rpc';
import type { EngineCallEnvelope } from '../preload';

export const FORWARDED_NOTIFICATIONS: readonly string[] = [
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

/**
 * Minimal structural typing of the {@code EngineRpc} surface
 * {@link handleEngineCall} actually uses. Declared structurally — rather than
 * via {@code Pick<EngineRpc, 'call'>} — so test fakes can supply a
 * non-generic {@code call} signature without bumping into TS's strict
 * generic-method assignability checks.
 */
export interface EngineRpcCaller {
    call(method: string, params: unknown): Promise<unknown>;
}

export interface EngineRpcSubscriber {
    onNotification(method: string, listener: (params: unknown) => void): () => void;
}

export interface NotificationEnvelope {
    method: string;
    params: unknown;
}

export interface NotificationSink {
    send(envelope: NotificationEnvelope): void;
    isClosed(): boolean;
}

/**
 * Resolves the renderer-bound envelope for a single {@code engineCall} IPC
 * invocation. Always resolves (never throws) — engine errors are folded back
 * into {@code { ok: false, error }} so the preload bridge can hand the
 * renderer a typed Error without losing JSON-RPC {@code code}/{@code data}.
 */
export async function handleEngineCall(
    method: unknown,
    params: unknown,
    rpc: EngineRpcCaller,
): Promise<EngineCallEnvelope> {
    if (typeof method !== 'string' || method.length === 0) {
        return {
            ok: false,
            error: { code: -32600, message: 'Invalid request: method must be a non-empty string' },
        };
    }
    try {
        const result = await rpc.call(method, params);
        return { ok: true, result };
    } catch (err) {
        if (err instanceof EngineRpcError) {
            return {
                ok: false,
                error: { code: err.code, message: err.message, data: err.data },
            };
        }
        const message = err instanceof Error ? err.message : String(err);
        return {
            ok: false,
            error: { code: -32603, message },
        };
    }
}

/**
 * Wires every method in {@code methods} to forward {@code engineRpc}
 * notifications onto {@code sink}. Returns a teardown that detaches every
 * forwarder. Notifications are dropped silently when {@code sink.isClosed()}
 * returns {@code true} (e.g. the {@code BrowserWindow} has been destroyed).
 */
export function registerNotificationForwarders(
    rpc: EngineRpcSubscriber,
    sink: NotificationSink,
    methods: readonly string[] = FORWARDED_NOTIFICATIONS,
): () => void {
    const unsubs: Array<() => void> = [];
    for (const method of methods) {
        const off = rpc.onNotification(method, (params: unknown) => {
            if (sink.isClosed()) {
                return;
            }
            sink.send({ method, params });
        });
        unsubs.push(off);
    }
    return () => {
        for (const off of unsubs) {
            off();
        }
    };
}
