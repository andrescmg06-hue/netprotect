"use client";

import Script from "next/script";
import { useCallback, useRef, useState } from "react";
import { FcGoogle } from "react-icons/fc";

import { useAuth } from "@/contexts/AuthContext";

type CredentialResponse = { credential: string };

/** Minimal slice of the real Google Identity Services API this button needs — kept local
 * (not merged into the global `Window.google` augmentation in GoogleSignInButton.tsx) so the
 * two files' type declarations for the same global can't conflict. */
type GoogleId = {
  initialize: (config: { client_id: string; callback: (response: CredentialResponse) => void }) => void;
  prompt: (callback?: (notification: { isNotDisplayed(): boolean; isSkippedMoment(): boolean }) => void) => void;
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
};

const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID ?? "";

/** A custom-styled "Acceder con Google" button, not a reskin of Google's own <iframe> button
 * (that one keeps its official look elsewhere in the app — see GoogleSignInButton.tsx). This
 * calls the real, documented alternative: `google.accounts.id.prompt()` opens Google's own
 * sign-in surface (One Tap / the account chooser), and the callback receives the same real ID
 * token GoogleSignInButton hands to AuthContext. If Google declines to show that prompt (e.g.
 * third-party cookies blocked, or the user dismissed it recently), this falls back to rendering
 * Google's real button in place of the custom one — degraded look, but sign-in still works,
 * rather than a custom button that sometimes silently does nothing.
 */
export function GoogleButton() {
  const { signInWithGoogleIdToken } = useAuth();
  const [scriptReady, setScriptReady] = useState(false);
  const [status, setStatus] = useState<"idle" | "connecting" | "error">("idle");
  const [fallback, setFallback] = useState(false);
  const fallbackRef = useRef<HTMLDivElement>(null);
  const initialized = useRef(false);

  const handleCredential = useCallback(
    (response: CredentialResponse) => {
      setStatus("connecting");
      signInWithGoogleIdToken(response.credential).catch(() => {
        setStatus("error");
      });
    },
    [signInWithGoogleIdToken]
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
    id.prompt((notification) => {
      if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
        setStatus("idle");
        setFallback(true);
        if (fallbackRef.current) {
          id.renderButton(fallbackRef.current, { theme: "filled_blue", size: "large", text: "signin_with", shape: "pill" });
        }
      }
    });
  }, [ensureInitialized]);

  if (!GOOGLE_CLIENT_ID) {
    return (
      <p className="rounded-xl border border-brand-border bg-brand-panel px-4 py-3 text-sm text-brand-muted" role="alert">
        Falta configurar NEXT_PUBLIC_GOOGLE_WEB_CLIENT_ID.
      </p>
    );
  }

  return (
    <div className="flex w-full flex-col items-stretch gap-3">
      <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" onReady={() => setScriptReady(true)} />

      {!fallback && (
        <button
          type="button"
          onClick={handleClick}
          disabled={!scriptReady || status === "connecting"}
          className="flex h-[clamp(48px,4.1vw,58px)] w-full items-center justify-center gap-[12px] rounded-[10px] border border-[#D8DEE9] bg-white text-[clamp(16px,1.2vw,20px)] font-semibold text-[#1F2937] shadow-[0_2px_6px_rgba(20,40,90,0.06)] transition-colors hover:bg-[#F8F9FA] hover:shadow-[0_3px_8px_rgba(20,40,90,0.09)] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-primary disabled:cursor-not-allowed disabled:opacity-60"
        >
          <FcGoogle size={30} aria-hidden="true" />
          {status === "connecting" ? "Conectando…" : "Continuar con Google"}
        </button>
      )}

      {/* Google's own real button, shown only if the custom trigger above couldn't display its
          prompt — real sign-in, Google's default look, not this screen's custom styling. */}
      <div ref={fallbackRef} className={fallback ? "flex justify-center" : "hidden"} />

      {status === "error" && (
        <p className="text-sm text-red-600" role="alert">
          No se pudo completar el inicio de sesión. Intenta de nuevo.
        </p>
      )}
    </div>
  );
}
