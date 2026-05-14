import ComparisonResult from './ComparisonResult';
import ScanDefinition from './ScanDefinition';

/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.Scan}.
 *
 * Field naming mirrors the Java entity exactly (per
 * {@code docs/ipc-contract.md} "Domain payload field names are camelCase
 * and mirror the corresponding Java entity field names exactly"). Two
 * exceptions worth flagging:
 *
 *  - Timestamps are ISO 8601 strings on the wire (per the contract's
 *    "Timestamps are ISO 8601 strings in UTC" rule), so {@code scanStarted}
 *    and {@code whenCompleted} are typed as {@code string}, not {@code Date}.
 *    Convert to a Date at the rendering layer if needed.
 *  - {@code results} and {@code resultCount} are both optional. List-style
 *    endpoints (e.g. {@code assurance.loadScans}) project a thin
 *    {@code resultCount} scalar and omit the heavy {@code results} array;
 *    the per-scan endpoint ({@code assurance.loadScanResults}) returns the
 *    full results separately.
 *  - {@code scanDef} is optional and may be a thin reference (only
 *    {@code id} + {@code name}) when delivered by a list endpoint.
 */
interface Scan {
    id: number;

    /** ISO 8601 (UTC) timestamp string. Always present. */
    scanStarted: string;

    /**
     * ISO 8601 (UTC) timestamp string, or {@code null} for an in-flight or
     * aborted scan that never completed.
     */
    whenCompleted: string | null;

    /**
     * Reference to the scan definition this run belongs to. List-view
     * endpoints return a thin reference (only {@code id} + {@code name});
     * detail endpoints can return the full graph.
     */
    scanDef?: ScanDefinition;

    /**
     * Number of comparison results attached to the scan. Populated by list
     * endpoints (e.g. {@code assurance.loadScans}) so the UI can render a
     * column without round-tripping the full results.
     */
    resultCount?: number;

    /** Full comparison results. Omitted from list-view payloads. */
    results?: ComparisonResult[];
}

export default Scan;
