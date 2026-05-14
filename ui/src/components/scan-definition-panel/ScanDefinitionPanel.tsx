import { Col, Row, Container, Form, Button, FloatingLabel } from 'react-bootstrap';
import { useEffect, useMemo, useState } from 'react';

import PathSelector from '../path-selector/PathSelector';
import { engine, EngineCallError } from '../../api/engine';
import type ScanDefinition from '../../model/ScanDefinition';
import type AssuranceMergeStrategy from '../../model/enums/AssuranceMergeStrategy';

interface ScanDefinitionPanelProps {
    /**
     * The scan definition to edit. Pass an empty object (or one without `id`)
     * to start a create flow. Updates to this prop replace the form state.
     */
    scanDefinition: ScanDefinition | null | undefined;
    /**
     * Invoked after a successful save with the persisted definition (id and
     * scanMapping populated). The parent should use this to refresh any list
     * views and dismiss the panel.
     */
    onSaved: (saved: ScanDefinition) => void;
    /** Invoked when the user cancels without saving. */
    onCancelButtonClicked: () => void;
}

/**
 * Form state held locally while the user edits a definition. The 2.0 UI
 * shows a single source/target pair per definition; if the underlying entity
 * carries multiple mappings (from 1.x data), the first is loaded for edit
 * and the remainder are preserved verbatim on save (see
 * {@link buildSavePayload}).
 */
interface FormState {
    name: string;
    source: string;
    target: string;
    mergeStrategy: AssuranceMergeStrategy;
    autoResolveConflicts: boolean;
    includeNonCreationTimestamps: boolean;
    includeAdvancedAttributes: boolean;
}

const STRATEGY_ORDER: readonly AssuranceMergeStrategy[] = ['SOURCE', 'TARGET', 'BOTH'];

const STRATEGY_LABELS: Record<AssuranceMergeStrategy, string> = {
    SOURCE: 'Source',
    TARGET: 'Target',
    BOTH: 'Both',
};

function initialFormState(scanDefinition: ScanDefinition | null | undefined): FormState {
    const firstMapping = scanDefinition?.scanMapping?.[0];
    return {
        name: scanDefinition?.name ?? '',
        source: firstMapping?.source?.location ?? '',
        target: firstMapping?.target?.location ?? '',
        mergeStrategy: scanDefinition?.mergeStrategy ?? 'SOURCE',
        autoResolveConflicts: scanDefinition?.autoResolveConflicts ?? false,
        includeNonCreationTimestamps: scanDefinition?.includeNonCreationTimestamps ?? false,
        includeAdvancedAttributes: scanDefinition?.includeAdvancedAttributes ?? false,
    };
}

