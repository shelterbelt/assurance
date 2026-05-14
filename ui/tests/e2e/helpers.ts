import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

const PROJECT_ROOT = path.resolve(__dirname, '..', '..');

/**
 * Resolves the packaged-app binary path for the current platform + arch.
 * Returns {@code null} when the binary is missing (typically when
 * {@code mvn -P!development,package-engine -DskipTests package} +
 * {@code yarn package} have not been run yet) so the caller can
 * {@code test.skip} with a helpful message rather than fail with a cryptic
 * spawn error.
 */
export function resolvePackagedBinary(): { binary: string; appDir: string } | null {
    const platform = process.platform;
    const arch = process.arch;
    const outDir = path.join(PROJECT_ROOT, 'out', `Assurance-${platform}-${arch}`);

    let binary: string;
    if (platform === 'darwin') {
        binary = path.join(outDir, 'Assurance.app', 'Contents', 'MacOS', 'Assurance');
    } else if (platform === 'win32') {
        binary = path.join(outDir, 'Assurance.exe');
    } else {
        binary = path.join(outDir, 'Assurance');
    }

    if (!fs.existsSync(binary)) {
        return null;
    }
    return { binary, appDir: outDir };
}

export const PACKAGE_PREREQ_HINT =
    "Packaged build not found under ui/out/. " +
    "Run `mvn '-P!development,package-engine' -DskipTests package` from the engine/ directory, " +
    'then `yarn package` from ui/ to produce it before running yarn test:e2e.';

/**
 * Layout produced by {@link createScanFixture}.
 *
 * - {@code root}: temp dir to use as {@code HOME} for the spawned engine.
 *   The engine resolves {@code ~/.assurance/assurance.mv.db} relative to
 *   this, so all H2 state and any restore-staging directories land inside
 *   the temp tree and the developer's real {@code ~/.assurance} is left
 *   untouched.
 * - {@code source} / {@code target}: divergent directories the scan should
 *   compare. Designed so the scan produces at least one result (a file
 *   present on one side but not the other).
 */
export interface ScanFixture {
    root: string;
    source: string;
    target: string;
}

export async function createScanFixture(): Promise<ScanFixture> {
    const root = await fs.promises.mkdtemp(path.join(os.tmpdir(), 'assurance-e2e-'));
    const source = path.join(root, 'source');
    const target = path.join(root, 'target');
    await fs.promises.mkdir(source);
    await fs.promises.mkdir(target);

    // Both sides agree on this file — exercises the equal-file path.
    await fs.promises.writeFile(path.join(source, 'shared.txt'), 'shared content\n');
    await fs.promises.writeFile(path.join(target, 'shared.txt'), 'shared content\n');

    // Source-only and target-only files — guarantee the engine produces
    // a non-empty result list for the assertion in the happy-path test.
    await fs.promises.writeFile(path.join(source, 'source-only.txt'), 'only on source\n');
    await fs.promises.writeFile(path.join(target, 'target-only.txt'), 'only on target\n');

    return { root, source, target };
}

export async function destroyScanFixture(fixture: ScanFixture): Promise<void> {
    await fs.promises.rm(fixture.root, { recursive: true, force: true });
}
