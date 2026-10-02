"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";

type Step = "email" | "otp" | "password";

export default function ForgotPasswordPage() {
  const router = useRouter();
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleRequestOtp(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await api.requestPasswordReset(email);
      setStep("otp");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't send the reset code");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleVerifyOtp(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await api.verifyPasswordResetOtp(email, otp);
      setStep("password");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Invalid or expired code");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleConfirm(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await api.confirmPasswordReset(email, otp, password);
      router.push("/login");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't reset your password");
    } finally {
      setSubmitting(false);
    }
  }

  const inputClass =
    "mb-4 w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20";
  const buttonClass =
    "w-full rounded-full bg-gradient-to-r from-primary to-primary-deep py-2.5 text-sm font-bold text-background shadow-lg shadow-primary/25 transition-transform hover:scale-[1.02] disabled:opacity-50";

  return (
    <div className="glass rounded-3xl border border-white/10 p-8 shadow-2xl">
      <h1 className="mb-1 text-2xl font-extrabold tracking-tight text-text-primary">Reset your password</h1>
      <p className="mb-6 text-sm text-text-secondary">
        {step === "email" && "We'll email you a code to reset your password."}
        {step === "otp" && `Enter the code we sent to ${email}.`}
        {step === "password" && "Choose a new password."}
      </p>

      {step === "email" && (
        <form onSubmit={handleRequestOtp}>
          <label className="mb-1 block text-xs font-semibold text-text-secondary">Email</label>
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} />
          {error && <p className="mb-4 text-sm text-error">{error}</p>}
          <button type="submit" disabled={submitting} className={buttonClass}>
            {submitting ? "Sending…" : "Send code"}
          </button>
        </form>
      )}

      {step === "otp" && (
        <form onSubmit={handleVerifyOtp}>
          <label className="mb-1 block text-xs font-semibold text-text-secondary">Code</label>
          <input required value={otp} onChange={(e) => setOtp(e.target.value)} className={inputClass} />
          {error && <p className="mb-4 text-sm text-error">{error}</p>}
          <button type="submit" disabled={submitting} className={buttonClass}>
            {submitting ? "Verifying…" : "Verify code"}
          </button>
        </form>
      )}

      {step === "password" && (
        <form onSubmit={handleConfirm}>
          <label className="mb-1 block text-xs font-semibold text-text-secondary">New password</label>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClass}
          />
          {error && <p className="mb-4 text-sm text-error">{error}</p>}
          <button type="submit" disabled={submitting} className={buttonClass}>
            {submitting ? "Saving…" : "Reset password"}
          </button>
        </form>
      )}

      <p className="mt-4 text-center text-sm text-text-secondary">
        <Link href="/login" className="font-semibold text-primary hover:underline">
          Back to log in
        </Link>
      </p>
    </div>
  );
}
