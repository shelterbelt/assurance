import { Button, ListGroup, Container, Row, Col } from 'react-bootstrap';
import { useCallback, useEffect, useMemo, useState } from 'react';

import { engine, EngineCallError } from '../../api/engine';
import type Scan from '../../model/Scan';
import { isScanInFlight, type ScanRunState, INITIAL_SCAN_RUN_STATE } from '../../hooks/scanRunReducer';

type HistoryLoadState = 'loading' | 'ready' | 'error';

interface HistoryPanelProps {
    /**
     * Lifted scan-run state from {@code MainWindow}. The history list reloads
     * after each {@code scanCompleted} (and once on mount) so newly-finished
     * scans appear without a manual refresh.
     */
    scanRunState?: ScanRunState;
    /**
     * Which scan row is highlighted and targeted by Delete / Resolve; mirrors
     * {@code MainWindow.selectedScanId} so history selection drives the
     * results pane (Swing parity).
     */
    selectedScanId: number | null;
    /**
     * Invoked when the user selects a scan in the list. The parent should set
     * {@code selectedScanId} and load results for that scan (same as Swing
     * {@code ScanHistoryPanel} firing {@code ScanResultsLoadedEvent}).
     */
    onHistoricalScanSelected: (scanId: number, scanDefinitionDisplayName: string | null) => void;
    /**
     * Invoked after a successful {@code engine.deleteScan(scanId)}. The parent
     * uses this to clear any state that referenced the deleted scan (e.g.
     * {@code MainWindow.selectedScanId} when the deleted row was the one
     * displayed in the results pane).
     */
    onScanDeleted?: (scanId: number) => void;
    /**
     * {@code true} while {@code assurance.mergeScan} is in flight for
     * {@code selectedScanId}. Disables Resolve (same semantics as Scan tab Merge).
     */
    mergeInFlight?: boolean;
    /**
     * Starts a whole-scan merge for the given id ({@code engine.mergeScan}).
     * Swing History "Resolve" delegates to the same workflow.
     */
    onResolveSelectedScan?: (scanId: number) => void | Promise<void>;
    /**
     * Incremented by the parent when a merge completes so this panel can
     * re-fetch {@code loadScans} and refresh counts.
     */
    historyRefreshKey?: number;
}

