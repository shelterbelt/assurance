# Repository Reorganization — TODO

Flat checklist of tasks from `tasks/plan.md`, in execution order. Check off as completed. Pause at each 🟢 Checkpoint for human review.

## Phase 0 — Preconditions

- [ ] **T0.1** Stabilize working tree. Resolve unstaged `assurance/pom.xml` change (commit or revert). `.assurance/` is already in `.gitignore` — no action needed there.
- [ ] **T0.2** Baseline verification. `mvn clean test -Pdevelopment` from `assurance/` green; `yarn install && yarn lint && yarn test` from `assurance-ui/assurance/` green. Capture pass counts as the "before" reference.

🟢 **CP-0** — Tree clean, current build green. **← awaiting human review**

## Phase 1 — UI flatten + rename to `ui/`

- [ ] **T1.1** `git mv assurance-ui/assurance ui`; remove the empty wrapper (`.DS_Store` then `rmdir assurance-ui`).
- [ ] **T1.2** Fix UI-internal path refs. `ui/forge.config.ts`: `ENGINE_BUNDLE_DIR` depth drops by one (`'..', '..', 'assurance', …` → `'..', 'assurance', …`) and the three `assurance/` hint strings. `ui/tests/e2e/helpers.ts`: two error-message strings.
- [ ] **T1.3** Update root docs (`README.md`, `SPEC.md`, `docs/dev-getting-started.md`, `docs/ipc-contract.md`). Grep clean for `assurance-ui` outside `tasks/*-2.0.md`.
- [ ] **T1.4** Reinstall + integration verify. `cd ui && rm -rf node_modules out && yarn install && yarn lint && yarn test && yarn start` all clean; one happy-path round-trip.

🟢 **CP-A** — UI at `ui/`; engine still launches from `assurance/`. **← awaiting human review**

## Phase 2 — Engine rename to `engine/`

- [ ] **T2.1** `git mv assurance engine`.
- [ ] **T2.2** Update `ui/forge.config.ts`. `ENGINE_BUNDLE_DIR` path components `'assurance'` → `'engine'`; the three hint strings updated; no remaining mention of `assurance/` as a directory.
- [ ] **T2.3** Update root docs (`README.md`, `SPEC.md`, `docs/dev-getting-started.md`). Grep clean for `(\.\./)?assurance/` outside Java-package contexts and `tasks/*-2.0.md`.
- [ ] **T2.4** Update `.vscode/launch.json` `projectName` from `"assurance"` to `"engine"` (directory-coupled in this workspace).
- [ ] **T2.5** Verify Maven build from `engine/` and `yarn start` from `ui/`. Manual happy-path smoke test (load scan list, run scan, merge a result).

🟢 **CP-B** — Engine at `engine/`, UI at `ui/`; full smoke test passes. **← awaiting human review**

## Phase 3 — Final sweep

- [ ] **T3.1** Comprehensive grep for residual references. `grep -rnE "(\.\./)?assurance/|assurance-ui" . --exclude-dir=.git --exclude-dir=node_modules --exclude-dir=target --exclude-dir=out` returns only Java-package or archived-2.0-plan matches.
- [ ] **T3.2** Manual parity smoke test. End-to-end through scan create, scan run, per-result merge, restore.
- [ ] **T3.3** One combined commit covering both renames, reference updates, IDE config, and (if not separately committed in T0.1) the prior `pom.xml` cleanup. Rename-only subject line.

🟢 **CP-C** — Reorganization complete; ready to push. **← awaiting human review**
