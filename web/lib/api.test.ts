// Run with `npm test` (node --test; Node 24 strips the TypeScript natively - no test deps).
import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";

// Minimal browser globals api.ts touches.
const storage = new Map<string, string>();
Object.assign(globalThis, {
  window: globalThis,
  localStorage: {
    getItem: (k: string) => storage.get(k) ?? null,
    setItem: (k: string, v: string) => void storage.set(k, v),
    removeItem: (k: string) => void storage.delete(k),
  },
});

const { api, ApiError, saveTokens } = await import("./api.ts");

type Call = { url: string; auth?: string };
let calls: Call[] = [];

function mockFetch(handler: (url: string, auth?: string) => { status: number; body: unknown }) {
  globalThis.fetch = (async (url: string, init?: RequestInit) => {
    const auth = (init?.headers as Record<string, string> | undefined)?.Authorization;
    calls.push({ url, auth });
    const { status, body } = handler(url, auth);
    return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
  }) as typeof fetch;
}

beforeEach(() => {
  storage.clear();
  calls = [];
});

test("a 401 triggers one refresh, then the original request is retried with the new token", async () => {
  saveTokens({ access: "old", refresh: "r1" });
  mockFetch((url, auth) => {
    if (url.endsWith("/users/token/refresh")) return { status: 200, body: { tokens: { access: "new", refresh: "r2" } } };
    return auth === "Bearer new" ? { status: 200, body: { id: 1 } } : { status: 401, body: { message: "Unauthorized" } };
  });

  assert.deepEqual(await api.profile(), { id: 1 });
  assert.deepEqual(
    calls.map((c) => c.auth ?? "refresh"),
    ["Bearer old", "refresh", "Bearer new"],
  );
  assert.equal(storage.get("musicroom_refresh"), "r2");
});

test("a failed refresh surfaces the original 401 instead of looping", async () => {
  saveTokens({ access: "old", refresh: "revoked" });
  mockFetch(() => ({ status: 401, body: { message: "Unauthorized" } }));

  await assert.rejects(api.profile(), (err: unknown) => err instanceof ApiError && err.status === 401);
  assert.equal(calls.length, 2);
});

test("validation errors (arrays of messages) are joined into one readable message", async () => {
  mockFetch(() => ({ status: 400, body: { message: ["a is bad", "b is bad"] } }));
  await assert.rejects(api.profile(), { message: "a is bad, b is bad" });
});
