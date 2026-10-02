"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { EventRolesResponse, OtherUser, PendingEventInvite } from "@/lib/types";

export function EventManagePanel({
  eventId,
  currentUserRole,
  onChanged,
}: {
  eventId: number | string;
  currentUserRole: "owner" | "editor" | "listener";
  onChanged: () => void;
}) {
  const [open, setOpen] = useState(false);
  const [roles, setRoles] = useState<EventRolesResponse | null>(null);
  const [pending, setPending] = useState<PendingEventInvite[]>([]);
  const [users, setUsers] = useState<OtherUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | "">("");
  const [inviteRole, setInviteRole] = useState<"organizer" | "manager" | "attendee">("attendee");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  async function loadPanelData() {
    try {
      const [rolesRes, pendingRes, usersRes] = await Promise.all([
        api.eventRoles(eventId),
        api.eventPendingInvites(eventId),
        api.otherUsers(),
      ]);
      setRoles(rolesRes);
      setPending(pendingRes.pendingInvites);
      setUsers(usersRes);
    } catch {
      // manage panel data is secondary to the event page itself - fail quietly
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-open, not a render loop
    if (open) loadPanelData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedUserId) return;
    setBusy(true);
    setMessage(null);
    try {
      const res = await api.inviteToEvent(eventId, selectedUserId, inviteRole);
      setMessage(res.message);
      setSelectedUserId("");
      await loadPanelData();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Couldn't send invite");
    } finally {
      setBusy(false);
    }
  }

  async function handleAssignEditor(userId: number) {
    setBusy(true);
    try {
      await api.assignEditor(eventId, userId);
      await loadPanelData();
      onChanged();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Couldn't assign editor");
    } finally {
      setBusy(false);
    }
  }

  async function handleTransferOwnership(userId: number) {
    if (!window.confirm("Transfer ownership? You'll become an editor.")) return;
    setBusy(true);
    try {
      await api.transferOwnership(eventId, userId);
      await loadPanelData();
      onChanged();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Couldn't transfer ownership");
    } finally {
      setBusy(false);
    }
  }

  async function handleRemoveRole(userId: number) {
    setBusy(true);
    try {
      await api.removeUserRole(eventId, userId);
      await loadPanelData();
      onChanged();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Couldn't remove user");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mb-8">
      <button
        onClick={() => setOpen((v) => !v)}
        className="rounded-full border border-white/10 px-4 py-2 text-sm font-semibold text-text-primary transition-colors hover:bg-white/5"
      >
        {open ? "Hide management" : "Manage event"}
      </button>

      {open && (
        <div className="glass mt-4 rounded-2xl border border-white/10 p-6 shadow-xl">
          <h3 className="mb-3 text-sm font-bold uppercase tracking-widest text-text-tertiary">Invite someone</h3>
          <form onSubmit={handleInvite} className="mb-6 flex flex-wrap gap-2">
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value ? Number(e.target.value) : "")}
              className="rounded-xl border border-white/5 bg-white/[0.03] px-3 py-2 text-sm text-text-primary focus:border-primary/40 focus:outline-none"
            >
              <option value="">Select a user…</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName} (@{u.userName})
                </option>
              ))}
            </select>
            <select
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value as typeof inviteRole)}
              className="rounded-xl border border-white/5 bg-white/[0.03] px-3 py-2 text-sm text-text-primary focus:border-primary/40 focus:outline-none"
            >
              <option value="attendee">Attendee</option>
              {currentUserRole === "owner" && <option value="manager">Manager (editor)</option>}
              {currentUserRole === "owner" && <option value="organizer">Organizer (owner)</option>}
            </select>
            <button
              type="submit"
              disabled={busy || !selectedUserId}
              className="rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background disabled:opacity-50"
            >
              Invite
            </button>
          </form>

          {message && <p className="mb-4 text-xs text-text-secondary">{message}</p>}

          {pending.length > 0 && (
            <>
              <h3 className="mb-2 text-sm font-bold uppercase tracking-widest text-text-tertiary">Pending invites</h3>
              <ul className="mb-6 space-y-1 text-sm text-text-secondary">
                {pending.map((p) => (
                  <li key={p.userId}>
                    {p.name} — invited as {p.role}
                  </li>
                ))}
              </ul>
            </>
          )}

          <h3 className="mb-2 text-sm font-bold uppercase tracking-widest text-text-tertiary">Roles</h3>
          <div className="space-y-1">
            {roles &&
              Object.entries(roles.usersByRole).flatMap(([role, list]) =>
                list.map((u) => (
                  <div key={u.id} className="flex items-center justify-between rounded-lg px-2 py-1.5 text-sm hover:bg-white/[0.03]">
                    <span className="text-text-primary">
                      {u.name} <span className="text-text-tertiary">· {role}</span>
                    </span>
                    {currentUserRole === "owner" && role !== "owner" && (
                      <div className="flex gap-2">
                        {role === "listener" && (
                          <button onClick={() => handleAssignEditor(u.id)} disabled={busy} className="text-xs font-semibold text-primary hover:underline">
                            Make editor
                          </button>
                        )}
                        <button onClick={() => handleTransferOwnership(u.id)} disabled={busy} className="text-xs font-semibold text-primary hover:underline">
                          Make owner
                        </button>
                        <button onClick={() => handleRemoveRole(u.id)} disabled={busy} className="text-xs font-semibold text-error hover:underline">
                          Remove
                        </button>
                      </div>
                    )}
                  </div>
                )),
              )}
          </div>
        </div>
      )}
    </div>
  );
}
