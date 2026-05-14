import { useEffect, useState } from 'react';

import { engine, EngineCallError } from '../../api/engine';

interface PingResult {
    pong: boolean;
    timestamp: string;
}

/**
 * Dev-only engine connectivity indicator. Polls `assurance.ping` through the
 * renderer → main → JSON-RPC → Java engine path and reflects status as a
 * colored dot. Rendered only when `process.env.NODE_ENV !== 'production'`.
 */
export default function EnginePingButton() {
    const [connected, setConnected] = useState<boolean | null>(null);
    const [status, setStatus] = useState<string>('Checking engine connection...');

    useEffect(() => {
        let active = true;
        const pollConnection = async () => {
            let isConnected = false;
            try {
                const result = await engine.call<PingResult>('assurance.ping', {});
                isConnected = result.pong === true;
            } catch (err) {
                isConnected = false;
                if (err instanceof EngineCallError) {
                    console.debug('[dev-tools] ping failed', err.code, err.message);
                } else {
                    const message = err instanceof Error ? err.message : String(err);
                    console.debug('[dev-tools] ping failed', message);
                }
            }

            if (!active) {
                return;
            }
            setConnected(isConnected);
            setStatus(isConnected ? 'Engine connected' : 'Engine disconnected');
        };

        void pollConnection();
        const intervalId = window.setInterval(() => {
            void pollConnection();
        }, 3000);

        return () => {
            active = false;
            window.clearInterval(intervalId);
        };
    }, []);

    const indicatorClassName = connected === null
        ? 'dev-tools-ping-icon dev-tools-ping-icon-checking'
        : connected
            ? 'dev-tools-ping-icon dev-tools-ping-icon-connected'
            : 'dev-tools-ping-icon dev-tools-ping-icon-disconnected';

    return (
        <div className="dev-tools-bar" role="status" aria-live="polite">
            <span
                className="dev-tools-ping-button"
                aria-label={status}
                title={status}
            >
                <span className={indicatorClassName} aria-hidden="true" />
            </span>
            <span className="dev-tools-status" data-testid="ping-status">{status}</span>
        </div>
    );
}
