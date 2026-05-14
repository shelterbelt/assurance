
import {
    INITIAL_RESULT_MERGE_STATE,
    hasAnyResultMergeInFlight,
    isResultMergeInFlight,
    resultMergeReducer,
    type ResultMergeAction,
    type ResultMergeRunState,
    type ResultMergeState,
} from '../resultMergeReducer';

function reduce(initial: ResultMergeState, actions: ResultMergeAction[]): ResultMergeState {
    return actions.reduce(resultMergeReducer, initial);
}

function expectRun(state: ResultMergeState, resultId: number): ResultMergeRunState {
    const run = state.runs[resultId];
    if (!run) {
        throw new Error(`expected a run for resultId=${resultId}`);
    }
    return run;
}

test('initial state is empty', () => {
    expect(INITIAL_RESULT_MERGE_STATE.runs).toEqual({});
    expect(hasAnyResultMergeInFlight(INITIAL_RESULT_MERGE_STATE)).toBe(false);
});

test('REQUEST → ACCEPTED → PROGRESS → COMPLETED happy path', () => {
    const result = reduce(INITIAL_RESULT_MERGE_STATE, [
        { type: 'REQUEST', resultId: 314, strategy: 'SOURCE' },
        { type: 'ACCEPTED', resultId: 314, scanId: 42 },
        {
            type: 'PROGRESS',
            resultId: 314,
            scanId: 42,
            progress: { phase: 'copying', bytesProcessed: 1024, bytesTotal: 4096 },
        },
        { type: 'COMPLETED', resultId: 314, scanId: 42 },
    ]);

    const run = expectRun(result, 314);
    expect(run.status).toBe('completed');
    expect(run.scanId).toBe(42);
    expect(run.strategy).toBe('SOURCE');
    expect(run.progress).toBe(null);
    expect(run.error).toBe(null);
});

test('REJECTED parks the entry in error', () => {
    const result = reduce(INITIAL_RESULT_MERGE_STATE, [
        { type: 'REQUEST', resultId: 7, strategy: 'TARGET' },
        {
            type: 'REJECTED',
            resultId: 7,
            error: { code: -32007, message: 'already running' },
        },
    ]);

    const run = expectRun(result, 7);
    expect(run.status).toBe('error');
    expect(run.error?.code).toBe(-32007);
    expect(run.error?.message).toBe('already running');
    expect(run.progress).toBe(null);
});

test('FAILED carries error and clears progress', () => {
    const result = reduce(INITIAL_RESULT_MERGE_STATE, [
        { type: 'REQUEST', resultId: 11, strategy: 'BOTH' },
        { type: 'ACCEPTED', resultId: 11, scanId: 99 },
        {
            type: 'PROGRESS',
            resultId: 11,
            scanId: 99,
            progress: { phase: 'copying', bytesProcessed: 1 },
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
    const result = resultMergeReducer(INITIAL_RESULT_MERGE_STATE, {
        type: 'STARTED',
        resultId: 50,
        scanId: 700,
    });

    const run = expectRun(result, 50);
    expect(run.status).toBe('running');
    expect(run.scanId).toBe(700);
});

test('COMPLETED for an unknown resultId still records terminal feedback', () => {
    const result = resultMergeReducer(INITIAL_RESULT_MERGE_STATE, {
        type: 'COMPLETED',
        resultId: 50,
        scanId: 700,
    });

    const run = expectRun(result, 50);
    expect(run.status).toBe('completed');
    expect(run.scanId).toBe(700);
});

test('isResultMergeInFlight is true while starting or running', () => {
    const requested = resultMergeReducer(INITIAL_RESULT_MERGE_STATE, {
        type: 'REQUEST',
        resultId: 1,
        strategy: 'SOURCE',
    });
    expect(isResultMergeInFlight(requested, 1)).toBe(true);
    expect(isResultMergeInFlight(requested, 999)).toBe(false);

    const running = resultMergeReducer(requested, {
        type: 'STARTED',
        resultId: 1,
        scanId: 88,
    });
    expect(isResultMergeInFlight(running, 1)).toBe(true);

    const completed = resultMergeReducer(running, {
        type: 'COMPLETED',
        resultId: 1,
        scanId: 88,
    });
    expect(isResultMergeInFlight(completed, 1)).toBe(false);
});

test('hasAnyResultMergeInFlight reflects across multiple entries', () => {
    let state = INITIAL_RESULT_MERGE_STATE;
    state = resultMergeReducer(state, { type: 'REQUEST', resultId: 1, strategy: 'SOURCE' });
    state = resultMergeReducer(state, { type: 'REQUEST', resultId: 2, strategy: 'TARGET' });
    expect(hasAnyResultMergeInFlight(state)).toBe(true);

    state = resultMergeReducer(state, { type: 'COMPLETED', resultId: 1, scanId: 10 });
    expect(hasAnyResultMergeInFlight(state)).toBe(true);

    state = resultMergeReducer(state, { type: 'COMPLETED', resultId: 2, scanId: 10 });
    expect(hasAnyResultMergeInFlight(state)).toBe(false);
});

test('DISMISS removes a single entry without affecting others', () => {
    let state = INITIAL_RESULT_MERGE_STATE;
    state = resultMergeReducer(state, { type: 'REQUEST', resultId: 1, strategy: 'SOURCE' });
    state = resultMergeReducer(state, { type: 'COMPLETED', resultId: 1, scanId: 10 });
    state = resultMergeReducer(state, { type: 'REQUEST', resultId: 2, strategy: 'TARGET' });
    state = resultMergeReducer(state, {
        type: 'FAILED',
        resultId: 2,
        scanId: 10,
        error: { code: -32001, message: 'boom' },
    });

    const after = resultMergeReducer(state, { type: 'DISMISS', resultId: 1 });
    expect(after.runs[1]).toBe(undefined);
    const surviving = expectRun(after, 2);
    expect(surviving.status).toBe('failed');
});

test('PROGRESS for unknown resultId is dropped (no synthetic entry)', () => {
    const result = resultMergeReducer(INITIAL_RESULT_MERGE_STATE, {
        type: 'PROGRESS',
        resultId: 1,
        scanId: 10,
        progress: { phase: 'copying' },
    });
    expect(result.runs).toEqual({});
});

test('concurrent merges across distinct resultIds are tracked independently', () => {
    let state = INITIAL_RESULT_MERGE_STATE;
    state = resultMergeReducer(state, { type: 'REQUEST', resultId: 1, strategy: 'SOURCE' });
    state = resultMergeReducer(state, { type: 'REQUEST', resultId: 2, strategy: 'TARGET' });
    state = resultMergeReducer(state, { type: 'ACCEPTED', resultId: 1, scanId: 10 });
    state = resultMergeReducer(state, { type: 'ACCEPTED', resultId: 2, scanId: 10 });

    expect(expectRun(state, 1).status).toBe('running');
    expect(expectRun(state, 2).status).toBe('running');

    state = resultMergeReducer(state, { type: 'COMPLETED', resultId: 1, scanId: 10 });
    expect(expectRun(state, 1).status).toBe('completed');
    expect(expectRun(state, 2).status).toBe('running');
});
