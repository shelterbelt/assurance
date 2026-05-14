/**
 * TypeScript mirror of {@code com.markallenjohnson.assurance.model.entities.ApplicationConfiguration}.
 *
 * Data carrier only: field names match the Java entity exactly. The persisted
 * {@code ignoredFileNames} and {@code ignoredFileExtensions} columns are
 * comma-separated strings; the engine exposes parsed list views via
 * {@code ignoredFileNamesCollection} / {@code ignoredFileExtensionsCollection}
 * which mirror the Java {@code @Transient} fields. The list views are marked
 * optional because the engine may or may not include them on the wire.
 */
interface ApplicationConfiguration {
    id: number;

    ignoredFileNames?: string;

    ignoredFileExtensions?: string;

    numberOfScanThreads?: number;

    ignoredFileNamesCollection?: string[];

    ignoredFileExtensionsCollection?: string[];
}

export default ApplicationConfiguration;
