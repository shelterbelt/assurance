# Assurance 2.0 — UI Modernization Specification

**Branch:** `release-2.0`
**Status:** In progress
**Last updated:** 2026-05-05

## 1. Objective

Modernize the Assurance user interface by replacing the Java Swing UI of the 1.x line with a cross-platform Electron application built in TypeScript and React, while preserving the existing Java backend as the scanning, comparison, and synchronization engine.

**Headline outcome:** A user installs a single Assurance application, launches it, and gets feature parity with 1.x through a modern UI — with no manual server setup, no network/cloud dependencies, and no functional regressions in the underlying engine.

**Target users:** End users running Assurance on macOS, Windows, or Linux who need to compare and optionally synchronize the contents of two or more local file system directories. The audience is unchanged from 1.x.

**Non-goals (this release):**
- New end-user features beyond the UI re-implementation.
- Multi-user, networked, or cloud-hosted operation.
- Replacing or rewriting the Java engine.

## 2. Acceptance Criteria

The 2.0 release is considered "done" when **all** of the following hold:

1. **Functional parity with 1.x.** Every feature available in the Swing UI is reachable and functional in the Electron UI: defining scans (incl. mapping definitions and merge strategies), running scans, viewing comparison results, performing synchronization actions, and viewing scan history.
2. **Cross-platform packaging.** The application packages cleanly for macOS, Windows, and Linux via electron-forge, producing installers/bundles that launch the Java engine internally without user-visible setup.
3. **Backward compatibility with 1.x scan databases.** Existing H2 databases written by Assurance 1.x are readable and usable by 2.0 without manual migration.
4. **No backend regression.** The existing Java unit test suite passes; any required Java-side changes are minimal and accompanied by test updates.
5. **UI test coverage.** The Electron UI has its own automated test suite (component-level, plus contract tests for the IPC layer at minimum) that runs in CI.

## 3. Architecture

### High-level

```
┌──────────────────────────────────────────────┐
│  Electron Application (single user binary)   │
│                                              │
│  ┌────────────────┐    ┌──────────────────┐  │
│  │ Renderer (UI)  │◀──▶│ Main (Node)      │  │
│  │ React + TS     │IPC │ - lifecycle      │  │
│  │ Bootstrap/SCSS │    │ - dialogs        │  │
│  └────────────────┘    │ - engine bridge  │  │
│                        └────────┬─────────┘  │
│                                 │ local      │
│                                 │ transport  │
│                        ┌────────▼─────────┐  │
│                        │ Java Engine      │  │
│                        │ Spring + Hibernate│ │
│                        │ H2 (local file)  │  │
│                        └──────────────────┘  │
└──────────────────────────────────────────────┘
```

### Process model

- **Renderer process:** React/TypeScript UI. No Node integration; communicates with the main process exclusively through `contextBridge`-exposed APIs declared in `preload.ts`.
- **Main process:** Electron `main` (Node). Owns window lifecycle, native dialogs, and the lifecycle of the embedded Java engine (start on app `ready`, shut down on `before-quit`).
- **Java engine:** Long-running child process spawned by the main process. Existing Spring/Hibernate stack with H2 persistence on the local filesystem. Not user-facing; not separately startable by the user.

### IPC between Electron and Java engine

**Transport:** WebSocket over loopback, on an ephemeral port assigned at engine startup and advertised to the Electron main process during the lifecycle handshake.

**Wire format:** JSON-RPC 2.0. Every frame is a JSON object with the `"jsonrpc": "2.0"` member. Engine-bound calls are requests; engine-emitted progress events are server-initiated notifications.

Requirements:
- Binds to loopback only (no exposure to the network).
- Uses an ephemeral, OS-assigned port — never a fixed published port.
- Survives the app launching/closing without orphaning processes.
- Has a documented message contract that is versioned. The contract lives in `docs/ipc-contract.md`.

### Lifecycle handshake protocol

1. **Engine launch.** Electron main spawns the Java engine with the arguments `--headless --port=0`. Stdout and stderr of the engine are piped back to the main process and logged.
2. **Ready line.** The engine prints exactly one line to stdout when its WebSocket listener is bound:
   ```
   ASSURANCE_READY ws://127.0.0.1:<port> v<protocolVersion>
   ```
   `<port>` is the OS-assigned ephemeral port. `<protocolVersion>` is a positive integer that increments on any breaking wire change. The line is a contract — the format must not drift.
3. **Connect.** Electron main parses the ready line, opens a WebSocket connection to the advertised URL, and sends the JSON-RPC `hello` request:
   ```json
   {"jsonrpc":"2.0","method":"hello","params":{"uiVersion":"<x.y.z>","protocolVersion":<int>},"id":1}
   ```
