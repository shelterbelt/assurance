import { ListGroup, Col, Row, Button, Collapse } from 'react-bootstrap';
import { useCallback, useLayoutEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react';

import type ComparisonResult from '../../model/ComparisonResult';
import type AssuranceResultReason from '../../model/enums/AssuranceResultReason';
import type AssuranceResultResolution from '../../model/enums/AssuranceResultResolution';
import type FileReference from '../../model/FileReference';
import type { ResultMergeRunState } from '../../hooks/resultMergeReducer';
import type { RestoreItemRunState } from '../../hooks/restoreItemReducer';

/** Stable key for the single visible row banner; progress text updates do not change this. */
type ResultRowBannerIdentity =
    | 'merge-running'
    | 'restore-running'
    | 'merge-error'
    | 'restore-error'
    | 'merge-completed'
    | 'restore-completed';

interface ResolvedResultRowBanner {
    identity: ResultRowBannerIdentity;
    role: 'status' | 'alert';
    variantClass: 'merge-status-running' | 'merge-status-error' | 'merge-status-completed';
    message: string;
    dismissible: boolean;
    onDismiss?: () => void;
}

interface BannerSlotEntry {
    identity: ResultRowBannerIdentity;
    banner: ResolvedResultRowBanner;
}

interface ResultItemRendererProps {
    result: ComparisonResult;
    /** Mirrors {@code selectedResultId} from {@link ResultsPanel} for menu + styling. */
    isSelected: boolean;
    /**
     * Per-row merge lifecycle state, sourced from {@link useResultMerge}'s
     * {@code state.runs[result.id]}. {@code undefined} when no merge has been
     * initiated for this row in the current session.
     */
    mergeRun?: ResultMergeRunState;
    /**
     * Per-row restore lifecycle state, sourced from {@link useRestoreItem}'s
     * {@code state.runs[result.id]}. {@code undefined} when no restore has
     * been initiated for this row in the current session.
     */
    restoreRun?: RestoreItemRunState;
    onResultItemSelected: (resultId: number | null) => void;
    onKeyboardMoveSelection: (fromId: number, direction: 'up' | 'down') => void;
    onMergeButtonClicked: (resultId: number, side: 'source' | 'target') => void;
    /** Clears a terminal {@code error} or {@code failed} merge entry for this row. */
    onDismissMergeError: (resultId: number) => void;
    onRestoreButtonClicked: (resultId: number) => void;
    /** Clears a terminal {@code error} or {@code failed} restore entry for this row. */
    onDismissRestoreError: (resultId: number) => void;
    /**
     * When {@code true} (default), the row scrolls into view when it becomes
     * selected. Set {@code false} when a parent virtualized list owns
     * scrolling via {@code scrollToIndex}.
     */
    scrollSelectionIntoView?: boolean;
    /** For virtualized lists: total options in the listbox (omit when not virtualized). */
    ariaSetSize?: number;
    /** For virtualized lists: 1-based index of this option (omit when not virtualized). */
    ariaPosInSet?: number;
}

export default function ResultItemRenderer({
    result,
    isSelected,
    mergeRun,
    restoreRun,
    onResultItemSelected,
    onKeyboardMoveSelection,
    onMergeButtonClicked,
    onDismissMergeError,
    onRestoreButtonClicked,
    onDismissRestoreError,
    scrollSelectionIntoView = true,
    ariaSetSize,
    ariaPosInSet,
}: ResultItemRendererProps) {
    const [expanded, setExpanded] = useState(false);
    const itemRef = useRef<HTMLDivElement>(null);
    const wasSelectedRef = useRef(false);

    const handleExpandButtonClicked = () => setExpanded((prev) => !prev);
    const mergeInFlight =
        mergeRun?.status === 'starting' || mergeRun?.status === 'running';

    const restoreInFlight =
        restoreRun?.status === 'starting' || restoreRun?.status === 'running';

    // The engine's restoreDeletedItem only does meaningful work when the
    // result was previously merged via deletion; rendering the button only
    // for those resolutions matches the legacy 1.x ComparisonResultPanel
    // semantics (the same button toggled between merge and restore based on
    // resolution state).
    const restoreApplicable =
        result.resolution === 'DELETE_SOURCE' || result.resolution === 'DELETE_TARGET';

    // Restore and merge touch the same engine resource (this comparison
    // result), so each disables the other while in flight.
    const anyRowMutationInFlight = mergeInFlight || restoreInFlight;

    useLayoutEffect(() => {
        if (scrollSelectionIntoView && isSelected && !wasSelectedRef.current) {
            const el = itemRef.current;
            if (el) {
                // Avoid the window jumping from focus(); still scroll within the results list.
                el.focus({ preventScroll: true });
                el.scrollIntoView({ block: 'nearest', inline: 'nearest' });
            }
        }
        if (!scrollSelectionIntoView && isSelected && !wasSelectedRef.current) {
            const el = itemRef.current;
            if (el) {
                el.focus({ preventScroll: true });
            }
        }
        wasSelectedRef.current = isSelected;
    }, [isSelected, scrollSelectionIntoView]);

    const itemClassName = ['scan-result-item', isSelected ? 'scan-result-item-selected' : '']
        .filter(Boolean)
        .join(' ');

    const handleRowKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
        if (e.target !== e.currentTarget) {
            return;
        }
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            onKeyboardMoveSelection(result.id, 'down');
            return;
        }
        if (e.key === 'ArrowUp') {
            e.preventDefault();
            onKeyboardMoveSelection(result.id, 'up');
        }
    };

    return (
        <ListGroup.Item
            as="div"
            ref={itemRef}
            role="option"
            aria-selected={isSelected}
            aria-setsize={ariaSetSize}
            aria-posinset={ariaPosInSet}
            tabIndex={0}
            className={itemClassName}
            data-result-id={result.id}
            onClick={() => onResultItemSelected(result.id)}
            onFocus={(ev) => {
                if (ev.target === ev.currentTarget) {
                    onResultItemSelected(result.id);
                }
            }}
            onKeyDown={handleRowKeyDown}
        >
            <ResultRowStatusBannerSlot
                mergeRun={mergeRun}
                restoreRun={restoreRun}
                onDismissMergeError={onDismissMergeError}
                onDismissRestoreError={onDismissRestoreError}
                resultId={result.id}
            />
            <Row className="scan-result-main-row">
                <Col className="source-col">
                    <ResultItemDetails
                        reference={result.source ?? null}
                        reason={result.reason}
                        resolution={result.resolution}
                        expanded={expanded}
                        side="source"
                    />
                </Col>
                <Col className="merge-actions-col">
                    <div className="merge-actions-container">
                        <Button
                            className="result-merge-button merge-source"
                            variant="secondary"
                            disabled={anyRowMutationInFlight}
                            onClick={() => {
                                onMergeButtonClicked(result.id, 'source');
                            }}
                            aria-label="Replace target with source"
                        />
                        <Button
                            className="result-merge-button merge-target"
                            variant="secondary"
                            disabled={anyRowMutationInFlight}
                            onClick={() => {
                                onMergeButtonClicked(result.id, 'target');
                            }}
                            aria-label="Replace source with target"
                        />
                        {restoreApplicable ? (
                            <Button
                                className="result-restore-button"
                                variant="secondary"
                                disabled={anyRowMutationInFlight}
                                onClick={() => {
                                    onRestoreButtonClicked(result.id);
                                }}
                                aria-label="Restore deleted item"
                                title="Restore deleted item"
                            >
                                {'↺'}
                            </Button>
                        ) : null}
                    </div>
                </Col>
                <Col className="target-col">
                    <ResultItemDetails
                        reference={result.target ?? null}
                        reason={result.reason}
                        resolution={result.resolution}
                        expanded={expanded}
                        side="target"
                    />
                </Col>
            </Row>
            <Row className="expand-button-row" onClick={handleExpandButtonClicked}>
                <span className="row-expand-button-indicator">...</span>
            </Row>
        </ListGroup.Item>
    );
}

