import AssuranceMergeStrategy from "./enums/AssuranceMergeStrategy";
import ScanMappingDefinition from "./ScanMappingDefinition";

/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.ScanDefinition}.
 *
 * {@code scanMapping} is optional in type positions because some in-memory
 * or partial payloads may omit it; {@code assurance.loadScanDefinitions}
 * returns full mappings for editing.
 */
interface ScanDefinition {
    id: number;

    name: string;

    mergeStrategy?: AssuranceMergeStrategy;

    autoResolveConflicts?: boolean;

    includeNonCreationTimestamps?: boolean;

    includeAdvancedAttributes?: boolean;

    scanMapping?: ScanMappingDefinition[];
}

export default ScanDefinition;
