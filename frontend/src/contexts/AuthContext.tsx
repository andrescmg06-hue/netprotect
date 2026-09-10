"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

import {
  type CurrentUser,
  fetchCurrentUser,
  loginWithGoogle,
  logout as logoutRequest,
  refreshTokens,
} from "@/lib/apiClient";

const REFRESH_TOKEN_STORAGE_KEY = "netprotect.refresh_token";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

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

  useEffect(() => {
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
        storeRefreshToken(tokens.refresh_token);
        setAccessToken(tokens.access_token);
        return fetchCurrentUser(tokens.access_token);
      })
      .then((currentUser) => {
        setUser(currentUser);
        setStatus("authenticated");
      })
      .catch(() => {
        storeRefreshToken(null);
        setAccessToken(null);
        setUser(null);
        setStatus("unauthenticated");
      });
  }, []);

  const signInWithGoogleIdToken = useCallback(async (idToken: string) => {
    const tokens = await loginWithGoogle(idToken);
    storeRefreshToken(tokens.refresh_token);
    setAccessToken(tokens.access_token);
    const currentUser = await fetchCurrentUser(tokens.access_token);
    setUser(currentUser);
    setStatus("authenticated");
  }, []);

  const signOut = useCallback(async () => {
    const storedRefreshToken = readStoredRefreshToken();
    storeRefreshToken(null);
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
    if (storedRefreshToken) {
      await logoutRequest(storedRefreshToken).catch(() => {
        // best-effort: the local session is already cleared either way
      });
    }
  }, []);

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