function resolveResultRowBanner(
    mergeRun: ResultMergeRunState | undefined,
    restoreRun: RestoreItemRunState | undefined,
    resultId: number,
    onDismissMergeError: (resultId: number) => void,
    onDismissRestoreError: (resultId: number) => void,
): ResolvedResultRowBanner | null {
    const mergeInFlight =
        mergeRun?.status === 'starting' || mergeRun?.status === 'running';
    const mergeError =
        mergeRun?.status === 'error' || mergeRun?.status === 'failed' ? mergeRun : null;
    const mergeCompleted = mergeRun?.status === 'completed';

    const restoreInFlight =
        restoreRun?.status === 'starting' || restoreRun?.status === 'running';
    const restoreError =
        restoreRun?.status === 'error' || restoreRun?.status === 'failed' ? restoreRun : null;
    const restoreCompleted = restoreRun?.status === 'completed';

    if (mergeInFlight && mergeRun) {
        const phase = mergeRun.progress?.phase ?? (mergeRun.status === 'starting' ? 'starting' : 'merging');
        const item = mergeRun.progress?.currentItem;
        return {
            identity: 'merge-running',
            role: 'status',
            variantClass: 'merge-status-running',
            message: `Merging (${mergeRun.strategy.toLowerCase()}): ${phase}${item ? ` — ${item}` : ''}`,
            dismissible: false,
        };
    }
    if (restoreInFlight && restoreRun) {
        const phase =
            restoreRun.progress?.phase ?? (restoreRun.status === 'starting' ? 'starting' : 'restoring');
        const item = restoreRun.progress?.currentItem;
        return {
            identity: 'restore-running',
            role: 'status',
            variantClass: 'merge-status-running',
            message: `Restoring: ${phase}${item ? ` — ${item}` : ''}`,
            dismissible: false,
        };
    }
    if (mergeError) {
        const code = mergeError.error?.code;
        const message = mergeError.error?.message ?? 'Merge failed';
        return {
            identity: 'merge-error',
            role: 'alert',
            variantClass: 'merge-status-error',
            message: `Merge failed${code !== undefined ? ` (${code})` : ''}: ${message}`,
            dismissible: true,
            onDismiss: () => onDismissMergeError(resultId),
        };
    }
    if (restoreError) {
        const code = restoreError.error?.code;
        const message = restoreError.error?.message ?? 'Restore failed';
        return {
            identity: 'restore-error',
            role: 'alert',
            variantClass: 'merge-status-error',
            message: `Restore failed${code !== undefined ? ` (${code})` : ''}: ${message}`,
            dismissible: true,
            onDismiss: () => onDismissRestoreError(resultId),
        };
    }
    if (mergeCompleted) {
        return {
            identity: 'merge-completed',
            role: 'status',
            variantClass: 'merge-status-completed',
            message: 'Merge complete.',
            dismissible: true,
            onDismiss: () => onDismissMergeError(resultId),
        };
    }
    if (restoreCompleted) {
        return {
            identity: 'restore-completed',
            role: 'status',
            variantClass: 'merge-status-completed',
            message: 'Restore complete.',
            dismissible: true,
            onDismiss: () => onDismissRestoreError(resultId),
        };
    }
    return null;
}

