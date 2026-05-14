import type FileReference from './FileReference';
import type AssuranceResultReason from './enums/AssuranceResultReason';
import type AssuranceResultResolution from './enums/AssuranceResultResolution';

/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.ComparisonResult}.
 *
 * Field naming and shape mirror the Java entity exactly per
 * {@code docs/ipc-contract.md}. The {@code scan} back-reference is
 * intentionally omitted: on the wire a {@code ComparisonResult} is always
 * delivered inside a parent context (either a {@code Scan} or a
 * {@code loadScanResults} response keyed on {@code scanId}), so the
 * back-reference would create a cycle.
 *
 * {@code source} / {@code target} are present whenever the engine has a
 * file reference to project. They may carry an inline {@link FileAttributes}
 * payload when the engine has computed it; consumers must tolerate
 * {@code source}/{@code target} or their {@code fileAttributes} being
 * absent (e.g. for a {@code SOURCE_DOES_NOT_EXIST} or
 * {@code TARGET_DOES_NOT_EXIST} result).
 */
interface ComparisonResult {
    id: number;

    source?: FileReference;

    target?: FileReference;

    reason: AssuranceResultReason;

    resolution: AssuranceResultResolution;

    /**
     * Empty string when no error has been recorded; never {@code null} on
     * the wire (the Java setter trims to 255 characters and the column has
     * a NOT NULL default).
     */
    resolutionError?: string;
}

export default ComparisonResult;
