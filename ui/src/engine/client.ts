import { EventEmitter } from 'events';
import WebSocket, { RawData } from 'ws';

/**
 * Client-side IPC protocol version. Must equal the server's
 * {@code Application.IPC_PROTOCOL_VERSION}. Bump in lockstep on any breaking
 * wire change (see docs/ipc-contract.md "Versioning").
 */
export const IPC_PROTOCOL_VERSION = 1;

const HELLO_TIMEOUT_MS = 10_000;
const PROTOCOL_MISMATCH_ERROR_CODE = -32000;
/**
 * W3C-standard {@code WebSocket} OPEN readyState value. Hard-coded rather
 * than read off {@code WebSocket.OPEN} so the readiness gate also works
 * under jsdom (Jest), where {@code import WebSocket from 'ws'} resolves to
 * {@code ws/browser.js} — a stub function whose static {@code OPEN}
 * property is {@code undefined}. The numeric constant is fixed by the W3C
 * WebSocket specification.
 */
const WS_OPEN = 1;

export interface EngineClientConnectOptions {
  url: string;
  protocolVersion: number;
  uiVersion: string;
  helloTimeoutMs?: number;
}

export interface EngineHandshakeInfo {
  engineVersion: string;
  protocolVersion: number;
}

export interface EngineClientFatalError extends Error {
  reason: 'protocol-mismatch' | 'handshake-failed' | 'connection-closed' | 'connection-error';
  details?: unknown;
}

interface JsonRpcResponse {
  jsonrpc: '2.0';
  id?: number | string | null;
  result?: unknown;
  error?: {
    code: number;
    message: string;
    data?: unknown;
  };
  method?: string;
  params?: unknown;
}

function makeFatalError(
  reason: EngineClientFatalError['reason'],
  message: string,
  details?: unknown,
): EngineClientFatalError {
  const err = new Error(message) as EngineClientFatalError;
  err.reason = reason;
  err.details = details;
  return err;
}

/**
 * Singleton WebSocket client owned by the Electron main process. Connects to
 * the Java engine over loopback after the engine prints its ready line, runs
 * the JSON-RPC `hello`/`welcome` handshake, and then exposes the open socket
 * for higher-level RPC plumbing (T1.4).
 *
 * Per SPEC §3, this client does not auto-reconnect: a closed socket after the
 * handshake completes is a fatal session error.
 */
class EngineClient extends EventEmitter {
  private socket: WebSocket | null = null;
  private handshakeInfo: EngineHandshakeInfo | null = null;
  private fatalError: EngineClientFatalError | null = null;
  /**
   * Set to {@code true} the moment we initiate a deliberate teardown of the
   * connection — either by sending the JSON-RPC {@code shutdown} notification
   * (which causes the engine to reply with a normal close frame) or by
   * calling {@link close} directly. The post-handshake {@code 'close'} and
   * {@code 'error'} listeners check this flag and stay silent when it is
   * set, so a clean app-quit does not surface as a fatal error to the user.
   *
   * The flag is set-once: per SPEC §3 this client does not auto-reconnect,
   * so once we've torn the session down it stays down.
   */
  private deliberateShutdown = false;

