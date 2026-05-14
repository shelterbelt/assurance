import { render, screen, fireEvent } from '@testing-library/react';
import type { ComponentProps } from 'react';

import ScanList from '../ScanList';
import type ScanDefinition from '../../../model/ScanDefinition';

function makeScanDefinition(id: number, name: string): ScanDefinition {
    return {
        id,
        name,
        mergeStrategy: 'SOURCE',
        autoResolveConflicts: false,
        includeNonCreationTimestamps: false,
        includeAdvancedAttributes: false,
        scanMapping: [],
    };
}

function renderReadyList(overrides: Partial<ComponentProps<typeof ScanList>> = {}) {
    const baseScans = [makeScanDefinition(1, 'Daily backup')];
    return render(
        <ScanList
            scans={baseScans}
            selectedScan={0}
            loadState="ready"
            errorMessage={null}
            onScanSelected={() => undefined}
            onNewButtonClick={() => undefined}
            onDeleteButtonClick={() => undefined}
            onRetry={() => undefined}
            {...overrides}
        />,
    );
}

test('double-clicking a scan definition requests edit mode for that definition', () => {
    const onScanEditRequested = jest.fn();
    renderReadyList({ onScanEditRequested });

    const row = screen.getByText('Daily backup');
    fireEvent.doubleClick(row);

    expect(onScanEditRequested).toHaveBeenCalledTimes(1);
    expect(onScanEditRequested.mock.calls[0][0]).toMatchObject({
        id: 1,
        name: 'Daily backup',
    });
});

