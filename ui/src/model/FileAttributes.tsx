/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.FileAttributes}.
 *
 * Data carrier only: field names match the Java entity exactly; behaviour
 * lives on the engine side. All fields except {@code id} are optional because
 * the Java entity allows nulls (boxed wrappers) and not all attribute
 * categories are populated on every platform (DOS-only fields on macOS, etc.).
 *
 * Per {@code docs/ipc-contract.md} ("Timestamps are ISO 8601 strings in
 * UTC"), date fields are typed as ISO 8601 strings on the wire — the Java
 * entity stores {@code java.util.Date} but JSON-RPC payloads carry the
 * string form.
 *
 * The {@code fileReference} back-reference is intentionally omitted; on the
 * wire {@code FileAttributes} is always nested *inside* its
 * {@code FileReference} parent, so the back-reference would create a
 * cycle.
 */
interface FileAttributes {
    id: number;

    contentsHash?: string;

    creationTime?: string;

    isDirectory?: boolean;

    isOther?: boolean;

    isRegularFile?: boolean;

    isSymbolicLink?: boolean;

    lastAccessTime?: string;

    lastModifiedTime?: string;

    size?: number;

    isArchive?: boolean;

    isHidden?: boolean;

    isReadOnly?: boolean;

    isSystem?: boolean;

    groupName?: string;

    owner?: string;

    permissions?: string;

    fileOwner?: string;

    aclDescription?: string;

    userDefinedAttributesHash?: string;
}

export default FileAttributes;