export default function ScanDefinitionPanel({
    scanDefinition,
    onSaved,
    onCancelButtonClicked,
}: ScanDefinitionPanelProps) {
    const isEdit = Boolean(scanDefinition?.id);
    const titleAction = isEdit ? 'Edit' : 'Add New';

    const [form, setForm] = useState<FormState>(() => initialFormState(scanDefinition));
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setForm(initialFormState(scanDefinition));
        setError(null);
    }, [scanDefinition]);

    const validation = useMemo(() => validateForm(form), [form]);

    const update = <K extends keyof FormState>(key: K, value: FormState[K]) => {
        setForm((prev) => ({ ...prev, [key]: value }));
    };

    async function handleConfirm() {
        if (!validation.ok) {
            setError(validation.message);
            return;
        }
        setSubmitting(true);
        setError(null);
        try {
            const saved = await engine.saveScanDefinition(buildSavePayload(scanDefinition, form));
            onSaved(saved);
        } catch (err) {
            if (err instanceof EngineCallError) {
                setError(`Engine error ${err.code}: ${err.message}`);
            } else {
                setError(err instanceof Error ? err.message : String(err));
            }
        } finally {
            setSubmitting(false);
        }
    }

    return (
        <Container className="scan-definition-panel">
            <Row>
                <span className="scan-definition-panel-title">{titleAction} Scan Definition</span>
            </Row>
            <Row>
                <Form onSubmit={(event) => { event.preventDefault(); void handleConfirm(); }}>
                    <Form.Group controlId="scanDefNameField">
                        <FloatingLabel label="Scan Name">
                            <Form.Control
                                className="scan-name-field"
                                placeholder="Scan Name"
                                value={form.name}
                                onChange={(event) => update('name', event.target.value)}
                                isInvalid={!validation.ok && !form.name.trim()}
                            />
                        </FloatingLabel>
                    </Form.Group>
                    <Container className="scan-locations-group">
                        <Form.Group controlId="scanDefSourceLocationField">
                            <PathSelector
                                label="Source Location"
                                additionalClasses={['source-location']}
                                dialogOptions={{ title: 'Select Source' }}
                                value={form.source}
                                onChange={(path) => update('source', path)}
                            />
                        </Form.Group>
                        <Form.Group controlId="scanDefTargetLocationField">
                            <PathSelector
                                label="Target Location"
                                additionalClasses={['target-location']}
                                dialogOptions={{ title: 'Select Target' }}
                                value={form.target}
                                onChange={(path) => update('target', path)}
                            />
                        </Form.Group>
                    </Container>
                    <Container className="scan-merge-options-group">
                        <Form.Group controlId="scanDefMergeStrategyField">
                            <FloatingLabel label="Merge Strategy">
                                <Form.Select
                                    className="scan-merge-strategy-field"
                                    size="sm"
                                    value={form.mergeStrategy}
                                    onChange={(event) =>
                                        update('mergeStrategy', event.target.value as AssuranceMergeStrategy)
                                    }
                                >
                                    {STRATEGY_ORDER.map((strategy) => (
                                        <option key={strategy} value={strategy}>
                                            {STRATEGY_LABELS[strategy]}
                                        </option>
                                    ))}
                                </Form.Select>
                            </FloatingLabel>
                        </Form.Group>
                        <Form.Group controlId="scanDefAutoMergeField">
                            <Form.Check
                                type="checkbox"
                                className="scan-auto-merge-field"
                                label="Automatically Resolve Conflicts"
                                checked={form.autoResolveConflicts}
                                onChange={(event) => update('autoResolveConflicts', event.target.checked)}
                            />
                        </Form.Group>
                        <Form.Group controlId="scanDefIncludeTimestampsField">
                            <Form.Check
                                type="checkbox"
                                className="scan-include-timestamps-field"
                                label="Include Timestamps Other Than Create Date"
                                checked={form.includeNonCreationTimestamps}
                                onChange={(event) => update('includeNonCreationTimestamps', event.target.checked)}
                            />
                        </Form.Group>
                        <Form.Group controlId="scanDefIncludeAdvancedAttributesField">
                            <Form.Check
                                type="checkbox"
                                className="scan-include-advanced-attributes-field"
                                label="Include Advanced Attributes"
                                checked={form.includeAdvancedAttributes}
                                onChange={(event) => update('includeAdvancedAttributes', event.target.checked)}
                            />
                        </Form.Group>
                    </Container>
                </Form>
            </Row>
            {error ? (
                <Row className="scan-definition-error">
                    <Col className="px-0">
                        <span role="alert">{error}</span>
                    </Col>
                </Row>
            ) : null}
            <Row className="spacer-row" />
            <Row className="actions-row">
                <Col className="px-0">
                    <Button
                        className="scan-definition-manage-button"
                        variant="secondary"
                        onClick={() => { void handleConfirm(); }}
                        disabled={submitting || !validation.ok}
                    >
                        {submitting ? 'Saving…' : 'OK'}
                    </Button>
                </Col>
                <Col className="spacer-col" />
                <Col className="px-0">
                    <Button
                        className="scan-definition-manage-button"
                        variant="secondary"
                        onClick={onCancelButtonClicked}
                        disabled={submitting}
                    >
                        Cancel
                    </Button>
                </Col>
            </Row>
        </Container>
    );
}

interface ValidationResult {
    ok: boolean;
    message: string | null;
}

function validateForm(form: FormState): ValidationResult {
    if (!form.name.trim()) {
        return { ok: false, message: 'Scan name is required.' };
    }
    if (!form.source.trim()) {
        return { ok: false, message: 'Source location is required.' };
    }
    if (!form.target.trim()) {
        return { ok: false, message: 'Target location is required.' };
    }
    return { ok: true, message: null };
}

/**
 * Builds the wire payload for {@code assurance.saveScanDefinition}. We update
 * the first mapping in place if one exists (preserving its id and any
 * exclusions) and append it as a new mapping otherwise. Additional mappings
 * beyond index 0 are passed through unchanged so that legacy 1.x data with
 * multiple mappings round-trips losslessly even though the 2.0 form only
 * exposes one pair.
 */
function buildSavePayload(
    original: ScanDefinition | null | undefined,
    form: FormState,
): ScanDefinition {
    const existingMappings = original?.scanMapping ?? [];
    const [first, ...rest] = existingMappings;
    const mergedFirst = {
        ...(first ?? {}),
        source: { ...(first?.source ?? {}), location: form.source.trim() },
        target: { ...(first?.target ?? {}), location: form.target.trim() },
    };
    return {
        ...(original ?? {}),
        id: original?.id,
        name: form.name.trim(),
        mergeStrategy: form.mergeStrategy,
        autoResolveConflicts: form.autoResolveConflicts,
        includeNonCreationTimestamps: form.includeNonCreationTimestamps,
        includeAdvancedAttributes: form.includeAdvancedAttributes,
        scanMapping: [mergedFirst, ...rest],
    } as ScanDefinition;
}
