"use client";

import { useCallback, useEffect, useState } from "react";

import { DashboardShell } from "@/components/DashboardShell";
import { LoginHero } from "@/components/LoginHero";
import { LoginPanel } from "@/components/LoginPanel";
import { useAuth } from "@/contexts/AuthContext";

import styles from "./page.module.css";

type ApiState = "checking" | "online" | "offline";

type ReadyPayload = {
  status: "ready";
  backend: "connected";
  database: "connected";
  redis: "connected";
};

function fetchReadiness(baseUrl: string, signal: AbortSignal): Promise<ReadyPayload> {
  return fetch(`${baseUrl}/api/v1/health/ready`, { signal, cache: "no-store" }).then(
    (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.json() as Promise<ReadyPayload>;
    },
  );
}

/** Sprint 24 — Panel web completo. Before this sprint, everything (health check, login, device
 * list and every per-device panel) lived stacked in this one file. Now an authenticated tutor is
 * handed straight to DashboardShell, which owns the real navigation across the 16 sections (see
 * lib/dashboardSections.ts); this file goes back to just being the auth gate, plus the same
 * infrastructure check from Sprint 1 kept on the pre-login landing, where it's still useful
 * evidence that Web → Backend → PostgreSQL/Redis works before anyone signs in.
 *
 * Sprint 33: the pre-login view is now LoginHero (the infrastructure check) + LoginPanel (the
 * Google button) side by side — same fetch/retry logic as before, new layout.
 */
export default function Home() {
  const { status: authStatus, user, accessToken, signOut } = useAuth();
  const [apiState, setApiState] = useState<ApiState>("checking");
  const [detail, setDetail] = useState("Verificando que todos los servicios estén disponibles…");
  const [ready, setReady] = useState<ReadyPayload | null>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (authStatus === "authenticated") {
      return;
    }
    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 5000);

    fetchReadiness(baseUrl, controller.signal)
      .then((payload) => {
        setReady(payload);
        setApiState("online");
        setDetail("Cadena Web → Backend → PostgreSQL validada. Redis también está disponible.");
      })
      .catch(() => {
        setApiState("offline");
        setDetail("No se pudo validar la infraestructura. Revisa backend, PostgreSQL, Redis y CORS.");
      })
      .finally(() => {
        window.clearTimeout(timeout);
      });

    return () => {
      controller.abort();
      window.clearTimeout(timeout);
    };
  }, [attempt, authStatus]);

  const checkInfrastructure = useCallback(() => {
    setApiState("checking");
    setReady(null);
    setDetail("Verificando que todos los servicios estén disponibles…");
    setAttempt((current) => current + 1);
  }, []);

  if (authStatus === "authenticated" && user && accessToken) {
    return <DashboardShell accessToken={accessToken} user={user} onSignOut={() => void signOut()} />;
  }

  return (
    <main className={styles.page}>
      <div className={styles.heroColumn}>
        <LoginHero apiState={apiState} detail={detail} ready={ready} onRetry={checkInfrastructure} />
      </div>
      <LoginPanel authStatus={authStatus === "loading" ? "loading" : "unauthenticated"} />
    </main>
  );
}
