"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { MovingBorderButton } from "./ui/moving-border";

const PERKS: [string, string, string][] = [
  ["Playlists you own", "3", "Unlimited"],
  ["Events you organize", "3", "Unlimited"],
  ["Private playlists & events", "—", "✓"],
];

const inputClass =
  "w-full rounded-xl border border-white/5 bg-white/[0.03] px-3.5 py-2.5 text-sm text-text-primary transition-colors focus:border-primary/40 focus:outline-none focus:ring-2 focus:ring-primary/20";
const labelClass = "mb-1 block text-xs font-semibold text-text-secondary";

// "4242424242424242" -> "4242 4242 4242 4242"
const groupDigits = (v: string) => v.replace(/\D/g, "").slice(0, 19).replace(/(\d{4})(?=\d)/g, "$1 ");

// "1230" -> "12/30"
function formatExpiry(v: string) {
  const d = v.replace(/\D/g, "").slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
}

export function PlanCard() {
  const { user, refreshProfile } = useAuth();
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [card, setCard] = useState({ holderName: "", number: "", expiry: "", cvc: "" });

  if (!user) return null;
  const price = new Intl.NumberFormat(undefined, { style: "currency", currency: user.plan.currency }).format(user.plan.amount);

  async function pay(e: React.FormEvent) {
    e.preventDefault();
    const [mm, yy] = card.expiry.split("/");
    setBusy(true);
    setError(null);
    try {
      await api.subscribe("premium", {
        holderName: card.holderName.trim(),
        number: card.number,
        expMonth: Number(mm),
        expYear: 2000 + Number(yy),
        cvc: card.cvc,
      });
      await refreshProfile();
      setCheckoutOpen(false);
      setCard({ holderName: "", number: "", expiry: "", cvc: "" });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Payment failed");
    } finally {
      setBusy(false);
    }
  }

  async function cancel() {
    if (!window.confirm("Cancel Premium? Your existing playlists and events are kept, but you won't be able to create more beyond the free limits.")) return;
    setBusy(true);
    setError(null);
    try {
      await api.subscribe("free");
      await refreshProfile();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't cancel your subscription");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="glass rounded-2xl border border-white/10 p-6 shadow-xl">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold text-text-primary">
            Current plan:{" "}
            <span className={user.isPremium ? "text-primary" : "text-text-secondary"}>{user.isPremium ? "Premium" : "Free"}</span>
          </p>
          {user.isPremium ? (
            <p className="mt-1 text-xs text-text-secondary">
              {price}/{user.plan.interval}
              {user.card && ` · ${user.card.brand} •••• ${user.card.last4}`}
              {user.premiumSince && ` · since ${new Date(user.premiumSince).toLocaleDateString()}`}
            </p>
          ) : (
            <p className="mt-1 text-xs text-text-secondary">Premium is {price}/{user.plan.interval}. Cancel anytime.</p>
          )}
        </div>
        {user.isPremium ? (
          <button
            onClick={cancel}
            disabled={busy}
            className="shrink-0 rounded-full border border-text-tertiary px-4 py-2 text-sm font-semibold text-text-primary hover:border-text-secondary disabled:opacity-50"
          >
            Cancel Premium
          </button>
        ) : (
          !checkoutOpen && (
            <MovingBorderButton onClick={() => setCheckoutOpen(true)} className="px-4 py-2 font-semibold">
              Upgrade to Premium
            </MovingBorderButton>
          )
        )}
      </div>

      <table className="mb-2 w-full text-sm">
        <thead>
          <tr className="text-left text-xs text-text-tertiary">
            <th className="py-1 font-semibold" />
            <th className="py-1 font-semibold">Free</th>
            <th className="py-1 font-semibold text-primary">Premium</th>
          </tr>
        </thead>
        <tbody>
          {PERKS.map(([perk, free, premium]) => (
            <tr key={perk} className="border-t border-white/5">
              <td className="py-2 text-text-secondary">{perk}</td>
              <td className="py-2 text-text-primary">{free}</td>
              <td className="py-2 text-text-primary">{premium}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {checkoutOpen && !user.isPremium && (
        <form onSubmit={pay} className="mt-4 border-t border-white/10 pt-4">
          <h3 className="mb-1 font-bold text-text-primary">Checkout — {price}/{user.plan.interval}</h3>
          <p className="mb-4 text-xs text-text-tertiary">
            Demo payment, no money is charged. Use 4242 4242 4242 4242 with any future date and any CVC.
            4000 0000 0000 0002 simulates a declined card.
          </p>
          <div className="mb-3">
            <label htmlFor="cc-name" className={labelClass}>Name on card</label>
            <input
              id="cc-name"
              autoComplete="cc-name"
              required
              value={card.holderName}
              onChange={(e) => setCard({ ...card, holderName: e.target.value })}
              className={inputClass}
            />
          </div>
          <div className="mb-3">
            <label htmlFor="cc-number" className={labelClass}>Card number</label>
            <input
              id="cc-number"
              autoComplete="cc-number"
              inputMode="numeric"
              required
              placeholder="1234 1234 1234 1234"
              value={card.number}
              onChange={(e) => setCard({ ...card, number: groupDigits(e.target.value) })}
              className={inputClass}
            />
          </div>
          <div className="mb-4 grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="cc-exp" className={labelClass}>Expiry (MM/YY)</label>
              <input
                id="cc-exp"
                autoComplete="cc-exp"
                inputMode="numeric"
                required
                pattern="(0[1-9]|1[0-2])/\d{2}"
                placeholder="MM/YY"
                value={card.expiry}
                onChange={(e) => setCard({ ...card, expiry: formatExpiry(e.target.value) })}
                className={inputClass}
              />
            </div>
            <div>
              <label htmlFor="cc-csc" className={labelClass}>CVC</label>
              <input
                id="cc-csc"
                autoComplete="cc-csc"
                inputMode="numeric"
                required
                pattern="\d{3,4}"
                value={card.cvc}
                onChange={(e) => setCard({ ...card, cvc: e.target.value.replace(/\D/g, "").slice(0, 4) })}
                className={inputClass}
              />
            </div>
          </div>
          {error && <p className="mb-3 text-sm text-error">{error}</p>}
          <div className="flex gap-3">
            <button
              type="submit"
              disabled={busy}
              className="rounded-full bg-gradient-to-r from-primary to-primary-deep px-5 py-2 text-sm font-bold text-background shadow-lg shadow-primary/25 disabled:opacity-50"
            >
              {busy ? "Processing…" : `Pay ${price}`}
            </button>
            <button
              type="button"
              onClick={() => setCheckoutOpen(false)}
              className="rounded-full border border-white/10 px-5 py-2 text-sm font-semibold text-text-secondary hover:text-text-primary"
            >
              Cancel
            </button>
          </div>
        </form>
      )}
      {error && !checkoutOpen && <p className="text-sm text-error">{error}</p>}
    </div>
  );
}