  /**
   * Connects to the engine, performs the JSON-RPC handshake, and resolves with
   * the engine's reported version info. Rejects with an
   * {@link EngineClientFatalError} on mismatch, handshake error, or socket
   * failure before `welcome` is received.
   */
  async connect(opts: EngineClientConnectOptions): Promise<EngineHandshakeInfo> {
    if (this.socket) {
      throw new Error('Engine client already connected.');
    }

    const helloTimeoutMs = opts.helloTimeoutMs ?? HELLO_TIMEOUT_MS;

    const socket = new WebSocket(opts.url);
    this.socket = socket;

    return new Promise<EngineHandshakeInfo>((resolve, reject) => {
      let settled = false;

      const finishOk = (info: EngineHandshakeInfo) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        detachHandshakeListeners();
        this.handshakeInfo = info;
        this.attachPostHandshakeListeners(socket);
        resolve(info);
      };

      const finishErr = (err: EngineClientFatalError) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        detachHandshakeListeners();
        this.fatalError = err;
        this.socket = null;
        try { socket.close(); } catch { /* ignore */ }
        reject(err);
      };

      const detachHandshakeListeners = () => {
        socket.off('open', onOpen);
        socket.off('message', onMessage);
        socket.off('close', onClose);
        socket.off('error', onError);
      };

      const timer = setTimeout(() => {
        finishErr(makeFatalError(
          'handshake-failed',
          `Engine did not respond to hello within ${helloTimeoutMs}ms.`,
        ));
      }, helloTimeoutMs);

      const onOpen = () => {
        const helloFrame = JSON.stringify({
          jsonrpc: '2.0',
          method: 'hello',
          params: {
            uiVersion: opts.uiVersion,
            protocolVersion: opts.protocolVersion,
          },
          id: 1,
        });
        try {
          socket.send(helloFrame);
        } catch (err) {
          finishErr(makeFatalError('handshake-failed', 'Failed to send hello frame.', err));
        }
      };

      const onMessage = (data: RawData) => {
        let parsed: JsonRpcResponse;
        try {
          parsed = JSON.parse(typeof data === 'string' ? data : data.toString('utf8')) as JsonRpcResponse;
        } catch (err) {
          finishErr(makeFatalError('handshake-failed', 'Engine returned non-JSON during handshake.', err));
          return;
        }

        if (parsed.id !== 1) {
          // Pre-handshake we expect only the welcome response. Anything else is a
          // protocol violation.
          finishErr(makeFatalError(
            'handshake-failed',
            `Engine sent unexpected frame before completing handshake: ${JSON.stringify(parsed)}`,
          ));
          return;
        }

        if (parsed.error) {
          const reason: EngineClientFatalError['reason'] =
            parsed.error.code === PROTOCOL_MISMATCH_ERROR_CODE ? 'protocol-mismatch' : 'handshake-failed';
          finishErr(makeFatalError(reason, `Engine handshake error: ${parsed.error.message}`, parsed.error));
          return;
        }

        const result = parsed.result as Partial<EngineHandshakeInfo> | undefined;
        if (!result || typeof result.engineVersion !== 'string' || typeof result.protocolVersion !== 'number') {
          finishErr(makeFatalError('handshake-failed', 'Engine welcome payload missing required fields.', parsed.result));
          return;
        }

        if (result.protocolVersion !== opts.protocolVersion) {
          finishErr(makeFatalError(
            'protocol-mismatch',
            `Engine protocol version ${result.protocolVersion} does not match UI protocol version ${opts.protocolVersion}.`,
            result,
          ));
          return;
        }

        finishOk({ engineVersion: result.engineVersion, protocolVersion: result.protocolVersion });
      };

      const onClose = (code: number, reasonBuf: Buffer) => {
        const reasonText = reasonBuf?.toString('utf8') ?? '';
        finishErr(makeFatalError(
          'connection-closed',
          `Engine WebSocket closed during handshake (code=${code}, reason=${reasonText}).`,
          { code, reason: reasonText },
        ));
      };

      const onError = (err: Error) => {
        finishErr(makeFatalError('connection-error', `Engine WebSocket error: ${err.message}`, err));
      };

      socket.on('open', onOpen);
      socket.on('message', onMessage);
      socket.on('close', onClose);
      socket.on('error', onError);
    });
  }

  /**
   * Returns the negotiated handshake info, or null if the client has not yet
   * completed a successful handshake.
   */
  getHandshakeInfo(): EngineHandshakeInfo | null {
    return this.handshakeInfo;
  }

  isConnected(): boolean {
    return this.socket !== null && this.socket.readyState === WS_OPEN && this.handshakeInfo !== null;
  }

  /**
   * Sends a raw text frame on the underlying socket. Used by higher-level RPC
   * plumbing (T1.4) once the client is connected.
   */
  sendFrame(frame: string): void {
    if (!this.socket || this.socket.readyState !== WS_OPEN) {
      throw new Error('Engine client is not connected.');
    }
    this.socket.send(frame);
  }

  /**
   * Sends a JSON-RPC `shutdown` notification (no `id`) and returns immediately.
   * Per SPEC §3, the client then waits at the process level for the engine to
   * exit (handled by {@link stopEngine}).
   *
   * Marks the upcoming socket close as deliberate <em>before</em> sending —
   * the engine replies to {@code shutdown} by closing the socket with
   * {@code CloseFrame.NORMAL} (code 1000), and the close-frame can race back
   * to our {@code 'close'} listener before this method even returns. Setting
   * the flag first ensures the listener correctly classifies the close as
   * expected rather than fatal.
   */
  sendShutdownNotification(): void {
    if (!this.socket || this.socket.readyState !== WS_OPEN) {
      return;
    }
    this.deliberateShutdown = true;
    try {
      this.socket.send(JSON.stringify({ jsonrpc: '2.0', method: 'shutdown' }));
    } catch (err) {
      console.warn('[engine] Failed to send shutdown notification:', err);
    }
  }

  /**
   * Closes the underlying socket without further handshakes. Idempotent.
   *
   * Marks the close as deliberate so the post-handshake {@code 'close'}
   * listener does not surface a fatal error to the user — see
   * {@link deliberateShutdown}.
   */
  close(): void {
    this.deliberateShutdown = true;
    if (this.socket) {
      try { this.socket.close(); } catch { /* ignore */ }
      this.socket = null;
    }
    this.handshakeInfo = null;
  }

  getFatalError(): EngineClientFatalError | null {
    return this.fatalError;
  }

  /**
   * Test-only: drives the post-handshake state machine against a caller-
   * supplied fake socket so unit tests can exercise the close/error/message
   * pathways without spinning up a real WebSocket server. Production code
   * goes through {@link connect}.
   */
  attachPostHandshakeListenersForTest(socket: WebSocket): void {
    this.socket = socket;
    this.attachPostHandshakeListeners(socket);
  }

  private attachPostHandshakeListeners(socket: WebSocket): void {
    socket.on('message', (data) => {
      // Higher-level RPC dispatcher (T1.4) consumes these.
      this.emit('frame', typeof data === 'string' ? data : data.toString('utf8'));
    });
    socket.on('close', (code, reasonBuf) => {
      // Clean up regardless of cause. Only escalate to 'fatal' when the
      // close was unilateral (engine died, network glitch, etc.). A close
      // we initiated — via close() or sendShutdownNotification() — is part
      // of the normal app-quit path and must not surface a user-facing
      // error dialog (which would also block app.quit() on the modal).
      this.socket = null;
      this.handshakeInfo = null;
      if (this.deliberateShutdown) {
        return;
      }
      const reason = reasonBuf?.toString('utf8') ?? '';
      const err = makeFatalError(
        'connection-closed',
        `Engine WebSocket closed unexpectedly (code=${code}, reason=${reason}).`,
        { code, reason },
      );
      this.fatalError = err;
      this.emit('fatal', err);
    });
    socket.on('error', (err) => {
      // Sockets sometimes emit a write/end error during deliberate
      // teardown (e.g. our shutdown frame's send racing the engine's
      // close). Swallow those — the corresponding 'close' will follow and
      // is already handled above. Non-deliberate errors remain fatal.
      if (this.deliberateShutdown) {
        return;
      }
      const fatal = makeFatalError('connection-error', `Engine WebSocket error: ${err.message}`, err);
      this.fatalError = fatal;
      this.emit('fatal', fatal);
    });
  }
}

export const engineClient = new EngineClient();
export type { EngineClient };

/**
 * Test-only factory: returns a fresh {@link EngineClient} so unit tests can
 * exercise the post-handshake state machine in isolation from the
 * module-level singleton (which would otherwise carry state between cases).
 * Production code uses {@link engineClient}.
 */
export function newEngineClientForTest(): EngineClient {
  return new EngineClient();
}
