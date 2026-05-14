import { Button, Container, Row } from 'react-bootstrap';

interface ActionsPanelProps {
    /**
     * Id of the currently-selected scan definition. Zero/falsy means
     * "nothing selected" and disables the Scan / Scan and Merge buttons.
     */
    selectedScan: number;
    /**
     * {@code true} while a scan is starting or running. Prevents the user
     * from launching another concurrent scan from this UI surface; the
     * engine would also reject duplicates with {@code -32003}, but
     * disabling here gives faster feedback.
     */
    scanInFlight?: boolean;
    onScanClicked?: () => void;
    onScanAndMergeClicked?: () => void;
    /**
     * {@code true} when there is a historical scan whose results are being
     * displayed and which can therefore be merged. Drives the enabled state
     * of the dedicated Merge button.
     */
    canMergeHistoryScan?: boolean;
    /**
     * {@code true} while a whole-scan merge is in flight for the displayed
     * historical scan. Disables the Merge button (the engine would also
     * reject duplicates with {@code -32006}).
     */
    mergeInFlight?: boolean;
    onMergeClicked?: () => void;
}

export default function ActionsPanel({
    selectedScan,
    scanInFlight = false,
    onScanClicked,
    onScanAndMergeClicked,
    canMergeHistoryScan = false,
    mergeInFlight = false,
    onMergeClicked,
}: ActionsPanelProps) {
    const scanDisabled = !selectedScan || scanInFlight;
    const scanLabel = scanInFlight ? 'Scanning…' : 'Scan';
    // The Merge button operates on the displayed historical scan, not the
    // currently-selected scan definition — they are independent affordances.
    const mergeDisabled = !canMergeHistoryScan || scanInFlight || mergeInFlight;
    const mergeLabel = mergeInFlight ? 'Merging…' : 'Merge';
    return (
        <Container className="actions-panel-container">
            <Row>
                <Button
                    className="action-button"
                    variant="secondary"
                    disabled={scanDisabled}
                    onClick={onScanClicked}
                >
                    {scanLabel}
                </Button>
                <br />
            </Row>
            <Row className="spacer-row" />
            <Row>
                <Button
                    className="action-button"
                    variant="secondary"
                    disabled={scanDisabled}
                    onClick={onScanAndMergeClicked}
                >
                    Scan and Merge
                </Button>
                <br />
            </Row>
            <Row className="spacer-row" />
            <Row>
                <Button
                    className="action-button"
                    variant="secondary"
                    disabled={mergeDisabled}
                    onClick={onMergeClicked}
                >
                    {mergeLabel}
                </Button>
                <br />
            </Row>
        </Container>
    );
}
