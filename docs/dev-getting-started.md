# Assurance 2.0 — Developer Getting Started

This document covers what you need to run Assurance 2.0 from source during development.

## Prerequisites

- **JDK 17+** on `PATH` (or set `JAVA_BIN`).
- **Maven 3.8+** on `PATH`.
- **Node.js 18+** and **Yarn Classic (1.x)** on `PATH`.

## One-time engine prep

The Electron app launches the Java engine as a child process. In dev mode, the launcher resolves the engine from the local Maven build output. Build it once after a fresh checkout (and re-run after Java code changes):

```bash
cd engine
mvn package -Pdevelopment -DskipTests
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency_libs
```

This populates:
- `engine/target/classes/` — compiled engine classes
- `engine/target/dependency_libs/` — runtime jars (Spring, Hibernate, log4j, …)

(`dependency_libs` matches the convention used by the existing Windows build profiles. The launcher also accepts `target/dependency/` as a fallback.)

The Electron launcher (`ui/src/engine/launcher.ts`) builds its classpath from these two directories.

## Running the UI

```bash
cd ui
yarn install     # first time only
yarn start
```

The Electron main process spawns the Java engine in headless mode (`--headless --port=0`) on app startup and shuts it down on quit. Engine stdout/stderr is forwarded to the Electron main process's console with `[engine:out]` / `[engine:err]` prefixes.

You should see, near the top of the console output:

```
[engine] Spawning: java -cp <classpath> com.markallenjohnson.assurance.Application --headless --port=0
[engine:out] ASSURANCE_READY ws://127.0.0.1:54321 v1
[engine] Ready on ws://127.0.0.1:54321 (protocolVersion=1).
[engine] Connected. engineVersion=2.0.0, protocolVersion=1.
```

The port is OS-assigned and will differ per launch.

## Verifying end-to-end IPC

In dev builds (`yarn start`) the main window includes a small "Ping engine (dev)"
button at the bottom of the content area. Click it to round-trip
`assurance.ping` from the renderer through the main process, the WebSocket
transport, and back from the Java engine. A successful click renders something
like:

```
pong=true at 2026-05-05T20:30:40.123Z
```

The button is gated on `process.env.NODE_ENV !== 'production'` and is therefore
absent from packaged builds.

## Verifying the first feature vertical (load scan definitions)

The "Scan" tab's left-hand list now reads its data from the live engine via
`assurance.loadScanDefinitions`. Three states are observable:

- **Loading** — "Loading scan definitions…" appears briefly while the call is in flight.
- **Ready** — A `ListGroup` of scan-definition names. A fresh database renders
  the empty-state placeholder ("No scan definitions yet. Click *New* to create one.")
  instead of an empty list.
- **Error** — On engine error (typically JSON-RPC `-32001`), the panel shows
  the error message and a **Retry** button that re-issues the call.

You can manually exercise the error state by killing the engine process while
the app is running (`pkill -f markallenjohnson.assurance.Application`) and then
clicking **Retry**.

## H2 database location and freshness

The engine stores its H2 database under `<cwd>/.assurance/assurance.mv.db`
relative to the directory the JVM is launched from (the H2 URL in
`engine/src/main/resources/properties/database.properties` is the relative
path `jdbc:h2:./.assurance/assurance`). The Electron launcher sets cwd
differently in each mode:

- **Dev mode** (`yarn start`): cwd = `engine/` (the Maven project root). DB
  lives at `engine/.assurance/assurance.mv.db` and is wiped/recreated
  per dev session.
- **Packaged mode** (the installed `.app` / `.exe` / `.deb` / `.rpm`): cwd =
  the user's home directory (`os.homedir()`). DB lives at
  `~/.assurance/assurance.mv.db`, matching the **legacy 1.x location** so a
  user upgrading from 1.x sees their existing scan history. (Setting cwd to
  the bundle's resource directory would write inside the read-only `.app` —
  fixed in T4.5.)

If you have a stale H2 file from a prior major-version (typical error:
*"Unsupported database file version"*), move it aside before launching:

```bash
# dev mode
mv engine/.assurance/assurance.mv.db engine/.assurance/assurance.mv.db.bak

# packaged mode
mv ~/.assurance/assurance.mv.db ~/.assurance/assurance.mv.db.bak
```

A fresh schema will be installed automatically on next launch.

## Verifying engine teardown

To confirm the Java engine is terminated when the app quits:

