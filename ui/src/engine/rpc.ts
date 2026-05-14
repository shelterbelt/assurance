import { EngineClient, EngineClientFatalError, engineClient } from './client';

/**
 * JSON-RPC 2.0 request/response correlator over the {@link EngineClient}
 * transport. Owns the {@code id} allocator, the in-flight request map, and
 * the notification fan-out registry.
 *
 * Wire shapes match {@code docs/ipc-contract.md}.
 */

export const JSONRPC_VERSION = '2.0';

const HELLO_RESERVED_ID = 1;
const REQUEST_TIMEOUT_MS = 30_000;

export interface JsonRpcError {
  code: number;
  message: string;
  data?: unknown;
}

export class EngineRpcError extends Error {
  readonly code: number;
  readonly data: unknown;

  constructor(error: JsonRpcError) {
    super(error.message);
    this.name = 'EngineRpcError';
    this.code = error.code;
    this.data = error.data;
  }
}

interface JsonRpcEnvelope {
  jsonrpc: '2.0';
  id?: number | string | null;
  method?: string;
  params?: unknown;
  result?: unknown;
  error?: JsonRpcError;
}

interface PendingRequest {
  resolve: (value: unknown) => void;
  reject: (reason: Error) => void;
  method: string;
  timer: ReturnType<typeof setTimeout>;
}

type NotificationListener = (params: unknown) => void;

/**
 * Encodes a JSON-RPC 2.0 request frame. Exported so tests can exercise the
 * envelope shape without a transport.
 */
export function encodeRequest(method: string, params: unknown, id: number | string): string {
  const envelope: JsonRpcEnvelope = {
    jsonrpc: JSONRPC_VERSION,
    method,
    id,
  };
  if (params !== undefined) {
    envelope.params = params;
  }
  return JSON.stringify(envelope);
}

/**
 * Decodes a single inbound JSON-RPC text frame. Throws on malformed input;
 * does not validate semantics beyond shape.
 */
export function decodeFrame(frame: string): JsonRpcEnvelope {
  const parsed = JSON.parse(frame) as unknown;
  if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
    throw new Error('Frame is not a JSON object.');
  }
  const env = parsed as JsonRpcEnvelope;
  if (env.jsonrpc !== JSONRPC_VERSION) {
    throw new Error(`Frame jsonrpc version is ${env.jsonrpc ?? 'absent'}, expected '${JSONRPC_VERSION}'.`);
  }
  return env;
}

export class EngineRpc {
  private readonly client: EngineClient;
  private readonly pending = new Map<number, PendingRequest>();
  private readonly notificationListeners = new Map<string, Set<NotificationListener>>();
  private nextId = HELLO_RESERVED_ID + 1;
  private wired = false;
  private terminated: Error | null = null;
  private requestTimeoutMs: number;

  constructor(client: EngineClient, requestTimeoutMs: number = REQUEST_TIMEOUT_MS) {
    this.client = client;
    this.requestTimeoutMs = requestTimeoutMs;
  }

  /**
   * Sends an `assurance.<verb><Noun>` request and resolves with the JSON-RPC
   * `result` payload. Rejects with an {@link EngineRpcError} on a JSON-RPC
   * error response, or with a generic {@code Error} on transport failure.
   */
  async call<TResult = unknown>(method: string, params: unknown = {}): Promise<TResult> {
    this.ensureWired();
    if (this.terminated) {
      throw this.terminated;
    }

    const id = this.allocateId();
    const frame = encodeRequest(method, params, id);

    return new Promise<TResult>((resolve, reject) => {
      const timer = setTimeout(() => {
        const pending = this.pending.get(id);
        if (!pending) return;
        this.pending.delete(id);
        pending.reject(new Error(
          `Engine call '${method}' (id=${id}) timed out after ${this.requestTimeoutMs}ms.`,
        ));
      }, this.requestTimeoutMs);

      this.pending.set(id, {
        resolve: (value) => resolve(value as TResult),
        reject,
        method,
        timer,
      });

      try {
        this.client.sendFrame(frame);
      } catch (err) {
        const pending = this.pending.get(id);
        if (pending) {
          clearTimeout(pending.timer);
          this.pending.delete(id);
        }
        reject(err instanceof Error ? err : new Error(String(err)));
      }
    });
  }

  /**
   * Registers a listener for server-initiated notifications with the given
   * method name. Returns an unsubscribe function. Multiple listeners are
   * permitted per method.
   */
  onNotification(method: string, listener: NotificationListener): () => void {
    this.ensureWired();
    let listeners = this.notificationListeners.get(method);
    if (!listeners) {
      listeners = new Set();
      this.notificationListeners.set(method, listeners);
    }
    listeners.add(listener);
    return () => {
      const set = this.notificationListeners.get(method);
      if (!set) return;
      set.delete(listener);
      if (set.size === 0) {
        this.notificationListeners.delete(method);
      }
    };
  }

  /**
   * Test-only hook: dispatches a frame as if it had arrived from the engine.
   * Real production use goes through the {@link EngineClient}'s `frame` event.
   */
  ingestFrameForTest(frame: string): void {
    this.handleFrame(frame);
  }

  /** Returns the count of currently in-flight requests. */
  inFlightCount(): number {
    return this.pending.size;
  }

  private ensureWired(): void {
    if (this.wired) return;
    this.client.on('frame', (frame: string) => this.handleFrame(frame));
    this.client.on('fatal', (err: EngineClientFatalError) => this.failAllPending(err));
    this.wired = true;
  }

  private handleFrame(frame: string): void {
    let envelope: JsonRpcEnvelope;
    try {
      envelope = decodeFrame(frame);
    } catch (err) {
      console.warn('[rpc] Discarding malformed inbound frame:', err);
      return;
    }

    if (envelope.method && (envelope.id === undefined || envelope.id === null)) {
      this.dispatchNotification(envelope.method, envelope.params);
      return;
    }

    if (envelope.id === undefined || envelope.id === null || typeof envelope.id !== 'number') {
      console.warn('[rpc] Discarding response with non-numeric id:', envelope.id);
      return;
    }

    const pending = this.pending.get(envelope.id);
    if (!pending) {
      console.warn(`[rpc] Discarding response for unknown id=${envelope.id}.`);
      return;
    }
    clearTimeout(pending.timer);
    this.pending.delete(envelope.id);

    if (envelope.error) {
      pending.reject(new EngineRpcError(envelope.error));
      return;
    }
    pending.resolve(envelope.result);
  }

  private dispatchNotification(method: string, params: unknown): void {
    const listeners = this.notificationListeners.get(method);
    if (!listeners || listeners.size === 0) {
      // Silent drop: notifications without subscribers are not errors.
      return;
    }
    for (const listener of Array.from(listeners)) {
      try {
        listener(params);
      } catch (err) {
        console.error(`[rpc] Notification listener for '${method}' threw:`, err);
      }
    }
  }

  private failAllPending(reason: Error): void {
    this.terminated = reason;
    for (const [, pending] of this.pending) {
      clearTimeout(pending.timer);
      pending.reject(reason);
    }
    this.pending.clear();
  }

  private allocateId(): number {
    const id = this.nextId;
    this.nextId += 1;
    return id;
  }
}

export const engineRpc = new EngineRpc(engineClient);
