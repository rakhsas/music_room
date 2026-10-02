"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";

export default function RegisterPage() {
  const router = useRouter();
  const { loginWithGoogleIdToken } = useAuth();
  const [fullName, setFullName] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await api.register({ full_name: fullName, username, email, password });
      setDone(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't create your account");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleGoogle(idToken: string) {
    setError(null);
    try {
      await loginWithGoogleIdToken(idToken);
      router.push("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't sign up with Google");
    }
  }

  if (done) {
    return (
      <div className="glass rounded-3xl border border-white/10 p-8 shadow-2xl text-center">
        <h1 className="mb-2 text-2xl font-extrabold tracking-tight text-text-primary">Check your email</h1>
        <p className="text-sm text-text-secondary">
          We sent a verification link to <span className="text-text-primary">{email}</span>. Verify your account, then{" "}
          <Link href="/login" className="font-semibold text-primary hover:underline">
            log in
          </Link>
          .
        </p>
      </div>
    );
  }

  return (
    <div className="glass rounded-3xl border border-white/10 p-8 shadow-2xl">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight text-text-primary">Create your account</h1>

      <form onSubmit={handleSubmit}>
      <label className="mb-1 block text-xs font-semibold text-text-secondary">Full name</label>
      <input
        required
        value={fullName}
        onChange={(e) => setFullName(e.target.value)}
        className="mb-4 w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
      />

      <label className="mb-1 block text-xs font-semibold text-text-secondary">Username</label>
      <input
        required
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        className="mb-4 w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
      />

      <label className="mb-1 block text-xs font-semibold text-text-secondary">Email</label>
      <input
        type="email"
        required
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="mb-4 w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
      />

      <label className="mb-1 block text-xs font-semibold text-text-secondary">Password</label>
      <input
        type="password"
        required
        minLength={8}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        className="mb-4 w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
      />

      {error && <p className="mb-4 text-sm text-error">{error}</p>}

      <button
        type="submit"
        disabled={submitting}
        className="w-full rounded-full bg-gradient-to-r from-primary to-primary-deep py-2.5 text-sm font-bold text-background shadow-lg shadow-primary/25 transition-transform hover:scale-[1.02] disabled:opacity-50"
      >
        {submitting ? "Creating account…" : "Sign up"}
      </button>
      </form>

      <div className="my-5 flex items-center gap-3 text-xs text-text-tertiary">
        <span className="h-px flex-1 bg-white/10" />
        or
        <span className="h-px flex-1 bg-white/10" />
      </div>

      <GoogleSignInButton onCredential={handleGoogle} onError={setError} />

      <p className="mt-4 text-center text-sm text-text-secondary">
        Already have an account?{" "}
        <Link href="/login" className="font-semibold text-primary hover:underline">
          Log in
        </Link>
      </p>
    </div>
  );
}
