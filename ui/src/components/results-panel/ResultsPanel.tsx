import { useVirtualizer } from '@tanstack/react-virtual';
import { ListGroup } from 'react-bootstrap';
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';

import ResultItemRenderer from '../result-item-renderer/ResultItemRenderer';
import { engine, EngineCallError, type MergeStrategy } from '../../api/engine';
import type ComparisonResult from '../../model/ComparisonResult';
import { isScanInFlight, type ScanRunState, INITIAL_SCAN_RUN_STATE } from '../../hooks/scanRunReducer';
import { useResultMerge } from '../../hooks/useResultMerge';
import { useRestoreItem } from '../../hooks/useRestoreItem';

type LoadState = 'idle' | 'loading' | 'ready' | 'error';

interface ResultsPanelProps {
    /**
     * Scan id to display results for. {@code null} or {@code 0} means "no
     * scan selected": the panel renders an empty/onboarding placeholder.
     * Owned by {@code MainWindow}; usually wired to the most recent
     * scan-run lifecycle ({@code scanRunState.scanId} after completion).
     */
    scanId: number | null;
    /**
     * Lifted scan-run state from {@code MainWindow}. The panel watches the
     * tuple (status, scanId) so it can refresh as soon as a fresh run
     * finishes.
     */
    scanRunState?: ScanRunState;
    /**
     * Bumped by the parent whenever an out-of-band engine mutation (e.g. a
     * whole-scan merge fired from {@code ScanPanel}) has potentially changed
     * the comparison results for the displayed scan. The panel reloads on
     * every change.
     */
    reloadVersion?: number;
    /**
     * Lifted selection used by Electron menu actions (e.g. Replace Source /
     * Replace Target for the currently-selected comparison row).
     */
    selectedResultId?: number | null;
    onResultSelected?: (resultId: number | null) => void;
    /**
     * Human-readable label for the scan whose results are shown (typically the
     * scan definition display name). When {@code null}, status copy omits the
     * numeric scan id.
     */
    scanDisplayName?: string | null;
}