interface ResultRowStatusBannerSlotProps {
    mergeRun?: ResultMergeRunState;
    restoreRun?: RestoreItemRunState;
    resultId: number;
    onDismissMergeError: (resultId: number) => void;
    onDismissRestoreError: (resultId: number) => void;
}

function ResultRowStatusBannerSlot({
    mergeRun,
    restoreRun,
    resultId,
    onDismissMergeError,
    onDismissRestoreError,
}: ResultRowStatusBannerSlotProps) {
    const resolved = useMemo(
        () =>
            resolveResultRowBanner(
                mergeRun,
                restoreRun,
                resultId,
                onDismissMergeError,
                onDismissRestoreError,
            ),
        [mergeRun, restoreRun, resultId, onDismissMergeError, onDismissRestoreError],
    );

    const [slot, setSlot] = useState<{ active: BannerSlotEntry | null; leaving: BannerSlotEntry | null }>(
        () => ({
            active: resolved ? { identity: resolved.identity, banner: resolved } : null,
            leaving: null,
        }),
    );

    useLayoutEffect(() => {
        setSlot((prev) => {
            if (resolved === null) {
                if (prev.active === null) {
                    return prev.leaving ? { active: null, leaving: prev.leaving } : prev;
                }
                return { active: null, leaving: prev.active };
            }
            const nextEntry: BannerSlotEntry = { identity: resolved.identity, banner: resolved };
            if (prev.active === null) {
                return { active: nextEntry, leaving: prev.leaving };
            }
            if (prev.active.identity === resolved.identity) {
                if (!prev.leaving && prev.active.banner.message === resolved.message) {
                    return prev;
                }
                return { active: nextEntry, leaving: prev.leaving };
            }
            return { active: nextEntry, leaving: prev.active };
        });
    }, [resolved]);

    const clearLeaving = useCallback(() => {
        setSlot((s) => (s.leaving ? { ...s, leaving: null } : s));
    }, []);

    const showSlot = slot.active !== null || slot.leaving !== null;
    if (!showSlot) {
        return null;
    }

    return (
        <div className="result-status-banner-slot">
            {slot.leaving ? (
                <div
                    key={`${slot.leaving.identity}-out`}
                    className={`result-status-banner merge-status-exit ${slot.leaving.banner.variantClass}`}
                    role={slot.leaving.banner.role === 'alert' ? 'alert' : 'status'}
                    onAnimationEnd={(e) => {
                        if (e.target === e.currentTarget) {
                            clearLeaving();
                        }
                    }}
                >
                    <span className="result-status-banner-message">{slot.leaving.banner.message}</span>
                    {slot.leaving.banner.dismissible ? (
                        <button
                            type="button"
                            className="result-status-dismiss"
                            aria-label="Dismiss"
                            onClick={(e) => {
                                e.stopPropagation();
                                slot.leaving?.banner.onDismiss?.();
                            }}
                        >
                            <span aria-hidden="true">×</span>
                        </button>
                    ) : null}
                </div>
            ) : null}
            {slot.active ? (
                <div
                    key={`${slot.active.identity}-in`}
                    className={`result-status-banner merge-status-enter ${slot.active.banner.variantClass}`}
                    role={slot.active.banner.role === 'alert' ? 'alert' : 'status'}
                >
                    <span className="result-status-banner-message">{slot.active.banner.message}</span>
                    {slot.active.banner.dismissible ? (
                        <button
                            type="button"
                            className="result-status-dismiss"
                            aria-label="Dismiss"
                            onClick={(e) => {
                                e.stopPropagation();
                                slot.active?.banner.onDismiss?.();
                            }}
                        >
                            <span aria-hidden="true">×</span>
                        </button>
                    ) : null}
                </div>
            ) : null}
        </div>
    );
}

