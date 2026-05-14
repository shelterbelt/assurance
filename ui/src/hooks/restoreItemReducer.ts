/**
 * Pure-function reducer that models the lifecycle of one or more in-flight
 * single-result restores as observed by the renderer.
 *
 * The renderer never inspects the underlying WebSocket frames directly; it
 * subscribes to the {@code assurance.restore*} notification family via
 * {@link engine.onRestoreNotification}, normalizes each notification into a
 * {@link RestoreItemAction}, and reduces it onto the previous state.
 *
 * Restores are keyed by {@code resultId} (one in-flight restore per result,
 * enforced server-side via {@code -32008}). The state machine for any single
 * entry mirrors the per-result merge lifecycle: {@code starting} →
 * {@code running} → {@code completed} / {@code failed} / {@code error}.
 *
 * Lifecycle transitions intentionally tolerate out-of-order delivery —
 * see {@link resultMergeReducer} for the full rationale; the rules are
 * identical here, with {@code resultId} as the key and no strategy.
 */

import type { RestoreProgressPayload } from '../api/engine';

export type RestoreItemStatus =
    /** The user initiated a restore and we are awaiting the synchronous response. */
    | 'starting'
    /**
     * The engine acknowledged the restore (either via a synchronous
     * {@code restoreDeletedItem} response or an
     * {@code assurance.restoreStarted} notification). The restore is
     * in flight.
     */
    | 'running'
    /** Terminal state for a restore that completed successfully. */
    | 'completed'
    /** Terminal state for a restore that failed mid-flight. */
    | 'failed'
    /**
     * Terminal state for a synchronous rejection of the
     * {@code restoreDeletedItem} call. No engine-side restore is underway.
     */
    | 'error';

export interface RestoreItemError {
    /** JSON-RPC error code (typically negative). */
    code: number;
    message: string;
    /** Optional structured payload mirroring the JSON-RPC {@code data} field. */
    data?: unknown;
}

export interface RestoreItemRunState {
    resultId: number;
    /** Parent scan id. {@code null} until the engine confirms the restore. */
    scanId: number | null;
    status: RestoreItemStatus;
    /** Most recent {@code assurance.restoreProgress} payload, if any. */
    progress: RestoreProgressPayload | null;
    /** Populated only in the {@code failed} or {@code error} state. */
    error: RestoreItemError | null;
}

/**
 * Map keyed by {@code resultId}. Stored as a plain object (JSON-serializable,
 * hashable by reference) so it works cleanly with React's strict-mode
 * double-render and hot-reload.
 */
export interface RestoreItemState {
    runs: Record<number, RestoreItemRunState>;
}

export const INITIAL_RESTORE_ITEM_STATE: RestoreItemState = {
    runs: {},
};

export type RestoreItemAction =
    | { type: 'REQUEST'; resultId: number }
    | { type: 'ACCEPTED'; resultId: number; scanId: number }
    | { type: 'REJECTED'; resultId: number; error: RestoreItemError }
    | { type: 'STARTED'; resultId: number; scanId: number }
    | {
          type: 'PROGRESS';
          resultId: number;
          scanId: number;
          progress: RestoreProgressPayload;
      }
    | { type: 'COMPLETED'; resultId: number; scanId: number }
    | { type: 'FAILED'; resultId: number; scanId: number; error: RestoreItemError }
    /** UI-driven dismissal of a single terminal entry. */
    | { type: 'DISMISS'; resultId: number };

function setRun(
    state: RestoreItemState,
    resultId: number,
    update: RestoreItemRunState,
): RestoreItemState {
    return { runs: { ...state.runs, [resultId]: update } };
}

function removeRun(state: RestoreItemState, resultId: number): RestoreItemState {
    if (!(resultId in state.runs)) {
        return state;
    }
    const next = { ...state.runs };
    delete next[resultId];
    return { runs: next };
}

export function restoreItemReducer(
    state: RestoreItemState,
    action: RestoreItemAction,
): RestoreItemState {
    switch (action.type) {
        case 'REQUEST':
            return setRun(state, action.resultId, {
                resultId: action.resultId,
                scanId: null,
                status: 'starting',
                progress: null,
                error: null,
            });

        case 'ACCEPTED': {
            const run = state.runs[action.resultId];
            if (!run) {
                return state;
            }
            return setRun(state, action.resultId, {
                ...run,
                status: run.status === 'starting' ? 'running' : run.status,
                scanId: action.scanId,
                error: null,
            });
        }

        case 'REJECTED': {
            const run = state.runs[action.resultId];
            if (!run) {
                return state;
            }
            return setRun(state, action.resultId, {
                ...run,
                status: 'error',
                progress: null,
                error: action.error,
            });
        }

        case 'STARTED': {
            const existing = state.runs[action.resultId];
            if (!existing) {
                // Out-of-band start (e.g. another renderer initiated). Create a
                // synthetic entry so terminal notifications can attach.
                return setRun(state, action.resultId, {
                    resultId: action.resultId,
                    scanId: action.scanId,
                    status: 'running',
                    progress: null,
                    error: null,
                });
            }
            if (existing.status !== 'starting' && existing.status !== 'running') {
                return state;
            }
            return setRun(state, action.resultId, {
                ...existing,
                status: 'running',
                scanId: action.scanId,
                error: null,
            });
        }

        case 'PROGRESS': {
            const existing = state.runs[action.resultId];
            if (!existing) {
                return state;
            }
            return setRun(state, action.resultId, {
                ...existing,
                status: 'running',
                scanId: action.scanId,
                progress: action.progress,
            });
        }

        case 'COMPLETED': {
            const existing = state.runs[action.resultId] ?? {
                resultId: action.resultId,
                scanId: action.scanId,
                status: 'running' as RestoreItemStatus,
                progress: null,
                error: null,
            };
            return setRun(state, action.resultId, {
                ...existing,
                status: 'completed',
                scanId: action.scanId,
                progress: null,
                error: null,
            });
        }

        case 'FAILED': {
            const existing = state.runs[action.resultId] ?? {
                resultId: action.resultId,
                scanId: action.scanId,
                status: 'running' as RestoreItemStatus,
                progress: null,
                error: null,
            };
            return setRun(state, action.resultId, {
                ...existing,
                status: 'failed',
                scanId: action.scanId,
                progress: null,
                error: action.error,
            });
        }

        case 'DISMISS':
            return removeRun(state, action.resultId);
    }
}

/**
 * Convenience predicate: {@code true} while a restore for the given result
 * is being requested or is actively running. UI components can disable
 * their restore button while this is {@code true} for the row in question.
 */
export function isRestoreInFlight(state: RestoreItemState, resultId: number): boolean {
    const run = state.runs[resultId];
    if (!run) {
        return false;
    }
    return run.status === 'starting' || run.status === 'running';
}

/**
 * Returns {@code true} if any restore is currently in flight (anywhere in
 * the map).
 */
export function hasAnyRestoreInFlight(state: RestoreItemState): boolean {
    for (const run of Object.values(state.runs)) {
        if (run.status === 'starting' || run.status === 'running') {
            return true;
        }
    }
    return false;
}
