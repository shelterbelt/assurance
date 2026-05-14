/**
 * React hook that owns the renderer-side state machine for a single scan run.
 *
 * Responsibilities:
 *   - Subscribe (once) to {@code assurance.scan*} notifications and translate
 *     each into a {@link ScanRunAction}.
 *   - Expose a {@code start(scanDefinitionId, merge, scanDefinitionDisplayName?)}
 *     callback that invokes {@code engine.performScan} and reduces the result
 *     onto state (optional display name is for feedback UI copy).
 *   - Tolerate component unmounts: the unsubscribe is returned from the
 *     subscription effect, and a mounted-flag guards the post-await dispatch.
 *
 * The actual transition logic lives in {@link scanRunReducer} so it can be
 * unit-tested without React.
 */

import { useCallback, useEffect, useReducer, useRef } from 'react';

import { engine, EngineCallError, type ScanNotificationParams } from '../api/engine';
import {
    INITIAL_SCAN_RUN_STATE,
    type ScanRunAction,
    type ScanRunState,
    scanRunReducer,
} from './scanRunReducer';

export interface UseScanRunResult {
    state: ScanRunState;
    /**
     * Triggers a new scan run. Resolves once the synchronous {@code performScan}
     * call has been dispatched (success or failure); progress arrives through
     * notifications. Will refuse to start a second run while one is already
     * in flight (callers should also disable their UI based on
     * {@link isScanInFlight}).
     */
    start: (scanDefinitionId: number, merge?: boolean, scanDefinitionDisplayName?: string | null) => Promise<void>;
    /** Resets the lifecycle back to {@code idle}. */
    reset: () => void;
}

export function useScanRun(): UseScanRunResult {
    const [state, dispatch] = useReducer(scanRunReducer, INITIAL_SCAN_RUN_STATE);
    const mountedRef = useRef(true);
    // Mirror of the latest reducer state so async callbacks can read it
    // without participating in React closures (which would otherwise capture
    // a stale `state` and cause spurious re-runs).
    const stateRef = useRef(state);
    stateRef.current = state;

    useEffect(() => {
        mountedRef.current = true;
        const unsubscribe = engine.onScanNotification((notification) => {
            if (!mountedRef.current) {
                return;
            }
            const action = notificationToAction(notification);
            if (action) {
                dispatch(action);
            }
        });
        return () => {
            mountedRef.current = false;
            unsubscribe();
        };
    }, []);

    const start = useCallback(async (
        scanDefinitionId: number,
        merge = false,
        scanDefinitionDisplayName?: string | null,
    ): Promise<void> => {
        const current = stateRef.current;
        if (current.status === 'starting' || current.status === 'running') {
            // Defensive guard; the UI should already be disabled.
            return;
        }
        dispatch({
            type: 'REQUEST',
            scanDefinitionId,
            scanDefinitionName: scanDefinitionDisplayName ?? null,
        });
        try {
            const scanId = await engine.performScan(scanDefinitionId, merge);
            if (!mountedRef.current) {
                return;
            }
            dispatch({ type: 'ACCEPTED', scanDefinitionId, scanId });
        } catch (err) {
            if (!mountedRef.current) {
                return;
            }
            const code = err instanceof EngineCallError ? err.code : -32603;
            const message = err instanceof Error ? err.message : String(err);
            const data = err instanceof EngineCallError ? err.data : undefined;
            dispatch({
                type: 'REJECTED',
                scanDefinitionId,
                error: { code, message, data },
            });
        }
    }, []);

    const reset = useCallback(() => {
        dispatch({ type: 'RESET' });
    }, []);

    return { state, start, reset };
}

/**
 * Translates a typed engine notification envelope into a reducer action.
 * Returns {@code null} for envelopes whose payload doesn't match the
 * documented contract — keeps the reducer simple by rejecting malformed
 * input at the boundary.
 */
function notificationToAction(notification: ScanNotificationParams): ScanRunAction | null {
    const { scanId } = notification.params;
    if (typeof scanId !== 'number' || !Number.isFinite(scanId)) {
        return null;
    }
    switch (notification.method) {
        case 'assurance.scanStarted':
            return { type: 'STARTED', scanId };
        case 'assurance.scanProgress':
            return {
                type: 'PROGRESS',
                scanId,
                progress: notification.params.progress ?? {},
            };
        case 'assurance.scanCompleted': {
            const resultCount = notification.params.resultCount;
            return {
                type: 'COMPLETED',
                scanId,
                resultCount: typeof resultCount === 'number' ? resultCount : 0,
            };
        }
        case 'assurance.scanFailed': {
            const error = notification.params.error ?? { code: -32001, message: 'Scan failed' };
            return {
                type: 'FAILED',
                scanId,
                error: { code: error.code, message: error.message },
            };
        }
        default:
            return null;
    }
}
