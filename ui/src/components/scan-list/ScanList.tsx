import { Button, ListGroup, Container, Row, Col } from 'react-bootstrap';
import { useEffect, useRef } from 'react';

import type ScanDefinition from '../../model/ScanDefinition';

export type ScanListLoadState = 'loading' | 'ready' | 'error';

interface ScanListProps {
    scans: ScanDefinition[];
    selectedScan: number;
    loadState: ScanListLoadState;
    errorMessage: string | null;
    /**
     * When non-null, an additional banner is shown above the action row to
     * surface a transient mutation failure (e.g. a delete that the engine
     * rejected). Decoupled from {@code errorMessage}, which is reserved for
     * load-time failures.
     */
    mutationError?: string | null;
    /** Disables the action buttons during in-flight mutations. */
    deleting?: boolean;
    /**
     * Disables the New / Delete buttons while a scan is running, since the
     * engine refuses concurrent mutations against a definition that is in
     * use by an active run.
     */
    scanInFlight?: boolean;
    onScanSelected: (scanId: number) => void;
    /**
     * Opens the selected scan definition for editing. Mirrors the legacy
     * Swing behavior where double-clicking an entry in the scan list opened
     * that definition in the editor.
     */
    onScanEditRequested?: (scan: ScanDefinition) => void;
    onNewButtonClick: () => void;
    onDeleteButtonClick: () => void;
    onRetry: () => void;
}

export default function ScanList({
    scans,
    selectedScan,
    loadState,
    errorMessage,
    mutationError = null,
    deleting = false,
    scanInFlight = false,
    onScanSelected,
    onScanEditRequested,
    onNewButtonClick,
    onDeleteButtonClick,
    onRetry,
}: ScanListProps) {
    const listBodyMountRef = useRef<HTMLDivElement>(null);
    const actionsDisabled = deleting || scanInFlight;
    const deleteDisabled = actionsDisabled || !selectedScan || loadState !== 'ready';

    useEffect(() => {
        if (loadState !== 'ready' || scans.length === 0 || !selectedScan) {
            return;
        }
        const root = listBodyMountRef.current;
        if (!root) {
            return;
        }
        const node = root.querySelector(`[data-assurance-scan-definition-id="${String(selectedScan)}"]`);
        if (node instanceof HTMLElement) {
            node.focus();
        }
    }, [loadState, scans, selectedScan]);

    return (
        <Container className="scan-list-container">
            <Row>
                <Col className="px-0">
                    <div ref={listBodyMountRef} className="scan-list-body-mount">
                        {renderListBody({
                            scans,
                            loadState,
                            errorMessage,
                            selectedScan,
                            onScanSelected,
                            onScanEditRequested,
                            onRetry,
                        })}
                    </div>
                </Col>
            </Row>
            {mutationError ? (
                <Row className="scan-list-mutation-error">
                    <Col className="px-0">
                        <span role="alert">{mutationError}</span>
                    </Col>
                </Row>
            ) : null}
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button
                        className="scan-manage-button"
                        variant="secondary"
                        onClick={onNewButtonClick}
                        disabled={actionsDisabled}
                    >
                        New
                    </Button>
                </Col>
                <Col className="spacer-col" />
                <Col className="px-0">
                    <Button
                        className="scan-manage-button"
                        variant="secondary"
                        disabled={deleteDisabled}
                        onClick={onDeleteButtonClick}
                    >
                        {deleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </Col>
            </Row>
        </Container>
    );
}

interface ListBodyProps {
    scans: ScanDefinition[];
    loadState: ScanListLoadState;
    errorMessage: string | null;
    selectedScan: number;
    onScanSelected: (scanId: number) => void;
    onScanEditRequested?: (scan: ScanDefinition) => void;
    onRetry: () => void;
}

function renderListBody({
    scans,
    loadState,
    errorMessage,
    selectedScan,
    onScanSelected,
    onScanEditRequested,
    onRetry,
}: ListBodyProps) {
    if (loadState === 'loading') {
        return (
            <div className="scan-list-status scan-list-status-loading" role="status">
                Loading scan definitions…
            </div>
        );
    }

    if (loadState === 'error') {
        return (
            <div className="scan-list-status scan-list-status-error" role="alert">
                <div className="scan-list-status-message">
                    {errorMessage ?? 'Could not load scan definitions.'}
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
                No scan definitions yet. Click <strong>New</strong> to create one.
            </div>
        );
    }

    return (
        <ListGroup className="scan-list-group">
            {scans.map((scan) => (
                <ListGroup.Item
                    className="scan-list-item"
                    action
                    key={scan.id}
                    active={scan.id === selectedScan}
                    data-assurance-scan-definition-id={scan.id}
                    onClick={() => onScanSelected(scan.id)}
                    onDoubleClick={() => onScanEditRequested?.(scan)}
                    href={'#' + scan.id}
                >
                    {scan.name}
                </ListGroup.Item>
            ))}
        </ListGroup>
    );
}
