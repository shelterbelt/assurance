import { spawn, ChildProcess } from 'child_process';
import { EventEmitter } from 'events';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

const SHUTDOWN_GRACE_MS = 2000;
const SHUTDOWN_HARD_MS = 4000;
const READY_TIMEOUT_MS = 30_000;

/**
 * The engine prints exactly this line to stdout when its WebSocket listener is bound:
 *   ASSURANCE_READY ws://127.0.0.1:<port> v<protocolVersion>
 *
 * Defined in SPEC.md §3 "Lifecycle handshake protocol" and docs/ipc-contract.md.
 */
const READY_LINE_PATTERN = /^ASSURANCE_READY ws:\/\/127\.0\.0\.1:(\d+) v(\d+)\s*$/;

export interface EngineLaunchOptions {
  /** Dev mode: path to the assurance/ Maven project (used to locate target/classes + target/dependency_libs). */
  engineDir?: string;
  /** Dev mode: java executable name or absolute path. Defaults to the system 'java'. */
  javaBin?: string;
  /** Bundled mode: absolute path to the jlink-trimmed JRE root (its bin/java[.exe] is invoked). */
  bundledJrePath?: string;
  /** Bundled mode: absolute path to the shaded engine jar (Main-Class manifest entry expected). */
  bundledEngineJar?: string;
  /**
   * Bundled mode: cwd the JVM is launched from. Defaults to the user's home
   * directory ({@link os.homedir}) so the engine's H2 database — which is
   * declared as the relative path {@code ./.assurance/assurance} in
   * {@code database.properties} — lands at {@code ~/.assurance/} on every
   * platform, matching the legacy 1.x location and surviving app upgrades.
   */
  bundledCwd?: string;
}

export interface EngineReadyInfo {
  port: number;
  protocolVersion: number;
  url: string;
}

/**
 * Pure helper that builds the {@code child_process.spawn} arguments from the
 * supplied options. Two modes:
 *
 * - <em>Bundled</em>: when both {@link EngineLaunchOptions.bundledJrePath} and
 *   {@link EngineLaunchOptions.bundledEngineJar} are present, spawns
 *   {@code <jre>/bin/java[.exe] -jar <jar> --headless --port=0}. Used in
 *   packaged builds where the JRE + shaded jar live under
 *   {@code <process.resourcesPath>/engine/}.
 * - <em>Dev</em>: otherwise resolves {@code engineDir} (env override allowed)
 *   and builds a classpath from {@code target/classes} +
 *   {@code target/dependency_libs/*}, invoking the
 *   {@code com.markallenjohnson.assurance.Application} main class via the
 *   system or supplied {@code javaBin}.
 *
 * Extracted as a pure function so unit tests can verify both branches without
 * actually spawning a process.
 */
export function buildEngineSpawnCommand(opts: EngineLaunchOptions = {}): {
  executable: string;
  args: string[];
  cwd: string;
} {
  const jvmArgs = buildJvmArgs();
  if (opts.bundledJrePath && opts.bundledEngineJar) {
    const javaBinaryName = process.platform === 'win32' ? 'java.exe' : 'java';
    const executable = path.join(opts.bundledJrePath, 'bin', javaBinaryName);
    return {
      executable,
      args: [...jvmArgs, '-jar', opts.bundledEngineJar, '--headless', '--port=0'],
      // The H2 path in database.properties is relative ('./.assurance/assurance'),
      // so cwd is what controls where the database actually lives. Default to
      // the user's home so the packaged 2.0 app reads/writes the same
      // ~/.assurance/assurance.mv.db that the legacy 1.x Swing app used.
      // Setting cwd=<jre> would write the DB inside the read-only .app bundle.
      cwd: opts.bundledCwd ?? os.homedir(),
    };
  }

  const engineDir = resolveEngineDir(opts);
  const classpath = buildClasspath(engineDir);
  const javaBin = opts.javaBin ?? process.env.JAVA_BIN ?? 'java';
  return {
    executable: javaBin,
    args: [
      ...jvmArgs,
      '-cp',
      classpath,
      'com.markallenjohnson.assurance.Application',
      '--headless',
      '--port=0',
    ],
    cwd: engineDir,
  };
}

