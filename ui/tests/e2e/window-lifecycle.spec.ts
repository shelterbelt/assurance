import { test, expect, _electron, ElectronApplication } from '@playwright/test';

import {
    PACKAGE_PREREQ_HINT,
    ScanFixture,
    createScanFixture,
    destroyScanFixture,
    resolvePackagedBinary,
} from './helpers';

/**
 * Window lifecycle E2E coverage. The renderer ↔ main-process boundary has
 * two distinct lifetimes — the app and the BrowserWindow — and conflating
 * them caused real bugs (per-window IPC registration crashed Electron's
 * ipcMain.handle on the second window-open). The happy-path test covers
 * the single-window first-open case; this file locks down what happens
 * when the window's lifetime is shorter than the app's.
 *
 * Regression: "Attempted to register a second handler for 'selectPath'"
 * on macOS dock-click reopen. The original code did
 * ipcMain.handle('selectPath', ...) inside createWindow(), so:
 *   1. User opens the app (createWindow #1, IPC registration #1 → fine).
 *   2. User closes the window (legitimate on macOS — app stays resident).
 *   3. User clicks the dock icon → app.on('activate') called createWindow()
 *      again → ipcMain.handle threw because the handler from step 1 was
 *      still registered (Electron's contract: handle() throws on duplicate
 *      registration).
 *
 * The fix lifted IPC handler registration to app-lifetime
 * (registerAppLifetimeIPC in src/index.ts, called once from
 * app.whenReady) and routed the engine-notification sink through a
 * mutable currentMainWindow that the same long-lived forwarders read
 * lazily. The unit-test peer
 * (src/ipc/__tests__/main-handlers.test.ts → "a sink that reads its
 * target window lazily survives a close+reopen cycle") covers the
 * notification fan-out half; this E2E covers the ipcMain.handle half,
 * which can only be exercised against a real Electron runtime.
 */

let electronApp: ElectronApplication | null = null;
let fixture: ScanFixture | null = null;

test.afterEach(async () => {
    if (electronApp) {
        await electronApp.close();
        electronApp = null;
    }
    if (fixture) {
        await destroyScanFixture(fixture).catch(() => undefined);
        fixture = null;
    }
});

test('close then reopen the main window does not crash with a duplicate-handler error', async () => {
    const packaged = resolvePackagedBinary();
    test.skip(packaged === null, PACKAGE_PREREQ_HINT);
    if (!packaged) {
        return;
    }

    fixture = await createScanFixture();
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

    const firstWindow = await electronApp.firstWindow();
    await firstWindow.waitForLoadState('domcontentloaded');
    await expect(firstWindow.locator('.scan-list-container')).toBeVisible({ timeout: 60_000 });

    // Step 2: programmatically close the window (does not quit the app on
    // macOS — app stays resident, mirroring the user's flow).
    await electronApp.evaluate(({ BrowserWindow }) => {
        const win = BrowserWindow.getAllWindows()[0];
        if (win) {
            win.close();
        }
    });

    // Wait for the window to actually destruct so the next createWindow()
    // is a clean second invocation, not a no-op re-show.
    await electronApp.evaluate(({ BrowserWindow }) => {
        return new Promise<void>((resolve) => {
            const tick = () => {
                if (BrowserWindow.getAllWindows().length === 0) {
                    resolve();
                } else {
                    setTimeout(tick, 50);
                }
            };
            tick();
        });
    });

    // Step 3: reopen via app.emit('activate') — the same path Electron
    // uses on dock-click. Pre-fix this would throw inside createWindow's
    // registerIPCHandlers call. Post-fix it must complete silently and
    // produce a fresh window.
    await electronApp.evaluate(({ app }) => {
        app.emit('activate', { preventDefault: () => undefined } as Electron.Event, false);
    });

    const secondWindow = await electronApp.waitForEvent('window', { timeout: 30_000 });
    await secondWindow.waitForLoadState('domcontentloaded');
    await expect(secondWindow.locator('.scan-list-container')).toBeVisible({ timeout: 60_000 });
});
