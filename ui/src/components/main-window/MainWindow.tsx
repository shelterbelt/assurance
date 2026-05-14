import { Row, Col, TabContent, TabPane, TabContainer, Container, Collapse, Button } from 'react-bootstrap';
import { useCallback, useEffect, useState } from 'react';

import NavBar from '../nav-bar/NavBar';
import ScanPanel from '../scan-panel/ScanPanel';
import HistoryPanel from '../history-panel/HistoryPanel';
import FeedbackPanel from '../feedback-panel/FeedbackPanel';
import ResultsPanel from '../results-panel/ResultsPanel';
import ScanDefinitionPanel from '../scan-definition-panel/ScanDefinitionPanel';
import SettingsPanel from '../settings-panel/SettingsPanel';
import EnginePingButton from '../dev-tools/EnginePingButton';
import '../dev-tools/EnginePingButton.scss';
import './MainWindow.scss';
import { dispatchMainMenuCommand, type MainMenuTabKey } from '../../main-menu/dispatchMainMenuCommand';
import type ScanDefinition from '../../model/ScanDefinition';
import { useScanRun } from '../../hooks/useScanRun';
import { isScanInFlight } from '../../hooks/scanRunReducer';
import { useMergeScan } from '../../hooks/useMergeScan';
import { isMergeScanInFlight } from '../../hooks/mergeScanReducer';

const isDevBuild = process.env.NODE_ENV !== 'production';

