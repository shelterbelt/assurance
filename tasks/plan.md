# Implementation Plan: Repository Reorganization

**Source:** User request 2026-05-13
**Branch:** `release-2.0`
**Status:** Plan drafted, awaiting human review
**Prior plan archived as:** `tasks/plan-2.0.md` (the 2.0 UI modernization plan; 32/34 tasks complete)

## Overview

Rename and flatten the top-level directory layout to clearer role-based names. No code semantics change; this is filesystem and reference housekeeping only.

| Current path | Target path |
|---|---|
| `assurance/` | `engine/` |
| `assurance-ui/assurance/` | `ui/` |
| `assurance-ui/` (wrapper) | _(removed)_ |

## Architecture Decisions

- **Two slices, sequential.** UI rename + flatten first (Phase 1), then engine rename (Phase 2). Each phase leaves the repo in a working, runnable state so we can pause for review between them.
- **`git mv`, not delete-then-add.** Preserves per-file history; `git log --follow <newpath>` resolves cleanly.
- **`node_modules/` and `target/` are not moved.** Both are gitignored; each phase clears and rebuilds them from the new path.
- **Java package namespace `com.markallenjohnson.assurance` is unchanged.** Renaming the package would be a far larger change with no value to this reorg.
- **Maven `<artifactId>assurance</artifactId>` is unchanged by default.** See Open Questions if you want to reconsider.
- **Prior plan archived, not deleted.** `tasks/plan.md` and `tasks/todo.md` are recreated in this reorg; the 2.0-era versions live next to them as `tasks/plan-2.0.md` / `tasks/todo-2.0.md`.

## Affected Files (verified by grep)

**UI flatten + rename — references to update:**
- `assurance-ui/assurance/forge.config.ts` — depth of `ENGINE_BUNDLE_DIR` resolution changes; 3 string mentions of `assurance/`
- `assurance-ui/assurance/tests/e2e/helpers.ts` — 2 error-message strings
- `README.md` — 3 occurrences
- `SPEC.md` — Sections 4 (project tree) and 6 (commands)
- `docs/dev-getting-started.md` — ~10 `cd` instructions
- `docs/ipc-contract.md` — 1 model-mirror reference

**Engine rename — additional references:**
- `ui/forge.config.ts` (after Phase 1) — `ENGINE_BUNDLE_DIR` path components + 3 hint strings mentioning the `assurance/` project
- `README.md` — beyond UI-related, plus build instructions
- `SPEC.md` — Section 4 project tree, Section 6 command tables
- `docs/dev-getting-started.md` — Maven invocations
- `.vscode/launch.json` — `"projectName": "assurance"` may need updating (depends on whether VS Code Java extension uses directory name or Maven `<artifactId>`)

**Out of scope (do NOT touch):**
- Java package `com.markallenjohnson.assurance.*` (Java namespace, not OS path)
- The runtime H2 database location `~/.assurance/assurance.mv.db` (user-data, not source)
- The `.assurance/` untracked dir in the current working tree (a separate cleanup item, see T0.1)

## Dependency Graph

```
P0  Preconditions
     │
     ▼
P1  UI flatten + rename → ui/
     1.1 git mv directory
     1.2 Fix ui/-internal path refs
     1.3 Update root docs
     1.4 Reinstall + verify
     │
     ▼  🟢 CP-A: UI at ui/; engine still at assurance/
     │
P2  Engine rename → engine/
     2.1 git mv assurance engine
     2.2 Fix ui/forge.config.ts engine refs
     2.3 Update root docs
     2.4 Update IDE configs
     2.5 Verify integration
     │
     ▼  🟢 CP-B: Both renames complete; full smoke test
     │
P3  Final sweep
     3.1 Comprehensive grep
     3.2 Manual parity smoke test
     3.3 Commit
     │
     ▼  🟢 CP-C: Reorg complete
```

## Task List

### Phase 0 — Preconditions

#### T0.1 — Stabilize working tree
**Description:** Resolve the currently-unstaged `assurance/pom.xml` change (commit or revert). `.assurance/` is already covered by `.gitignore` (lines 9-10) and no longer surfaces in `git status` — no action needed there.
**Acceptance:**
- [ ] `git status` is clean apart from the planned reorg files
**Verify:**
- `git status --short` shows no working-tree changes outside the reorg scope
**Files likely touched:** `assurance/pom.xml`
**Scope:** XS

#### T0.2 — Baseline verification
**Description:** Run the full build/test from both projects at their current paths to capture a "before" reference.
**Acceptance:**
- [ ] `mvn clean test -Pdevelopment` from `assurance/` is green
- [ ] `yarn install && yarn lint && yarn test` from `assurance-ui/assurance/` is green
- [ ] Test counts and lint warnings recorded for comparison after the reorg
**Verify:** Run the commands; note the totals.
**Scope:** XS

