/**
 * Pure-function reducer that models the lifecycle of a single scan run as
 * observed by the renderer.
 *
 * The renderer never inspects the underlying WebSocket frames directly; it
 * subscribes to the {@code assurance.scan*} notification family via
 * {@link engine.onScanNotification}, normalizes each notification into a
 * {@link ScanRunAction}, and reduces it onto the previous state. Keeping the
 * reducer pure isolates the state machine from React/Electron globals and
 * makes the lifecycle directly unit-testable.
 *
 * Lifecycle transitions intentionally tolerate out-of-order delivery:
 *   - A {@code REQUEST} initiated by the user moves us into {@code starting}.
 *   - A {@code STARTED} notification confirms the engine accepted the run
 *     and pins the {@code scanId}.
 *   - A {@code STARTED} arriving without a prior request is still honored
 *     (e.g. when a scan was started by a sibling renderer); the reducer
 *     adopts the engine's {@code scanId} and transitions to {@code running}.
 *   - A {@code REJECTED} action surfaces a synchronous failure of the
 *     {@code assurance.performScan} call (e.g. {@code -32003} already
 *     running) and parks the state in {@code error} until the next request.
 *   - {@code COMPLETED} and {@code FAILED} are accepted only when they
 *     reference the active {@code scanId}; stale notifications from a prior
 *     run are dropped.
 */

import type { ScanProgressPayload } from '../api/engine';

export type ScanRunStatus =
    /** No scan is running and no recent terminal state is being shown. */
    | 'idle'
    /**
     * The user initiated a scan and we are awaiting the response from
     * {@code assurance.performScan}.
     */
    | 'starting'
    /**
     * The engine acknowledged the run (either via a synchronous
     * {@code performScan} response or an {@code assurance.scanStarted}
     * notification). The scan is in flight.
     */
    | 'running'
    /** Terminal state for a scan that completed successfully. */
    | 'completed'
    /** Terminal state for a scan that failed mid-flight. */
    | 'failed'
    /**
     * Terminal state for a synchronous rejection of the {@code performScan}
     * call (e.g. validation, "already running"). No {@code scanId} is
     * available because the engine never accepted the request.
     */
    | 'error';

export interface ScanRunError {
    /** JSON-RPC error code (typically negative). */
    code: number;
    message: string;
    /** Optional structured payload mirroring the JSON-RPC {@code data} field. */
    data?: unknown;
}

export interface ScanRunState {
    status: ScanRunStatus;
    /**
     * Identifier of the scan-definition the most recent run targets. Used to
     * scope notifications (we keep state for the most recent run only).
     */
    scanDefinitionId: number | null;
    /**
     * Display name of the scan definition for UI copy (e.g. feedback bar).
     * Set when the user starts a scan from the list; {@code null} if unknown.
     */
    scanDefinitionName: string | null;
    /**
     * Engine-assigned id of the active scan. {@code null} until the engine
     * accepts the request (either via synchronous response or a
     * {@code scanStarted} notification).
     */
    scanId: number | null;
    /** Most recent {@code assurance.scanProgress} payload, if any. */
    progress: ScanProgressPayload | null;
    /**
     * Number of comparison results attached to the scan after completion.
     * Populated only in the {@code completed} state.
     */
    resultCount: number | null;
    /** Populated only in the {@code failed} or {@code error} state. */
    error: ScanRunError | null;
}

export const INITIAL_SCAN_RUN_STATE: ScanRunState = {
    status: 'idle',
    scanDefinitionId: null,
    scanDefinitionName: null,
    scanId: null,
    progress: null,
    resultCount: null,
    error: null,
};

export type ScanRunAction =
    /**
     * The user clicked a Scan / Scan and Merge button for the given
     * definition. Flushes any prior terminal state and parks in
     * {@code starting}.
     */
    | { type: 'REQUEST'; scanDefinitionId: number; scanDefinitionName?: string | null }
    /**
     * Synchronous {@code performScan} success. Pins the engine-assigned
     * {@code scanId} for subsequent notification correlation.
     */
    | { type: 'ACCEPTED'; scanDefinitionId: number; scanId: number }
    /**
     * Synchronous {@code performScan} rejection. Final state until the user
     * dismisses or retries.
     */
    | { type: 'REJECTED'; scanDefinitionId: number; error: ScanRunError }
    /**
     * Server-initiated {@code assurance.scanStarted} notification. Treated
     * as authoritative for the active {@code scanId} when one was not yet
     * known.
     */
    | { type: 'STARTED'; scanId: number }
    /** Server-initiated {@code assurance.scanProgress} notification. */
    | { type: 'PROGRESS'; scanId: number; progress: ScanProgressPayload }
    /** Server-initiated {@code assurance.scanCompleted} notification. */
    | { type: 'COMPLETED'; scanId: number; resultCount: number }
    /** Server-initiated {@code assurance.scanFailed} notification. */
    | { type: 'FAILED'; scanId: number; error: ScanRunError }
    /** UI-driven reset (e.g. "Dismiss" button on the feedback panel). */
    | { type: 'RESET' };

export function scanRunReducer(state: ScanRunState, action: ScanRunAction): ScanRunState {
    switch (action.type) {
        case 'REQUEST':
            return {
                status: 'starting',
                scanDefinitionId: action.scanDefinitionId,
                scanDefinitionName:
                    typeof action.scanDefinitionName === 'string' && action.scanDefinitionName.trim() !== ''
                        ? action.scanDefinitionName.trim()
                        : null,
                scanId: null,
                progress: null,
                resultCount: null,
                error: null,
            };

        case 'ACCEPTED':
            if (state.scanDefinitionId !== action.scanDefinitionId) {
                return state;
            }
            return {
                ...state,
                status: state.status === 'starting' ? 'running' : state.status,
                scanId: action.scanId,
                error: null,
            };

        case 'REJECTED':
            if (state.scanDefinitionId !== action.scanDefinitionId) {
                return state;
            }
            return {
                ...state,
                status: 'error',
                scanId: null,
                progress: null,
                error: action.error,
            };

        case 'STARTED': {
            if (state.scanId !== null && state.scanId !== action.scanId) {
                // A notification for a prior run; drop it.
                return state;
            }
            if (state.status !== 'starting' && state.status !== 'running') {
                return state;
            }
            return {
                ...state,
                status: 'running',
                scanId: action.scanId,
                error: null,
            };
        }

        case 'PROGRESS': {
            if (state.scanId !== null && state.scanId !== action.scanId) {
                return state;
            }
            return {
                ...state,
                status: 'running',
                scanId: action.scanId,
                progress: action.progress,
            };
        }

        case 'COMPLETED': {
            if (state.scanId !== null && state.scanId !== action.scanId) {
                return state;
            }
            return {
                ...state,
                status: 'completed',
                scanId: action.scanId,
                resultCount: action.resultCount,
                progress: null,
                error: null,
            };
        }

        case 'FAILED': {
            if (state.scanId !== null && state.scanId !== action.scanId) {
                return state;
            }
            return {
                ...state,
                status: 'failed',
                scanId: action.scanId,
                progress: null,
                error: action.error,
            };
        }

        case 'RESET':
            return INITIAL_SCAN_RUN_STATE;
    }
}

/**
 * Convenience predicate: {@code true} while a scan is being requested or is
 * actively running, {@code false} in idle/terminal states. Action panels and
 * other "Scan" buttons can disable themselves while this is {@code true}.
 */
export function isScanInFlight(state: ScanRunState): boolean {
    return state.status === 'starting' || state.status === 'running';
}
