import { test, expect, _electron, ElectronApplication } from '@playwright/test';

import {
    PACKAGE_PREREQ_HINT,
    ScanFixture,
    createScanFixture,
    destroyScanFixture,
    resolvePackagedBinary,
} from './helpers';

/**
 * Phase 5 / T5.3 — single happy-path Playwright test.
 *
 * Boots the packaged Electron app, creates a scan definition over two
 * diverging temp directories, runs a scan, and asserts that at least one
 * comparison result is rendered. Per `tasks/plan.md` this is the one
 * end-to-end smoke that locks down the full renderer ↔ main ↔ WS ↔ engine
 * round trip; richer interactive flows live in the manual parity walkthrough
 * (T6.1).
 *
 * The test is deliberately conservative about timing because engine
 * cold-start (JVM + Spring context + H2 schema install + WebSocket bind)
 * dominates the wall-clock budget on a fresh DB. The {@code playwright.config.ts}
 * global timeout matches.
 *
 * Sandboxing strategy
 * -------------------
 * The Electron launcher's bundled mode resolves the engine's cwd from
 * {@code os.homedir()}; the engine's H2 URL is a relative
 * {@code ./.assurance/assurance}. Pointing {@code HOME} (and
 * {@code USERPROFILE} on Windows) at a fixture-owned temp directory before
 * launch redirects the entire side-effect surface — the H2 file, any
 * restore-staging directories, and the legacy 1.x location resolution — into
 * the fixture so the developer's real {@code ~/.assurance/} is never
 * touched. The fixture is unconditionally removed in {@code test.afterEach}.
 */

let electronApp: ElectronApplication | null = null;
let fixture: ScanFixture | null = null;

/**
 * Cooperative shutdown path:
 *   Playwright's {@link ElectronApplication#close()} sends app.quit() to
 *   the main process, which in our case fires {@code before-quit} →
 *   {@code engineClient.sendShutdownNotification()} → engine replies with
 *   a normal close-frame → {@code stopEngine()} awaits the Java child's
 *   exit (SIGTERM grace then SIGKILL safety in src/engine/launcher.ts) →
 *   {@code app.quit()} resolves and the Electron main process exits.
 *
 *   The {@link EngineClient} explicitly classifies the engine's reply
 *   close-frame as "deliberate" so it does not surface as a fatal-error
 *   dialog (see src/engine/client.ts {@code deliberateShutdown}). Without
 *   that classification, the modal dialog would block app.quit() and the
 *   teardown would hang here — that bug was originally masked by a
 *   force-kill workaround in this file; the workaround was removed once
 *   the underlying close-handler regression was fixed.
 */
test.afterEach(async () => {
    if (electronApp) {
        await electronApp.close();
        electronApp = null;
    }
    if (fixture) {
        try {
            await destroyScanFixture(fixture);
        } catch (err) {
            console.warn('[e2e] destroyScanFixture failed:', err);
        }
        fixture = null;
    }
});

test('create a scan definition, run a scan, see a result', async () => {
    const packaged = resolvePackagedBinary();
    test.skip(packaged === null, PACKAGE_PREREQ_HINT);
    if (!packaged) {
        return;
    }

    fixture = await createScanFixture();

    // HOME / USERPROFILE redirection lands the engine's H2 database under
    // the fixture so the test never writes to the real ~/.assurance/. The
    // launcher inherits this env into the spawned Java child.
    const env = {
        ...process.env,
        HOME: fixture.root,
        USERPROFILE: fixture.root,
    } as Record<string, string>;

    electronApp = await _electron.launch({
        executablePath: packaged.binary,
        args: [],
        env,
    });

    const appWindow = await electronApp.firstWindow();
    await appWindow.waitForLoadState('domcontentloaded');

    // The Scan tab is the default; wait for the scan-list shell to be
    // present (loading or empty state both render this container) before
    // driving the form so we know the renderer has booted.
    await expect(appWindow.locator('.scan-list-container')).toBeVisible({ timeout: 60_000 });

    // The "New" button lives inside the scan-list footer; scope selectors
    // to specific containers so they don't collide with the NavBar tabs
    // (which also render with role=button) or the ActionsPanel buttons.
    const scanList = appWindow.locator('.scan-list-container');
    await scanList.locator('.scan-manage-button', { hasText: 'New' }).click();
    const definitionPanel = appWindow.locator('.scan-definition-panel');
    await expect(definitionPanel).toBeVisible();

    const scanName = `e2e-${Date.now()}`;
    await definitionPanel.locator('.scan-name-field').fill(scanName);
    await definitionPanel.locator('.source-location').fill(fixture.source);
    await definitionPanel.locator('.target-location').fill(fixture.target);

    // Confirm the definition. ScanDefinitionPanel hides itself on success
    // and bumps the parent's scanDefinitionsVersion so the list reloads.
    await definitionPanel
        .locator('.scan-definition-manage-button', { hasText: /^OK$/ })
        .click();
    await expect(definitionPanel).toBeHidden({ timeout: 30_000 });

    // The new entry should appear in the list. Click it to select.
    const scanRow = scanList.locator('.scan-list-item', { hasText: scanName });
    await expect(scanRow).toBeVisible({ timeout: 30_000 });
    await scanRow.click();

    // With a definition selected, the Scan button enables and fires
    // assurance.performScan(id, merge=false). The engine returns the new
    // scanId synchronously, then progress + completion notifications stream
    // back over the WS transport. The Scan button lives inside the
    // ActionsPanel; we scope to that container to avoid colliding with the
    // NavBar's "Scan" tab (also role=button).
    const actionsPanel = appWindow.locator('.actions-panel-container');
    const scanButton = actionsPanel.locator('.action-button', { hasText: /^Scan$/ });
    await expect(scanButton).toBeEnabled();
    await scanButton.click();

    // FeedbackPanel renders "Scan <id> completed: N results." once the
    // engine emits assurance.scanCompleted. Two of the three fixture files
    // are divergent so we expect at least 2 results — but the wording
    // ("results"/"result") is what we anchor on, not the count.
    const completedBanner = appWindow.locator('.feedback-row-success', {
        hasText: /Scan \d+ completed/,
    });
    await expect(completedBanner).toBeVisible({ timeout: 60_000 });

    // ResultsPanel auto-promotes the just-finished scan and reloads. The
    // helpers.ts fixture guarantees a non-empty result list (source-only
    // and target-only files); we assert the list group has at least one
    // rendered row, then specifically that the source-only and target-only
    // basenames are reachable (tolerant to ordering, which is up to the
    // engine).
    const resultItems = appWindow.locator('.scan-result-item');
    await expect(resultItems.first()).toBeVisible({ timeout: 30_000 });
    expect(await resultItems.count()).toBeGreaterThanOrEqual(2);

    const resultsPanel = appWindow.locator('.results-panel-container');
    await expect(resultsPanel).toContainText('source-only.txt');
    await expect(resultsPanel).toContainText('target-only.txt');
});