export default function MainWindow() {
    const [definitionPanelActive, setDefinitionPanelActive] = useState(false);
    const [editingScanDefinition, setEditingScanDefinition] = useState<ScanDefinition | null>(null);
    const [activeTabKey, setActiveTabKey] = useState<MainMenuTabKey>('ScanContent');
    const [scanDefinitionsVersion, setScanDefinitionsVersion] = useState(0);
    const [settingsPanelOpen, setSettingsPanelOpen] = useState(false);
    /**
     * Id of the scan whose comparison results are currently displayed in
     * {@link ResultsPanel}. Set when a scan completes or when the user picks
     * a row on the History tab (Swing parity).
     */
    const [selectedScanId, setSelectedScanId] = useState<number | null>(null);
    /**
     * When results are driven by a History row, the scan definition display
     * name for {@link ResultsPanel} copy. Cleared when a fresh run completes
     * so the in-flight definition name from {@code scanRun} takes precedence.
     */
    const [resultsScanLabelFromHistory, setResultsScanLabelFromHistory] = useState<string | null>(null);
    const [selectedResultId, setSelectedResultId] = useState<number | null>(null);
    const [scanMenuState, setScanMenuState] = useState({
        canNewScanDefiniton: false,
        canDeleteScanDefiniton: false,
        canScan: false,
        canScanAndMerge: false,
    });
    const [resultsReloadVersion, setResultsReloadVersion] = useState(0);
    /** Bumped when a whole-scan merge finishes so {@link HistoryPanel} can reload scan metadata. */
    const [historyRefreshKey, setHistoryRefreshKey] = useState(0);
    const scanRun = useScanRun();
    const scanInFlight = isScanInFlight(scanRun.state);
    // Bumping resultsReloadVersion lets {@code ResultsPanel} reload after a
    // whole-scan merge mutates the underlying comparison results.
    const mergeScan = useMergeScan({
        onCompleted: () => {
            setResultsReloadVersion((v) => v + 1);
            setHistoryRefreshKey((k) => k + 1);
        },
    });
    const mergeInFlightForSelected =
        selectedScanId !== null && isMergeScanInFlight(mergeScan.state, selectedScanId);
    const mergeRunForSelected =
        selectedScanId !== null ? (mergeScan.state.runs[selectedScanId] ?? null) : null;

    /** Label for results status copy: history pick overrides auto-run name. */
    const resultsScanDisplayName =
        resultsScanLabelFromHistory ??
        (selectedScanId !== null &&
        selectedScanId > 0 &&
        scanRun.state.scanId !== null &&
        Number(scanRun.state.scanId) === Number(selectedScanId)
            ? scanRun.state.scanDefinitionName
            : null);

    const handleHistoricalScanSelected = useCallback(
        (scanId: number, scanDefinitionDisplayName: string | null) => {
            setSelectedScanId(scanId);
            const trimmed = scanDefinitionDisplayName?.trim();
            setResultsScanLabelFromHistory(trimmed && trimmed.length > 0 ? trimmed : null);
        },
        [],
    );

    // Auto-promote the just-finished scan to "displayed" so the user sees
    // its results without an extra click.
    useEffect(() => {
        if (scanRun.state.status === 'completed' && scanRun.state.scanId !== null) {
            setSelectedScanId(scanRun.state.scanId);
            setResultsScanLabelFromHistory(null);
        }
    }, [scanRun.state.status, scanRun.state.scanId]);

    // Menu actions for results (Replace Source/Target, Attributes) must not
    // target stale rows after the user switches scans.
    useEffect(() => {
        setSelectedResultId(null);
    }, [selectedScanId]);

    const activateDefinitionPanel = useCallback((toEdit?: ScanDefinition) => {
        setEditingScanDefinition(toEdit ?? null);
        setDefinitionPanelActive(true);
    }, []);

    const handleScanDefinitionSaved = useCallback(() => {
        setDefinitionPanelActive(false);
        setEditingScanDefinition(null);
        setScanDefinitionsVersion((v) => v + 1);
    }, []);

    const handleScanDefinitionCancel = useCallback(() => {
        setDefinitionPanelActive(false);
        setEditingScanDefinition(null);
    }, []);

    const canSwitchTabs = !definitionPanelActive && !scanInFlight;

    useEffect(() => {
        const subscribe = window.assuranceapi?.onMainMenuCommand;
        if (!subscribe) return;

        const unsubscribe = subscribe((command) => {
            // View-scope commands must respect the same disabled semantics as
            // the NavBar.
            if (!canSwitchTabs && (command === 'viewScan' || command === 'viewHistory')) {
                return;
            }

            dispatchMainMenuCommand(command, {
                selectedResultId,
                canSwitchTabs,
                setActiveTabKey,
                activateDefinitionPanel: () => activateDefinitionPanel(),
                setSettingsPanelOpen: (open) => setSettingsPanelOpen(open),
            });
        });

        return () => unsubscribe();
    }, [
        canSwitchTabs,
        selectedResultId,
        activateDefinitionPanel,
        setActiveTabKey,
        setSettingsPanelOpen,
    ]);

    // Push enablement state down into the Electron main-process menu so
    // items that require a focused target are actually disabled.
    useEffect(() => {
        const setMainMenuState = window.assuranceapi?.setMainMenuState;
        if (!setMainMenuState) return;

        const canResultActions = selectedResultId !== null && canSwitchTabs;

        setMainMenuState({
            canViewScan: canSwitchTabs,
            canViewHistory: canSwitchTabs,
            canDisplaySettings: canSwitchTabs,
            ...scanMenuState,
            canReplaceSource: canResultActions,
            canReplaceTarget: canResultActions,
            canSourceAttributes: canResultActions,
            canTargetAttributes: canResultActions,
        });
    }, [canSwitchTabs, selectedResultId, scanMenuState]);

    return (
        <>
            <TabContainer
                activeKey={activeTabKey}
                onSelect={(key) => {
                    if (!key) return;
                    if (!canSwitchTabs) return;
                    const next = key as MainMenuTabKey;
                    if (next === 'ScanContent' || next === 'HistoryContent') {
                        setActiveTabKey(next);
                    }
                }}
                id="navController"
            >
                <Row className="main-window-nav-row align-items-center">
                    <Col xs="auto" className="main-window-settings-col">
                        <Button
                            variant="link"
                            className="main-window-settings-button"
                            onClick={() => setSettingsPanelOpen(true)}
                            aria-label="Open settings menu"
                            title="Settings"
                        >
                            <span className="main-window-settings-glyph" aria-hidden="true">⋮</span>
                            <span className="visually-hidden">Settings</span>
                        </Button>
                    </Col>
                    <Col xs="auto" className="main-window-nav-center-col">
                        <NavBar disabled={definitionPanelActive || scanInFlight} />
                    </Col>
                </Row>
                <TabContent className="tab-area">
                    <TabPane eventKey="ScanContent">
                        <Col className="scan-tab-pane">
                            <Collapse in={definitionPanelActive}>
                                <Row>
                                    <ScanDefinitionPanel
                                        scanDefinition={editingScanDefinition}
                                        onSaved={handleScanDefinitionSaved}
                                        onCancelButtonClicked={handleScanDefinitionCancel}
                                    />
                                </Row>
                            </Collapse>
                            <Collapse in={!definitionPanelActive}>
                                <Row>
                                    <ScanPanel
                                        onNewButtonClicked={() => activateDefinitionPanel()}
                                        onEditScanDefinitionClicked={(scanDefinition) =>
                                            activateDefinitionPanel(scanDefinition)
                                        }
                                        scanDefinitionsVersion={scanDefinitionsVersion}
                                        scanRunState={scanRun.state}
                                        onStartScan={scanRun.start}
                                        selectedHistoryScanId={selectedScanId}
                                        mergeInFlight={mergeInFlightForSelected}
                                        onMergeSelectedScan={mergeScan.start}
                                        onMainMenuStateChange={(state) => setScanMenuState(state)}
                                    />
                                </Row>
                            </Collapse>
                        </Col>
                    </TabPane>
                    <TabPane eventKey="HistoryContent">
                        <HistoryPanel
                            scanRunState={scanRun.state}
                            selectedScanId={selectedScanId}
                            mergeInFlight={mergeInFlightForSelected}
                            onResolveSelectedScan={mergeScan.start}
                            historyRefreshKey={historyRefreshKey}
                            onHistoricalScanSelected={handleHistoricalScanSelected}
                            onScanDeleted={(deletedId) => {
                                if (selectedScanId === deletedId) {
                                    setSelectedScanId(null);
                                    setResultsScanLabelFromHistory(null);
                                }
                            }}
                        />
                    </TabPane>
                </TabContent>
            </TabContainer>
            <Container className="content-area">
                <Row className="main-window-feedback-row">
                    <Col>
                        <FeedbackPanel
                            scanRunState={scanRun.state}
                            onDismiss={scanRun.reset}
                            mergeRun={mergeRunForSelected}
                            onDismissMerge={
                                selectedScanId !== null
                                    ? () => mergeScan.dismiss(selectedScanId)
                                    : undefined
                            }
                        />
                    </Col>
                </Row>
                <Row className="flex-fill">
                    <Col>
                        <ResultsPanel
                            scanId={selectedScanId}
                            scanDisplayName={resultsScanDisplayName}
                            scanRunState={scanRun.state}
                            reloadVersion={resultsReloadVersion}
                            selectedResultId={selectedResultId}
                            onResultSelected={(id) => setSelectedResultId(id)}
                        />
                    </Col>
                </Row>
                {isDevBuild ? (
                    <Row className="main-window-dev-tools-row">
                        <Col>
                            <EnginePingButton />
                        </Col>
                    </Row>
                ) : null}
            </Container>
            <SettingsPanel show={settingsPanelOpen} onClose={() => setSettingsPanelOpen(false)} />
        </>
    );
}
