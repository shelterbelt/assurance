/**
 * React hook that owns the renderer-side state machine for one or more
 * concurrent single-result merges.
 *
 * Responsibilities:
 *   - Subscribe (once) to {@code assurance.resultMerge*} notifications and
 *     translate each into a {@link ResultMergeAction}.
 *   - Expose a {@code start(resultId, strategy)} callback that invokes
 *     {@code engine.mergeScanResult} and reduces the result onto state.
 *   - Expose a {@code dismiss(resultId)} callback that clears a single
 *     terminal entry so the row's UI returns to its idle shape.
 *   - Tolerate component unmounts: the unsubscribe is returned from the
 *     subscription effect, and a mounted-flag guards the post-await dispatch.
 *
 * The actual transition logic lives in {@link resultMergeReducer} so it can be
 * unit-tested without React.
 */

import { useCallback, useEffect, useReducer, useRef } from 'react';

import {
    engine,
    EngineCallError,
    type MergeStrategy,
    type ResultMergeNotificationParams,
} from '../api/engine';
import {
    INITIAL_RESULT_MERGE_STATE,
    type ResultMergeAction,
    type ResultMergeState,
    resultMergeReducer,
} from './resultMergeReducer';
import { parseResultScanPair } from './rpcNotificationIds';

export interface UseResultMergeResult {
    state: ResultMergeState;
    /**
     * Triggers a merge for the given comparison result. Resolves once the
     * synchronous {@code mergeScanResult} call has been dispatched (success
     * or failure); progress arrives through notifications.
     */
    start: (resultId: number, strategy: MergeStrategy) => Promise<void>;
    /** Clears a single terminal entry (e.g. when the user dismisses a banner). */
    dismiss: (resultId: number) => void;
}

export interface UseResultMergeOptions {
    /**
     * Optional callback fired when a merge transitions to {@code completed}.
     * Useful for parent panels that need to reload data once the engine has
     * mutated it (e.g. {@code ResultsPanel} re-fetching the comparison
     * results).
     */
    onCompleted?: (resultId: number, scanId: number) => void;
}

export function useResultMerge(options: UseResultMergeOptions = {}): UseResultMergeResult {
    const [state, dispatch] = useReducer(resultMergeReducer, INITIAL_RESULT_MERGE_STATE);
    const mountedRef = useRef(true);
    const stateRef = useRef(state);
    stateRef.current = state;

    const onCompletedRef = useRef(options.onCompleted);
    onCompletedRef.current = options.onCompleted;

    useEffect(() => {
        mountedRef.current = true;
        const unsubscribe = engine.onResultMergeNotification((notification) => {
            if (!mountedRef.current) {
                return;
            }
            const action = notificationToAction(notification);
            if (action) {
                dispatch(action);
                if (action.type === 'COMPLETED' && onCompletedRef.current) {
                    onCompletedRef.current(action.resultId, action.scanId);
                }
            }
        });
        return () => {
            mountedRef.current = false;
            unsubscribe();
        };
    }, []);

    const start = useCallback(
        async (resultId: number, strategy: MergeStrategy): Promise<void> => {
            const current = stateRef.current.runs[resultId];
            if (current && (current.status === 'starting' || current.status === 'running')) {
                return;
            }
            dispatch({ type: 'REQUEST', resultId, strategy });
            try {
                const ack = await engine.mergeScanResult(resultId, strategy);
                if (!mountedRef.current) {
                    return;
                }
                dispatch({ type: 'ACCEPTED', resultId, scanId: ack.scanId });
            } catch (err) {
                if (!mountedRef.current) {
                    return;
                }
                const code = err instanceof EngineCallError ? err.code : -32603;
                const message = err instanceof Error ? err.message : String(err);
                const data = err instanceof EngineCallError ? err.data : undefined;
                dispatch({
                    type: 'REJECTED',
                    resultId,
                    error: { code, message, data },
                });
            }
        },
        [],
    );

    const dismiss = useCallback((resultId: number) => {
        dispatch({ type: 'DISMISS', resultId });
    }, []);

    return { state, start, dismiss };
}

function notificationToAction(
    notification: ResultMergeNotificationParams,
): ResultMergeAction | null {
    const pair = parseResultScanPair(notification.params);
    if (!pair) {
        return null;
    }
    const { resultId, scanId } = pair;
    switch (notification.method) {
        case 'assurance.resultMergeStarted':
            return { type: 'STARTED', resultId, scanId };
        case 'assurance.resultMergeProgress':
            return {
                type: 'PROGRESS',
                resultId,
                scanId,
                progress: notification.params.progress ?? {},
            };
        case 'assurance.resultMergeCompleted':
            return { type: 'COMPLETED', resultId, scanId };
        case 'assurance.resultMergeFailed': {
            const error = notification.params.error ?? {
                code: -32001,
                message: 'Result merge failed',
            };
            return {
                type: 'FAILED',
                resultId,
                scanId,
                error: { code: error.code, message: error.message },
            };
        }
        default:
            return null;
    }
}
