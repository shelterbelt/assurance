import { Row, Col, Container } from 'react-bootstrap';
import { useCallback, useEffect, useState } from 'react';

import ScanList, { ScanListLoadState } from '../scan-list/ScanList';
import ActionsPanel from '../actions-panel/ActionsPanel';
import { engine, EngineCallError } from '../../api/engine';
import type ScanDefinition from '../../model/ScanDefinition';
import { isScanInFlight, INITIAL_SCAN_RUN_STATE, type ScanRunState } from '../../hooks/scanRunReducer';

/** After (re)loading definitions: keep prior selection if it still exists; otherwise first row; none if empty. */
function selectionAfterDefinitionsLoad(definitions: ScanDefinition[], previousId: number): number {
    if (definitions.length === 0) {
        return 0;
    }
    if (previousId !== 0 && definitions.some((d) => d.id === previousId)) {
        return previousId;
    }
    return definitions[0].id;
}

interface ScanPanelProps {
    onNewButtonClicked: () => void;
    /**
     * Opens an existing scan definition in edit mode.
     */
    onEditScanDefinitionClicked?: (scanDefinition: ScanDefinition) => void;
    /**
     * Bumped by the parent whenever something outside the panel mutates the
     * persisted scan-definition collection (e.g. after a save). The panel
     * reloads from the engine on every change.
     */
    scanDefinitionsVersion?: number;
    /**
     * Lifted scan-run state owned by {@code MainWindow}. Drives both the
     * action buttons and the disabled state of the list-mutation buttons
     * while a scan is in flight.
     */
    scanRunState?: ScanRunState;
    /**
     * Callback invoked when the user clicks Scan / Scan and Merge. Receives
     * the selected definition id, the {@code merge} flag, and the definition's
     * display name for progress UI (optional).
     */
    onStartScan?: (
        scanDefinitionId: number,
        merge: boolean,
        scanDefinitionDisplayName?: string | null,
    ) => void | Promise<void>;
    /**
     * Id of the scan currently displayed in the results pane (the most
     * recently completed scan, or one selected from history). Drives the
     * "Merge" button: when {@code null}, no historical scan is available to
     * merge.
     */
    selectedHistoryScanId?: number | null;
    /**
     * {@code true} while a whole-scan merge is in flight for
     * {@code selectedHistoryScanId}. Disables the Merge button.
     */
    mergeInFlight?: boolean;
    /**
     * Callback invoked when the user clicks Merge. Receives the
     * {@code selectedHistoryScanId}.
     */
    onMergeSelectedScan?: (scanId: number) => void | Promise<void>;

    /**
     * Lifted enablement signals for the Electron main-process menu.
     * Mirrors the button disabled semantics in the Swing legacy UI.
     */
    onMainMenuStateChange?: (state: {
        canNewScanDefiniton: boolean;
        canDeleteScanDefiniton: boolean;
        canScan: boolean;
        canScanAndMerge: boolean;
    }) => void;
}

export default function ScanPanel({
    onNewButtonClicked,
    onEditScanDefinitionClicked,
    scanDefinitionsVersion = 0,
    scanRunState = INITIAL_SCAN_RUN_STATE,
    onStartScan,
    selectedHistoryScanId = null,
    mergeInFlight = false,
    onMergeSelectedScan,
    onMainMenuStateChange,
}: ScanPanelProps) {
    const [scanDefinitions, setScanDefinitions] = useState<ScanDefinition[]>([]);
    const [selectedScan, setSelectedScan] = useState<number>(0);
    const [loadState, setLoadState] = useState<ScanListLoadState>('loading');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [mutationError, setMutationError] = useState<string | null>(null);
    const [deleting, setDeleting] = useState(false);
    const scanInFlight = isScanInFlight(scanRunState);

    // The scan-list's internal selection controls which actions are enabled.
    const canNewScanDefiniton = !deleting && !scanInFlight;
    const canDeleteScanDefiniton =
        !deleting && !scanInFlight && loadState === 'ready' && Boolean(selectedScan);
    const canScan = loadState === 'ready' && Boolean(selectedScan) && !scanInFlight;
    const canScanAndMerge = canScan;

    useEffect(() => {
        onMainMenuStateChange?.({
            canNewScanDefiniton,
            canDeleteScanDefiniton,
            canScan,
            canScanAndMerge,
        });
    }, [
        onMainMenuStateChange,
        canNewScanDefiniton,
        canDeleteScanDefiniton,
        canScan,
        canScanAndMerge,
    ]);

    const reload = useCallback(async () => {
        setLoadState('loading');
        setErrorMessage(null);
        setMutationError(null);
        try {
            const definitions = await engine.loadScanDefinitions();
            setScanDefinitions(definitions);
            setSelectedScan((previousId) => selectionAfterDefinitionsLoad(definitions, previousId));
            setLoadState('ready');
        } catch (err) {
            if (err instanceof EngineCallError) {
                setErrorMessage(`Engine error ${err.code}: ${err.message}`);
            } else {
                setErrorMessage(err instanceof Error ? err.message : String(err));
            }
            setScanDefinitions([]);
            setSelectedScan(0);
            setLoadState('error');
        }
    }, []);

    useEffect(() => {
        void reload();
    }, [reload, scanDefinitionsVersion]);

    const handleScanSelected = useCallback((scanID: number) => {
        setSelectedScan(scanID);
        setMutationError(null);
    }, []);

    const handleScanEditRequested = useCallback(
        (scanDefinition: ScanDefinition) => {
            onEditScanDefinitionClicked?.(scanDefinition);
        },
        [onEditScanDefinitionClicked],
    );

    const handleDeleteButtonClick = useCallback(async () => {
        if (!selectedScan) {
            return;
        }
        setDeleting(true);
        setMutationError(null);
        try {
            await engine.deleteScanDefinition(selectedScan);
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
    }, [selectedScan, reload]);

    const handleStartScan = useCallback(
        (merge: boolean) => {
            if (!selectedScan || !onStartScan) {
                return;
            }
            const def = scanDefinitions.find((s) => s.id === selectedScan);
            void onStartScan(selectedScan, merge, def?.name ?? null);
        },
        [onStartScan, selectedScan, scanDefinitions],
    );

    const handleMergeSelected = useCallback(() => {
        if (selectedHistoryScanId === null || !onMergeSelectedScan) {
            return;
        }
        void onMergeSelectedScan(selectedHistoryScanId);
    }, [onMergeSelectedScan, selectedHistoryScanId]);

    return (
        <Container className="scan-operations-container">
            <Row>
                <Col className="ps-0">
                    <ScanList
                        scans={scanDefinitions}
                        selectedScan={selectedScan}
                        loadState={loadState}
                        errorMessage={errorMessage}
                        mutationError={mutationError}
                        deleting={deleting}
                        scanInFlight={scanInFlight}
                        onScanSelected={handleScanSelected}
                        onScanEditRequested={handleScanEditRequested}
                        onDeleteButtonClick={() => { void handleDeleteButtonClick(); }}
                        onNewButtonClick={onNewButtonClicked}
                        onRetry={reload}
                    />
                </Col>
                <Col className="pe-0">
                    <ActionsPanel
                        selectedScan={selectedScan}
                        scanInFlight={scanInFlight}
                        onScanClicked={() => handleStartScan(false)}
                        onScanAndMergeClicked={() => handleStartScan(true)}
                        canMergeHistoryScan={selectedHistoryScanId !== null}
                        mergeInFlight={mergeInFlight}
                        onMergeClicked={handleMergeSelected}
                    />
                </Col>
            </Row>
        </Container>
    );
}
