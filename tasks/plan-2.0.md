# Assurance 2.0 — Implementation Plan

**Source spec:** `SPEC.md`
**Branch:** `release-2.0`
**Last updated:** 2026-05-05

This plan breaks the 2.0 UI modernization into vertically-sliced tasks ordered by dependency. Foundation tasks come first because no feature works without engine-launch + IPC plumbing; after that the work proceeds one user-visible feature at a time, each task delivering a complete end-to-end path from React component to Java engine and back.

---

## Current State (verified)

- **UI scaffolding:** All 11 panel components exist (`actions-panel`, `feedback-panel`, `history-panel`, `main-window`, `nav-bar`, `path-selector`, `result-item-renderer`, `results-panel`, `scan-definition-panel`, `scan-list`, `scan-panel`). All except `PathSelector` use mock/placeholder state.
- **Real IPC wired:** Only `PathSelector` → `window.assuranceapi.selectPath` → main-process `dialog.showOpenDialog`. No engine-bound IPC exists yet.
- **TypeScript model:** `Scan`, `ScanDefinition`, `ScanMappingDefinition`, `ComparisonResult`, `AssuranceMergeStrategy`. **Missing:** `ApplicationConfiguration`, `FileReference`, `FileAttributes`.
- **Java engine surface (`ApplicationDelegate`):** `loadApplicationInitializationState`, `performScan(scanDefinition[, merge])`, `mergeScan(scan)`, `mergeScanResult(result, strategy)`, `saveScanDefinition`, `loadApplicationConfiguration`, `saveApplicationConfiguration`, `loadScanDefinitions`, `deleteScanDefinition`, `loadScanResults(scan)`, `loadScans`, `deleteScan`, `restoreDeletedItem`, plus an event observer pattern (`addEventObserver` / `fireEvent`).
- **Java boot:** `Application.java` launches a Swing `MainWindow`. No WebSocket server, no headless mode.
- **Tests:** Java JUnit suite exists (preserved). UI has no tests.
- **Packaging:** Maven profiles for Mac/Windows dev/intrelease/release exist for the Swing app; electron-forge configured for the UI but engine bundling is not wired in.

## Dependency Graph

```
P0  Decisions
     │
     ▼
P1  Foundations (must complete in order)
     1.1 Engine launch (Electron spawns Java process)
     1.2 WebSocket server in Java + ephemeral port advertised on stdout
     1.3 WebSocket client in Electron main
     1.4 IPC envelope + version handshake
     1.5 Renderer ↔ main "engineCall" bridge
     1.6 End-to-end echo round-trip (skeleton walking)
     │
     ▼
P2  First vertical slice + remaining domain types
     2.1 Add TS types: ApplicationConfiguration, FileReference, FileAttributes
     2.2 Wire scan-list to live loadScanDefinitions (read-only feature, full path)
     │
     ▼
P3  Feature verticals (parallelizable once P2 lands)
     3.1 Save scan definition (create/edit)
     3.2 Delete scan definition
     3.3 Run scan (with progress events streamed via WS)
     3.4 View scan history (loadScans)
     3.5 View comparison results (loadScanResults)
     3.6 Per-result merge (mergeScanResult)
     3.7 Whole-scan merge (mergeScan)
     3.8 Restore deleted item (restoreDeletedItem)
     3.9 Application configuration load/save
     3.10 Delete scan history entry
     │
     ▼
P4  Packaging & cross-platform verification
     4.1 Bundle JRE + engine jar inside Electron build
     4.2 macOS package + smoke test
     4.3 Windows package + smoke test
     4.4 Linux package + smoke test
     4.5 1.x H2 database backward-compat verification
     │
     ▼
P5  UI test suite
     5.1 Component test framework + first tests
     5.2 IPC contract tests (renderer + main, engine mocked at WS boundary)
     5.3 End-to-end test (one full happy-path scenario)
     │
     ▼
P6  Manual parity UAT + release
     6.1 1.x→2.0 feature parity walkthrough
     6.2 Tag release artifact
```

P5 (UI tests) is intentionally placed near the end so it can be designed against real component shapes, but **IPC contract tests (5.2) should land alongside or immediately after Phase 1.6** to lock the wire format early. Each P3 task includes a small smoke test for its happy path.