interface ResultItemDetailsProps {
    reference: FileReference | null;
    reason: AssuranceResultReason;
    resolution: AssuranceResultResolution;
    expanded: boolean;
    side: 'source' | 'target';
}

/** No path from the engine — treat as missing for display (same as null reference). */
function isReferenceMissing(reference: FileReference | null): boolean {
    if (reference === null) {
        return true;
    }
    const loc = reference.location;
    return loc === undefined || loc === '';
}

/**
 * Engine still sends a path for the absent side of SOURCE_DOES_NOT_EXIST /
 * TARGET_DOES_NOT_EXIST rows; {@link isReferenceMissing} alone is not enough.
 *
 * After a successful merge, {@code resolution} is updated (e.g.
 * {@code REPLACE_TARGET}) while {@code reason} stays the scan-time value; the
 * UI must not keep showing the missing glyph for a side that was just filled in.
 */
function isReasonMissingSide(
    reason: AssuranceResultReason,
    side: 'source' | 'target',
    resolution: AssuranceResultResolution,
): boolean {
    if (reason === 'SOURCE_DOES_NOT_EXIST' && side === 'source') {
        return resolution !== 'REPLACE_SOURCE';
    }
    if (reason === 'TARGET_DOES_NOT_EXIST' && side === 'target') {
        return resolution !== 'REPLACE_TARGET';
    }
    return false;
}