export default function HistoryPanel({
    scanRunState = INITIAL_SCAN_RUN_STATE,
    selectedScanId,
    onHistoricalScanSelected,
    onScanDeleted,
    mergeInFlight = false,
    onResolveSelectedScan,
    historyRefreshKey = 0,
}: HistoryPanelProps) {
    const [scans, setScans] = useState<Scan[]>([]);
    const [loadState, setLoadState] = useState<HistoryLoadState>('loading');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [mutationError, setMutationError] = useState<string | null>(null);
    const [deleting, setDeleting] = useState(false);
    const scanInFlight = isScanInFlight(scanRunState);

    const reload = useCallback(async () => {
        setLoadState('loading');
        setErrorMessage(null);
        try {
            const next = await engine.loadScans();
            setScans(next);
            setLoadState('ready');
        } catch (err) {
            if (err instanceof EngineCallError) {
                setErrorMessage(`Engine error ${err.code}: ${err.message}`);
            } else {
                setErrorMessage(err instanceof Error ? err.message : String(err));
            }
            setScans([]);
            setLoadState('error');
        }
    }, []);

    useEffect(() => {
        void reload();
    }, [reload]);

    useEffect(() => {
        if (historyRefreshKey <= 0) {
            return;
        }
        void reload();
    }, [reload, historyRefreshKey]);

    // Refresh the history list whenever a scan terminates. We watch the
    // tuple (status, scanId): a fresh `completed`/`failed` notification for
    // a different scan will retrigger reload, but consecutive renders for
    // the same terminal state will not.
    useEffect(() => {
        if (scanRunState.status === 'completed' || scanRunState.status === 'failed') {
            void reload();
        }
    }, [reload, scanRunState.status, scanRunState.scanId]);

    const handleScanRowClicked = useCallback(
        (scan: Scan) => {
            setMutationError(null);
            onHistoricalScanSelected(scan.id, scan.scanDef?.name ?? null);
        },
        [onHistoricalScanSelected],
    );

    const handleResolveButtonClick = useCallback(() => {
        if (selectedScanId === null || selectedScanId <= 0 || !onResolveSelectedScan) {
            return;
        }
        setMutationError(null);
        void onResolveSelectedScan(selectedScanId);
    }, [selectedScanId, onResolveSelectedScan]);

    const handleDeleteButtonClick = useCallback(async () => {
        if (selectedScanId === null || selectedScanId <= 0) {
            return;
        }
        const targetId = selectedScanId;
        setDeleting(true);
        setMutationError(null);
        try {
            await engine.deleteScan(targetId);
            onScanDeleted?.(targetId);
            await reload();
        } catch (err) {
            if (err instanceof EngineCallError) {
                setMutationError(`Engine error ${err.code}: ${err.message}`);
            } else {
                setMutationError(err instanceof Error ? err.message : String(err));
            }
        } finally {
            setDeleting(false);
        }
    }, [selectedScanId, reload, onScanDeleted]);

    const sortedScans = useMemo(() => {
        // Newest first. `scanStarted` is an ISO 8601 string so a
        // lexicographic compare is also chronological.
        return [...scans].sort((a, b) => b.scanStarted.localeCompare(a.scanStarted));
    }, [scans]);

    const actionsDisabled =
        selectedScanId === null || selectedScanId <= 0 || scanInFlight || deleting || mergeInFlight;

    return (
        <Container className="previous-scans-list-container">
            <Row>
                <Col className="px-0">
                    {renderListBody({
                        scans: sortedScans,
                        loadState,
                        errorMessage,
                        selectedScanId,
                        onScanRowClicked: handleScanRowClicked,
                        onRetry: () => { void reload(); },
                    })}
                </Col>
            </Row>
            {mutationError ? (
                <Row>
                    <Col className="px-0">
                        <div className="scan-list-status scan-list-status-error" role="alert">
                            <div className="scan-list-status-message">{mutationError}</div>
                        </div>
                    </Col>
                </Row>
            ) : null}
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button
                        className="results-manage-button"
                        variant="secondary"
                        disabled={actionsDisabled}
                        onClick={() => { void handleDeleteButtonClick(); }}
                    >
                        {deleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </Col>
                <Col className="spacer-col" />
                <Col className="px-0">
                    <Button
                        className="results-manage-button"
                        variant="secondary"
                        disabled={actionsDisabled || !onResolveSelectedScan}
                        onClick={handleResolveButtonClick}
                    >
                        {mergeInFlight ? 'Resolving…' : 'Resolve'}
                    </Button>
                </Col>
            </Row>
        </Container>
    );
}

interface ListBodyProps {
    scans: Scan[];
    loadState: HistoryLoadState;
    errorMessage: string | null;
    selectedScanId: number | null;
    onScanRowClicked: (scan: Scan) => void;
    onRetry: () => void;
}

function renderListBody({
    scans,
    loadState,
    errorMessage,
    selectedScanId,
    onScanRowClicked,
    onRetry,
}: ListBodyProps) {
    if (loadState === 'loading') {
        return (
            <div className="scan-list-status scan-list-status-loading" role="status">
                Loading scan history…
            </div>
        );
    }

    if (loadState === 'error') {
        return (
            <div className="scan-list-status scan-list-status-error" role="alert">
                <div className="scan-list-status-message">
                    {errorMessage ?? 'Could not load scan history.'}
                </div>
                <Button size="sm" variant="outline-secondary" onClick={onRetry}>
                    Retry
                </Button>
            </div>
        );
    }

    if (scans.length === 0) {
        return (
            <div className="scan-list-status scan-list-status-empty">
                No scans yet. Run a scan from the <strong>Scan</strong> tab to populate history.
            </div>
        );
    }

    return (
        <ListGroup className="previous-scans-list-group">
            {scans.map((scan) => (
                <ListGroup.Item
                    className="scan-list-item"
                    action
                    key={scan.id}
                    active={selectedScanId !== null && scan.id === selectedScanId}
                    onClick={() => onScanRowClicked(scan)}
                    href={'#' + scan.id}
                >
                    {formatScanLabel(scan)}
                </ListGroup.Item>
            ))}
        </ListGroup>
    );
}

function formatScanLabel(scan: Scan): string {
    const name = scan.scanDef?.name ?? 'Anonymous scan';
    const when = formatStartedAt(scan.scanStarted);
    const inFlight = scan.whenCompleted == null;
    const count = typeof scan.resultCount === 'number'
        ? ` — ${scan.resultCount} result${scan.resultCount === 1 ? '' : 's'}`
        : '';
    const tail = inFlight ? ' (in progress)' : '';
    return `${name} — ${when}${count}${tail}`;
}

function formatStartedAt(iso: string): string {
    const parsed = new Date(iso);
    if (Number.isNaN(parsed.getTime())) {
        return iso;
    }
    return parsed.toLocaleString();
}
