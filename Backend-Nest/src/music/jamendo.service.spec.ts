import { of } from 'rxjs';
import { JamendoService } from './jamendo.service';

// In-memory stand-in for Redis so the unit test needs no server.
const mockStore = new Map<string, string>();
jest.mock('ioredis', () => ({
  __esModule: true,
  default: jest.fn().mockImplementation(() => ({
    on: jest.fn(),
    disconnect: jest.fn(),
    get: jest.fn(async (k: string) => mockStore.get(k) ?? null),
    set: jest.fn(async (k: string, v: string) => void mockStore.set(k, v)),
  })),
}));

const ok = (results: unknown[]) => ({ data: { headers: { status: 'success' }, results } });

function makeService(responses: ReturnType<typeof ok>[]) {
  const http = { get: jest.fn(() => of(responses.shift() ?? ok([]))) };
  const config = { get: () => 'client', getOrThrow: () => 'redis://unused' };
  return { service: new JamendoService(http as any, config as any), http };
}

beforeEach(() => mockStore.clear());

describe('JamendoService cache', () => {
  it('retries when Jamendo spuriously answers with zero results', async () => {
    const { service, http } = makeService([ok([]), ok([]), ok([{ id: '1' }])]);
    const data = await service.trackSearch('love');
    expect(data.results).toEqual([{ id: '1' }]);
    expect(http.get).toHaveBeenCalledTimes(3);
  });

  it('never caches an empty response (the bug that hid tracks for an hour)', async () => {
    const { service, http } = makeService([ok([]), ok([]), ok([]), ok([{ id: '1' }])]);
    expect((await service.trackSearch('love')).results).toEqual([]);
    expect(mockStore.size).toBe(0);
    expect((await service.trackSearch('love')).results).toEqual([{ id: '1' }]);
    expect(http.get).toHaveBeenCalledTimes(4);
  });

  it('serves a cached non-empty response without calling Jamendo again', async () => {
    const { service, http } = makeService([ok([{ id: '1' }])]);
    await service.trackSearch('love');
    await service.trackSearch('love');
    expect(http.get).toHaveBeenCalledTimes(1);
  });

  it('skips the network entirely for an empty id list', async () => {
    const { service, http } = makeService([]);
    expect(await service.tracksByIds([])).toEqual({ results: [] });
    expect(http.get).not.toHaveBeenCalled();
  });
});
