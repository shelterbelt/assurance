import { Modal, Form, Button, FloatingLabel, Alert } from 'react-bootstrap';
import { useEffect, useState } from 'react';

import { engine, EngineCallError } from '../../api/engine';
import type ApplicationConfiguration from '../../model/ApplicationConfiguration';

interface SettingsPanelProps {
    show: boolean;
    onClose: () => void;
}

interface FormState {
    ignoredFileNames: string;
    ignoredFileExtensions: string;
    numberOfScanThreads: number;
}

const DEFAULT_FORM: FormState = {
    ignoredFileNames: '',
    ignoredFileExtensions: '',
    numberOfScanThreads: 4,
};

const MIN_THREADS = 2;
const MAX_THREADS = 32;

function configToForm(config: ApplicationConfiguration): FormState {
    return {
        ignoredFileNames: config.ignoredFileNames ?? '',
        ignoredFileExtensions: config.ignoredFileExtensions ?? '',
        numberOfScanThreads: config.numberOfScanThreads ?? DEFAULT_FORM.numberOfScanThreads,
    };
}

export default function SettingsPanel({ show, onClose }: SettingsPanelProps) {
    const [config, setConfig] = useState<ApplicationConfiguration | null>(null);
    const [form, setForm] = useState<FormState>(DEFAULT_FORM);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!show) {
            return;
        }
        let cancelled = false;
        setLoading(true);
        setError(null);
        engine
            .loadApplicationConfiguration()
            .then((loaded) => {
                if (cancelled) {
                    return;
                }
                setConfig(loaded);
                setForm(configToForm(loaded));
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }
                if (err instanceof EngineCallError) {
                    setError(`Engine error ${err.code}: ${err.message}`);
                } else {
                    setError(err instanceof Error ? err.message : String(err));
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false);
                }
            });
        return () => {
            cancelled = true;
        };
    }, [show]);

    const update = <K extends keyof FormState>(key: K, value: FormState[K]) => {
        setForm((prev) => ({ ...prev, [key]: value }));
    };

    const threadsInRange =
        Number.isInteger(form.numberOfScanThreads)
            && form.numberOfScanThreads >= MIN_THREADS
            && form.numberOfScanThreads <= MAX_THREADS;
    const canSave = !loading && !saving && threadsInRange;

    async function handleConfirm() {
        if (!canSave) {
            return;
        }
        setSaving(true);
        setError(null);
        try {
            const payload: ApplicationConfiguration = {
                ...(config ?? ({} as ApplicationConfiguration)),
                id: config?.id ?? 0,
                ignoredFileNames: form.ignoredFileNames,
                ignoredFileExtensions: form.ignoredFileExtensions,
                numberOfScanThreads: form.numberOfScanThreads,
            };
            const saved = await engine.saveApplicationConfiguration(payload);
            setConfig(saved);
            onClose();
        } catch (err) {
            if (err instanceof EngineCallError) {
                setError(`Engine error ${err.code}: ${err.message}`);
            } else {
                setError(err instanceof Error ? err.message : String(err));
            }
        } finally {
            setSaving(false);
        }
    }

    function handleCancel() {
        if (saving) {
            return;
        }
        onClose();
    }

    return (
        <Modal show={show} onHide={handleCancel} backdrop="static" centered>
            <Modal.Header closeButton={!saving}>
                <Modal.Title>Settings</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                {loading ? (
                    <div role="status">Loading…</div>
                ) : (
                    <Form
                        onSubmit={(event) => {
                            event.preventDefault();
                            void handleConfirm();
                        }}
                    >
                        <Form.Group controlId="settingsIgnoredFileNamesField" className="mb-3">
                            <FloatingLabel label="Ignored Files">
                                <Form.Control
                                    placeholder=".DS_Store, Thumbs.db"
                                    value={form.ignoredFileNames}
                                    onChange={(event) => update('ignoredFileNames', event.target.value)}
                                    disabled={saving}
                                />
                            </FloatingLabel>
                            <Form.Text className="text-muted">
                                Comma-separated file names. Matched files are skipped during scanning.
                            </Form.Text>
                        </Form.Group>
                        <Form.Group controlId="settingsIgnoredFileExtensionsField" className="mb-3">
                            <FloatingLabel label="Ignored File Extensions">
                                <Form.Control
                                    placeholder="*.tmp, *.bak"
                                    value={form.ignoredFileExtensions}
                                    onChange={(event) => update('ignoredFileExtensions', event.target.value)}
                                    disabled={saving}
                                />
                            </FloatingLabel>
                            <Form.Text className="text-muted">
                                Comma-separated extensions. Matched files are skipped during scanning.
                            </Form.Text>
                        </Form.Group>
                        <Form.Group controlId="settingsNumberOfScanThreadsField" className="mb-3">
                            <FloatingLabel label="Number of Threads">
                                <Form.Control
                                    type="number"
                                    min={MIN_THREADS}
                                    max={MAX_THREADS}
                                    step={1}
                                    value={form.numberOfScanThreads}
                                    onChange={(event) => {
                                        const parsed = Number.parseInt(event.target.value, 10);
                                        update(
                                            'numberOfScanThreads',
                                            Number.isFinite(parsed) ? parsed : DEFAULT_FORM.numberOfScanThreads,
                                        );
                                    }}
                                    isInvalid={!threadsInRange}
                                    disabled={saving}
                                />
                            </FloatingLabel>
                            <Form.Text className="text-muted">
                                Worker threads used during a scan. {MIN_THREADS}–{MAX_THREADS}.
                            </Form.Text>
                        </Form.Group>
                    </Form>
                )}
                {error ? (
                    <Alert variant="danger" role="alert" className="mt-2">
                        {error}
                    </Alert>
                ) : null}
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={handleCancel} disabled={saving}>
                    Cancel
                </Button>
                <Button variant="primary" onClick={() => void handleConfirm()} disabled={!canSave}>
                    {saving ? 'Saving…' : 'OK'}
                </Button>
            </Modal.Footer>
        </Modal>
    );
}
