
import {
    INITIAL_SCAN_RUN_STATE,
    isScanInFlight,
    scanRunReducer,
    type ScanRunAction,
    type ScanRunState,
} from '../scanRunReducer';

function reduce(initial: ScanRunState, actions: ScanRunAction[]): ScanRunState {
    return actions.reduce(scanRunReducer, initial);
}

test('initial state is idle with no run details', () => {
    expect(INITIAL_SCAN_RUN_STATE.status).toBe('idle');
    expect(INITIAL_SCAN_RUN_STATE.scanDefinitionId).toBe(null);
    expect(INITIAL_SCAN_RUN_STATE.scanDefinitionName).toBe(null);
    expect(INITIAL_SCAN_RUN_STATE.scanId).toBe(null);
    expect(INITIAL_SCAN_RUN_STATE.progress).toBe(null);
    expect(INITIAL_SCAN_RUN_STATE.resultCount).toBe(null);
    expect(INITIAL_SCAN_RUN_STATE.error).toBe(null);
    expect(isScanInFlight(INITIAL_SCAN_RUN_STATE)).toBe(false);
});

test('REQUEST → ACCEPTED → PROGRESS → COMPLETED happy path', () => {
    const result = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'ACCEPTED', scanDefinitionId: 11, scanId: 42 },
        { type: 'PROGRESS', scanId: 42, progress: { phase: 'comparing', itemsProcessed: 3, itemsTotal: 5 } },
        { type: 'COMPLETED', scanId: 42, resultCount: 7 },
    ]);

    expect(result.status).toBe('completed');
    expect(result.scanDefinitionId).toBe(11);
    expect(result.scanDefinitionName).toBe(null);
    expect(result.scanId).toBe(42);
    expect(result.resultCount).toBe(7);
    expect(result.progress).toBe(null);
    expect(result.error).toBe(null);
});

test('REQUEST followed by STARTED notification (out-of-order ACCEPTED) still transitions to running', () => {
    const result = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'STARTED', scanId: 42 },
    ]);

    expect(result.status).toBe('running');
    expect(result.scanId).toBe(42);
});

test('isScanInFlight is true while starting or running', () => {
    const starting = scanRunReducer(INITIAL_SCAN_RUN_STATE, { type: 'REQUEST', scanDefinitionId: 1 });
    expect(isScanInFlight(starting)).toBe(true);

    const running = scanRunReducer(starting, { type: 'STARTED', scanId: 99 });
    expect(isScanInFlight(running)).toBe(true);

    const completed = scanRunReducer(running, { type: 'COMPLETED', scanId: 99, resultCount: 0 });
    expect(isScanInFlight(completed)).toBe(false);
});

test('REJECTED parks state in error and clears scanId', () => {
    const after = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        {
            type: 'REJECTED',
            scanDefinitionId: 11,
            error: { code: -32003, message: 'already running' },
        },
    ]);

    expect(after.status).toBe('error');
    expect(after.scanId).toBe(null);
    expect(after.error?.code).toBe(-32003);
    expect(after.error?.message).toBe('already running');
});

test('FAILED carries error details and clears progress', () => {
    const after = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'STARTED', scanId: 42 },
        { type: 'PROGRESS', scanId: 42, progress: { phase: 'comparing' } },
        { type: 'FAILED', scanId: 42, error: { code: -32001, message: 'boom' } },
    ]);

    expect(after.status).toBe('failed');
    expect(after.scanId).toBe(42);
    expect(after.progress).toBe(null);
    expect(after.error?.code).toBe(-32001);
});

test('stale notifications targeting a different scanId are ignored', () => {
    const running = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'ACCEPTED', scanDefinitionId: 11, scanId: 42 },
    ]);

    const afterStaleProgress = scanRunReducer(running, {
        type: 'PROGRESS',
        scanId: 99,
        progress: { phase: 'old' },
    });
    expect(afterStaleProgress.progress).toBe(null);

    const afterStaleCompleted = scanRunReducer(running, {
        type: 'COMPLETED',
        scanId: 99,
        resultCount: 0,
    });
    expect(afterStaleCompleted.status).toBe('running');
    expect(afterStaleCompleted.resultCount).toBe(null);
});

test('STARTED arriving after a terminal state is ignored', () => {
    const completed = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'ACCEPTED', scanDefinitionId: 11, scanId: 42 },
        { type: 'COMPLETED', scanId: 42, resultCount: 1 },
    ]);

    const stale = scanRunReducer(completed, { type: 'STARTED', scanId: 42 });
    expect(stale.status).toBe('completed');
});

test('RESET returns the reducer to idle', () => {
    const failed = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'ACCEPTED', scanDefinitionId: 11, scanId: 42 },
        { type: 'FAILED', scanId: 42, error: { code: -32001, message: 'boom' } },
    ]);

    const reset = scanRunReducer(failed, { type: 'RESET' });
    expect(reset).toEqual(INITIAL_SCAN_RUN_STATE);
});

test('REQUEST after a terminal state flushes prior run details', () => {
    const completed = reduce(INITIAL_SCAN_RUN_STATE, [
        { type: 'REQUEST', scanDefinitionId: 11 },
        { type: 'ACCEPTED', scanDefinitionId: 11, scanId: 42 },
        { type: 'COMPLETED', scanId: 42, resultCount: 7 },
    ]);

    const next = scanRunReducer(completed, { type: 'REQUEST', scanDefinitionId: 12 });
    expect(next.status).toBe('starting');
    expect(next.scanDefinitionId).toBe(12);
    expect(next.scanId).toBe(null);
    expect(next.resultCount).toBe(null);
});

test('REQUEST carries optional scan definition display name', () => {
    const after = scanRunReducer(INITIAL_SCAN_RUN_STATE, {
        type: 'REQUEST',
        scanDefinitionId: 3,
        scanDefinitionName: '  Nightly  ',
    });
    expect(after.scanDefinitionName).toBe('Nightly');
});

test('ACCEPTED for a different definition than the current REQUEST is ignored', () => {
    const starting = scanRunReducer(INITIAL_SCAN_RUN_STATE, {
        type: 'REQUEST',
        scanDefinitionId: 11,
    });
    const after = scanRunReducer(starting, {
        type: 'ACCEPTED',
        scanDefinitionId: 99,
        scanId: 42,
    });

    expect(after.scanId).toBe(null);
    expect(after.status).toBe('starting');
});
