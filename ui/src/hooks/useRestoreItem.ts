/**
 * React hook that owns the renderer-side state machine for one or more
 * concurrent single-result restores.
 *
 * Responsibilities:
 *   - Subscribe (once) to {@code assurance.restore*} notifications and
 *     translate each into a {@link RestoreItemAction}.
 *   - Expose a {@code start(resultId)} callback that invokes
 *     {@code engine.restoreDeletedItem} and reduces the result onto state.
 *   - Expose a {@code dismiss(resultId)} callback that clears a single
 *     terminal entry so the row's UI returns to its idle shape.
 *   - Tolerate component unmounts: the unsubscribe is returned from the
 *     subscription effect, and a mounted-flag guards the post-await dispatch.
 *
 * The actual transition logic lives in {@link restoreItemReducer} so it can
 * be unit-tested without React.
 */

import { useCallback, useEffect, useReducer, useRef } from 'react';

import { engine, EngineCallError, type RestoreNotificationParams } from '../api/engine';
import {
    INITIAL_RESTORE_ITEM_STATE,
    type RestoreItemAction,
    type RestoreItemState,
    restoreItemReducer,
} from './restoreItemReducer';
import { parseResultScanPair } from './rpcNotificationIds';

export interface UseRestoreItemResult {
    state: RestoreItemState;
    /**
     * Triggers a restore for the given comparison result. Resolves once the
     * synchronous {@code restoreDeletedItem} call has been dispatched
     * (success or failure); progress arrives through notifications.
     */
    start: (resultId: number) => Promise<void>;
    /** Clears a single terminal entry (e.g. when the user dismisses a banner). */
    dismiss: (resultId: number) => void;
}

export interface UseRestoreItemOptions {
    /**
     * Optional callback fired when a restore transitions to {@code completed}.
     * Useful for parent panels that need to reload data once the engine has
     * mutated it (e.g. {@code ResultsPanel} re-fetching the comparison
     * results so the freshly-restored row reflects the post-restore
     * filesystem state).
     */
    onCompleted?: (resultId: number, scanId: number) => void;
}

export function useRestoreItem(options: UseRestoreItemOptions = {}): UseRestoreItemResult {
    const [state, dispatch] = useReducer(restoreItemReducer, INITIAL_RESTORE_ITEM_STATE);
    const mountedRef = useRef(true);
    const stateRef = useRef(state);
    stateRef.current = state;

    const onCompletedRef = useRef(options.onCompleted);
    onCompletedRef.current = options.onCompleted;

    useEffect(() => {
        mountedRef.current = true;
        const unsubscribe = engine.onRestoreNotification((notification) => {
            if (!mountedRef.current) {
                return;
            }
            const action = notificationToAction(notification);
            if (action) {
                dispatch(action);
                if (action.type === 'COMPLETED' && onCompletedRef.current) {
                    onCompletedRef.current(action.resultId, action.scanId);
                }
                if (action.type === 'COMPLETED') {
                    // Restore completion should reset row controls back to idle
                    // without requiring an extra manual "Dismiss" click.
                    // We keep failed/error entries sticky for explicit user
                    // acknowledgement, but successful restores auto-clear.
                    // Defer DISMISS to a microtask so one commit can observe
                    // {@code completed} (parents may reload from that state).
                    const rid = action.resultId;
                    queueMicrotask(() => {
                        if (!mountedRef.current) {
                            return;
                        }
                        dispatch({ type: 'DISMISS', resultId: rid });
                    });
                }
            }
        });
        return () => {
            mountedRef.current = false;
            unsubscribe();
        };
    }, []);

    const start = useCallback(async (resultId: number): Promise<void> => {
        const current = stateRef.current.runs[resultId];
        if (current && (current.status === 'starting' || current.status === 'running')) {
            return;
        }
        dispatch({ type: 'REQUEST', resultId });
        try {
            const ack = await engine.restoreDeletedItem(resultId);
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
    }, []);

    const dismiss = useCallback((resultId: number) => {
        dispatch({ type: 'DISMISS', resultId });
    }, []);

    return { state, start, dismiss };
}

function notificationToAction(notification: RestoreNotificationParams): RestoreItemAction | null {
    const pair = parseResultScanPair(notification.params);
    if (!pair) {
        return null;
    }
    const { resultId, scanId } = pair;
    switch (notification.method) {
        case 'assurance.restoreStarted':
            return { type: 'STARTED', resultId, scanId };
        case 'assurance.restoreProgress':
            return {
                type: 'PROGRESS',
                resultId,
                scanId,
                progress: notification.params.progress ?? {},
            };
        case 'assurance.restoreCompleted':
            return { type: 'COMPLETED', resultId, scanId };
        case 'assurance.restoreFailed': {
            const error = notification.params.error ?? {
                code: -32001,
                message: 'Restore failed',
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
