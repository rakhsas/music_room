"use client";

import Script from "next/script";
import { useRef, useState } from "react";

// Minimal typing for the slice of Google Identity Services we use - the full
// library ships no first-party types for this global.
interface GoogleCredentialResponse {
  credential: string;
}
interface GoogleIdApi {
  initialize: (config: { client_id: string; callback: (res: GoogleCredentialResponse) => void }) => void;
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
}
declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdApi } };
  }
}

const CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

export function GoogleSignInButton({ onCredential, onError }: { onCredential: (idToken: string) => void; onError: (message: string) => void }) {
  const buttonRef = useRef<HTMLDivElement>(null);
  const [loaded, setLoaded] = useState(false);

  function initialize() {
    if (!window.google || !buttonRef.current) return;
    if (!CLIENT_ID) {
      onError("Google Sign-In isn't configured yet (missing NEXT_PUBLIC_GOOGLE_CLIENT_ID).");
      return;
    }
    window.google.accounts.id.initialize({
      client_id: CLIENT_ID,
      callback: (res) => onCredential(res.credential),
    });
    window.google.accounts.id.renderButton(buttonRef.current, {
      theme: "filled_black",
      size: "large",
      shape: "pill",
      width: 320,
    });
    setLoaded(true);
  }

  return (
    <div>
      <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" onLoad={initialize} />
      <div ref={buttonRef} className="flex justify-center" />
      {!loaded && !CLIENT_ID && (
        <p className="text-center text-xs text-text-tertiary">
          Google Sign-In needs a Web OAuth client ID (set NEXT_PUBLIC_GOOGLE_CLIENT_ID).
        </p>
      )}
    </div>
  );
}
