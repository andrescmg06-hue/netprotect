"use client";

import Script from "next/script";
import { useCallback, useRef, useState } from "react";

import { useAuth } from "@/contexts/AuthContext";

import styles from "./GoogleButton.module.css";

type CredentialResponse = { credential: string };

/** Minimal slice of the real Google Identity Services API this button needs — kept local (not
 * merged into a global `Window.google` augmentation) so this is the only file that declares/uses
 * `window.google` (D5). */
type GoogleId = {
  initialize: (config: { client_id: string; callback: (response: CredentialResponse) => void }) => void;
  prompt: (
    callback?: (notification: {
      isNotDisplayed(): boolean;
      isSkippedMoment(): boolean;
      isDismissedMoment(): boolean;
    }) => void,
  ) => void;
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
};

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID ?? "";

/** If Google's prompt never reports back (FedCM and blocked third-party cookies can skip every
 * notification callback), the button must not stay disabled forever on "Conectando…". */
const PROMPT_TIMEOUT_MS = 10_000;

/** The official 4-colour Google "G", inlined (no icon library needed). */
const GOOGLE_G = [
  {
    fill: "#FFC107",
    d: "M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z",
  },
  {
    fill: "#FF3D00",
    d: "M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z",
  },
  {
    fill: "#4CAF50",
    d: "M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z",
  },
  {
    fill: "#1976D2",
    d: "M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z",
  },
];

/** A custom-styled "Continuar con Google" button — not a reskin of Google's own <iframe> button.
 * It calls the documented `google.accounts.id.prompt()` alternative: Google's own sign-in surface
 * (One Tap / account chooser) receives the same real ID token that AuthContext consumes. If Google
 * declines to show that prompt (third-party cookies blocked, or dismissed recently), it falls back
 * to rendering Google's real button — degraded look, sign-in still works. */
export function GoogleButton() {
  const { signInWithGoogleIdToken } = useAuth();
  const [scriptReady, setScriptReady] = useState(false);
  const [status, setStatus] = useState<"idle" | "connecting" | "error">("idle");
  const [fallback, setFallback] = useState(false);
  const fallbackRef = useRef<HTMLDivElement>(null);
  const initialized = useRef(false);
  const promptTimer = useRef<number | null>(null);

  const clearPromptTimer = useCallback(() => {
    if (promptTimer.current !== null) {
      window.clearTimeout(promptTimer.current);
      promptTimer.current = null;
    }
  }, []);

  const handleCredential = useCallback(
    (response: CredentialResponse) => {
      clearPromptTimer();
      setStatus("connecting");
      signInWithGoogleIdToken(response.credential).catch(() => {
        setStatus("error");
      });
    },
    [signInWithGoogleIdToken, clearPromptTimer]
  );

  const ensureInitialized = useCallback(() => {
    const google = (window as unknown as { google?: { accounts: { id: GoogleId } } }).google;
    if (!google) return null;
    if (!initialized.current) {
      google.accounts.id.initialize({ client_id: GOOGLE_CLIENT_ID, callback: handleCredential });
      initialized.current = true;
    }
    return google.accounts.id;
  }, [handleCredential]);

  const handleClick = useCallback(() => {
    const id = ensureInitialized();
    if (!id) return;
    setStatus("connecting");
    clearPromptTimer();
    promptTimer.current = window.setTimeout(() => {
      promptTimer.current = null;
      setStatus("idle");
    }, PROMPT_TIMEOUT_MS);
    id.prompt((notification) => {
      if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
        clearPromptTimer();
        setStatus("idle");
        setFallback(true);
        if (fallbackRef.current) {
          id.renderButton(fallbackRef.current, {
            theme: "filled_blue",
            size: "large",
            text: "signin_with",
            shape: "pill",
          });
        }
      } else if (notification.isDismissedMoment()) {
        // The person closed the prompt without choosing an account: let them try again.
        clearPromptTimer();
        setStatus("idle");
      }
    });
  }, [ensureInitialized, clearPromptTimer]);

  if (!GOOGLE_CLIENT_ID) {
    return (
      <p className={styles.missing} role="alert">
        Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID.
      </p>
    );
  }

  return (
    <div className={styles.wrap}>
      <Script
        src="https://accounts.google.com/gsi/client"
        strategy="afterInteractive"
        onReady={() => setScriptReady(true)}
      />

      {!fallback && (
        <button
          type="button"
          onClick={handleClick}
          disabled={!scriptReady || status === "connecting"}
          className={styles.button}
        >
          <svg width="30" height="30" viewBox="0 0 48 48" aria-hidden="true" xmlns="http://www.w3.org/2000/svg">
            {GOOGLE_G.map((path) => (
              <path key={path.fill} fill={path.fill} d={path.d} />
            ))}
          </svg>
          {status === "connecting" ? "Conectando…" : "Continuar con Google"}
        </button>
      )}

      {/* Google's own real button, shown only if the custom trigger above couldn't display its
          prompt — real sign-in, Google's default look, not this screen's custom styling. */}
      <div ref={fallbackRef} className={fallback ? styles.fallback : styles.fallbackHidden} />

      {status === "error" && (
        <p className={styles.error} role="alert">
          No se pudo completar el inicio de sesión. Intenta de nuevo.
        </p>
      )}
    </div>
  );
}