/**
 * After {@code DELETE_TARGET} / {@code DELETE_SOURCE}, the engine moves the
 * surviving file to deleted-items staging but keeps the original path in
 * {@code FILE_REFERENCE} so restore can put it back. The path no longer
 * resolves on disk — treat that side like missing in the UI (both sides can
 * then show the missing glyph for one-missing-one-present rows).
 */
function isDeletionMissingSide(resolution: AssuranceResultResolution, side: 'source' | 'target'): boolean {
    if (resolution === 'DELETE_TARGET' && side === 'target') {
        return true;
    }
    if (resolution === 'DELETE_SOURCE' && side === 'source') {
        return true;
    }
    return false;
}

function shouldShowMissingFileGraphic(
    reference: FileReference | null,
    reason: AssuranceResultReason,
    side: 'source' | 'target',
    resolution: AssuranceResultResolution,
): boolean {
    return (
        isReferenceMissing(reference) ||
        isReasonMissingSide(reason, side, resolution) ||
        isDeletionMissingSide(resolution, side)
    );
}

/** Document-with-X glyph: indicates the expected side has no file on disk. */
function MissingFileGraphic({ side, expectedPath }: { side: 'source' | 'target'; expectedPath?: string }) {
    const sideLabel = side === 'source' ? 'Source file missing' : 'Target file missing';
    const label = expectedPath ? `${sideLabel}: ${expectedPath}` : sideLabel;
    return (
        <div className="result-missing-file" role="img" aria-label={label} title={label}>
            <svg
                className="result-missing-file-svg"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 48 56"
                fill="none"
                aria-hidden="true"
            >
                <path
                    d="M10 4h18l12 12v36a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinejoin="round"
                />
                <path d="M28 4v12h12" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
                <path
                    d="M17 33l14 14m0-14L17 47"
                    stroke="currentColor"
                    strokeWidth="2.25"
                    strokeLinecap="round"
                />
            </svg>
        </div>
    );
}

/** Single-line summary text: ellipsis at minimum font size; native title tooltip when clipped. */
function TruncatingSummaryLine({
    text,
    className,
    expanded,
}: {
    text: string;
    className: string;
    expanded: boolean;
}) {
    const ref = useRef<HTMLParagraphElement>(null);
    const [overflowing, setOverflowing] = useState(false);

    const measure = useCallback(() => {
        const el = ref.current;
        if (!el || expanded) {
            setOverflowing(false);
            return;
        }
        setOverflowing(el.scrollWidth > el.clientWidth + 1);
    }, [expanded]);

    useLayoutEffect(() => {
        measure();
        const el = ref.current;
        if (!el || expanded) {
            return undefined;
        }
        const ro = new ResizeObserver(measure);
        ro.observe(el);
        return () => ro.disconnect();
    }, [text, expanded, measure]);

    const lineClass = expanded ? 'result-summary-line-expanded' : 'result-summary-line-clamped';

    return (
        <p
            ref={ref}
            className={`${className} ${lineClass}`.trim()}
            title={!expanded && overflowing ? text : undefined}
        >
            {text}
        </p>
    );
}

