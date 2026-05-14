
import {
    INITIAL_RESTORE_ITEM_STATE,
    hasAnyRestoreInFlight,
    isRestoreInFlight,
    restoreItemReducer,
    type RestoreItemAction,
    type RestoreItemRunState,
    type RestoreItemState,
} from '../restoreItemReducer';

function reduce(initial: RestoreItemState, actions: RestoreItemAction[]): RestoreItemState {
    return actions.reduce(restoreItemReducer, initial);
}

function expectRun(state: RestoreItemState, resultId: number): RestoreItemRunState {
    const run = state.runs[resultId];
    if (!run) {
        throw new Error(`expected a run for resultId=${resultId}`);
    }
    return run;
}

test('initial state is empty', () => {
    expect(INITIAL_RESTORE_ITEM_STATE.runs).toEqual({});
    expect(hasAnyRestoreInFlight(INITIAL_RESTORE_ITEM_STATE)).toBe(false);
});

test('REQUEST → ACCEPTED → PROGRESS → COMPLETED happy path', () => {
    const result = reduce(INITIAL_RESTORE_ITEM_STATE, [
        { type: 'REQUEST', resultId: 314 },
        { type: 'ACCEPTED', resultId: 314, scanId: 42 },
        {
            type: 'PROGRESS',
            resultId: 314,
            scanId: 42,
            progress: { phase: 'restoring', bytesProcessed: 1024, bytesTotal: 4096 },
        },
        { type: 'COMPLETED', resultId: 314, scanId: 42 },
    ]);

    const run = expectRun(result, 314);
    expect(run.status).toBe('completed');
    expect(run.scanId).toBe(42);
    expect(run.progress).toBe(null);
    expect(run.error).toBe(null);
});

test('REJECTED parks the entry in error', () => {
    const result = reduce(INITIAL_RESTORE_ITEM_STATE, [
        { type: 'REQUEST', resultId: 7 },
        {
            type: 'REJECTED',
            resultId: 7,
            error: { code: -32008, message: 'already running' },
        },
    ]);

    const run = expectRun(result, 7);
    expect(run.status).toBe('error');
    expect(run.error?.code).toBe(-32008);
    expect(run.error?.message).toBe('already running');
    expect(run.progress).toBe(null);
});

test('FAILED carries error and clears progress', () => {
    const result = reduce(INITIAL_RESTORE_ITEM_STATE, [
        { type: 'REQUEST', resultId: 11 },
        { type: 'ACCEPTED', resultId: 11, scanId: 99 },
        {
            type: 'PROGRESS',
            resultId: 11,
            scanId: 99,
            progress: { phase: 'restoring', bytesProcessed: 1 },
        },
        {
            type: 'FAILED',
            resultId: 11,
            scanId: 99,
            error: { code: -32001, message: 'disk full' },
        },
    ]);

    const run = expectRun(result, 11);
    expect(run.status).toBe('failed');
    expect(run.error?.code).toBe(-32001);
    expect(run.progress).toBe(null);
});

test('STARTED without a prior REQUEST creates a synthetic running entry', () => {
    const result = restoreItemReducer(INITIAL_RESTORE_ITEM_STATE, {
        type: 'STARTED',
        resultId: 50,
        scanId: 700,
    });

    const run = expectRun(result, 50);
    expect(run.status).toBe('running');
    expect(run.scanId).toBe(700);
});

test('COMPLETED for an unknown resultId still records terminal feedback', () => {
    const result = restoreItemReducer(INITIAL_RESTORE_ITEM_STATE, {
        type: 'COMPLETED',
        resultId: 50,
        scanId: 700,
    });

    const run = expectRun(result, 50);
    expect(run.status).toBe('completed');
    expect(run.scanId).toBe(700);
});

test('isRestoreInFlight is true while starting or running', () => {
    const requested = restoreItemReducer(INITIAL_RESTORE_ITEM_STATE, {
        type: 'REQUEST',
        resultId: 1,
    });
    expect(isRestoreInFlight(requested, 1)).toBe(true);
    expect(isRestoreInFlight(requested, 999)).toBe(false);

    const running = restoreItemReducer(requested, {
        type: 'STARTED',
        resultId: 1,
        scanId: 88,
    });
    expect(isRestoreInFlight(running, 1)).toBe(true);

    const completed = restoreItemReducer(running, {
        type: 'COMPLETED',
        resultId: 1,
        scanId: 88,
    });
    expect(isRestoreInFlight(completed, 1)).toBe(false);
});

test('hasAnyRestoreInFlight reflects across multiple entries', () => {
    let state = INITIAL_RESTORE_ITEM_STATE;
    state = restoreItemReducer(state, { type: 'REQUEST', resultId: 1 });
    state = restoreItemReducer(state, { type: 'REQUEST', resultId: 2 });
    expect(hasAnyRestoreInFlight(state)).toBe(true);

    state = restoreItemReducer(state, { type: 'COMPLETED', resultId: 1, scanId: 10 });
    expect(hasAnyRestoreInFlight(state)).toBe(true);

    state = restoreItemReducer(state, { type: 'COMPLETED', resultId: 2, scanId: 10 });
    expect(hasAnyRestoreInFlight(state)).toBe(false);
});

test('DISMISS removes a single entry without affecting others', () => {
    let state = INITIAL_RESTORE_ITEM_STATE;
    state = restoreItemReducer(state, { type: 'REQUEST', resultId: 1 });
    state = restoreItemReducer(state, { type: 'COMPLETED', resultId: 1, scanId: 10 });
    state = restoreItemReducer(state, { type: 'REQUEST', resultId: 2 });
    state = restoreItemReducer(state, {
        type: 'FAILED',
        resultId: 2,
        scanId: 10,
        error: { code: -32001, message: 'boom' },
    });

    const after = restoreItemReducer(state, { type: 'DISMISS', resultId: 1 });
    expect(after.runs[1]).toBe(undefined);
    const surviving = expectRun(after, 2);
    expect(surviving.status).toBe('failed');
});

test('PROGRESS for unknown resultId is dropped (no synthetic entry)', () => {
    const result = restoreItemReducer(INITIAL_RESTORE_ITEM_STATE, {
        type: 'PROGRESS',
        resultId: 1,
        scanId: 10,
        progress: { phase: 'restoring' },
    });
    expect(result.runs).toEqual({});
});

test('concurrent restores across distinct resultIds are tracked independently', () => {
    let state = INITIAL_RESTORE_ITEM_STATE;
    state = restoreItemReducer(state, { type: 'REQUEST', resultId: 1 });
    state = restoreItemReducer(state, { type: 'REQUEST', resultId: 2 });
    state = restoreItemReducer(state, { type: 'ACCEPTED', resultId: 1, scanId: 10 });
    state = restoreItemReducer(state, { type: 'ACCEPTED', resultId: 2, scanId: 10 });

    expect(expectRun(state, 1).status).toBe('running');
    expect(expectRun(state, 2).status).toBe('running');

    state = restoreItemReducer(state, { type: 'COMPLETED', resultId: 1, scanId: 10 });
    expect(expectRun(state, 1).status).toBe('completed');
    expect(expectRun(state, 2).status).toBe('running');
});
