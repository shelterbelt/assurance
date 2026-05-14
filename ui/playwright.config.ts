import { defineConfig } from '@playwright/test';

/**
 * Playwright config for Assurance's E2E suite.
 *
 * The suite runs against the packaged Electron app under
 * {@code out/Assurance-<platform>-<arch>/}. Engine cold-start (Java JVM +
 * Spring context + H2 schema install + WebSocket bind) takes 10–30 seconds
 * on a fresh DB; the scan itself and the cooperative app.quit() teardown
 * fit comfortably inside the remainder. The global {@code timeout} is
 * sized to absorb a slow cold-start plus the test body plus teardown with
 * room to spare — anything beyond this would indicate a real regression
 * (e.g. the engine's shutdown hook hanging) rather than benign warmup.
 * Trace + screenshot + video on first failure make CI post-mortems
 * tractable without re-running.
 */
export default defineConfig({
    testDir: 'tests/e2e',
    timeout: 120_000,
    expect: {
        timeout: 30_000,
    },
    // E2E tests share a single package binary and write to ~/.assurance under
    // a redirected HOME — running them in parallel against the same out/
    // tree would race on engine startup ports and DB files.
    fullyParallel: false,
    workers: 1,
    forbidOnly: !!process.env.CI,
    reporter: 'list',
    use: {
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
    },
});
