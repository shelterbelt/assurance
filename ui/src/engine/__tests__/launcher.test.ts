import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

import { buildEngineSpawnCommand } from '../launcher';

function expectedJvmArgs(): string[] {
  if (process.platform === 'darwin') {
    return ['-Dapple.awt.UIElement=true'];
  }
  return [];
}

test('bundled mode: spawns the bundled JRE binary against the shaded jar with cwd defaulting to user home', () => {
  const opts = {
    bundledJrePath: '/opt/assurance/engine/jre',
    bundledEngineJar: '/opt/assurance/engine/engine.jar',
  };
  const cmd = buildEngineSpawnCommand(opts);

  const expectedExe = path.join(
    '/opt/assurance/engine/jre',
    'bin',
    process.platform === 'win32' ? 'java.exe' : 'java',
  );
  expect(cmd.executable).toBe(expectedExe);
  expect(cmd.args).toEqual([
    ...expectedJvmArgs(),
    '-jar',
    '/opt/assurance/engine/engine.jar',
    '--headless',
    '--port=0',
  ]);
  // cwd MUST default to user home so the H2 DB lands at ~/.assurance/ matching
  // the 1.x location. Setting cwd=<jre> would write inside the read-only
  // .app bundle on macOS.
  expect(cmd.cwd).toBe(os.homedir());
});

test('bundled mode: explicit bundledCwd option overrides the user-home default', () => {
  const cmd = buildEngineSpawnCommand({
    bundledJrePath: '/opt/assurance/engine/jre',
    bundledEngineJar: '/opt/assurance/engine/engine.jar',
    bundledCwd: '/var/data/assurance-sandbox',
  });
  expect(cmd.cwd).toBe('/var/data/assurance-sandbox');
});

test('bundled mode: requires both bundledJrePath and bundledEngineJar (jre alone falls through to dev mode)', () => {
  const previous = process.env.ASSURANCE_ENGINE_DIR;
  process.env.ASSURANCE_ENGINE_DIR = '/nonexistent-engine-dir-for-test';
  try {
    expect(() => buildEngineSpawnCommand({ bundledJrePath: '/jre-only' })).toThrow(
      /Engine classes directory not found/,
    );
  } finally {
    if (previous === undefined) {
      delete process.env.ASSURANCE_ENGINE_DIR;
    } else {
      process.env.ASSURANCE_ENGINE_DIR = previous;
    }
  }
});

test('bundled mode: requires both bundledJrePath and bundledEngineJar (jar alone falls through to dev mode)', () => {
  const previous = process.env.ASSURANCE_ENGINE_DIR;
  process.env.ASSURANCE_ENGINE_DIR = '/nonexistent-engine-dir-for-test';
  try {
    expect(() => buildEngineSpawnCommand({ bundledEngineJar: '/jar-only.jar' })).toThrow(
      /Engine classes directory not found/,
    );
  } finally {
    if (previous === undefined) {
      delete process.env.ASSURANCE_ENGINE_DIR;
    } else {
      process.env.ASSURANCE_ENGINE_DIR = previous;
    }
  }
});

test('dev mode: surfaces a helpful error when target/classes is missing', () => {
  const previous = process.env.ASSURANCE_ENGINE_DIR;
  process.env.ASSURANCE_ENGINE_DIR = '/nonexistent-engine-dir-for-test';
  try {
    expect(() => buildEngineSpawnCommand({})).toThrow(/Engine classes directory not found/);
  } finally {
    if (previous === undefined) {
      delete process.env.ASSURANCE_ENGINE_DIR;
    } else {
      process.env.ASSURANCE_ENGINE_DIR = previous;
    }
  }
});

test('dev mode: includes platform JVM flags before classpath/main args', () => {
  const engineDir = fs.mkdtempSync(path.join(os.tmpdir(), 'assurance-launcher-test-'));
  try {
    fs.mkdirSync(path.join(engineDir, 'target', 'classes'), { recursive: true });
    fs.mkdirSync(path.join(engineDir, 'target', 'dependency_libs'), { recursive: true });

    const cmd = buildEngineSpawnCommand({
      engineDir,
      javaBin: '/usr/bin/java',
    });

    expect(cmd.executable).toBe('/usr/bin/java');
    expect(cmd.args.slice(0, expectedJvmArgs().length)).toEqual(expectedJvmArgs());

    const cpIndex = cmd.args.indexOf('-cp');
    expect(cpIndex).toBeGreaterThanOrEqual(0);
    expect(cmd.args[cpIndex + 2]).toBe('com.markallenjohnson.assurance.Application');
    expect(cmd.args.slice(-2)).toEqual(['--headless', '--port=0']);
    expect(cmd.cwd).toBe(engineDir);
  } finally {
    fs.rmSync(engineDir, { recursive: true, force: true });
  }
});