export default function ResultsPanel({
    scanId,
    scanRunState = INITIAL_SCAN_RUN_STATE,
    reloadVersion = 0,
    selectedResultId = null,
    onResultSelected,
    scanDisplayName = null,
}: ResultsPanelProps) {
    const [results, setResults] = useState<ComparisonResult[]>([]);
    const [loadState, setLoadState] = useState<LoadState>('idle');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const scanInFlight = isScanInFlight(scanRunState);
    const loadStateRef = useRef<LoadState>(loadState);
    loadStateRef.current = loadState;

    const reload = useCallback(async (id: number | null) => {
        if (!id || id <= 0) {
            setResults([]);
            setLoadState('idle');
            setErrorMessage(null);
            return;
        }
        setLoadState('loading');
        setErrorMessage(null);
        try {
            const next = await engine.loadScanResults(id);
            setResults(next);
            setLoadState('ready');
        } catch (err) {
            if (err instanceof EngineCallError) {
                setErrorMessage(`Engine error ${err.code}: ${err.message}`);
            } else {
                setErrorMessage(err instanceof Error ? err.message : String(err));
            }
            setResults([]);
            setLoadState('error');
        }
    }, []);

    /**
     * Re-fetches one comparison row after merge/restore so the list stays on
     * {@code ready} without the full-panel loading flash from {@link reload}.
     */
    const refreshComparisonResult = useCallback(
        async (resultId: number) => {
            if (scanId === null || scanId <= 0) {
                return;
            }
            if (loadStateRef.current !== 'ready') {
                void reload(scanId);
                return;
            }
            try {
                const row = await engine.loadComparisonResult(resultId);
                setResults((prev) => {
                    if (!prev.some((r) => r.id === resultId)) {
                        return prev;
                    }
                    return prev.map((r) => (r.id === resultId ? row : r));
                });
            } catch {
                void reload(scanId);
            }
        },
        [scanId, reload],
    );

    // Single-result merges live here so the panel can refresh after each
    // completion (the engine has just resolved one row's comparison).
    const merge = useResultMerge({
        onCompleted: (resultId, mergedScanId) => {
            if (scanId !== null && scanId > 0 && Number(mergedScanId) === Number(scanId)) {
                void refreshComparisonResult(resultId);
            }
        },
    });
    // Restore-deleted-item lives next to merge: same per-row lifecycle, same
    // post-completion reload semantics (the engine has just re-created the
    // file referenced by one row).
    const restore = useRestoreItem({
        onCompleted: (resultId, restoredScanId) => {
            if (scanId !== null && scanId > 0 && Number(restoredScanId) === Number(scanId)) {
                void refreshComparisonResult(resultId);
            }
        },
    });

    useEffect(() => {
        void reload(scanId);
    }, [reload, scanId, reloadVersion]);

    // Drop selection when the current id is no longer in the loaded list (reload, merge, etc.).
    useEffect(() => {
        if (selectedResultId === null || selectedResultId === undefined) {
            return;
        }
        if (loadState !== 'ready') {
            return;
        }
        if (results.some((r) => r.id === selectedResultId)) {
            return;
        }
        onResultSelected?.(null);
    }, [loadState, results, selectedResultId, onResultSelected]);

    // When the active scan finishes, refresh results — the engine has just
    // populated the database row that loadScanResults reads from.
    useEffect(() => {
        if (
            scanRunState.status === 'completed' &&
            scanRunState.scanId !== null &&
            scanId !== null &&
            scanId > 0 &&
            Number(scanRunState.scanId) === Number(scanId)
        ) {
            void reload(scanRunState.scanId);
        }
    }, [reload, scanRunState.status, scanRunState.scanId, scanId]);

    const handleResultItemSelected = useCallback(
        (resultId: number | null) => {
            onResultSelected?.(resultId);
        },
        [onResultSelected],
    );

    const handleKeyboardMoveSelection = useCallback(
        (fromId: number, direction: 'up' | 'down') => {
            const idx = results.findIndex((r) => r.id === fromId);
            if (idx < 0) {
                return;
            }
            const nextIdx = direction === 'down' ? Math.min(results.length - 1, idx + 1) : Math.max(0, idx - 1);
            if (nextIdx === idx) {
                return;
            }
            onResultSelected?.(results[nextIdx].id);
        },
        [results, onResultSelected],
    );

    const handleMergeButtonClicked = useCallback(
        (resultId: number, side: 'source' | 'target') => {
            const strategy: MergeStrategy = side === 'source' ? 'SOURCE' : 'TARGET';
            void merge.start(resultId, strategy);
        },
        [merge],
    );

    const handleRestoreButtonClicked = useCallback(
        (resultId: number) => {
            void restore.start(resultId);
        },
        [restore],
    );

    return (
        <div className="results-panel-container">
            {renderBody({
                results,
                loadState,
                errorMessage,
                scanInFlight,
                scanDisplayName,
                mergeState: merge.state,
                restoreState: restore.state,
                selectedResultId,
                onResultItemSelected: handleResultItemSelected,
                onKeyboardMoveSelection: handleKeyboardMoveSelection,
                onMergeButtonClicked: handleMergeButtonClicked,
                onDismissMergeError: merge.dismiss,
                onRestoreButtonClicked: handleRestoreButtonClicked,
                onDismissRestoreError: restore.dismiss,
                onRetry: () => {
                    void reload(scanId);
                },
            })}
        </div>
    );
}

interface BodyProps {
    results: ComparisonResult[];
    loadState: LoadState;
    errorMessage: string | null;
    scanInFlight: boolean;
    scanDisplayName: string | null;
    mergeState: ReturnType<typeof useResultMerge>['state'];
    restoreState: ReturnType<typeof useRestoreItem>['state'];
    selectedResultId: number | null;
    onResultItemSelected: (resultId: number | null) => void;
    onKeyboardMoveSelection: (fromId: number, direction: 'up' | 'down') => void;
    onMergeButtonClicked: (resultId: number, side: 'source' | 'target') => void;
    onDismissMergeError: (resultId: number) => void;
    onRestoreButtonClicked: (resultId: number) => void;
    onDismissRestoreError: (resultId: number) => void;
    onRetry: () => void;
}

/** Collapsed row height hint — {@link ResultItemRenderer} main row is ~100px + expand chrome; expanded rows re-measure. */
const RESULT_ROW_ESTIMATE_PX = 132;
const RESULT_VIRTUAL_OVERSCAN = 10;

type VirtualizedResultsListProps = Pick<
    BodyProps,
    | 'results'
    | 'mergeState'
    | 'restoreState'
    | 'selectedResultId'
    | 'onResultItemSelected'
    | 'onKeyboardMoveSelection'
    | 'onMergeButtonClicked'
    | 'onDismissMergeError'
    | 'onRestoreButtonClicked'
    | 'onDismissRestoreError'
>;

