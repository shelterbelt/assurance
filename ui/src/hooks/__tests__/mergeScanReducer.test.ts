
import {
    INITIAL_MERGE_SCAN_STATE,
    hasAnyMergeScanInFlight,
    isMergeScanInFlight,
    mergeScanReducer,
    type MergeScanAction,
    type MergeScanRunState,
    type MergeScanState,
} from '../mergeScanReducer';

function reduce(initial: MergeScanState, actions: MergeScanAction[]): MergeScanState {
    return actions.reduce(mergeScanReducer, initial);
}

function expectRun(state: MergeScanState, scanId: number): MergeScanRunState {
    const run = state.runs[scanId];
    if (!run) {
        throw new Error(`expected a run for scanId=${scanId}`);
    }
    return run;
}

test('initial state is empty', () => {
    expect(INITIAL_MERGE_SCAN_STATE.runs).toEqual({});
    expect(hasAnyMergeScanInFlight(INITIAL_MERGE_SCAN_STATE)).toBe(false);
});

test('REQUEST → ACCEPTED → PROGRESS → COMPLETED happy path', () => {
    const result = reduce(INITIAL_MERGE_SCAN_STATE, [
        { type: 'REQUEST', scanId: 42 },
        { type: 'ACCEPTED', scanId: 42 },
        {
            type: 'PROGRESS',
            scanId: 42,
            progress: { phase: 'merging', currentItem: 'a.txt', itemsProcessed: 1 },
        },
        { type: 'COMPLETED', scanId: 42, itemsMerged: 17 },
    ]);

    const run = expectRun(result, 42);
    expect(run.status).toBe('completed');
    expect(run.itemsMerged).toBe(17);
    expect(run.progress).toBe(null);
    expect(run.error).toBe(null);
});

test('REJECTED parks the entry in error', () => {
    const result = reduce(INITIAL_MERGE_SCAN_STATE, [
        { type: 'REQUEST', scanId: 7 },
        {
            type: 'REJECTED',
            scanId: 7,
            error: { code: -32006, message: 'already running' },
        },
    ]);

    const run = expectRun(result, 7);
    expect(run.status).toBe('error');
    expect(run.error?.code).toBe(-32006);
    expect(run.error?.message).toBe('already running');
    expect(run.progress).toBe(null);
});

test('FAILED carries error and clears progress', () => {
    const result = reduce(INITIAL_MERGE_SCAN_STATE, [
        { type: 'REQUEST', scanId: 99 },
        { type: 'ACCEPTED', scanId: 99 },
        {
            type: 'PROGRESS',
            scanId: 99,
            progress: { phase: 'merging', currentItem: 'b.txt' },
        },
        {
            type: 'FAILED',
            scanId: 99,
            error: { code: -32001, message: 'disk full' },
        },
    ]);

    const run = expectRun(result, 99);
    expect(run.status).toBe('failed');
    expect(run.error?.code).toBe(-32001);
    expect(run.progress).toBe(null);
});

test('STARTED without a prior REQUEST creates a synthetic running entry', () => {
    const result = mergeScanReducer(INITIAL_MERGE_SCAN_STATE, {
        type: 'STARTED',
        scanId: 700,
    });

    const run = expectRun(result, 700);
    expect(run.status).toBe('running');
});

test('COMPLETED for an unknown scanId still records terminal feedback', () => {
    const result = mergeScanReducer(INITIAL_MERGE_SCAN_STATE, {
        type: 'COMPLETED',
        scanId: 700,
        itemsMerged: 3,
    });

    const run = expectRun(result, 700);
    expect(run.status).toBe('completed');
    expect(run.itemsMerged).toBe(3);
});

test('isMergeScanInFlight is true while starting or running', () => {
    const requested = mergeScanReducer(INITIAL_MERGE_SCAN_STATE, {
        type: 'REQUEST',
        scanId: 1,
    });
    expect(isMergeScanInFlight(requested, 1)).toBe(true);
    expect(isMergeScanInFlight(requested, 999)).toBe(false);

    const running = mergeScanReducer(requested, {
        type: 'STARTED',
        scanId: 1,
    });
    expect(isMergeScanInFlight(running, 1)).toBe(true);

    const completed = mergeScanReducer(running, {
        type: 'COMPLETED',
        scanId: 1,
        itemsMerged: 0,
    });
    expect(isMergeScanInFlight(completed, 1)).toBe(false);
});

test('hasAnyMergeScanInFlight reflects across multiple entries', () => {
    let state = INITIAL_MERGE_SCAN_STATE;
    state = mergeScanReducer(state, { type: 'REQUEST', scanId: 1 });
    state = mergeScanReducer(state, { type: 'REQUEST', scanId: 2 });
    expect(hasAnyMergeScanInFlight(state)).toBe(true);

    state = mergeScanReducer(state, { type: 'COMPLETED', scanId: 1, itemsMerged: 0 });
    expect(hasAnyMergeScanInFlight(state)).toBe(true);

    state = mergeScanReducer(state, { type: 'COMPLETED', scanId: 2, itemsMerged: 5 });
    expect(hasAnyMergeScanInFlight(state)).toBe(false);
});

test('DISMISS removes a single entry without affecting others', () => {
    let state = INITIAL_MERGE_SCAN_STATE;
    state = mergeScanReducer(state, { type: 'REQUEST', scanId: 1 });
    state = mergeScanReducer(state, { type: 'COMPLETED', scanId: 1, itemsMerged: 0 });
    state = mergeScanReducer(state, { type: 'REQUEST', scanId: 2 });
    state = mergeScanReducer(state, {
        type: 'FAILED',
        scanId: 2,
        error: { code: -32001, message: 'boom' },
    });

    const after = mergeScanReducer(state, { type: 'DISMISS', scanId: 1 });
    expect(after.runs[1]).toBe(undefined);
    const surviving = expectRun(after, 2);
    expect(surviving.status).toBe('failed');
});

test('PROGRESS for unknown scanId is dropped (no synthetic entry)', () => {
    const result = mergeScanReducer(INITIAL_MERGE_SCAN_STATE, {
        type: 'PROGRESS',
        scanId: 1,
        progress: { phase: 'merging' },
    });
    expect(result.runs).toEqual({});
});

test('concurrent merges across distinct scanIds are tracked independently', () => {
    let state = INITIAL_MERGE_SCAN_STATE;
    state = mergeScanReducer(state, { type: 'REQUEST', scanId: 1 });
    state = mergeScanReducer(state, { type: 'REQUEST', scanId: 2 });
    state = mergeScanReducer(state, { type: 'ACCEPTED', scanId: 1 });
    state = mergeScanReducer(state, { type: 'ACCEPTED', scanId: 2 });

    expect(expectRun(state, 1).status).toBe('running');
    expect(expectRun(state, 2).status).toBe('running');

    state = mergeScanReducer(state, { type: 'COMPLETED', scanId: 1, itemsMerged: 4 });
    expect(expectRun(state, 1).status).toBe('completed');
    expect(expectRun(state, 2).status).toBe('running');
});
