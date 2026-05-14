import { EventEmitter } from 'events';

import { decodeFrame, encodeRequest, EngineRpc, EngineRpcError, JSONRPC_VERSION } from '../rpc';

interface FakeFatalError extends Error {
  reason?: string;
  details?: unknown;
}

class FakeEngineClient extends EventEmitter {
  sent: string[] = [];

  sendFrame(frame: string): void {
    this.sent.push(frame);
  }
}

function makeRpc(): { rpc: EngineRpc; client: FakeEngineClient } {
  const client = new FakeEngineClient();
  // Cast to satisfy the EngineRpc constructor signature without pulling the
  // real EngineClient (which depends on the `ws` runtime).
  const rpc = new EngineRpc(client as unknown as ConstructorParameters<typeof EngineRpc>[0]);
  return { rpc, client };
}

test('encodeRequest builds a valid JSON-RPC 2.0 envelope', () => {
  const frame = encodeRequest('assurance.ping', { foo: 'bar' }, 7);
  const parsed = JSON.parse(frame);
  expect(parsed.jsonrpc).toBe(JSONRPC_VERSION);
  expect(parsed.method).toBe('assurance.ping');
  expect(parsed.params).toEqual({ foo: 'bar' });
  expect(parsed.id).toBe(7);
});

test('encodeRequest omits params when undefined', () => {
  const frame = encodeRequest('m', undefined, 1);
  const parsed = JSON.parse(frame);
  expect('params' in parsed).toBe(false);
});

test('decodeFrame rejects non-objects', () => {
  expect(() => decodeFrame('[]')).toThrow(/not a JSON object/);
  expect(() => decodeFrame('null')).toThrow(/not a JSON object/);
  expect(() => decodeFrame('"x"')).toThrow(/not a JSON object/);
});

test('decodeFrame rejects mismatched jsonrpc version', () => {
  expect(() => decodeFrame('{"jsonrpc":"1.0"}')).toThrow(/jsonrpc version/);
});

test('call() resolves with the result for a matching response', async () => {
  const { rpc, client } = makeRpc();

  const promise = rpc.call<{ pong: boolean }>('assurance.ping', {});

  expect(client.sent.length).toBe(1);
  const sent = JSON.parse(client.sent[0]);
  expect(sent.method).toBe('assurance.ping');
  expect(typeof sent.id).toBe('number');

  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    result: { pong: true },
    id: sent.id,
  }));

  const result = await promise;
  expect(result).toEqual({ pong: true });
  expect(rpc.inFlightCount()).toBe(0);
});

test('call() rejects with EngineRpcError on JSON-RPC error', async () => {
  const { rpc, client } = makeRpc();
  const promise = rpc.call('assurance.notReal', {});

  const sent = JSON.parse(client.sent[0]);
  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    error: { code: -32601, message: 'Method not found', data: { foo: 1 } },
    id: sent.id,
  }));

  await expect(promise).rejects.toMatchObject({
    code: -32601,
    message: 'Method not found',
    data: { foo: 1 },
  });
  await expect(promise).rejects.toBeInstanceOf(EngineRpcError);
});

test('call() rejects on transport sendFrame failure', async () => {
  const { rpc, client } = makeRpc();
  const original = client.sendFrame.bind(client);
  client.sendFrame = (frame: string) => {
    if (frame.includes('"will-fail"')) {
      throw new Error('not connected');
    }
    return original(frame);
  };

  await expect(rpc.call('will-fail', {})).rejects.toThrow(/not connected/);
  expect(rpc.inFlightCount()).toBe(0);
});

test('multiple in-flight calls resolve to their own ids', async () => {
  const { rpc, client } = makeRpc();
  const a = rpc.call<number>('a');
  const b = rpc.call<number>('b');

  const sentA = JSON.parse(client.sent[0]);
  const sentB = JSON.parse(client.sent[1]);
  expect(sentA.id).not.toBe(sentB.id);

  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    result: 22,
    id: sentB.id,
  }));
  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    result: 11,
    id: sentA.id,
  }));

  expect(await a).toBe(11);
  expect(await b).toBe(22);
});

test('notifications dispatch to listeners and unsubscribe cleanly', () => {
  const { rpc } = makeRpc();
  const events: unknown[] = [];
  const off = rpc.onNotification('assurance.scanProgress', (params) => events.push(params));

  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    method: 'assurance.scanProgress',
    params: { scanId: 1, progress: { phase: 'comparing' } },
  }));

  expect(events.length).toBe(1);
  expect(events[0]).toEqual({ scanId: 1, progress: { phase: 'comparing' } });

  off();

  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    method: 'assurance.scanProgress',
    params: { scanId: 2 },
  }));
  expect(events.length).toBe(1);
});

test('fatal client error rejects all in-flight calls', async () => {
  const { rpc, client } = makeRpc();
  const promise = rpc.call('m', {});

  const fatal: FakeFatalError = new Error('boom');
  fatal.reason = 'connection-closed';
  client.emit('fatal', fatal);

  await expect(promise).rejects.toThrow(/boom/);
  expect(rpc.inFlightCount()).toBe(0);

  // Subsequent calls fail fast.
  await expect(rpc.call('m2')).rejects.toThrow(/boom/);
});

test('responses with unknown ids are silently dropped', () => {
  const { rpc } = makeRpc();
  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    result: 1,
    id: 9999,
  }));
  expect(rpc.inFlightCount()).toBe(0);
});

test('malformed frames are dropped without crashing pending calls', async () => {
  const { rpc, client } = makeRpc();
  const promise = rpc.call<number>('m');
  rpc.ingestFrameForTest('this is not json');

  const sent = JSON.parse(client.sent[0]);
  rpc.ingestFrameForTest(JSON.stringify({
    jsonrpc: JSONRPC_VERSION,
    result: 5,
    id: sent.id,
  }));
  expect(await promise).toBe(5);
});
