# Assurance IPC Contract

**Transport:** WebSocket on `127.0.0.1:<ephemeral-port>`
**Wire format:** JSON-RPC 2.0
**Protocol version:** `1`
**Status:** Initial draft (2026-05-05)

This document is the canonical wire contract between the Electron main process (client) and the Java engine (server). It is referenced from `SPEC.md` Section 3.

## Conventions

- All frames are JSON-RPC 2.0. Every frame includes `"jsonrpc": "2.0"`.
- Method names are namespaced: `assurance.<verb><Noun>` (e.g., `assurance.loadScanDefinitions`).
- IDs are integers, monotonically increasing per connection, allocated by the client.
- Notifications (no `id`) are server-initiated and unidirectional from engine to client.
- Domain payload field names are camelCase and mirror the corresponding Java entity field names exactly (the Java entities are the source of truth).
- Timestamps are ISO 8601 strings in UTC.
- IDs of persisted entities are positive integers (mirrors Hibernate-assigned ids).

## Versioning

`protocolVersion` is a single positive integer maintained on both client and server. **Increment on any breaking wire change**: renamed method, changed param shape, changed return shape, removed method, changed semantics. Adding a new method or a new optional field is **not** a breaking change.

Compatibility rule: client and server must agree on `protocolVersion` exactly. There is no negotiation. Mismatch closes the connection with error code `-32000`.

## Lifecycle

### 1. Engine ready line (out-of-band)

Printed once to engine stdout when the WebSocket listener is bound:

```
ASSURANCE_READY ws://127.0.0.1:<port> v<protocolVersion>
```

Not JSON-RPC. Parsed by the Electron launcher to discover the port and protocol version before the WebSocket connection is opened.

### 2. `hello` (client → server, request)