/**
 * JVM flags that suppress any accidental desktop/shell integration when the
 * engine is launched as a child process behind Electron.
 *
 * On macOS only, apple.awt.UIElement=true prevents the JVM from becoming a
 *   foreground Dock app (the "Duke" icon) if java.desktop is initialized.
 *
 * Do NOT set java.awt.headless=true here: even in --headless engine mode, the
 * legacy Spring XML still eagerly instantiates the Swing MainWindow bean during
 * context startup; forcing AWT headless makes that constructor throw
 * HeadlessException and the engine exits before printing ASSURANCE_READY.
 */
function buildJvmArgs(): string[] {
  if (process.platform === 'darwin') {
    return ['-Dapple.awt.UIElement=true'];
  }
  return [];
}

function resolveEngineDir(opts: EngineLaunchOptions): string {
  if (opts.engineDir) {
    return path.resolve(opts.engineDir);
  }
  if (process.env.ASSURANCE_ENGINE_DIR) {
    return path.resolve(process.env.ASSURANCE_ENGINE_DIR);
  }
  return path.resolve(process.cwd(), '..', '..', 'assurance');
}

function buildClasspath(engineDir: string): string {
  const classesDir = path.join(engineDir, 'target', 'classes');
  const dependencyCandidates = [
    path.join(engineDir, 'target', 'dependency_libs'),
    path.join(engineDir, 'target', 'dependency'),
  ];

  if (!fs.existsSync(classesDir)) {
    throw new Error(
      `Engine classes directory not found at ${classesDir}. ` +
      `Run 'mvn package -Pdevelopment' in the engine/ directory first.`,
    );
  }

  const dependencyDir = dependencyCandidates.find((dir) => fs.existsSync(dir));
  if (!dependencyDir) {
    throw new Error(
      `Engine dependency directory not found. Tried: ${dependencyCandidates.join(', ')}. ` +
      `Run 'mvn dependency:copy-dependencies -DoutputDirectory=target/dependency_libs' in the assurance/ directory first.`,
    );
  }

  const sep = process.platform === 'win32' ? ';' : ':';
  return [classesDir, path.join(dependencyDir, '*')].join(sep);
}

let engineProcess: ChildProcess | null = null;
let engineEvents: EventEmitter | null = null;
let pendingReady: Promise<EngineReadyInfo> | null = null;

/**
 * Splits an incoming chunk into complete lines, preserving any partial trailing
 * fragment for the next chunk. Returns the consumed lines and the new remainder.
 */
function consumeLines(buffer: string, chunk: string): { lines: string[]; remainder: string } {
  const combined = buffer + chunk;
  const parts = combined.split(/\r?\n/);
  const remainder = parts.pop() ?? '';
  return { lines: parts, remainder };
}

export function startEngine(opts: EngineLaunchOptions = {}): void {
  if (engineProcess) {
    throw new Error('Engine already started.');
  }

  const { executable, args, cwd } = buildEngineSpawnCommand(opts);
  const isBundled = Boolean(opts.bundledJrePath && opts.bundledEngineJar);
  console.log(
    `[engine] Spawning (${isBundled ? 'bundled' : 'dev'}): ${executable} ` +
    (isBundled
      ? `-jar <engine.jar> --headless --port=0`
      : `-cp <classpath> com.markallenjohnson.assurance.Application --headless --port=0`),
  );

  const proc = spawn(executable, args, {
    cwd,
    stdio: ['ignore', 'pipe', 'pipe'],
  });

  proc.stdout?.setEncoding('utf8');
  proc.stderr?.setEncoding('utf8');

  const events = new EventEmitter();
  engineEvents = events;

  let readySeen = false;
  let stdoutBuffer = '';

  proc.stdout?.on('data', (chunk: string) => {
    process.stdout.write(`[engine:out] ${chunk}`);

    if (readySeen) return;

    const { lines, remainder } = consumeLines(stdoutBuffer, chunk);
    stdoutBuffer = remainder;
    for (const line of lines) {
      const match = line.match(READY_LINE_PATTERN);
      if (match) {
        readySeen = true;
        const port = Number.parseInt(match[1], 10);
        const protocolVersion = Number.parseInt(match[2], 10);
        const info: EngineReadyInfo = {
          port,
          protocolVersion,
          url: `ws://127.0.0.1:${port}`,
        };
        events.emit('ready', info);
        break;
      }
    }
  });

  proc.stderr?.on('data', (chunk: string) => {
    process.stderr.write(`[engine:err] ${chunk}`);
  });

  proc.on('exit', (code, signal) => {
    console.log(`[engine] Process exited (code=${code}, signal=${signal}).`);
    if (engineProcess === proc) {
      engineProcess = null;
      engineEvents = null;
      pendingReady = null;
    }
    if (!readySeen) {
      events.emit('exit-before-ready', { code, signal });
    }
    events.emit('exit', { code, signal });
  });

  proc.on('error', (err) => {
    console.error('[engine] Failed to spawn engine process:', err);
    events.emit('error', err);
    if (engineProcess === proc) {
      engineProcess = null;
      engineEvents = null;
      pendingReady = null;
    }
  });

  engineProcess = proc;
  pendingReady = null;
}

