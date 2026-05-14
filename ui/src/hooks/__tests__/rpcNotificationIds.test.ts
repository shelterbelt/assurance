import { parseFiniteResultId, parseResultScanPair } from '../rpcNotificationIds';

describe('parseFiniteResultId', () => {
    test('accepts finite numbers', () => {
        expect(parseFiniteResultId(42)).toBe(42);
        expect(parseFiniteResultId(3.9)).toBe(3);
    });

    test('accepts integer-like strings', () => {
        expect(parseFiniteResultId('314')).toBe(314);
        expect(parseFiniteResultId('  12  ')).toBe(12);
    });

    test('rejects non-numeric', () => {
        expect(parseFiniteResultId(null)).toBeNull();
        expect(parseFiniteResultId(undefined)).toBeNull();
        expect(parseFiniteResultId('')).toBeNull();
        expect(parseFiniteResultId('abc')).toBeNull();
        expect(parseFiniteResultId(NaN)).toBeNull();
        expect(parseFiniteResultId(Infinity)).toBeNull();
    });
});

describe('parseResultScanPair', () => {
    test('parses string ids from notification params', () => {
        expect(parseResultScanPair({ resultId: '12', scanId: '34' })).toEqual({ resultId: 12, scanId: 34 });
    });

    test('parses mixed number and string', () => {
        expect(parseResultScanPair({ resultId: 99, scanId: '100' })).toEqual({ resultId: 99, scanId: 100 });
    });

    test('returns null when either id is missing or invalid', () => {
        expect(parseResultScanPair({ resultId: 1 })).toBeNull();
        expect(parseResultScanPair({ scanId: 2 })).toBeNull();
        expect(parseResultScanPair({ resultId: 'x', scanId: 1 })).toBeNull();
        expect(parseResultScanPair(null)).toBeNull();
        expect(parseResultScanPair([])).toBeNull();
    });
});
