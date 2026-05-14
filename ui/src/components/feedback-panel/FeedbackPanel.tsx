import { Button, ProgressBar } from 'react-bootstrap';

import { INITIAL_SCAN_RUN_STATE, type ScanRunState } from '../../hooks/scanRunReducer';
import type { MergeScanRunState } from '../../hooks/mergeScanReducer';

interface FeedbackPanelProps {
    /** Lifted scan-run state from {@code MainWindow}. */
    scanRunState?: ScanRunState;
    /**
     * Invoked when the user dismisses a terminal scan feedback banner
     * (completed/failed/error). Returns the scan panel to {@code idle}.
     */
    onDismiss?: () => void;
    /**
     * Optional whole-scan merge state for the scan whose results are
     * currently displayed. {@code null} when no merge is active. The panel
     * renders an additional row reflecting merge progress / completion when
     * present.
     */
    mergeRun?: MergeScanRunState | null;
    /**
     * Invoked when the user dismisses a terminal merge feedback banner. The
     * parent should clear the merge entry for the scan in question.
     */
    onDismissMerge?: () => void;
}

/** Primary label for in-progress scan feedback: "Scan {definition name}". */
function scanDefinitionLabel(state: ScanRunState): string {
    const name = state.scanDefinitionName?.trim();
    return name ? `Scan ${name}` : 'Scan';
}

export default function FeedbackPanel({
    scanRunState = INITIAL_SCAN_RUN_STATE,
    onDismiss,
    mergeRun = null,
    onDismissMerge,
}: FeedbackPanelProps) {
    return (
        <div className="feedback-panel-container">
            {renderScanBody(scanRunState, onDismiss)}
            {mergeRun ? renderMergeBody(mergeRun, onDismissMerge) : null}
        </div>
    );
}

function renderScanBody(state: ScanRunState, onDismiss?: () => void) {
    switch (state.status) {
        case 'idle':
            return null;

        case 'starting': {
            const name = state.scanDefinitionName?.trim();
            return (
                <div className="feedback-row" role="status">
                    <span>{name ? `Starting scan for ${name}…` : 'Starting scan…'}</span>
                </div>
            );
        }

        case 'running': {
            const progress = state.progress;
            const total = progress?.itemsTotal;
            const processed = progress?.itemsProcessed;
            const phase = progress?.phase ?? 'scanning';
            const currentItem = progress?.currentItem ?? '';
            const determinate =
                typeof total === 'number' && total > 0 && typeof processed === 'number'
                    ? { processed, total }
                    : null;
            return (
                <div className="feedback-row" role="status" aria-live="polite">
                    <div className="feedback-headline feedback-headline--progress">
                        <div className="feedback-headline-primary">
                            <strong>{scanDefinitionLabel(state)}</strong>
                            {currentItem ? <span className="feedback-phase">{phase}</span> : null}
                        </div>
                        {currentItem ? (
                            <div className="feedback-current-item" title={currentItem}>
                                {currentItem}
                            </div>
                        ) : (
                            <span className="feedback-phase">{phase}</span>
                        )}
                    </div>
                    {determinate ? (
                        <ProgressBar
                            className="feedback-progress"
                            now={Math.min(100, Math.round((determinate.processed / determinate.total) * 100))}
                            label={`${determinate.processed} / ${determinate.total}`}
                        />
                    ) : (
                        <ProgressBar className="feedback-progress" animated now={100} />
                    )}
                </div>
            );
        }

        case 'completed':
            return (
                <div className="feedback-row feedback-row-success" role="status">
                    <span>
                        {scanDefinitionLabel(state)} completed
                        {typeof state.resultCount === 'number'
                            ? `: ${state.resultCount} result${state.resultCount === 1 ? '' : 's'}`
                            : ''}
                        .
                    </span>
                    {onDismiss ? (
                        <Button
                            className="feedback-dismiss-button"
                            size="sm"
                            variant="outline-secondary"
                            onClick={onDismiss}
                        >
                            Dismiss
                        </Button>
                    ) : null}
                </div>
            );

        case 'failed':
        case 'error':
            return (
                <div className="feedback-row feedback-row-error" role="alert">
                    <span>
                        {state.status === 'failed'
                            ? `${scanDefinitionLabel(state)} failed`
                            : `Could not start scan${
                                  state.scanDefinitionName?.trim()
                                      ? ` (${state.scanDefinitionName.trim()})`
                                      : ''
                              }`}
                        {state.error ? `: ${state.error.message}` : '.'}
                    </span>
                    {onDismiss ? (
                        <Button
                            className="feedback-dismiss-button"
                            size="sm"
                            variant="outline-secondary"
                            onClick={onDismiss}
                        >
                            Dismiss
                        </Button>
                    ) : null}
                </div>
            );
    }
}

function renderMergeBody(run: MergeScanRunState, onDismiss?: () => void) {
    switch (run.status) {
        case 'starting':
            return (
                <div className="feedback-row" role="status">
                    <span>Starting merge…</span>
                </div>
            );

        case 'running': {
            const progress = run.progress;
            const total = progress?.itemsTotal;
            const processed = progress?.itemsProcessed;
            const phase = progress?.phase ?? 'merging';
            const currentItem = progress?.currentItem ?? '';
            const determinate =
                typeof total === 'number' && total > 0 && typeof processed === 'number'
                    ? { processed, total }
                    : null;
            return (
                <div className="feedback-row" role="status" aria-live="polite">
                    <div className="feedback-headline feedback-headline--progress">
                        <div className="feedback-headline-primary">
                            <strong>Merge of scan {run.scanId}</strong>
                            {currentItem ? <span className="feedback-phase">{phase}</span> : null}
                        </div>
                        {currentItem ? (
                            <div className="feedback-current-item" title={currentItem}>
                                {currentItem}
                            </div>
                        ) : (
                            <span className="feedback-phase">{phase}</span>
                        )}
                    </div>
                    {determinate ? (
                        <ProgressBar
                            className="feedback-progress"
                            now={Math.min(100, Math.round((determinate.processed / determinate.total) * 100))}
                            label={`${determinate.processed} / ${determinate.total}`}
                        />
                    ) : (
                        <ProgressBar className="feedback-progress" animated now={100} />
                    )}
                </div>
            );
        }

        case 'completed':
            return (
                <div className="feedback-row feedback-row-success" role="status">
                    <span>
                        Merge of scan {run.scanId} completed
                        {typeof run.itemsMerged === 'number'
                            ? `: ${run.itemsMerged} item${run.itemsMerged === 1 ? '' : 's'} merged`
                            : ''}
                        .
                    </span>
                    {onDismiss ? (
                        <Button
                            className="feedback-dismiss-button"
                            size="sm"
                            variant="outline-secondary"
                            onClick={onDismiss}
                        >
                            Dismiss
                        </Button>
                    ) : null}
                </div>
            );

        case 'failed':
        case 'error':
            return (
                <div className="feedback-row feedback-row-error" role="alert">
                    <span>
                        {run.status === 'failed' ? 'Merge failed' : 'Could not start merge'}
                        {run.error ? `: ${run.error.message}` : '.'}
                    </span>
                    {onDismiss ? (
                        <Button
                            className="feedback-dismiss-button"
                            size="sm"
                            variant="outline-secondary"
                            onClick={onDismiss}
                        >
                            Dismiss
                        </Button>
                    ) : null}
                </div>
            );
    }
}