/**
 * Resolves with the engine's ready info as soon as the
 * `ASSURANCE_READY ws://127.0.0.1:<port> v<protocolVersion>` line is observed
 * on stdout, or rejects if the engine exits or errors before then, or if the
 * ready line is not observed within {@link READY_TIMEOUT_MS}.
 */
export function whenEngineReady(timeoutMs: number = READY_TIMEOUT_MS): Promise<EngineReadyInfo> {
  if (!engineProcess || !engineEvents) {
    return Promise.reject(new Error('Engine has not been started.'));
  }
  if (pendingReady) {
    return pendingReady;
  }

  const events = engineEvents;
  pendingReady = new Promise<EngineReadyInfo>((resolve, reject) => {
    let settled = false;

    const onReady = (info: EngineReadyInfo) => {
      if (settled) return;
      settled = true;
      cleanup();
      resolve(info);
    };
    const onExit = (info: { code: number | null; signal: NodeJS.Signals | null }) => {
      if (settled) return;
      settled = true;
      cleanup();
      reject(new Error(
        `Engine process exited before printing the ready line ` +
        `(code=${info.code}, signal=${info.signal}).`,
      ));
    };
    const onError = (err: Error) => {
      if (settled) return;
      settled = true;
      cleanup();
      reject(err);
    };
    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      cleanup();
      reject(new Error(`Engine did not signal ready within ${timeoutMs}ms.`));
    }, timeoutMs);

    function cleanup() {
      clearTimeout(timer);
      events.off('ready', onReady);
      events.off('exit-before-ready', onExit);
      events.off('error', onError);
    }

    events.once('ready', onReady);
    events.once('exit-before-ready', onExit);
    events.once('error', onError);
  });

  return pendingReady;
}

export function stopEngine(): Promise<void> {
  const proc = engineProcess;
  if (!proc || proc.exitCode !== null || proc.signalCode !== null) {
    engineProcess = null;
    engineEvents = null;
    pendingReady = null;
    return Promise.resolve();
  }

  return new Promise<void>((resolve) => {
    let settled = false;
    const finish = () => {
      if (settled) return;
      settled = true;
      if (engineProcess === proc) {
        engineProcess = null;
        engineEvents = null;
        pendingReady = null;
      }
      resolve();
    };

    proc.once('exit', finish);

    proc.kill('SIGTERM');

    const sigkillTimer = setTimeout(() => {
      if (settled) return;
      console.warn(`[engine] Engine did not exit within ${SHUTDOWN_GRACE_MS}ms; sending SIGKILL.`);
      proc.kill('SIGKILL');
    }, SHUTDOWN_GRACE_MS);

    const safetyTimer = setTimeout(() => {
      console.warn(`[engine] Engine still alive after ${SHUTDOWN_HARD_MS}ms; abandoning wait.`);
      finish();
    }, SHUTDOWN_HARD_MS);

    proc.once('exit', () => {
      clearTimeout(sigkillTimer);
      clearTimeout(safetyTimer);
    });
  });
}

export function getEngineProcess(): ChildProcess | null {
  return engineProcess;
}