Sent by the client immediately after the WebSocket connection is established. Always `id: 1`.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "method": "hello",
  "params": {
    "uiVersion": "2.0.0",
    "protocolVersion": 1
  },
  "id": 1
}
```

**Success response:**
```json
{
  "jsonrpc": "2.0",
  "result": {
    "engineVersion": "2.0.0",
    "protocolVersion": 1
  },
  "id": 1
}
```

**Mismatch error response:** `-32000` (see Errors).

### 3. `shutdown` (client → server, notification)

Sent by the client during Electron `before-quit`. The engine acknowledges by closing the WebSocket and exiting the JVM cleanly. The client waits up to 2 seconds, then escalates with SIGTERM, then SIGKILL.

```json
{"jsonrpc":"2.0","method":"shutdown"}
```

## Methods

The 13 methods below correspond 1:1 to the Java `ApplicationDelegate` operations, plus a `ping` for liveness.

### `assurance.ping`

Liveness check. Used in development and tests.

- **Params:** `{}`
- **Result:** `{ "pong": true, "timestamp": "<iso8601>" }`
- **Errors:** none

---

### `assurance.loadScanDefinitions`

Returns all scan definitions known to the engine.

- **Params:** `{}`
- **Result:** `{ "scanDefinitions": ScanDefinition[] }` — each entry includes `scanMapping` (full source/target `FileReference` objects and exclusions) so UIs can populate an edit form without an extra load.
- **Errors:** `-32001` (engine error)

### `assurance.saveScanDefinition`

Creates a new scan definition or updates an existing one. Set `id` for update; omit for create.

- **Params:** `{ "scanDefinition": ScanDefinition }`
- **Result:** `{ "scanDefinition": ScanDefinition }` (with `id` populated)
- **Errors:** `-32602` (invalid params), `-32001` (engine error)

The request payload mirrors the persisted Java entity. Each `ScanMappingDefinition` carries `source`/`target` as `FileReference` objects (each with a `location` string), and `exclusions` as an array of `FileReference`. `name`, `source.location`, and `target.location` are all required and must be non-empty; missing or empty values produce `-32602`. The response includes the full saved graph (definition id and mapping ids populated).

### `assurance.deleteScanDefinition`

Deletes a scan definition by id.

- **Params:** `{ "scanDefinitionId": <int> }`
- **Result:** `{ "deleted": true }`
- **Errors:** `-32002` (not found), `-32001` (engine error)

---

### `assurance.performScan`

Starts a scan asynchronously. Returns immediately with the new `scanId`. Progress is delivered via server-initiated notifications (`assurance.scanStarted`, `assurance.scanProgress`, `assurance.scanCompleted`, `assurance.scanFailed`) bound to that `scanId`.

- **Params:** `{ "scanDefinitionId": <int>, "merge": <bool> }`
- **Result:** `{ "scanId": <int> }`
- **Errors:** `-32002` (definition not found), `-32003` (scan already running for this definition), `-32001`

### `assurance.mergeScan`

Starts an asynchronous merge of all results of a completed scan according to the scan definition's merge strategy. Returns immediately. Progress is delivered via server-initiated notifications (`assurance.mergeStarted`, `assurance.mergeProgress`, `assurance.mergeCompleted`, `assurance.mergeFailed`) bound to the same `scanId`.

- **Params:** `{ "scanId": <int> }`
- **Result:** `{ "scanId": <int> }`
- **Errors:** `-32002` (scan not found), `-32006` (merge already running for this scan), `-32001`

### `assurance.mergeScanResult`

Starts an asynchronous merge of a single comparison result. Returns immediately. Progress is delivered via server-initiated notifications (`assurance.resultMergeStarted`, `assurance.resultMergeProgress`, `assurance.resultMergeCompleted`, `assurance.resultMergeFailed`) bound to the `resultId`.

- **Params:** `{ "resultId": <int>, "strategy": "<AssuranceMergeStrategy>" }` (strategy values mirror the Java enum: `SOURCE`, `TARGET`, or `BOTH`).
- **Result:** `{ "resultId": <int>, "scanId": <int> }`
- **Errors:** `-32602` (invalid params, including unknown strategy), `-32002` (result not found), `-32007` (result merge already running for this result), `-32001`

### `assurance.restoreDeletedItem`

Starts an asynchronous restore of a previously deleted file or directory referenced by a comparison result. Returns immediately. Progress is delivered via server-initiated notifications (`assurance.restoreStarted`, `assurance.restoreProgress`, `assurance.restoreCompleted`, `assurance.restoreFailed`) bound to the `resultId`.

- **Params:** `{ "resultId": <int> }`
- **Result:** `{ "resultId": <int>, "scanId": <int> }`
- **Errors:** `-32002` (result not found), `-32008` (restore already running for this result), `-32001`

---

### `assurance.loadScans`

Returns the scan history.

- **Params:** `{}`
- **Result:** `{ "scans": Scan[] }`
- **Errors:** `-32001`

Each `Scan` is a list-view projection: `id`, ISO 8601 `scanStarted` and `whenCompleted` (the latter `null` for in-flight or aborted scans), a thin `scanDef` reference (only `id` and `name`), and a derived `resultCount` scalar. The full `results` collection is intentionally omitted from this method — load it separately via `assurance.loadScanResults` for a specific `scanId`.

### `assurance.loadScanResults`

Returns all comparison results for a given scan.

- **Params:** `{ "scanId": <int> }`
- **Result:** `{ "results": ComparisonResult[] }`
- **Errors:** `-32602` (invalid params), `-32002` (scan not found), `-32001`

Each `ComparisonResult` carries `id`, optional `source` and `target` `FileReference` objects (each with `id`, `location`, and an optional inline `fileAttributes` payload), `reason` (`AssuranceResultReason` enum name), `resolution` (`AssuranceResultResolution` enum name), and `resolutionError` (always a string; empty when none was recorded). `source` or `target` may be omitted entirely when the engine has no reference to project (e.g. a `SOURCE_DOES_NOT_EXIST` result has no source file reference).

### `assurance.loadComparisonResult`

Returns one persisted comparison result by primary key (same projection shape as elements of `assurance.loadScanResults`). Intended for UI refresh after a single-row merge or restore without re-fetching the entire scan.

- **Params:** `{ "resultId": <int> }`
- **Result:** `{ "result": ComparisonResult }`
- **Errors:** `-32602` (invalid params), `-32002` (result not found), `-32001`

### `assurance.deleteScan`

Deletes a scan and its results from history.

- **Params:** `{ "scanId": <int> }`
- **Result:** `{ "deleted": true }`
- **Errors:** `-32002`, `-32001`

---

### `assurance.loadApplicationConfiguration`

Returns the persisted application configuration.

- **Params:** `{}`
- **Result:** `{ "configuration": ApplicationConfiguration }`
- **Errors:** `-32001`

### `assurance.saveApplicationConfiguration`

Persists application configuration.

- **Params:** `{ "configuration": ApplicationConfiguration }`
- **Result:** `{ "configuration": ApplicationConfiguration }`
- **Errors:** `-32602`, `-32001`

## Notifications (server → client)

Notifications are bound to either a `scanId` (whole-scan operations) or a `resultId` (single-result operations). Single-result notifications also carry the parent `scanId` so the UI can route them to the correct scan view.

| Family | Bound to | Operation |
|---|---|---|
| `assurance.scan*` | `scanId` | `performScan` |
| `assurance.merge*` | `scanId` | `mergeScan` |
| `assurance.resultMerge*` | `resultId` (+ parent `scanId`) | `mergeScanResult` |
| `assurance.restore*` | `resultId` (+ parent `scanId`) | `restoreDeletedItem` |

All four families can fire against the same `scanId` over its lifetime (a scan finishes, then a merge runs, then individual results are merged or restored).

### `assurance.scanStarted`

Emitted when a scan begins.

```json
{"jsonrpc":"2.0","method":"assurance.scanStarted","params":{"scanId":42,"startedAt":"<iso8601>"}}
```

### `assurance.scanProgress`

Emitted periodically during a scan. The shape of `progress` is intentionally flexible to accommodate the engine's existing event payloads; the minimum guaranteed fields are `phase` and `itemsProcessed`.

```json
{"jsonrpc":"2.0","method":"assurance.scanProgress","params":{"scanId":42,"progress":{"phase":"comparing","itemsProcessed":1234,"itemsTotal":5000,"currentItem":"/path/to/file"}}}
```

### `assurance.scanCompleted`

Emitted on successful scan completion.

```json
{"jsonrpc":"2.0","method":"assurance.scanCompleted","params":{"scanId":42,"completedAt":"<iso8601>","resultCount":17}}
```

### `assurance.scanFailed`

Emitted if a scan terminates abnormally.

```json
{"jsonrpc":"2.0","method":"assurance.scanFailed","params":{"scanId":42,"failedAt":"<iso8601>","error":{"code":-32001,"message":"<human-readable>"}}}
```

### `assurance.mergeStarted`

Emitted when an `assurance.mergeScan` operation begins.

```json
{"jsonrpc":"2.0","method":"assurance.mergeStarted","params":{"scanId":42,"startedAt":"<iso8601>"}}
```

### `assurance.mergeProgress`

Emitted periodically during a merge. Like `scanProgress`, the inner `progress` shape is intentionally flexible to accommodate the engine's existing event payloads; the minimum guaranteed fields are `phase` and `itemsProcessed`.

```json
{"jsonrpc":"2.0","method":"assurance.mergeProgress","params":{"scanId":42,"progress":{"phase":"merging","itemsProcessed":1234,"itemsTotal":5000,"currentItem":"/path/to/file"}}}
```

### `assurance.mergeCompleted`

Emitted on successful merge completion.

```json
{"jsonrpc":"2.0","method":"assurance.mergeCompleted","params":{"scanId":42,"completedAt":"<iso8601>","itemsMerged":17}}
```

### `assurance.mergeFailed`

Emitted if a merge terminates abnormally.

```json
{"jsonrpc":"2.0","method":"assurance.mergeFailed","params":{"scanId":42,"failedAt":"<iso8601>","error":{"code":-32001,"message":"<human-readable>"}}}
```

### `assurance.resultMergeStarted`

Emitted when an `assurance.mergeScanResult` operation begins.

```json
{"jsonrpc":"2.0","method":"assurance.resultMergeStarted","params":{"resultId":314,"scanId":42,"startedAt":"<iso8601>"}}
```

### `assurance.resultMergeProgress`

Emitted periodically during a single-result merge. The minimum guaranteed fields in `progress` are `phase` and `bytesProcessed`.

```json
{"jsonrpc":"2.0","method":"assurance.resultMergeProgress","params":{"resultId":314,"scanId":42,"progress":{"phase":"copying","bytesProcessed":1048576,"bytesTotal":4194304,"currentItem":"/path/to/file"}}}
```

### `assurance.resultMergeCompleted`

Emitted on successful single-result merge completion.

```json
{"jsonrpc":"2.0","method":"assurance.resultMergeCompleted","params":{"resultId":314,"scanId":42,"completedAt":"<iso8601>"}}
```

### `assurance.resultMergeFailed`

Emitted if a single-result merge terminates abnormally.

```json
{"jsonrpc":"2.0","method":"assurance.resultMergeFailed","params":{"resultId":314,"scanId":42,"failedAt":"<iso8601>","error":{"code":-32001,"message":"<human-readable>"}}}
```

### `assurance.restoreStarted`

Emitted when an `assurance.restoreDeletedItem` operation begins.

```json
{"jsonrpc":"2.0","method":"assurance.restoreStarted","params":{"resultId":314,"scanId":42,"startedAt":"<iso8601>"}}
```

### `assurance.restoreProgress`

Emitted periodically during a restore. The minimum guaranteed fields in `progress` are `phase` and `bytesProcessed`.

```json
{"jsonrpc":"2.0","method":"assurance.restoreProgress","params":{"resultId":314,"scanId":42,"progress":{"phase":"restoring","bytesProcessed":1048576,"bytesTotal":4194304,"currentItem":"/path/to/file"}}}
```

### `assurance.restoreCompleted`

Emitted on successful restore completion.

```json
{"jsonrpc":"2.0","method":"assurance.restoreCompleted","params":{"resultId":314,"scanId":42,"completedAt":"<iso8601>"}}
```

### `assurance.restoreFailed`

Emitted if a restore terminates abnormally.

```json
{"jsonrpc":"2.0","method":"assurance.restoreFailed","params":{"resultId":314,"scanId":42,"failedAt":"<iso8601>","error":{"code":-32001,"message":"<human-readable>"}}}
```

## Errors

Errors follow JSON-RPC 2.0 error semantics: `{ "code": <int>, "message": <string>, "data"?: <any> }`.

### Standard JSON-RPC codes (passed through)

- `-32700` Parse error
- `-32600` Invalid request
- `-32601` Method not found
- `-32602` Invalid params
- `-32603` Internal error

### Assurance-specific codes (server-defined range, `-32000` to `-32099`)

| Code | Meaning |
|---|---|
| `-32000` | Protocol version mismatch (closes connection) |
| `-32001` | Engine error (unrecoverable; details in `data`) |
| `-32002` | Entity not found (scan, scan definition, or result) |
| `-32003` | Scan already running for this definition |
| `-32004` | Filesystem error (e.g., source/target unreachable) |
| `-32005` | Database error (H2 / Hibernate) |
| `-32006` | Merge already running for this scan |
| `-32007` | Result merge already running for this result |
| `-32008` | Restore already running for this result |

`data` may contain a structured payload (e.g., the failing entity id, the offending path, the underlying Java exception class name) — never a stack trace, never a full Throwable.

## Domain payload shapes

The shapes of `ScanDefinition`, `Scan`, `ComparisonResult`, `ScanMappingDefinition`, `ApplicationConfiguration`, `FileReference`, and `FileAttributes` are defined by the corresponding Java entity classes under `com.markallenjohnson.assurance.model.entities`. The TypeScript model files in `ui/src/model/` mirror these entities exactly — same field names, same nesting, same enum values.

When a Java entity gains, removes, or renames a field, the corresponding TS model file and this contract document must be updated in the same change set, and `protocolVersion` must increment if the change is breaking.

## Change log

| Date | Protocol version | Change |
|---|---|---|
| 2026-05-05 | 1 | Initial draft. `mergeScan`, `mergeScanResult`, and `restoreDeletedItem` are all async with their own progress notification families. |
| 2026-05-09 | 1 | Added `assurance.loadComparisonResult` for fetching a single `ComparisonResult` by id (additive; non-breaking). |
