/**
 * Pure-function reducer that models the lifecycle of one or more in-flight
 * single-result merges as observed by the renderer.
 *
 * The renderer never inspects the underlying WebSocket frames directly; it
 * subscribes to the {@code assurance.resultMerge*} notification family via
 * {@link engine.onResultMergeNotification}, normalizes each notification into
 * a {@link ResultMergeAction}, and reduces it onto the previous state.
 *
 * Unlike {@link scanRunReducer}, the engine permits concurrent merges across
 * distinct {@code resultId}s, so this reducer keeps a map keyed by
 * {@code resultId}. The state machine for any single entry mirrors the scan
 * lifecycle: {@code starting} → {@code running} → {@code completed} /
 * {@code failed} / {@code error}.
 *
 * Lifecycle transitions intentionally tolerate out-of-order delivery:
 *   - A {@code REQUEST} initiated by the user moves the entry into
 *     {@code starting}.
 *   - An {@code ACCEPTED} action confirms the synchronous
 *     {@code mergeScanResult} call succeeded.
 *   - A {@code STARTED} arriving without a prior request is still honored
 *     (e.g. when a merge was started by a sibling renderer); the reducer
 *     creates a new entry in the {@code running} state.
 *   - {@code REJECTED} surfaces a synchronous failure of
 *     {@code assurance.mergeScanResult} (e.g. {@code -32007} already
 *     running) and parks the entry in {@code error} until dismissed.
 *   - {@code COMPLETED} and {@code FAILED} are accepted unconditionally for
 *     the named {@code resultId}; if the entry doesn't exist yet (notification
 *     arrived before {@code REQUEST}), an entry is created so terminal
 *     feedback is never lost.
 */

import type { MergeStrategy, ResultMergeProgressPayload } from '../api/engine';

export type ResultMergeStatus =
    /** The user initiated a merge and we are awaiting the synchronous response. */
    | 'starting'
    /**
     * The engine acknowledged the merge (either via a synchronous
     * {@code mergeScanResult} response or an
     * {@code assurance.resultMergeStarted} notification). The merge is
     * in flight.
     */
    | 'running'
    /** Terminal state for a merge that completed successfully. */
    | 'completed'
    /** Terminal state for a merge that failed mid-flight. */
    | 'failed'
    /**
     * Terminal state for a synchronous rejection of the
     * {@code mergeScanResult} call. No engine-side merge is underway.
     */
    | 'error';

export interface ResultMergeError {
    /** JSON-RPC error code (typically negative). */
    code: number;
    message: string;
    /** Optional structured payload mirroring the JSON-RPC {@code data} field. */
    data?: unknown;
}

export interface ResultMergeRunState {
    resultId: number;
    /** Parent scan id. {@code null} until the engine confirms the merge. */
    scanId: number | null;
    /** Merge strategy the user requested. */
    strategy: MergeStrategy;
    status: ResultMergeStatus;
    /** Most recent {@code assurance.resultMergeProgress} payload, if any. */
    progress: ResultMergeProgressPayload | null;
    /** Populated only in the {@code failed} or {@code error} state. */
    error: ResultMergeError | null;
}

/**
 * Map keyed by {@code resultId}. Stored as a plain object (JSON-serializable,
 * hashable by reference) so it works cleanly with React's strict-mode
 * double-render and hot-reload.
 */
export interface ResultMergeState {
    runs: Record<number, ResultMergeRunState>;
}

export const INITIAL_RESULT_MERGE_STATE: ResultMergeState = {
    runs: {},
};

export type ResultMergeAction =
    | { type: 'REQUEST'; resultId: number; strategy: MergeStrategy }
    | { type: 'ACCEPTED'; resultId: number; scanId: number }
    | { type: 'REJECTED'; resultId: number; error: ResultMergeError }
    | { type: 'STARTED'; resultId: number; scanId: number }
    | {
          type: 'PROGRESS';
          resultId: number;
          scanId: number;
          progress: ResultMergeProgressPayload;
      }
    | { type: 'COMPLETED'; resultId: number; scanId: number }
    | { type: 'FAILED'; resultId: number; scanId: number; error: ResultMergeError }
    /** UI-driven dismissal of a single terminal entry. */
    | { type: 'DISMISS'; resultId: number };

function setRun(
    state: ResultMergeState,
    resultId: number,
    update: ResultMergeRunState,
): ResultMergeState {
    return { runs: { ...state.runs, [resultId]: update } };
}

function removeRun(state: ResultMergeState, resultId: number): ResultMergeState {
    if (!(resultId in state.runs)) {
        return state;
    }
    const next = { ...state.runs };
    delete next[resultId];
    return { runs: next };
}

export function resultMergeReducer(
    state: ResultMergeState,
    action: ResultMergeAction,
): ResultMergeState {
    switch (action.type) {
        case 'REQUEST':
            return setRun(state, action.resultId, {
                resultId: action.resultId,
                scanId: null,
                strategy: action.strategy,
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
                    strategy: 'SOURCE',
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
                strategy: 'SOURCE' as MergeStrategy,
                status: 'running' as ResultMergeStatus,
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
                strategy: 'SOURCE' as MergeStrategy,
                status: 'running' as ResultMergeStatus,
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
 * Convenience predicate: {@code true} while a merge for the given result is
 * being requested or is actively running. UI components can disable their
 * merge buttons while this is {@code true} for the row in question.
 */
export function isResultMergeInFlight(
    state: ResultMergeState,
    resultId: number,
): boolean {
    const run = state.runs[resultId];
    if (!run) {
        return false;
    }
    return run.status === 'starting' || run.status === 'running';
}

/**
 * Returns {@code true} if any merge is currently in flight (anywhere in the
 * map). Useful for the parent panel to disable a "Merge all" affordance, or
 * to know that progress feedback should remain visible.
 */
export function hasAnyResultMergeInFlight(state: ResultMergeState): boolean {
    for (const run of Object.values(state.runs)) {
        if (run.status === 'starting' || run.status === 'running') {
            return true;
        }
    }
    return false;
}