```bash
# in another terminal, while the app is running
ps -ef | grep markallenjohnson.assurance.Application | grep -v grep

# quit the app, then re-run
ps -ef | grep markallenjohnson.assurance.Application | grep -v grep
```

After quit, no matching process should remain.

## Troubleshooting

### `NoClassDefFoundError: com/fasterxml/jackson/databind/JsonNode` (or any other dependency) at engine startup

`mvn clean ...` (including `mvn clean test`) wipes `target/`, which removes
`target/dependency_libs/`. The launcher's classpath build silently uses the
empty directory, and Spring fails to introspect the IPC beans because their
runtime dependencies are gone. Re-run the dependency copy:

```bash
cd engine
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency_libs
```

The same applies if `target/classes/` is missing — re-run
`mvn package -Pdevelopment -DskipTests` from the `engine/` directory.

## Environment overrides

| Variable | Purpose |
|---|---|
| `ASSURANCE_ENGINE_DIR` | Absolute path to the `engine/` directory. Overrides the default lookup relative to `process.cwd()`. |
| `JAVA_BIN` | Path to the `java` executable. Defaults to `java` on `PATH`. |

## Packaging (production build)

Packaged builds bundle a trimmed JRE + a shaded engine jar so the installed app does **not** require a system Java install.

The pipeline is two-step on every platform:

```bash
# 1. Build the engine bundle for the current host (target/engine-bundle/current/).
cd engine
mvn '-P!development,package-engine' -DskipTests package

# 2. Package the Electron app, which copies the bundle into resources/engine/.
cd ../ui
yarn make
```

The Maven `package-engine` profile produces, on the host that runs it:

- `engine/target/engine-bundle/current/engine.jar` — shaded fat jar (Main-Class = `com.markallenjohnson.assurance.Application`, all runtime deps inlined).
- `engine/target/engine-bundle/current/jre/` — `jlink`-trimmed runtime image.

`forge.config.ts`'s `packageAfterCopy` hook copies that directory verbatim into the staged Electron app at `resources/engine/`. Before copying it asserts that the bundle's `jre/bin/java[.exe]` matches the platform `electron-forge` is packaging for, so a mistakenly-cross-host bundle (e.g. a macOS bundle being packaged into a Windows installer) fails fast with a clear error rather than silently shipping a broken app.

The launcher (`src/engine/launcher.ts`) detects packaged mode via `app.isPackaged` and spawns `<resources>/engine/jre/bin/java[.exe] -jar <resources>/engine/engine.jar --headless --port=0`.

Bundles are **host-shaped**: a macOS `target/engine-bundle/current/` works only inside a macOS Electron build, a Linux one only inside a Linux build, a Windows one only inside a Windows build. Run `mvn ... package-engine` on each host before invoking `yarn make` for that host. (No cross-compilation in this release.) Re-run the Maven step after any Java code change before re-packaging.

### Per-platform recipes

#### macOS

**Tooling:** JDK 17 (with `jlink` — Temurin / Liberica / Oracle all ship it), Maven 3.8+, Node 18+, Yarn 1.x.

```bash
cd engine
mvn '-P!development,package-engine' -DskipTests package
cd ../ui
yarn make
```

**Produces:**
- `out/make/zip/darwin/<arch>/Assurance-darwin-<arch>-2.0.0.zip` (zipped `.app`).
- `out/Assurance-darwin-<arch>/Assurance.app/` (the loose `.app`, useful for direct `open` smoke tests).

**Smoke:** Unzip the `.zip` outside the build tree (so quarantine is preserved), double-click `Assurance.app`. Code-signing is out of scope for this release, so a right-click → Open may be required on first launch.

A native `.dmg` maker is **not** wired in: on Node 18+ the `appdmg` library used by `@electron-forge/maker-dmg@^6.4.2` throws `TypeError [ERR_INVALID_ARG_VALUE]: The property 'options.recursive' is no longer supported`. Adding DMG support requires the full `electron-forge` 6.4 → 7.x upgrade and is deferred to release-artifact tagging (T6.2 in `tasks/plan.md`).

#### Windows

**Tooling:** JDK 17 (with `jlink`), Maven 3.8+, Node 18+, Yarn 1.x. No additional Windows-specific tooling is required for the Squirrel installer that `@electron-forge/maker-squirrel` produces.

```cmd
cd engine
mvn "-P!development,package-engine" -DskipTests package
cd ..\ui
yarn make
```