🟢 **CP-0**: Tree clean, current build green.

---

### Phase 1 — UI flatten + rename to `ui/`

#### T1.1 — Perform the directory move
**Description:** Move `assurance-ui/assurance/` to `ui/` via `git mv`. Remove the now-empty wrapper.
**Acceptance:**
- [ ] `ui/` contains every file previously under `assurance-ui/assurance/`
- [ ] `assurance-ui/` no longer exists
- [ ] `git status` reports renames (R), not delete+add
**Verify:**
- `git diff --stat HEAD` shows file paths changing prefix only
- `git log --follow ui/forge.config.ts` resolves to the pre-rename history
- The `.DS_Store` in the wrapper is removed (not migrated)
**Commands (reference):**
```
git mv assurance-ui/assurance ui
rm -f assurance-ui/.DS_Store
rmdir assurance-ui
```
**Scope:** XS

#### T1.2 — Fix UI-internal path references
**Description:** `ui/forge.config.ts` has an `ENGINE_BUNDLE_DIR` that resolves relative to `__dirname` with `'..', '..', 'assurance', 'target', ...`. After the flatten, `ui/forge.config.ts` is one level shallower than the old `assurance-ui/assurance/forge.config.ts`, so the leading `..` count drops by one. Also update the three hint strings that mention `assurance/`. Update both error-message strings in `tests/e2e/helpers.ts` that name `assurance-ui/assurance/`.
**Acceptance:**
- [ ] `ENGINE_BUNDLE_DIR` resolves to the correct absolute path (still pointing at the unchanged `assurance/target/engine-bundle/current/`)
- [ ] `grep -r "assurance-ui" ui/` returns nothing
**Verify:**
- `cd ui && rm -rf node_modules out && yarn install && yarn lint && yarn test` — all green
**Files likely touched:** `ui/forge.config.ts`, `ui/tests/e2e/helpers.ts`
**Scope:** S

#### T1.3 — Update root docs and planning files
**Description:** Replace `assurance-ui/assurance/` with `ui/` (and `cd assurance-ui/assurance` with `cd ui`, plus `../assurance-ui/assurance` etc.) across the markdown surfaces.
**Acceptance:**
- [ ] `README.md`, `SPEC.md`, `docs/dev-getting-started.md`, `docs/ipc-contract.md` all reference `ui/` only
- [ ] `grep -rn "assurance-ui" . --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target` returns only matches inside archived files (`tasks/*-2.0.md`)
**Files likely touched:** `README.md`, `SPEC.md`, `docs/dev-getting-started.md`, `docs/ipc-contract.md`
**Scope:** S

#### T1.4 — Reinstall and integration verify
**Description:** Confirm the dev loop works end-to-end at the new path before moving on.
**Acceptance:**
- [ ] `yarn install`, `yarn lint`, `yarn test` from `ui/` — all green
- [ ] `yarn start` from `ui/` spawns the engine and the renderer receives `ASSURANCE_READY`
- [ ] One happy-path smoke test (load scan definitions OR run a scan) round-trips
**Verify:** Manual; capture pass counts and compare against T0.2.
**Scope:** XS (verification only)

🟢 **CP-A**: UI lives at `ui/`. Engine still at `assurance/`. Dev loop works.

---

### Phase 2 — Engine rename to `engine/`

#### T2.1 — Perform the directory move
**Description:** `git mv assurance engine`. Maven sees no difference because `pom.xml` is relative.
**Acceptance:**
- [ ] `engine/pom.xml` and `engine/src/` exist; `assurance/` no longer exists at the top level
- [ ] `git log --follow engine/pom.xml` resolves to pre-rename history
**Verify:** `git status` shows clean renames.
**Scope:** XS

#### T2.2 — Update `ui/forge.config.ts` engine references
**Description:** Update `ENGINE_BUNDLE_DIR`'s path components (`'..', 'assurance', 'target', ...` → `'..', 'engine', 'target', ...`) and the three hint strings mentioning "assurance/ project" / "assurance/ directory".
**Acceptance:**
- [ ] `ENGINE_BUNDLE_DIR` resolves to `engine/target/engine-bundle/current/`
- [ ] No remaining mention of `assurance/` as a *directory* in `ui/forge.config.ts` (the Java package string in unrelated comments stays)
**Verify:** `cd ui && yarn start` — the `packageAfterCopy` hook resolves the engine bundle (when invoked).
**Files likely touched:** `ui/forge.config.ts`
**Scope:** XS