function VirtualizedResultsList({
    results,
    mergeState,
    restoreState,
    selectedResultId,
    onResultItemSelected,
    onKeyboardMoveSelection,
    onMergeButtonClicked,
    onDismissMergeError,
    onRestoreButtonClicked,
    onDismissRestoreError,
}: VirtualizedResultsListProps) {
    const scrollParentRef = useRef<HTMLDivElement>(null);

    const virtualizer = useVirtualizer({
        count: results.length,
        getScrollElement: () => scrollParentRef.current,
        estimateSize: () => RESULT_ROW_ESTIMATE_PX,
        overscan: RESULT_VIRTUAL_OVERSCAN,
        getItemKey: (index) => results[index].id,
    });

    useLayoutEffect(() => {
        if (selectedResultId === null || selectedResultId === undefined) {
            return;
        }
        const index = results.findIndex((r) => r.id === selectedResultId);
        if (index < 0) {
            return;
        }
        virtualizer.scrollToIndex(index, { align: 'auto' });
    }, [results, selectedResultId, virtualizer]);

    return (
        <ListGroup
            ref={scrollParentRef}
            className="results-list-group"
            role="listbox"
            aria-label="Scan comparison results"
            aria-multiselectable={false}
        >
            <div
                className="results-virtual-spacer"
                style={{
                    height: `${virtualizer.getTotalSize()}px`,
                    width: '100%',
                    position: 'relative',
                }}
            >
                {virtualizer.getVirtualItems().map((vi) => {
                    const result = results[vi.index];
                    return (
                        <div
                            key={vi.key}
                            className="results-virtual-row"
                            data-index={vi.index}
                            ref={virtualizer.measureElement}
                            style={{
                                position: 'absolute',
                                top: 0,
                                left: 0,
                                width: '100%',
                                transform: `translateY(${vi.start}px)`,
                            }}
                        >
                            <ResultItemRenderer
                                result={result}
                                isSelected={selectedResultId === result.id}
                                mergeRun={mergeState.runs[result.id]}
                                restoreRun={restoreState.runs[result.id]}
                                onResultItemSelected={onResultItemSelected}
                                onKeyboardMoveSelection={onKeyboardMoveSelection}
                                onMergeButtonClicked={onMergeButtonClicked}
                                onDismissMergeError={onDismissMergeError}
                                onRestoreButtonClicked={onRestoreButtonClicked}
                                onDismissRestoreError={onDismissRestoreError}
                                scrollSelectionIntoView={false}
                                ariaSetSize={results.length}
                                ariaPosInSet={vi.index + 1}
                            />
                        </div>
                    );
                })}
            </div>
        </ListGroup>
    );
}

function renderBody({
    results,
    loadState,
    errorMessage,
    scanInFlight,
    scanDisplayName,
    mergeState,
    restoreState,
    selectedResultId,
    onResultItemSelected,
    onKeyboardMoveSelection,
    onMergeButtonClicked,
    onDismissMergeError,
    onRestoreButtonClicked,
    onDismissRestoreError,
    onRetry,
}: BodyProps) {
    if (loadState === 'idle') {
        return (
            <div className="results-status results-status-empty">
                {scanInFlight
                    ? 'Scan in progress. Results will appear when it completes.'
                    : 'Select a completed scan from the History tab, or run a new scan, to see comparison results.'}
            </div>
        );
    }
    if (loadState === 'loading') {
        const name = scanDisplayName?.trim();
        return (
            <div className="results-status results-status-loading" role="status">
                {name ? `Loading results for ${name}…` : 'Loading comparison results…'}
            </div>
        );
    }
    if (loadState === 'error') {
        return (
            <div className="results-status results-status-error" role="alert">
                <div className="results-status-message">
                    {errorMessage ?? 'Could not load comparison results.'}
                </div>
                <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onRetry}>
                    Retry
                </button>
            </div>
        );
    }
    if (results.length === 0) {
        const name = scanDisplayName?.trim();
        return (
            <div className="results-status results-status-empty">
                {name
                    ? `${name} produced no comparison differences.`
                    : 'This scan produced no comparison differences.'}
            </div>
        );
    }
    return (
        <VirtualizedResultsList
            results={results}
            mergeState={mergeState}
            restoreState={restoreState}
            selectedResultId={selectedResultId}
            onResultItemSelected={onResultItemSelected}
            onKeyboardMoveSelection={onKeyboardMoveSelection}
            onMergeButtonClicked={onMergeButtonClicked}
            onDismissMergeError={onDismissMergeError}
            onRestoreButtonClicked={onRestoreButtonClicked}
            onDismissRestoreError={onDismissRestoreError}
        />
    );
}