(Or in PowerShell, drop the inner double-quotes around `-P!development,package-engine` since PowerShell tokenises `!` as the bang operator only inside strings.)

**Produces:**
- `out\make\squirrel.windows\<arch>\Assurance-2.0.0 Setup.exe` — Squirrel installer.
- `out\make\squirrel.windows\<arch>\RELEASES`, `*.nupkg` — Squirrel update manifest.
- `out\Assurance-win32-<arch>\Assurance.exe` (loose, useful for direct smoke tests).

**Smoke:** Run the Setup.exe, then launch `Assurance` from the Start Menu.

#### Linux

**Tooling:** JDK 17 (with `jlink`), Maven 3.8+, Node 18+, Yarn 1.x. For the RPM maker, install `rpmbuild`:
- Debian/Ubuntu: `sudo apt-get install rpm`
- Fedora/RHEL: `rpm-build` is part of the base toolchain.
- Arch: `sudo pacman -S rpm-tools`

(`MakerDeb` requires no extra packages.)

```bash
cd engine
mvn '-P!development,package-engine' -DskipTests package
cd ../ui
yarn make
```

**Produces:**
- `out/make/deb/<arch>/assurance_2.0.0_<arch>.deb`
- `out/make/rpm/<arch>/assurance-2.0.0-1.<arch>.rpm`
- `out/Assurance-linux-<arch>/Assurance` (loose ELF binary, useful for direct smoke tests).

**Smoke:** Install the appropriate package (`sudo dpkg -i ...` or `sudo rpm -i ...`) and launch from the desktop's app menu, or run the loose binary from `out/`.

## Running the UI test suites

Two scripts back the two layers of UI test coverage; both are wired into `package.json`.

### Unit + component + IPC contract tests (`yarn test`)

```bash
cd ui
yarn test
```

Jest + ts-jest + jsdom + React Testing Library. Picks up every file under `src/**/__tests__/**/*.test.{ts,tsx}`. This is the fast feedback loop and the mandatory gate before merge: it covers the renderer-side and main-side IPC contract on every JSON-RPC method documented in `docs/ipc-contract.md`, all of the renderer-side reducers and hooks, and component rendering where it pays off.

### End-to-end happy-path test (`yarn test:e2e`)

```bash
cd ui
yarn test:e2e
```

Playwright drives the packaged Electron app (not the dev build) via `_electron.launch` against `out/Assurance-<platform>-<arch>/`. The single happy-path scenario creates a scan definition over two diverging temp directories, runs a scan, and asserts that comparison results render in `ResultsPanel` — locking down the full renderer ↔ main-process ↔ WebSocket ↔ Java-engine round trip from one entry point.

**Prerequisite:** the packaged build must already exist. Run the production-build recipe first:

```bash
cd engine
mvn '-P!development,package-engine' -DskipTests package
cd ../ui
yarn package
```

The test self-skips with a helpful pointer if `out/Assurance-<platform>-<arch>/` is missing.

**Sandboxing:** the test launches Electron with `HOME` (and `USERPROFILE` on Windows) overridden to a fixture-owned temp directory under `os.tmpdir()`, so the engine's H2 database, any restore-staging directories, and the legacy 1.x location resolution all land inside the fixture. The developer's real `~/.assurance/` is never touched. `test.afterEach` removes the fixture unconditionally.

**Cooperative teardown:** `electronApp.close()` triggers `app.quit()` → `before-quit` → `engineClient.sendShutdownNotification()` → engine replies with a normal close-frame → `stopEngine()` awaits the Java child's exit → `app.quit()` resolves. The renderer's `EngineClient` classifies the engine's reply close as deliberate so it does not surface as a fatal-error dialog (see `ui/src/engine/client.ts` — `deliberateShutdown`). If you ever see this teardown hang again, the most likely culprit is a regression in that classification: the dialog is modal and would block `app.quit()` indefinitely.

**Wall-clock budget:** the global `timeout` in `playwright.config.ts` is 120 s to accommodate cold-start (JVM + Spring context + H2 schema install + WS bind = 10–30 s on a fresh DB), the scan itself, and the cooperative close. A successful run on developer hardware typically completes in 20–60 s.

## Running the legacy Swing UI

The legacy Swing UI remains functional throughout the 2.0 build-out and is invoked when the engine is launched **without** `--headless`:

```bash
cd engine
mvn package -Pdevelopment
java -cp "target/classes:target/dependency_libs/*" com.markallenjohnson.assurance.Application
```