## Checkpoints

Pause and review with the human at each `▼` boundary above. Specifically:

- **CP-A** (after P0): Decisions ratified; SPEC updated.
- **CP-B** (after P1): "Skeleton walking" — empty UI button triggers an engine round-trip end-to-end. The hardest plumbing is done.
- **CP-C** (after P2): First real feature works against the live Java engine. Pattern is proven; remaining feature work is repetition.
- **CP-D** (after P3): Functional parity reached running from `yarn start` (engine launched as child process from dev mode).
- **CP-E** (after P4): Cross-platform packaged builds verified on at least macOS, with Windows/Linux smoke-tested before tagging.
- **CP-F** (after P5/P6): Tests green, parity walkthrough signed off, ready to tag.

---

## Phase 0 — Decisions (no code)

Resolves the open questions in `SPEC.md` Section 10. Each decision is folded back into `SPEC.md`. **Recommended defaults** below; treat as proposals to accept or override.

### T0.1 — UI test framework
**Recommendation:** Jest + React Testing Library for unit/component, Playwright for one happy-path E2E against a packaged Electron build.
**Acceptance:** Choice committed to SPEC.md Section 5/8; a `yarn test` (and `yarn test:e2e`) script placeholder added to package.json.
**Verify:** `yarn test --listTests` runs cleanly with zero tests.

### T0.2 — Engine packaging strategy
**Recommendation:** `jpackage`-produced runtime (jlink-trimmed JRE) + shaded engine jar, copied into the Electron resources directory by an electron-forge `packageAfterCopy` hook. Per-platform variants produced by Maven profiles.
**Acceptance:** Decision documented in SPEC.md Section 10 (now Section 5/9). One paragraph describes how the engine binary lands inside the packaged Electron app for each platform.
**Verify:** Manual review.

### T0.3 — Lifecycle handshake protocol
**Recommendation:**
- Electron main spawns the Java process with `--headless --port=0`.
- Java prints exactly one line to stdout: `ASSURANCE_READY ws://127.0.0.1:<ephemeral-port> v<protocol-version>`.
- Electron main parses this line, opens a WS connection, and sends `{ "type": "hello", "uiVersion": "<x.y.z>" }`.
- Engine replies with `{ "type": "welcome", "engineVersion": "<x.y.z>", "protocolVersion": <int> }`.
- On Electron `before-quit`: send `{ "type": "shutdown" }`, wait up to 2s, then SIGTERM, then SIGKILL.
- If the engine dies mid-session, Electron surfaces a fatal error in the UI; no auto-restart in this release.

**Acceptance:** Protocol documented in SPEC.md (new Section "IPC Protocol"). Electron main and Java engine implementations match this exactly.
**Verify:** Reviewed against P1 implementation tasks.

### T0.4 — IPC message contract
**Recommendation:** JSON-RPC 2.0 over WebSocket. Request/response for engine calls; server-initiated notifications for events (scan progress, completion). Methods named `assurance.<verb><Noun>` (e.g., `assurance.loadScanDefinitions`). `protocolVersion` is an integer that increments on any breaking wire change; mismatch closes the connection with a structured error.
**Acceptance:** A `docs/ipc-contract.md` (or appendix in SPEC.md) lists every method, its params, return shape, and emitted notifications. Method list is the 13 ApplicationDelegate operations + a `ping` for liveness.
**Verify:** Document reviewed; method list cross-referenced against ApplicationDelegate.

---

## Phase 1 — Foundations

Each task is a thin slice; the phase as a whole produces "skeleton walking."

### T1.1 — Spawn Java engine from Electron main
**What:** Electron main process spawns the engine as a child process on `app.whenReady()` and tears it down on `before-quit`. Use a stub Java entry point initially (`--headless` mode that just prints `ASSURANCE_READY` and idles).
**Touches:** `assurance-ui/assurance/src/index.ts` (new `engine/launcher.ts`), `assurance/src/main/java/com/markallenjohnson/assurance/Application.java` (add headless branch).
**Acceptance:**
- App start spawns the engine; app quit terminates it (verified by checking process tree).
- No orphaned Java process after quit on any supported OS.
- Headless flag distinct from existing Swing entry; existing Swing entry remains functional (Swing UI not yet removed).
**Verify:** `yarn start`, observe child process via `ps`, quit and reverify; `mvn test` still green.