4. **Welcome.** The engine replies with the matching `welcome` response or a JSON-RPC error if the protocol versions are incompatible:
   ```json
   {"jsonrpc":"2.0","result":{"engineVersion":"<x.y.z>","protocolVersion":<int>},"id":1}
   ```
   On mismatch, the engine returns error code `-32000` ("protocol version mismatch") and closes the connection. Electron main surfaces a fatal-error window and exits.
5. **Shutdown.** On Electron `before-quit`, main sends a JSON-RPC notification `{"jsonrpc":"2.0","method":"shutdown"}`, then waits up to 2 seconds for the engine process to exit on its own. If the process is still alive, Electron main sends SIGTERM; after another 2 seconds, SIGKILL.
6. **Engine death mid-session.** If the WebSocket closes unexpectedly, Electron main surfaces a fatal-error window. **No auto-restart in this release.**

### Packaging and distribution

The packaged Electron application embeds a self-contained Java runtime so that no system Java install is required.

- **Engine runtime:** Built per platform via `jpackage` over a `jlink`-trimmed JRE. Output is a directory containing the `bin/`, `lib/`, and `legal/` subtrees.
- **Engine code:** Built as a shaded jar via the existing Maven profiles (one shaded jar per platform variant).
- **Bundling:** electron-forge's `packageAfterCopy` hook copies the platform-appropriate runtime + shaded jar into the Electron app's `resources/engine/` directory at package time.
- **Launch:** Electron main resolves the packaged engine binary via `process.resourcesPath` in production, and via a configurable path in development (`yarn start`).
- **Per-platform layout:** `resources/engine/{darwin-x64,darwin-arm64,win32-x64,linux-x64}/` holds the runtime and jar; the launcher selects the correct directory at runtime based on `process.platform` + `process.arch`.

### Domain model

The Java entities are authoritative. The TypeScript side mirrors them as data carriers only (no behavior). Existing Java entities to mirror:

- `ApplicationConfiguration`
- `Scan`
- `ScanDefinition`
- `ScanMappingDefinition`
- `ComparisonResult`
- `FileReference`
- `FileAttributes`
- Enum: `AssuranceMergeStrategy` (already mirrored in `src/model/enums/`)

TypeScript model files already exist for `Scan`, `ScanDefinition`, `ScanMappingDefinition`, `ComparisonResult`, and the merge strategy enum. The remaining entities (`ApplicationConfiguration`, `FileReference`, `FileAttributes`) need TypeScript counterparts before the IPC contract is finalized.

## 4. Project Structure

```
/                                       (repo root)
├── engine/                             Java engine (Maven project)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/markallenjohnson/assurance/...
│       └── test/java/...
├── ui/                                 Electron application (electron-forge)
│   ├── package.json
│   ├── forge.config.ts
│   ├── webpack.*.config.ts
│   ├── tsconfig.json
│   └── src/
│       ├── index.ts                Electron main process
│       ├── preload.ts              contextBridge surface
│       ├── renderer.ts             Renderer entry
│       ├── app.tsx                 Root React component
│       ├── index.html
│       ├── api/                    Main-side API handlers (selectPath, etc.)
│       ├── model/                  TS data carriers mirroring Java entities
│       │   └── enums/
│       ├── components/             One folder per UI component
│       │   ├── actions-panel/
│       │   ├── feedback-panel/
│       │   ├── history-panel/
│       │   ├── main-window/
│       │   ├── nav-bar/
│       │   ├── path-selector/
│       │   ├── result-item-renderer/
│       │   ├── results-panel/
│       │   ├── scan-definition-panel/
│       │   ├── scan-list/
│       │   └── scan-panel/
│       └── scss/                   Sass styles
├── README.md
├── CLAUDE.md
├── AGENTS.md
└── SPEC.md                             (this file)
```

**Conventions:**
- Each React component lives in its own kebab-case directory under `src/components/` with the `.tsx`, any local `.scss`, and (when added) tests colocated.
- TypeScript model files mirror Java entity names 1:1 (e.g., `ScanDefinition.tsx` mirrors `ScanDefinition.java`).
- Main-process code (`index.ts`, `preload.ts`, `api/`) never imports from `components/` and vice versa; renderer-to-main calls go through the `assuranceapi` bridge declared in `preload.ts`.

## 5. Tech Stack

Locked **stack composition**, version-fluid (upgrade to current stable releases during this release):

**UI / Electron side**
- Electron + electron-forge (webpack plugin)
- React 18+ with TypeScript
- Bootstrap + react-bootstrap for component primitives
- Sass / SCSS for styling
- WebSocket client (`ws` or browser-native, depending on transport choice)
- ESLint + `@typescript-eslint`

