"use client";

import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";

import { useAuth } from "@/contexts/AuthContext";

type GoogleCredentialResponse = { credential: string };

type GoogleAccountsId = {
  initialize: (config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
  }) => void;
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
};

declare global {
  interface Window {
    google?: { accounts: { id: GoogleAccountsId } };
  }
}

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID ?? "";

export function GoogleSignInButton() {
  const { signInWithGoogleIdToken } = useAuth();
  const buttonRef = useRef<HTMLDivElement>(null);
  const [scriptReady, setScriptReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCredential = useCallback(
    (response: GoogleCredentialResponse) => {
      setError(null);
      signInWithGoogleIdToken(response.credential).catch(() => {
        setError("No se pudo completar el inicio de sesión. Intenta de nuevo.");
      });
    },
    [signInWithGoogleIdToken]
  );

  useEffect(() => {
    if (!scriptReady || !window.google || !buttonRef.current) {
      return;
    }

    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: handleCredential,
    });
    window.google.accounts.id.renderButton(buttonRef.current, {
      theme: "outline",
      size: "large",
      text: "signin_with",
      shape: "rectangular",
    });
  }, [scriptReady, handleCredential]);

  if (!GOOGLE_CLIENT_ID) {
    return (
      <p className="authError">
        Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID.
      </p>
    );
  }

  return (
    <div>
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onReady={() => setScriptReady(true)}
      />
      <div ref={buttonRef} />
      {error && <p className="authError">{error}</p>}
    </div>
  );
}
