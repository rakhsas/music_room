"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { Device } from "@/lib/types";
import { PauseIcon, PlayIcon, NextIcon, PlusIcon, XIcon } from "@/components/icons";

const DEVICE_ID_KEY = "musicroom_device_id";

function getOrCreateDeviceId() {
  let id = localStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = `web-${crypto.randomUUID()}`;
    localStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
}

export default function DevicesPage() {
  const [myDevices, setMyDevices] = useState<Device[]>([]);
  const [delegatedToMe, setDelegatedToMe] = useState<Device[]>([]);
  const [thisDeviceId, setThisDeviceId] = useState<string | null>(null);
  const [registering, setRegistering] = useState(false);
  const [delegateEmail, setDelegateEmail] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const [mine, delegated] = await Promise.all([api.myDevices(), api.delegatedToMe()]);
      setMyDevices(mine);
      setDelegatedToMe(delegated);
    } catch {
      // best-effort
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- initial fetch-on-mount, not a render loop
    setThisDeviceId(getOrCreateDeviceId());
    load();
  }, []);

  async function registerThisDevice() {
    if (!thisDeviceId) return;
    setRegistering(true);
    setError(null);
    try {
      await api.registerDevice(thisDeviceId, "This browser", navigator.platform || "Web", "1.0.0");
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't register this device");
    } finally {
      setRegistering(false);
    }
  }

  async function delegate(deviceId: string) {
    const email = delegateEmail[deviceId]?.trim();
    if (!email) return;
    setBusy(deviceId);
    setError(null);
    try {
      await api.delegateDevice(deviceId, email);
      setDelegateEmail((prev) => ({ ...prev, [deviceId]: "" }));
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't delegate control");
    } finally {
      setBusy(null);
    }
  }

  async function revoke(deviceId: string, userId: number) {
    setBusy(deviceId);
    try {
      await api.revokeDevice(deviceId, userId);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't revoke access");
    } finally {
      setBusy(null);
    }
  }

  async function sendCommand(deviceId: string, action: "play" | "pause" | "skip") {
    setBusy(deviceId);
    setError(null);
    try {
      await api.sendDeviceCommand(deviceId, action);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't send command");
    } finally {
      setBusy(null);
    }
  }

  const isRegistered = myDevices.some((d) => d.deviceId === thisDeviceId);

  return (
    <div className="pt-6">
      <h1 className="mb-6 text-2xl font-bold tracking-tight text-text-primary">Devices</h1>
      {error && <p className="mb-4 text-sm text-error">{error}</p>}

      {!isRegistered && thisDeviceId && (
        <div className="glass mb-8 max-w-md rounded-2xl border border-white/10 p-5 shadow-xl">
          <p className="mb-3 text-sm text-text-secondary">
            Register this browser as a controllable device, so you can delegate playback control to a friend.
          </p>
          <button
            onClick={registerThisDevice}
            disabled={registering}
            className="rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background shadow-lg shadow-primary/25 disabled:opacity-50"
          >
            {registering ? "Registering…" : "Register this device"}
          </button>
        </div>
      )}

      <h2 className="mb-3 text-lg font-bold text-text-primary">Your devices</h2>
      <div className="mb-8 flex flex-col gap-3">
        {myDevices.map((d) => (
          <div key={d.deviceId} className="glass rounded-2xl border border-white/10 p-5 shadow-xl">
            <div className="mb-3 flex items-center justify-between">
              <div>
                <p className="text-sm font-bold text-text-primary">
                  {d.name} {d.deviceId === thisDeviceId && <span className="text-xs text-primary">(this browser)</span>}
                </p>
                <p className="text-xs text-text-tertiary">{d.platform}</p>
              </div>
            </div>

            <p className="mb-1 text-xs font-semibold text-text-secondary">Delegated to</p>
            <div className="mb-3 flex flex-wrap gap-2">
              {d.delegates?.map((del) => (
                <span key={del.id} className="flex items-center gap-1 rounded-full bg-white/5 px-3 py-1 text-xs text-text-primary">
                  {del.name}
                  <button onClick={() => revoke(d.deviceId, del.id)} disabled={busy === d.deviceId} aria-label={`Revoke ${del.name}`}>
                    <XIcon className="h-3 w-3 text-text-tertiary hover:text-error" />
                  </button>
                </span>
              ))}
              {(!d.delegates || d.delegates.length === 0) && <span className="text-xs text-text-tertiary">No one yet</span>}
            </div>

            <div className="flex gap-2">
              <input
                type="email"
                placeholder="friend@example.com"
                value={delegateEmail[d.deviceId] ?? ""}
                onChange={(e) => setDelegateEmail((prev) => ({ ...prev, [d.deviceId]: e.target.value }))}
                className="flex-1 rounded-xl border border-white/5 bg-white/[0.03] px-3 py-2 text-sm text-text-primary focus:border-primary/40 focus:outline-none"
              />
              <button
                onClick={() => delegate(d.deviceId)}
                disabled={busy === d.deviceId}
                className="flex items-center gap-1 rounded-full border border-white/10 px-3 py-2 text-xs font-semibold text-text-primary hover:bg-white/5 disabled:opacity-50"
              >
                <PlusIcon className="h-3.5 w-3.5" />
                Delegate
              </button>
            </div>
          </div>
        ))}
        {myDevices.length === 0 && <p className="text-sm text-text-secondary">No devices registered yet.</p>}
      </div>

      <h2 className="mb-3 text-lg font-bold text-text-primary">Delegated to you</h2>
      <div className="flex flex-col gap-3">
        {delegatedToMe.map((d) => (
          <div key={d.deviceId} className="glass flex items-center justify-between rounded-2xl border border-white/10 p-5 shadow-xl">
            <div>
              <p className="text-sm font-bold text-text-primary">{d.name}</p>
              <p className="text-xs text-text-tertiary">owned by {d.owner?.name}</p>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => sendCommand(d.deviceId, "play")}
                disabled={busy === d.deviceId}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-white/5 text-text-primary hover:bg-white/10 disabled:opacity-50"
                aria-label="Play"
              >
                <PlayIcon className="h-4 w-4" />
              </button>
              <button
                onClick={() => sendCommand(d.deviceId, "pause")}
                disabled={busy === d.deviceId}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-white/5 text-text-primary hover:bg-white/10 disabled:opacity-50"
                aria-label="Pause"
              >
                <PauseIcon className="h-4 w-4" />
              </button>
              <button
                onClick={() => sendCommand(d.deviceId, "skip")}
                disabled={busy === d.deviceId}
                className="flex h-9 w-9 items-center justify-center rounded-full bg-white/5 text-text-primary hover:bg-white/10 disabled:opacity-50"
                aria-label="Skip"
              >
                <NextIcon className="h-4 w-4" />
              </button>
            </div>
          </div>
        ))}
        {delegatedToMe.length === 0 && <p className="text-sm text-text-secondary">No devices delegated to you yet.</p>}
      </div>
    </div>
  );
}