### T1.2 — WebSocket server in the Java engine
**What:** Replace stub with a real WebSocket server (Java-WebSocket library or Spring's WebSocket support — pick whichever has minimal incremental dependency cost) bound to `127.0.0.1` on an ephemeral port. Print the ready-line per T0.3.
**Touches:** new `com.markallenjohnson.assurance.ipc` package; `pom.xml`.
**Acceptance:**
- Server binds to loopback only; verified via `lsof -i` / `netstat`.
- Port is OS-assigned (port `0`).
- Ready-line printed exactly once; matches T0.3 grammar.
- Server gracefully shuts down on JVM SIGTERM.
**Verify:** Manual telnet/wscat connection; check `lsof` shows binding to 127.0.0.1 only; `mvn test` green.

### T1.3 — WebSocket client in Electron main
**What:** After parsing the ready-line, Electron main opens a WS connection and exchanges `hello`/`welcome` per T0.3. Exposes a singleton `engineClient` to the rest of the main process.
**Touches:** `assurance-ui/assurance/src/engine/client.ts` (new); `package.json` (`ws` already present).
**Acceptance:**
- Successful handshake logs the engine version.
- Version mismatch closes the connection with a structured error and surfaces a fatal-error window in the UI.
- Reconnect is **not** attempted (per T0.3 — no auto-restart this release).
**Verify:** Run with matching versions (success path); manually edit Java protocol constant to force mismatch (failure path).

### T1.4 — IPC envelope + JSON-RPC 2.0 plumbing
**What:** On both sides, implement JSON-RPC 2.0 framing: request/response correlation by `id`, error envelope, notification (no `id`). Build a small typed `engineCall<Method>(params): Promise<Result>` helper in TS and a request dispatcher in Java that routes by method name.
**Touches:** `assurance-ui/assurance/src/engine/rpc.ts` (new); `com.markallenjohnson.assurance.ipc.RpcDispatcher` (new).
**Acceptance:**
- Dispatcher routes `assurance.ping` to a Java handler that returns `{ "pong": true }`.
- Unknown methods return JSON-RPC error `-32601`.
- Both sides have unit tests for the envelope (encode, decode, error cases).
**Verify:** Java JUnit tests for `RpcDispatcher`; TS tests for `rpc.ts` (Jest, harnessed once T0.1 lands; otherwise small standalone runner).

### T1.5 — Renderer ↔ main bridge for engine calls
**What:** Extend `preload.ts` to expose `window.assuranceapi.engineCall(method, params)`. Main forwards to `engineClient.call`. Renderer-side typed wrapper.
**Touches:** `preload.ts`, `index.ts`, `src/api/api.ts`, `src/api/engine.ts` (new typed wrapper).
**Acceptance:**
- Renderer code can call `await api.engine.call('assurance.ping', {})` and receive `{ pong: true }`.
- The bridge does not expose raw `ipcRenderer` to the renderer.
- Errors from the engine surface as rejected promises with `{ code, message, data }`.
**Verify:** Add a temporary developer-only "Ping" button to a panel; observe round-trip; remove the button before phase close.

### T1.6 — Skeleton-walking end-to-end demo
**What:** Wire a hidden dev-only "Ping engine" affordance and confirm the full path. Capture a short verification recipe in `docs/dev-getting-started.md`.
**Acceptance:** From a fresh `yarn start`, a click round-trips renderer → main → WS → Java → back, with the result rendered.
**Verify:** Manual end-to-end run on developer machine; recipe checked in.

🟢 **CP-B**: Foundation complete. Plumbing proven; remaining work is feature implementation against a known-good IPC pattern.

---

## Phase 2 — First vertical + domain type completion

### T2.1 — Add missing TypeScript model types
**What:** Add `ApplicationConfiguration.tsx`, `FileReference.tsx`, `FileAttributes.tsx` to `src/model/`. Mirror the Java entity field shapes; data carriers only.
**Acceptance:** All Java entities now have a TS counterpart. Field names match Java field names (camelCase). No behavior on the TS side.
**Verify:** `yarn lint`; visual diff against Java entity fields.

### T2.2 — Wire scan list to live engine (first real feature)
**What:** Replace the mock array in `ScanPanel.tsx` with a call to `assurance.loadScanDefinitions`. Add a Java handler that delegates to `ApplicationDelegate.loadScanDefinitions`. Loading and error states rendered.
**Touches:** Java RPC handler for `assurance.loadScanDefinitions`; `ScanPanel.tsx`; `ScanList.tsx` for empty/loading/error UX.
**Acceptance:**
- A previously-saved 1.x scan database, when present, lists its scan definitions in the UI.
- Empty database shows an empty-state message, not a crash.
- Engine error surfaces a non-fatal in-UI error, not a crash.
**Verify:** Manual run against (a) a fresh database, (b) a 1.x database from a real install. Add an IPC contract test for `assurance.loadScanDefinitions` per T5.2 hooks.

🟢 **CP-C**: Pattern proven. Remaining feature work is repetition of T2.2 against new methods.

---

## Phase 3 — Feature verticals (each is a vertical slice)

Each task in this phase follows the same shape: pick a UI flow, define the JSON-RPC method (or reuse), implement the Java handler over `ApplicationDelegate`, wire the React component, replace mock data, handle empty/loading/error states, add a smoke test.

| ID | Slice | Java method(s) | UI components touched |
|---|---|---|---|
| T3.1 | Save scan definition (new + edit) | `assurance.saveScanDefinition` | `ScanDefinitionPanel`, `ScanPanel` |
| T3.2 | Delete scan definition | `assurance.deleteScanDefinition` | `ScanList`, `ScanPanel` |
| T3.3 | Run a scan with live progress | `assurance.performScan` + progress notifications | `ActionsPanel`, `FeedbackPanel`, `ResultsPanel` |
| T3.4 | View scan history | `assurance.loadScans` | `HistoryPanel` |
| T3.5 | View comparison results for a historical scan | `assurance.loadScanResults` | `HistoryPanel`, `ResultsPanel`, `ResultItemRenderer` |
| T3.6 | Per-result merge | `assurance.mergeScanResult` | `ResultItemRenderer` |
| T3.7 | Whole-scan merge | `assurance.mergeScan` | `ActionsPanel` |
| T3.8 | Restore deleted item | `assurance.restoreDeletedItem` | `ResultItemRenderer` (or per UX decision) |
| T3.9 | Application configuration load/save | `assurance.loadApplicationConfiguration`, `assurance.saveApplicationConfiguration` | new settings affordance (mirror 1.x location) |
| T3.10 | Delete scan history entry | `assurance.deleteScan` | `HistoryPanel` |

**Acceptance (each task):**
- Mock data for that slice is gone; the panel reflects real engine state.
- Empty/loading/error states handled.
- Java unit test exists for the handler (or covered by `ApplicationDelegate`'s existing tests if delegating directly).
- IPC contract test exists (per T5.2).
- The corresponding 1.x feature behavior is reproduced in the new UI (verified against the 1.x parity checklist — see T6.1).

**Verify (each task):** Manual run; relevant Java tests green; lint clean.

T3.3 deserves special attention because it's the first task that uses **server-initiated notifications**. The Java event observer pattern (`addEventObserver` / `fireEvent`) maps onto JSON-RPC notifications: events are subscribed-to implicitly when a scan starts, and the engine emits `assurance.scanProgress` / `assurance.scanCompleted` notifications until the scan ends.

🟢 **CP-D**: Functional parity from `yarn start`. App is feature-complete in dev mode but not yet packaged.

---

## Phase 4 — Packaging & cross-platform

### T4.1 — Bundle JRE + engine jar inside Electron build
**What:** Implement T0.2's packaging recipe via an electron-forge `packageAfterCopy` hook that pulls the platform-appropriate `jpackage` runtime + shaded engine jar from a known Maven build output path, copying them into the Electron app's `resources/engine/` directory.
**Acceptance:** Packaged app contains the JRE and engine jar; no system Java required to run.
**Verify:** Uninstall system Java on a test VM; run the packaged app; engine launches.

### T4.2 — macOS package + smoke test
**What:** `yarn make` produces a working `.dmg` / `.app` on macOS. Smoke-test launches engine, lists scan definitions, runs a small scan, merges one result.
**Acceptance:** Smoke-test passes on macOS without code-signing concerns blocking the run (signing is out of scope for this plan).
**Verify:** Manual on developer Mac.

### T4.3 — Windows package + smoke test
**What:** Same as T4.2 on Windows (Squirrel installer).
**Acceptance:** Smoke test passes on a Windows VM.
**Verify:** Manual on Windows VM.

### T4.4 — Linux package + smoke test
**What:** Same as T4.2 on Linux (deb / rpm).
**Acceptance:** Smoke test passes on a Linux VM.
**Verify:** Manual on Linux VM.

### T4.5 — 1.x H2 database backward-compat verification
**What:** Locate or capture a real 1.x H2 database file. Run the packaged 2.0 app pointing at it; verify scan definitions, history, and results are all readable and that a new scan can be run and persisted without corrupting the file.
**Acceptance:** SPEC acceptance criterion #3 (backward-compat with 1.x scan databases) satisfied.
**Verify:** Manual; checksum the database before/after a no-op session.

🟢 **CP-E**: Cross-platform packaged builds verified.

---

## Phase 5 — UI test suite

### T5.1 — UI test framework wired up
**What:** Implement T0.1 — install Jest + RTL + Playwright; add `yarn test` and `yarn test:e2e` scripts; one trivial passing test per framework as a smoke check.
**Acceptance:** Both scripts run and report passing.
**Verify:** Run them.

### T5.2 — IPC contract tests
**What:** For every JSON-RPC method delivered in Phases 1–3, write a contract test that:
- On the renderer side, mocks `window.assuranceapi.engineCall` and verifies the call shape and return handling.
- On the main side, mocks the WS engine and verifies request/response/notification framing.
**Acceptance:** Every method in `docs/ipc-contract.md` has at least one contract test on each side.
**Verify:** `yarn test`; coverage report shows the IPC layer.

### T5.3 — One end-to-end happy-path test
**What:** A Playwright test that boots the packaged app (or development build), creates a scan definition, runs a scan against a temp directory, and verifies a result.
**Acceptance:** E2E test runs in CI on at least one platform.
**Verify:** `yarn test:e2e` passes.

🟢 **CP-F**: Automated test gate in place.

---

## Phase 6 — Parity walkthrough & release

### T6.1 — 1.x→2.0 parity checklist walkthrough
**What:** Build the parity checklist by exercising every menu item, dialog, action, and result interaction in the 1.x Swing UI. For each, verify the equivalent path in 2.0. File any gaps as new tasks before proceeding.
**Acceptance:** Checklist saved as `docs/parity-checklist.md`; every item passes against 2.0.
**Verify:** Walkthrough.

### T6.2 — Tag and release artifact
**What:** Cut the 2.0 release tag on `release-2.0`, produce signed-or-unsigned distribution artifacts per platform, write release notes summarizing the 1.x→2.0 changes.
**Acceptance:** Release tag exists; artifacts attached; release notes published per project convention.
**Verify:** Tag and artifacts visible.

🟢 **Done.**

---

## Risks & mitigations

- **Java engine startup time stretches the launch UX.** Mitigation: show an explicit "starting engine" splash; measure cold-start; consider trimming Spring context if it grows beyond a few seconds.
- **WebSocket port-binding races on Windows.** Mitigation: validated via T1.2 acceptance; the ephemeral-port approach plus stdout advertisement avoids the most common races.
- **JRE bundling bloats package size.** Mitigation: jlink-trimmed runtime in T4.1; revisit if package size exceeds project norms.
- **Backward-compat with 1.x DB schema breaks subtly.** Mitigation: T4.5 explicitly verifies; any schema change in Phase 1–3 must be flagged per SPEC boundary.
- **Test framework choice gets locked too early.** Mitigation: T0.1 makes the choice an explicit checkpoint decision; plan can substitute Vitest for Jest with minimal blast radius if needed.

## Out-of-scope (deferred)

- Code signing (macOS notarization, Windows Authenticode).
- Auto-update.
- Telemetry.
- Engine auto-restart on crash.
- New end-user features beyond 1.x parity.
