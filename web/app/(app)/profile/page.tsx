"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth-context";
import { api, ApiError } from "@/lib/api";
import { UserIcon } from "@/components/icons";
import { PlanCard } from "@/components/PlanCard";
import type { Friend } from "@/lib/types";

const PRIVACY_OPTIONS = ["public", "friends", "private"];

const inputClass =
  "w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20";
const labelClass = "mb-1 block text-xs font-semibold text-text-secondary";

export default function ProfilePage() {
  const { user, refreshProfile } = useAuth();

  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);
  const [form, setForm] = useState(() => ({
    full_name: user?.fullName ?? "",
    bio: user?.bio ?? "",
    date_of_birth: user?.dateOfBirth ?? "",
    phone_number: user?.phoneNumber ?? "",
    profile_privacy: user?.profilePrivacy ?? "public",
    email_privacy: user?.emailPrivacy ?? "private",
    phone_privacy: user?.phonePrivacy ?? "private",
    genres: user?.genres.join(", ") ?? "",
  }));

  if (!user) return null;

  function startEditing() {
    setForm({
      full_name: user!.fullName,
      bio: user!.bio,
      date_of_birth: user!.dateOfBirth ?? "",
      phone_number: user!.phoneNumber,
      profile_privacy: user!.profilePrivacy,
      email_privacy: user!.emailPrivacy,
      phone_privacy: user!.phonePrivacy,
      genres: user!.genres.join(", "),
    });
    setEditError(null);
    setEditing(true);
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setEditError(null);
    try {
      await api.updateProfile({
        ...form,
        genres: form.genres.split(",").map((g) => g.trim()).filter(Boolean),
      });
      await refreshProfile();
      setEditing(false);
    } catch (err) {
      setEditError(err instanceof ApiError ? err.message : "Couldn't save your profile");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="max-w-2xl pt-6">
      <div className="mb-8 flex items-center justify-between gap-6">
        <div className="flex items-center gap-6">
          <span className="flex h-28 w-28 items-center justify-center rounded-full bg-gradient-to-br from-primary-deep to-purple shadow-xl shadow-primary/20">
            <UserIcon className="h-14 w-14 text-white/90" />
          </span>
          <div>
            <p className="text-xs font-bold uppercase tracking-widest text-primary">Profile</p>
            <h1 className="mt-1 text-3xl font-extrabold tracking-tight text-text-primary">{user.fullName}</h1>
            <p className="text-sm text-text-secondary">
              @{user.userName} · {user.email}
            </p>
          </div>
        </div>
        {!editing && (
          <button
            onClick={startEditing}
            className="shrink-0 rounded-full border border-white/10 px-4 py-2 text-sm font-semibold text-text-primary transition-colors hover:bg-white/5"
          >
            Edit profile
          </button>
        )}
      </div>

      <PlanCard />

      {editing ? (
        <form onSubmit={handleSave} className="glass mt-6 rounded-2xl border border-white/10 p-6 shadow-xl">
          <h2 className="mb-4 text-lg font-bold text-text-primary">Edit profile</h2>

          <div className="mb-4">
            <label className={labelClass}>Full name</label>
            <input value={form.full_name} onChange={(e) => setForm({ ...form, full_name: e.target.value })} className={inputClass} />
          </div>

          <div className="mb-4">
            <label className={labelClass}>Bio</label>
            <textarea
              value={form.bio}
              onChange={(e) => setForm({ ...form, bio: e.target.value })}
              rows={3}
              className={inputClass}
            />
          </div>

          <div className="mb-4 grid grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Date of birth</label>
              <input
                type="date"
                value={form.date_of_birth}
                onChange={(e) => setForm({ ...form, date_of_birth: e.target.value })}
                className={inputClass}
              />
            </div>
            <div>
              <label className={labelClass}>Phone number</label>
              <input
                value={form.phone_number}
                onChange={(e) => setForm({ ...form, phone_number: e.target.value })}
                className={inputClass}
              />
            </div>
          </div>

          <div className="mb-4">
            <label className={labelClass}>Genres (comma-separated)</label>
            <input value={form.genres} onChange={(e) => setForm({ ...form, genres: e.target.value })} className={inputClass} />
          </div>

          <h3 className="mb-3 mt-6 text-xs font-bold uppercase tracking-widest text-text-tertiary">Privacy</h3>
          <div className="mb-4 grid grid-cols-3 gap-4">
            {(
              [
                ["profile_privacy", "Profile"],
                ["email_privacy", "Email"],
                ["phone_privacy", "Phone"],
              ] as const
            ).map(([key, label]) => (
              <div key={key}>
                <label className={labelClass}>{label}</label>
                <select
                  value={form[key]}
                  onChange={(e) => setForm({ ...form, [key]: e.target.value })}
                  className={inputClass}
                >
                  {PRIVACY_OPTIONS.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              </div>
            ))}
          </div>

          {editError && <p className="mb-3 text-sm text-error">{editError}</p>}

          <div className="flex gap-3">
            <button
              type="submit"
              disabled={saving}
              className="rounded-full bg-gradient-to-r from-primary to-primary-deep px-5 py-2 text-sm font-bold text-background shadow-lg shadow-primary/25 disabled:opacity-50"
            >
              {saving ? "Saving…" : "Save changes"}
            </button>
            <button
              type="button"
              onClick={() => setEditing(false)}
              className="rounded-full border border-white/10 px-5 py-2 text-sm font-semibold text-text-secondary hover:text-text-primary"
            >
              Cancel
            </button>
          </div>
        </form>
      ) : (
        <dl className="mt-8 grid grid-cols-2 gap-4 text-sm">
          <div>
            <dt className="text-text-tertiary">Bio</dt>
            <dd className="text-text-primary">{user.bio || "—"}</dd>
          </div>
          <div>
            <dt className="text-text-tertiary">Genres</dt>
            <dd className="text-text-primary">{user.genres.length ? user.genres.join(", ") : "—"}</dd>
          </div>
          <div>
            <dt className="text-text-tertiary">Privacy</dt>
            <dd className="text-text-primary">
              Profile: {user.profilePrivacy} · Email: {user.emailPrivacy} · Phone: {user.phonePrivacy}
            </dd>
          </div>
          <div>
            <dt className="text-text-tertiary">Member since</dt>
            <dd className="text-text-primary">{new Date(user.createdAt).toLocaleDateString()}</dd>
          </div>
        </dl>
      )}

      <FriendsSection />
    </div>
  );
}

// Friends here = people allowed to see fields set to "friends" privacy (one-directional).
function FriendsSection() {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [userName, setUserName] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  const load = () => api.friends().then(setFriends).catch(() => {});
  useEffect(() => {
    load();
  }, []);

  async function add(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    try {
      const res = await api.addFriend(userName.trim());
      setMessage(res.message);
      setUserName("");
      await load();
    } catch (err) {
      setMessage(err instanceof ApiError ? err.message : "Couldn't add friend");
    }
  }

  async function remove(id: number) {
    await api.removeFriend(id).catch(() => {});
    await load();
  }

  return (
    <div className="glass mt-8 rounded-2xl border border-white/10 p-6 shadow-xl">
      <h2 className="text-lg font-bold text-text-primary">Friends</h2>
      <p className="mb-4 text-xs text-text-secondary">People here can see the parts of your profile set to “friends”.</p>
      <form onSubmit={add} className="mb-4 flex gap-2">
        <input
          value={userName}
          onChange={(e) => setUserName(e.target.value)}
          placeholder="username"
          aria-label="Friend username"
          className={inputClass}
        />
        <button
          type="submit"
          disabled={!userName.trim()}
          className="shrink-0 rounded-full bg-gradient-to-r from-primary to-primary-deep px-4 py-2 text-sm font-semibold text-background disabled:opacity-50"
        >
          Add
        </button>
      </form>
      {message && <p className="mb-3 text-xs text-text-secondary">{message}</p>}
      <ul className="flex flex-col gap-2">
        {friends.map((f) => (
          <li key={f.id} className="flex items-center justify-between text-sm text-text-primary">
            <span>
              {f.fullName} <span className="text-text-tertiary">@{f.userName}</span>
            </span>
            <button onClick={() => remove(f.id)} className="text-xs text-text-secondary hover:text-error">
              Remove
            </button>
          </li>
        ))}
        {friends.length === 0 && <li className="text-sm text-text-tertiary">No friends added yet.</li>}
      </ul>
    </div>
  );
}