function ResultItemDetails({ reference, reason, resolution, expanded, side }: ResultItemDetailsProps) {
    const classes = ['result-item-details', `${side}-result-item`];
    if (expanded) {
        classes.push('expanded');
    }
    if (shouldShowMissingFileGraphic(reference, reason, side, resolution)) {
        const pathHint =
            reference?.location && reference.location.length > 0 ? reference.location : undefined;
        return (
            <div className={classes.join(' ')}>
                <MissingFileGraphic side={side} expectedPath={pathHint} />
            </div>
        );
    }
    const attrs = reference.fileAttributes;
    return (
        <div className={classes.join(' ')}>
            <TruncatingSummaryLine
                text={basename(reference.location)}
                className="file-name-field"
                expanded={expanded}
            />
            <TruncatingSummaryLine
                text={`Path: ${reference.location}`}
                className="file-attribute-field"
                expanded={expanded}
            />
            <TruncatingSummaryLine
                text={`Creation Time: ${formatDate(attrs?.creationTime)}`}
                className="file-attribute-field"
                expanded={expanded}
            />
            <TruncatingSummaryLine
                text={`Last Modified Time: ${formatDate(attrs?.lastModifiedTime)}`}
                className="file-attribute-field"
                expanded={expanded}
            />
            <Collapse in={expanded}>
                <div className="additional-file-attributes">
                    {renderAttribute('Directory', attrs?.isDirectory)}
                    {renderAttribute('Other', attrs?.isOther)}
                    {renderAttribute('Regular File', attrs?.isRegularFile)}
                    {renderAttribute('Symbolic Link', attrs?.isSymbolicLink)}
                    {renderAttribute('Last Access Time', formatDate(attrs?.lastAccessTime))}
                    {renderAttribute('File Size', formatSize(attrs?.size))}
                    {renderAttribute('Archive', attrs?.isArchive)}
                    {renderAttribute('Hidden', attrs?.isHidden)}
                    {renderAttribute('Read Only', attrs?.isReadOnly)}
                    {renderAttribute('System File', attrs?.isSystem)}
                    {renderAttribute('Group Name', attrs?.groupName)}
                    {renderAttribute('Owner', attrs?.owner)}
                    {renderAttribute('Permissions', attrs?.permissions)}
                    {renderAttribute('File Owner', attrs?.fileOwner)}
                    {renderAttribute('ACLs', attrs?.aclDescription)}
                    {renderAttribute('Contents Hash', attrs?.contentsHash)}
                    {renderAttribute('User-defined Attributes Hash', attrs?.userDefinedAttributesHash)}
                </div>
            </Collapse>
        </div>
    );
}

function renderAttribute(label: string, value: string | boolean | undefined) {
    return (
        <p className="file-attribute-field">
            {label}: {formatScalar(value)}
        </p>
    );
}

function formatScalar(value: string | boolean | undefined): string {
    if (value === undefined || value === null) {
        return '—';
    }
    if (typeof value === 'boolean') {
        return value ? 'Yes' : 'No';
    }
    return value === '' ? '—' : value;
}

function formatDate(iso: string | undefined): string {
    if (!iso) {
        return '—';
    }
    const parsed = new Date(iso);
    if (Number.isNaN(parsed.getTime())) {
        return iso;
    }
    return parsed.toLocaleString();
}

function formatSize(bytes: number | undefined): string {
    if (typeof bytes !== 'number' || !Number.isFinite(bytes)) {
        return '—';
    }
    if (bytes < 1024) {
        return `${bytes} B`;
    }
    const units = ['KB', 'MB', 'GB', 'TB'];
    let value = bytes;
    let unit = -1;
    do {
        value /= 1024;
        unit += 1;
    } while (value >= 1024 && unit < units.length - 1);
    return `${value.toFixed(value >= 10 ? 0 : 1)} ${units[unit]}`;
}

function basename(path: string): string {
    const trimmed = path.replace(/[\\/]+$/, '');
    const idx = Math.max(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'));
    return idx >= 0 ? trimmed.slice(idx + 1) : trimmed;
}
