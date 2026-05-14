import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';

import { engine } from '../../../api/engine';
import type ScanDefinition from '../../../model/ScanDefinition';
import { INITIAL_SCAN_RUN_STATE } from '../../../hooks/scanRunReducer';
import ScanDefinitionPanel from '../../scan-definition-panel/ScanDefinitionPanel';
import ScanPanel from '../ScanPanel';

function EditHarness() {
    const [editing, setEditing] = useState<ScanDefinition | null>(null);

    if (editing) {
        return (
            <ScanDefinitionPanel
                scanDefinition={editing}
                onSaved={() => setEditing(null)}
                onCancelButtonClicked={() => setEditing(null)}
            />
        );
    }

    return (
        <ScanPanel
            onNewButtonClicked={() => undefined}
            onEditScanDefinitionClicked={(def) => setEditing(def)}
            scanDefinitionsVersion={0}
            scanRunState={INITIAL_SCAN_RUN_STATE}
        />
    );
}

const scanWithMappings: ScanDefinition = {
    id: 1,
    name: 'Daily backup',
    mergeStrategy: 'SOURCE',
    autoResolveConflicts: false,
    includeNonCreationTimestamps: false,
    includeAdvancedAttributes: false,
    scanMapping: [
        {
            id: 10,
            source: { id: 101, location: '/data/source' },
            target: { id: 102, location: '/data/target' },
        },
    ],
};

beforeEach(() => {
    jest.spyOn(engine, 'loadScanDefinitions').mockResolvedValue([scanWithMappings]);
});

afterEach(() => {
    jest.restoreAllMocks();
});

test('after definitions load, the first scan is selected and focused', async () => {
    render(
        <ScanPanel
            onNewButtonClicked={() => undefined}
            scanDefinitionsVersion={0}
            scanRunState={INITIAL_SCAN_RUN_STATE}
        />,
    );

    const row = await screen.findByRole('link', { name: 'Daily backup' });
    await waitFor(() => {
        expect(row).toHaveClass('active');
        expect(row).toHaveFocus();
    });
});

test('edit scan shows source and target paths returned by loadScanDefinitions', async () => {
    const user = userEvent.setup();
    render(<EditHarness />);

    await screen.findByText('Daily backup');
    await user.dblClick(screen.getByText('Daily backup'));

    expect(await screen.findByPlaceholderText('Scan Name')).toHaveValue('Daily backup');
    expect(screen.getByPlaceholderText('Source Location')).toHaveValue('/data/source');
    expect(screen.getByPlaceholderText('Target Location')).toHaveValue('/data/target');
});
