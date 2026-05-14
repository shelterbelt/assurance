/**
 * React hook that owns the renderer-side state machine for one or more
 * concurrent whole-scan merges.
 *
 * Responsibilities:
 *   - Subscribe (once) to {@code assurance.merge*} notifications and translate
 *     each into a {@link MergeScanAction}.
 *   - Expose a {@code start(scanId)} callback that invokes
 *     {@code engine.mergeScan} and reduces the result onto state.
 *   - Expose a {@code dismiss(scanId)} callback that clears a single terminal
 *     entry so the UI returns to its idle shape for that scan.
 *   - Tolerate component unmounts: the unsubscribe is returned from the
 *     subscription effect, and a mounted-flag guards the post-await dispatch.
 *
 * The actual transition logic lives in {@link mergeScanReducer} so it can be
 * unit-tested without React.
 */

import { useCallback, useEffect, useReducer, useRef } from 'react';

import { engine, EngineCallError, type MergeNotificationParams } from '../api/engine';
import {
    INITIAL_MERGE_SCAN_STATE,
    type MergeScanAction,
    type MergeScanState,
    mergeScanReducer,
} from './mergeScanReducer';

export interface UseMergeScanResult {
    state: MergeScanState;
    /**
     * Triggers a whole-scan merge for the given scan. Resolves once the
     * synchronous {@code mergeScan} call has been dispatched (success or
     * failure); progress arrives through notifications.
     */
    start: (scanId: number) => Promise<void>;
    /** Clears a single terminal entry (e.g. when the user dismisses a banner). */
    dismiss: (scanId: number) => void;
}

export interface UseMergeScanOptions {
    /**
     * Optional callback fired when a merge transitions to {@code completed}.
     * Useful for parent panels that need to reload data once the engine has
     * mutated it (e.g. {@code ResultsPanel} re-fetching the comparison
     * results for the merged scan).
     */
    onCompleted?: (scanId: number, itemsMerged: number) => void;
}

export function useMergeScan(options: UseMergeScanOptions = {}): UseMergeScanResult {
    const [state, dispatch] = useReducer(mergeScanReducer, INITIAL_MERGE_SCAN_STATE);
    const mountedRef = useRef(true);
    const stateRef = useRef(state);
    stateRef.current = state;

    const onCompletedRef = useRef(options.onCompleted);
    onCompletedRef.current = options.onCompleted;

    useEffect(() => {
        mountedRef.current = true;
        const unsubscribe = engine.onMergeNotification((notification) => {
            if (!mountedRef.current) {
                return;
            }
            const action = notificationToAction(notification);
            if (action) {
                dispatch(action);
                if (action.type === 'COMPLETED' && onCompletedRef.current) {
                    onCompletedRef.current(action.scanId, action.itemsMerged);
                }
            }
        });
        return () => {
            mountedRef.current = false;
            unsubscribe();
        };
    }, []);

    const start = useCallback(async (scanId: number): Promise<void> => {
        const current = stateRef.current.runs[scanId];
        if (current && (current.status === 'starting' || current.status === 'running')) {
            return;
        }
        dispatch({ type: 'REQUEST', scanId });
        try {
            await engine.mergeScan(scanId);
            if (!mountedRef.current) {
                return;
            }
            dispatch({ type: 'ACCEPTED', scanId });
        } catch (err) {
            if (!mountedRef.current) {
                return;
            }
            const code = err instanceof EngineCallError ? err.code : -32603;
            const message = err instanceof Error ? err.message : String(err);
            const data = err instanceof EngineCallError ? err.data : undefined;
            dispatch({
                type: 'REJECTED',
                scanId,
                error: { code, message, data },
            });
        }
    }, []);

    const dismiss = useCallback((scanId: number) => {
        dispatch({ type: 'DISMISS', scanId });
    }, []);

    return { state, start, dismiss };
}

function notificationToAction(notification: MergeNotificationParams): MergeScanAction | null {
    const { scanId } = notification.params;
    if (typeof scanId !== 'number' || !Number.isFinite(scanId)) {
        return null;
    }
    switch (notification.method) {
        case 'assurance.mergeStarted':
            return { type: 'STARTED', scanId };
        case 'assurance.mergeProgress':
            return {
                type: 'PROGRESS',
                scanId,
                progress: notification.params.progress ?? {},
            };
        case 'assurance.mergeCompleted': {
            const itemsMerged = notification.params.itemsMerged;
            return {
                type: 'COMPLETED',
                scanId,
                itemsMerged: typeof itemsMerged === 'number' ? itemsMerged : 0,
            };
        }
        case 'assurance.mergeFailed': {
            const error = notification.params.error ?? {
                code: -32001,
                message: 'Whole-scan merge failed',
            };
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