#### T2.3 — Update root docs and planning files
**Description:** Replace `cd assurance` / `from \`assurance/\`` / SPEC Section 4 tree node etc. with `engine/`.
**Acceptance:**
- [ ] `README.md`, `SPEC.md`, `docs/dev-getting-started.md` reference `engine/` for the Maven project
- [ ] `grep -rE "(\.\./)?assurance/" . --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target` returns no matches (except in `tasks/*-2.0.md`)
**Files likely touched:** `README.md`, `SPEC.md`, `docs/dev-getting-started.md`
**Scope:** S

#### T2.4 — Update IDE configs
**Description:** `.vscode/launch.json` has `"projectName": "assurance"`, which is directory-coupled for this workspace. Change it to `"engine"`.
**Acceptance:**
- [ ] `.vscode/launch.json` `projectName` reads `"engine"`
- [ ] Reopening the workspace in VS Code resolves the "Application" launch configuration
**Files likely touched:** `.vscode/launch.json`
**Scope:** XS

#### T2.5 — Maven and integration verification
**Description:** Confirm Maven still builds from the new location and the engine still launches from `ui/`.
**Acceptance:**
- [ ] `cd engine && rm -rf target && mvn clean test -Pdevelopment` is green
- [ ] `cd ui && rm -rf out && yarn start` launches and engine handshakes
- [ ] Manual happy-path smoke test (load scan list, run a scan, merge a result)
**Scope:** XS (verification only)

🟢 **CP-B**: Engine at `engine/`. UI at `ui/`. Full smoke test passes.

---

### Phase 3 — Final sweep

#### T3.1 — Comprehensive grep for residual references
**Description:** One last grep pass for any reference to the old paths. Exclude the Java package name string and the archived 2.0 plan files.
**Acceptance:**
- [ ] `grep -rnE "(\.\./)?assurance/|assurance-ui" . --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target --exclude-dir=out` returns only `com.markallenjohnson.assurance` package references and `tasks/*-2.0.md` matches.
**Scope:** XS

#### T3.2 — Manual parity smoke test
**Description:** Final end-to-end run from `yarn start`. Load scan definitions, create one, run a scan, merge a result, restore a deleted item. Confirm everything still works.
**Acceptance:**
- [ ] All flows function exactly as before the reorg
**Scope:** XS (manual verification)

#### T3.3 — Commit
**Description:** One combined commit at the end of Phase 3 covering both renames, the reference updates, the IDE config change, and the prior `pom.xml` cleanup from T0.1 if not already separately committed.
**Acceptance:**
- [ ] `git log --stat -M` for the new commit shows file renames (not delete+add)
- [ ] Commit message clearly identifies "rename only, no behavior change"
**Scope:** XS

🟢 **CP-C**: Reorganization complete; ready to push.

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| A path reference is missed and only surfaces in `yarn package` builds | Med | T3.1 grep sweep + run `yarn package` once before tagging |
| `ENGINE_BUNDLE_DIR` depth math wrong in T1.2 or T2.2 | High | Each phase explicitly runs `yarn start` and `yarn package` (if practical) before moving on |
| `.vscode/launch.json` `projectName` is path-coupled and breaks Java tooling | Low | T2.4 verifies in-IDE; one-line fix if it does break |
| Stale `node_modules` / `target` from old paths confuse tooling | Med | Each phase deletes and reinstalls (`rm -rf node_modules out`, `mvn clean`) |
| Mid-reorg interruption leaves repo in a broken intermediate state | Med | Two-phase structure means a partial run still leaves either pre-Phase-1 or post-Phase-1 in a working state; `git reset --hard <baseline>` rolls back |

## Decisions (resolved 2026-05-13)

1. **Commit granularity:** One combined commit at the end of Phase 3.
2. **Maven `<artifactId>`:** Stays as `assurance` — Maven coordinate, not coupled to OS path.
3. **Untracked `.assurance/` directory:** Already covered by `.gitignore`; no action needed.
4. **`.vscode/launch.json` `projectName`:** Directory-coupled; T2.4 updates it to `engine`.

## Rollback

Because the reorg lands as a single combined commit at the end of Phase 3, there is no in-progress committed state to roll back to mid-flight. If the reorg is abandoned before T3.3 commits, `git restore .`, `git clean -fd ui engine`, and removing the recreated `tasks/plan.md` / `tasks/todo.md` (after `git mv -f tasks/plan-2.0.md tasks/plan.md` / `tasks/todo-2.0.md tasks/todo.md`) returns the working tree to baseline. After T3.3, `git reset --hard <baseline-commit>` is the rollback.
