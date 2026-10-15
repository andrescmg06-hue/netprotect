"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";

import {
  ApiError,
  type CurrentUser,
  fetchCurrentUser,
  loginWithGoogle,
  logout as logoutRequest,
  refreshTokens,
  type TokenPair,
} from "@/lib/apiClient";

const REFRESH_TOKEN_STORAGE_KEY = "netprotect.refresh_token";

const RENEW_BEFORE_EXPIRY_MS = 60_000;
const MIN_RENEW_DELAY_MS = 10_000;
const RETRY_AFTER_FAILURE_MS = 30_000;

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

/** When the current access token expires and when the next renewal attempt is due (epoch ms). */
type RenewalSchedule = { expiresAt: number; renewAt: number };

type AuthContextValue = {
  status: AuthStatus;
  user: CurrentUser | null;
  accessToken: string | null;
  signInWithGoogleIdToken: (idToken: string) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

function storeRefreshToken(token: string | null) {
  try {
    if (token) {
      sessionStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, token);
    } else {
      sessionStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
    }
  } catch {
    // sessionStorage unavailable (e.g. private browsing) — session just won't persist
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  // Sprint 24 finding: the initial state used to branch on `typeof window` / sessionStorage
  // right inside useState's initializer, so a returning tutor (sessionStorage already holding a
  // refresh token from a previous visit) hydrated with "loading" on the client against a
  // server-rendered "unauthenticated" tree — a real hydration mismatch, not just an artefact of
  // testing with a pre-seeded token. The initial state is now the same constant on server and
  // client; the effect below owns the entire transition, including "no stored token" ->
  // "unauthenticated" (it used to leave that case for the initializer to have already handled).
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [renewal, setRenewal] = useState<RenewalSchedule | null>(null);

  // Sprint 70 (B-05): the access token lives 15 minutes and used to be obtained once, on mount.
  // The refresh token rotates and is single-use, and /auth/refresh does not lock the session row:
  // redeeming the same one twice either answers 401 (ending the local session) or leaves two
  // valid sessions born from one token. So every renewal goes through `renew()`, which never
  // runs two at once, and the mount redemption below runs only once even under StrictMode.
  // The refs hold the latest refresh token and the in-flight renewal; `generation` is bumped each
  // time the local session ends, so a renewal that resolves afterwards cannot reopen it.
  const refreshTokenRef = useRef<string | null>(null);
  const inFlightRef = useRef<Promise<void> | null>(null);
  const generationRef = useRef(0);
  const initialLoadStartedRef = useRef(false);

  const adoptTokens = useCallback((tokens: TokenPair) => {
    refreshTokenRef.current = tokens.refresh_token;
    storeRefreshToken(tokens.refresh_token);
    setAccessToken(tokens.access_token);
    const now = Date.now();
    const lifetime = tokens.expires_in * 1000;
    setRenewal({
      expiresAt: now + lifetime,
      renewAt: now + Math.max(lifetime - RENEW_BEFORE_EXPIRY_MS, MIN_RENEW_DELAY_MS),
    });
  }, []);

  const endLocalSession = useCallback(() => {
    generationRef.current += 1;
    inFlightRef.current = null;
    refreshTokenRef.current = null;
    storeRefreshToken(null);
    setRenewal(null);
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
  }, []);

  const renew = useCallback(() => {
    const refreshToken = refreshTokenRef.current;
    if (inFlightRef.current || !refreshToken) return;
    const generation = generationRef.current;
    const attempt: Promise<void> = refreshTokens(refreshToken)
      .then(
        (tokens) => {
          if (generation !== generationRef.current) {
            // Signed out meanwhile: the server already rotated the session, so revoke the new
            // refresh token instead of leaving it valid and unheld.
            void logoutRequest(tokens.refresh_token).catch(() => {});
            return;
          }
          adoptTokens(tokens);
        },
        (error: unknown) => {
          if (generation !== generationRef.current) return;
          if (error instanceof ApiError && error.status === 401) {
            endLocalSession();
            return;
          }
          // Network error, 5xx, rate limit: keep the session and retry with the same token. If the
          // server did spend it before the failure, the retry gets a 401 and ends the session.
          const retryAt = Date.now() + RETRY_AFTER_FAILURE_MS;
          setRenewal((current) => current && { ...current, renewAt: retryAt });
        }
      )
      .finally(() => {
        if (inFlightRef.current === attempt) inFlightRef.current = null;
      });
    inFlightRef.current = attempt;
  }, [adoptTokens, endLocalSession]);

  useEffect(() => {
    if (status !== "authenticated" || !renewal) return;
    const timer = window.setTimeout(renew, Math.max(renewal.renewAt - Date.now(), 0));
    // Background tabs throttle timers, so the one above can fire long after the token expired.
    function onVisibilityChange() {
      // renewAt too: after a failed attempt the retry waits its 30 s even if the tab is
      // shown again and again during an outage.
      const now = Date.now();
      if (
        document.visibilityState === "visible" &&
        renewal &&
        now >= renewal.expiresAt &&
        now >= renewal.renewAt
      ) {
        renew();
      }
    }
    document.addEventListener("visibilitychange", onVisibilityChange);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, [status, renewal, renew]);

  useEffect(() => {
    // StrictMode runs mount effects twice in development; the stored token must be redeemed once.
    if (initialLoadStartedRef.current) return;
    initialLoadStartedRef.current = true;
    const storedRefreshToken = readStoredRefreshToken();

    // "No stored token" is folded into the same .then()/.catch() chain (via a rejected promise)
    // as an actual refresh failure, rather than a synchronous setState before the chain starts —
    // both end up at the same "unauthenticated" outcome, and every setState below still runs
    // inside a promise callback, never directly in the effect body.
    (storedRefreshToken
      ? refreshTokens(storedRefreshToken)
      : Promise.reject(new Error("no_stored_refresh_token"))
    )
      .then((tokens) => {
        adoptTokens(tokens);
        return fetchCurrentUser(tokens.access_token);
      })
      .then((currentUser) => {
        setUser(currentUser);
        setStatus("authenticated");
      })
      .catch(() => {
        // If the refresh succeeded but /auth/me failed, the server already rotated the session:
        // revoke the new refresh token rather than leave it valid and unheld.
        const adopted = refreshTokenRef.current;
        endLocalSession();
        if (adopted) void logoutRequest(adopted).catch(() => {});
      });
  }, [adoptTokens, endLocalSession]);

  const signInWithGoogleIdToken = useCallback(
    async (idToken: string) => {
      const tokens = await loginWithGoogle(idToken);
      adoptTokens(tokens);
      const currentUser = await fetchCurrentUser(tokens.access_token);
      setUser(currentUser);
      setStatus("authenticated");
    },
    [adoptTokens]
  );

  const signOut = useCallback(async () => {
    const storedRefreshToken = refreshTokenRef.current ?? readStoredRefreshToken();
    endLocalSession();
    if (storedRefreshToken) {
      await logoutRequest(storedRefreshToken).catch(() => {
        // best-effort: the local session is already cleared either way
      });
    }
  }, [endLocalSession]);

  const value = useMemo(
    () => ({ status, user, accessToken, signInWithGoogleIdToken, signOut }),
    [status, user, accessToken, signInWithGoogleIdToken, signOut]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
