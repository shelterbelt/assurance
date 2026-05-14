import FileAttributes from "./FileAttributes";
import ScanMappingDefinition from "./ScanMappingDefinition";

/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.FileReference}.
 *
 * Data carrier only: field names match the Java entity exactly. The Java
 * entity's transient {@code File} accessor is intentionally omitted — the
 * persisted {@code location} string is the authoritative representation on the
 * wire, and any platform-specific path interpretation belongs on the engine.
 */
interface FileReference {
    id: number;

    location?: string;

    fileAttributes?: FileAttributes;

    scanMappingDefinition?: ScanMappingDefinition;
}

export default FileReference;
