"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";

export default function LoginPage() {
  const { login, loginWithGoogleIdToken } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(email, password);
      router.push("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't log in");
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
      setError(err instanceof ApiError ? err.message : "Couldn't log in with Google");
    }
  }

  return (
    <div className="glass rounded-3xl border border-white/10 p-8 shadow-2xl">
      <h1 className="mb-6 text-2xl font-extrabold tracking-tight text-text-primary">Log in</h1>

      <form onSubmit={handleSubmit}>
        <label className="mb-1 block text-xs font-semibold text-text-secondary">Email</label>
        <input
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="mb-4 w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20"
        />

        <div className="mb-1 flex items-center justify-between">
          <label className="block text-xs font-semibold text-text-secondary">Password</label>
          <Link href="/forgot-password" className="text-xs font-semibold text-primary hover:underline">
            Forgot password?
          </Link>
        </div>
        <input
          type="password"
          required
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
          {submitting ? "Logging in…" : "Log in"}
        </button>
      </form>

      <div className="my-5 flex items-center gap-3 text-xs text-text-tertiary">
        <span className="h-px flex-1 bg-white/10" />
        or
        <span className="h-px flex-1 bg-white/10" />
      </div>

      <GoogleSignInButton onCredential={handleGoogle} onError={setError} />

      <p className="mt-4 text-center text-sm text-text-secondary">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="font-semibold text-primary hover:underline">
          Sign up
        </Link>
      </p>
    </div>
  );
}
