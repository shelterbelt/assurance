import * as fs from 'fs';
import * as path from 'path';
import type { ForgeConfig } from '@electron-forge/shared-types';
import { MakerSquirrel } from '@electron-forge/maker-squirrel';
import { MakerZIP } from '@electron-forge/maker-zip';
import { MakerDeb } from '@electron-forge/maker-deb';
import { MakerRpm } from '@electron-forge/maker-rpm';
import { AutoUnpackNativesPlugin } from '@electron-forge/plugin-auto-unpack-natives';
import { WebpackPlugin } from '@electron-forge/plugin-webpack';

import { mainConfig } from './webpack.main.config';
import { rendererConfig } from './webpack.renderer.config';

/**
 * Path to the host-shaped engine bundle produced by
 * {@code mvn -P!development,package-engine package} in the engine/ project.
 * The Maven profile writes both artifacts (engine.jar + jre/) under
 * {@code target/engine-bundle/current/} on the host that built it; the
 * packageAfterCopy hook below copies that directory into the staged Electron
 * app's {@code resources/engine/} so the launcher can find them at
 * {@code <process.resourcesPath>/engine/} in the packaged build.
 */
const ENGINE_BUNDLE_DIR = path.resolve(
  __dirname,
  '..',
  'engine',
  'target',
  'engine-bundle',
  'current',
);

async function copyDir(src: string, dst: string): Promise<void> {
  await fs.promises.mkdir(dst, { recursive: true });
  const entries = await fs.promises.readdir(src, { withFileTypes: true });
  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const dstPath = path.join(dst, entry.name);
    if (entry.isDirectory()) {
      await copyDir(srcPath, dstPath);
    } else if (entry.isSymbolicLink()) {
      const linkTarget = await fs.promises.readlink(srcPath);
      await fs.promises.symlink(linkTarget, dstPath);
    } else {
      await fs.promises.copyFile(srcPath, dstPath);
    }
  }
}

/**
 * Verifies that the bundle at {@code ENGINE_BUNDLE_DIR} was produced on a host
 * compatible with the platform we're packaging for. The bundle is host-shaped
 * (a macOS-built JRE has {@code jre/bin/java}, a Windows-built JRE has
 * {@code jre/bin/java.exe}, and they are not interchangeable). Without this
 * check, feeding a darwin bundle to a {@code yarn make} run on Windows (or
 * vice-versa) silently produces a broken installer that fails at first launch.
 *
 * The check is structural — we don't parse {@code jre/release}; we just look
 * for the binary the launcher will try to spawn at runtime.
 */
function assertBundleMatchesPlatform(bundleDir: string, platform: string): void {
  const winJava = path.join(bundleDir, 'jre', 'bin', 'java.exe');
  const unixJava = path.join(bundleDir, 'jre', 'bin', 'java');
  const expectedWin = platform === 'win32';
  const expectedPath = expectedWin ? winJava : unixJava;
  const oppositePath = expectedWin ? unixJava : winJava;
  if (!fs.existsSync(expectedPath)) {
    if (fs.existsSync(oppositePath)) {
      throw new Error(
        `Engine bundle at ${bundleDir} was produced on a different host than this ` +
        `yarn make run (target platform=${platform}). Found ${oppositePath} but ` +
        `expected ${expectedPath}. Re-run "mvn -P!development,package-engine ` +
        `-DskipTests package" on a ${platform} host before invoking electron-forge.`,
      );
    }
    throw new Error(
      `Engine bundle at ${bundleDir} is missing ${expectedPath}. ` +
      `The jlink output is incomplete or corrupted; re-run ` +
      `"mvn -P!development,package-engine -DskipTests package" in the engine/ directory.`,
    );
  }
}

async function packageAfterCopy(
  _forgeConfig: ForgeConfig,
  buildPath: string,
  _electronVersion: string,
  platform: string,
  arch: string,
): Promise<void> {
  const expectedJar = path.join(ENGINE_BUNDLE_DIR, 'engine.jar');
  const expectedJre = path.join(ENGINE_BUNDLE_DIR, 'jre');
  if (!fs.existsSync(expectedJar) || !fs.existsSync(expectedJre)) {
    throw new Error(
      `Engine bundle not found at ${ENGINE_BUNDLE_DIR}. ` +
      `Run "mvn -P!development,package-engine -DskipTests package" in the engine/ ` +
      `directory on this host (${platform}-${arch}) before invoking electron-forge.`,
    );
  }
  assertBundleMatchesPlatform(ENGINE_BUNDLE_DIR, platform);
  // buildPath is the staging "resources/app/" directory (or
  // Contents/Resources/app/ on macOS); resources/engine/ is its sibling.
  const targetEngineDir = path.resolve(buildPath, '..', 'engine');
  await fs.promises.rm(targetEngineDir, { recursive: true, force: true });
  await copyDir(ENGINE_BUNDLE_DIR, targetEngineDir);
  console.log(`[forge] Copied engine bundle from ${ENGINE_BUNDLE_DIR} to ${targetEngineDir}`);
}

const config: ForgeConfig = {
  packagerConfig: {
    asar: true,
    icon: './assets/assurance',
  },
  rebuildConfig: {},
  makers: [
    new MakerSquirrel({
      name: 'Assurance',
      setupIcon: './assets/assurance.ico',
      authors: 'Mark Johnson',
      description: 'Cross-platform application to analyze and synchronize the contents of file system directories.',
    }),
    new MakerZIP({}, ['darwin']),
    new MakerRpm({}),
    new MakerDeb({}),
  ],
  hooks: {
    packageAfterCopy,
  },
  plugins: [
    new AutoUnpackNativesPlugin({}),
    new WebpackPlugin({
      mainConfig,
      renderer: {
        config: rendererConfig,
        entryPoints: [
          {
            html: './src/index.html',
            js: './src/renderer.ts',
            name: 'main_window',
            preload: {
              js: './src/preload.ts',
            },
          },
        ],
      },
    }),
  ],
};

export default config;
