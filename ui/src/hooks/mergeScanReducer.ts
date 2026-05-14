/**
 * Pure-function reducer that models the lifecycle of one or more in-flight
 * whole-scan merges as observed by the renderer.
 *
 * The renderer never inspects the underlying WebSocket frames directly; it
 * subscribes to the {@code assurance.merge*} notification family via
 * {@link engine.onMergeNotification}, normalizes each notification into a
 * {@link MergeScanAction}, and reduces it onto the previous state.
 *
 * Whole-scan merges are keyed by {@code scanId} (one logical merge per scan
 * at a time, enforced server-side via {@code -32006}). The state machine
 * mirrors the per-result merge lifecycle: {@code starting} →
 * {@code running} → {@code completed} / {@code failed} / {@code error}.
 *
 * Lifecycle transitions intentionally tolerate out-of-order delivery:
 *   - A {@code REQUEST} initiated by the user moves the entry into
 *     {@code starting}.
 *   - An {@code ACCEPTED} action confirms the synchronous {@code mergeScan}
 *     call succeeded.
 *   - A {@code STARTED} arriving without a prior request is still honored
 *     (e.g. when a merge was started by a sibling renderer); the reducer
 *     creates a new entry in the {@code running} state.
 *   - {@code REJECTED} surfaces a synchronous failure of
 *     {@code assurance.mergeScan} (e.g. {@code -32006} already running) and
 *     parks the entry in {@code error} until dismissed.
 *   - {@code COMPLETED} and {@code FAILED} are accepted unconditionally for
 *     the named {@code scanId}; if the entry doesn't exist yet (notification
 *     arrived before {@code REQUEST}), an entry is created so terminal
 *     feedback is never lost.
 */

import type { MergeProgressPayload } from '../api/engine';

export type MergeScanStatus =
    /** The user initiated a merge and we are awaiting the synchronous response. */
    | 'starting'
    /**
     * The engine acknowledged the merge (either via a synchronous
     * {@code mergeScan} response or an {@code assurance.mergeStarted}
     * notification). The merge is in flight.
     */
    | 'running'
    /** Terminal state for a merge that completed successfully. */
    | 'completed'
    /** Terminal state for a merge that failed mid-flight. */
    | 'failed'
    /**
     * Terminal state for a synchronous rejection of the {@code mergeScan}
     * call. No engine-side merge is underway.
     */
    | 'error';

export interface MergeScanError {
    /** JSON-RPC error code (typically negative). */
    code: number;
    message: string;
    /** Optional structured payload mirroring the JSON-RPC {@code data} field. */
    data?: unknown;
}

export interface MergeScanRunState {
    scanId: number;
    status: MergeScanStatus;
    /** Most recent {@code assurance.mergeProgress} payload, if any. */
    progress: MergeProgressPayload | null;
    /**
     * Number of items merged after completion. Populated only in the
     * {@code completed} state.
     */
    itemsMerged: number | null;
    /** Populated only in the {@code failed} or {@code error} state. */
    error: MergeScanError | null;
}

/**
 * Map keyed by {@code scanId}. Stored as a plain object (JSON-serializable,
 * hashable by reference) so it works cleanly with React's strict-mode
 * double-render and hot-reload.
 */
export interface MergeScanState {
    runs: Record<number, MergeScanRunState>;
}

export const INITIAL_MERGE_SCAN_STATE: MergeScanState = {
    runs: {},
};

export type MergeScanAction =
    | { type: 'REQUEST'; scanId: number }
    | { type: 'ACCEPTED'; scanId: number }
    | { type: 'REJECTED'; scanId: number; error: MergeScanError }
    | { type: 'STARTED'; scanId: number }
    | { type: 'PROGRESS'; scanId: number; progress: MergeProgressPayload }
    | { type: 'COMPLETED'; scanId: number; itemsMerged: number }
    | { type: 'FAILED'; scanId: number; error: MergeScanError }
    /** UI-driven dismissal of a single terminal entry. */
    | { type: 'DISMISS'; scanId: number };

function setRun(
    state: MergeScanState,
    scanId: number,
    update: MergeScanRunState,
): MergeScanState {
    return { runs: { ...state.runs, [scanId]: update } };
}

function removeRun(state: MergeScanState, scanId: number): MergeScanState {
    if (!(scanId in state.runs)) {
        return state;
    }
    const next = { ...state.runs };
    delete next[scanId];
    return { runs: next };
}

export function mergeScanReducer(
    state: MergeScanState,
    action: MergeScanAction,
): MergeScanState {
    switch (action.type) {
        case 'REQUEST':
            return setRun(state, action.scanId, {
                scanId: action.scanId,
                status: 'starting',
                progress: null,
                itemsMerged: null,
                error: null,
            });

        case 'ACCEPTED': {
            const run = state.runs[action.scanId];
            if (!run) {
                return state;
            }
            return setRun(state, action.scanId, {
                ...run,
                status: run.status === 'starting' ? 'running' : run.status,
                error: null,
            });
        }

        case 'REJECTED': {
            const run = state.runs[action.scanId];
            if (!run) {
                return state;
            }
            return setRun(state, action.scanId, {
                ...run,
                status: 'error',
                progress: null,
                error: action.error,
            });
        }

        case 'STARTED': {
            const existing = state.runs[action.scanId];
            if (!existing) {
                // Out-of-band start (e.g. another renderer initiated). Create a
                // synthetic entry so terminal notifications can attach.
                return setRun(state, action.scanId, {
                    scanId: action.scanId,
                    status: 'running',
                    progress: null,
                    itemsMerged: null,
                    error: null,
                });
            }
            if (existing.status !== 'starting' && existing.status !== 'running') {
                return state;
            }
            return setRun(state, action.scanId, {
                ...existing,
                status: 'running',
                error: null,
            });
        }

        case 'PROGRESS': {
            const existing = state.runs[action.scanId];
            if (!existing) {
                return state;
            }
            return setRun(state, action.scanId, {
                ...existing,
                status: 'running',
                progress: action.progress,
            });
        }

        case 'COMPLETED': {
            const existing = state.runs[action.scanId] ?? {
                scanId: action.scanId,
                status: 'running' as MergeScanStatus,
                progress: null,
                itemsMerged: null,
                error: null,
            };
            return setRun(state, action.scanId, {
                ...existing,
                status: 'completed',
                progress: null,
                itemsMerged: action.itemsMerged,
                error: null,
            });
        }

        case 'FAILED': {
            const existing = state.runs[action.scanId] ?? {
                scanId: action.scanId,
                status: 'running' as MergeScanStatus,
                progress: null,
                itemsMerged: null,
                error: null,
            };
            return setRun(state, action.scanId, {
                ...existing,
                status: 'failed',
                progress: null,
                error: action.error,
            });
        }

        case 'DISMISS':
            return removeRun(state, action.scanId);
    }
}

/**
 * Convenience predicate: {@code true} while a whole-scan merge for the given
 * scan is being requested or is actively running. UI components can disable
 * their "Merge" button while this is {@code true}.
 */
export function isMergeScanInFlight(state: MergeScanState, scanId: number): boolean {
    const run = state.runs[scanId];
    if (!run) {
        return false;
    }
    return run.status === 'starting' || run.status === 'running';
}

/**
 * Returns {@code true} if any whole-scan merge is currently in flight
 * (anywhere in the map).
 */
export function hasAnyMergeScanInFlight(state: MergeScanState): boolean {
    for (const run of Object.values(state.runs)) {
        if (run.status === 'starting' || run.status === 'running') {
            return true;
        }
    }
    return false;
}