**Engine side (unchanged composition)**
- Java + Maven
- Spring Framework 6.x
- Hibernate
- H2 (local file)
- JUnit
- SLF4J / Log4J
- Java-WebSocket (loopback IPC server)
- Jackson (JSON-RPC envelope encoding/decoding)

**Additions** are allowed where they earn their keep (e.g., a UI test framework — see Section 7). New dependencies must be flagged for review before adoption (see Section 8).

## 6. Commands

### Java engine (from `engine/`)

| Task | macOS | Windows |
|---|---|---|
| Build | `mvn clean package -Pdevelopment` | `mvn clean package -Pdevelopment-windows` |
| Test | `mvn clean test -Pdevelopment` | `mvn clean test -Pdevelopment-windows` |
| Internal release package | `mvn clean package -Pintrelease` | `mvn clean package -Pintrelease-windows` |
| Release package | `mvn clean package -Prelease` | `mvn clean package -Prelease-windows` |

### Electron UI (from `ui/`)

| Task | Command |
|---|---|
| Install deps | `yarn install` |
| Run in dev | `yarn start` |
| Lint | `yarn lint` |
| Package | `yarn package` |
| Make installer | `yarn make` |
| Unit / component tests | `yarn test` |
| End-to-end test | `yarn test:e2e` |

## 7. Code Style and Quality

### TypeScript / React
- Function components with hooks; no class components.
- `strict` TypeScript settings preferred; deviations called out per file.
- ESLint must pass (`yarn lint`); no warnings ignored without comment justification.
- Components declare prop types via TypeScript interfaces; no `any` in public surfaces.
- Renderer code never imports `electron` directly — only `window.assuranceapi` (preload bridge).

### SCSS / Bootstrap
- Prefer Bootstrap utilities and react-bootstrap components before bespoke CSS.
- Component-local styles live next to the component; global styles live in `src/scss/`.

### Java
- Maintain the SonarLint baseline established in commit `34a805f`. New Java changes must not regress SonarLint findings.
- Existing Spring/Hibernate idioms preserved; no architectural rework on the engine.

## 8. Testing Strategy

### Java engine
- Existing JUnit suite must remain green for every change.
- Any engine change accompanies a corresponding unit test update or addition.

### Electron UI
- **Frameworks:** Jest + React Testing Library for unit and component tests; Playwright for end-to-end tests over a packaged or development build.
- **Contract tests** for the IPC layer are required: every method defined in `docs/ipc-contract.md` has a test that exercises both the renderer-side caller and the main-side handler, with the engine mocked at the WebSocket transport boundary.
- **Coverage target:** meaningful coverage of presentation logic and IPC boundaries; coverage percentage gates are not set in this spec — quality of tests over numeric thresholds.

### Manual / UAT
- Each merge-candidate build runs through a manual parity check against the 1.x feature list before being marked ready.
- Cross-platform: at minimum macOS verified before each release; Windows and Linux verified prior to a tagged release.

## 9. Boundaries

### Always
- Preserve Java domain model semantics. The TypeScript model mirrors Java entities — Java is the source of truth.
- Operate fully offline / client-only. The application is a single, self-contained desktop app from the user's perspective.
- Maintain backward compatibility with H2 scan databases written by Assurance 1.x.
- Address SonarLint findings on any Java code touched.
- Bind any local IPC transport to loopback only.

### Ask first
- Changes to Java domain entities or the H2 schema.
- Changes to the IPC transport choice or the IPC message contract once finalized.
- Adoption of new runtime dependencies (Java or Node).
- Changes to packaging or distribution mechanics.

### Never
- Introduce cloud, network-server, or multi-user functionality.
- Require the user to manually start a server, daemon, or backend process.
- Bind any local socket to a non-loopback interface.
- Break the Apache 2.0 licensing posture (e.g., introducing GPL dependencies into the runtime).
- Hard-code a fixed network port for the engine bridge.

## 10. Open Questions

All Phase 0 open questions have been resolved as of 2026-05-05 and folded into the relevant sections above:

- UI test framework — Jest + React Testing Library + Playwright (Section 8).
- Engine packaging — `jpackage`-trimmed JRE + shaded jar bundled via `packageAfterCopy` (Section 3, "Packaging and distribution").
- Lifecycle handshake — stdout ready-line + JSON-RPC `hello`/`welcome` (Section 3, "Lifecycle handshake protocol").
- IPC message contract — JSON-RPC 2.0; full method/notification/error catalogue in `docs/ipc-contract.md`.
