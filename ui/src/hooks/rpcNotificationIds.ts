/**
 * Normalizes JSON-RPC notification ids that may arrive as numbers or
 * decimal-string forms from the engine / IPC boundary.
 */
export function parseFiniteResultId(value: unknown): number | null {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return Math.trunc(value);
    }
    if (typeof value === 'string') {
        const t = value.trim();
        if (t.length > 0 && /^-?\d+(\.\d+)?$/.test(t)) {
            const n = Number(t);
            return Number.isFinite(n) ? Math.trunc(n) : null;
        }
    }
    return null;
}

/**
 * Reads {@code resultId} and {@code scanId} from a notification {@code params}
 * object. Returns {@code null} when either field is missing or not numeric.
 */
export function parseResultScanPair(params: unknown): { resultId: number; scanId: number } | null {
    if (!params || typeof params !== 'object' || Array.isArray(params)) {
        return null;
    }
    const o = params as Record<string, unknown>;
    const resultId = parseFiniteResultId(o.resultId);
    const scanId = parseFiniteResultId(o.scanId);
    if (resultId === null || scanId === null) {
        return null;
    }
    return { resultId, scanId };
}
