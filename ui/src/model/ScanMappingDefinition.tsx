import FileReference from "./FileReference";
import ScanDefinition from "./ScanDefinition";

/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.ScanMappingDefinition}.
 *
 * Field names match the persisted Java entity. Per the IPC contract, lazy
 * back-pointers ({@code scanDefinition}) and child collections that are not
 * always serialized ({@code exclusions}) are marked optional. The id is
 * omitted on create payloads and present on responses.
 *
 * Note: {@code source} and {@code target} are {@link FileReference} objects
 * (each carrying a {@code location} string), not bare strings, to match the
 * Java entity field types — see {@code docs/ipc-contract.md}.
 */
interface ScanMappingDefinition {
    id?: number;

    scanDefinition?: ScanDefinition;

    source?: FileReference;

    target?: FileReference;

    exclusions?: FileReference[];
}

export default ScanMappingDefinition;
