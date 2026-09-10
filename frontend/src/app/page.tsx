"use client";

import { useCallback, useEffect, useState } from "react";

import { DashboardShell } from "@/components/DashboardShell";
import { GoogleSignInButton } from "@/components/GoogleSignInButton";
import { useAuth } from "@/contexts/AuthContext";

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
 */
export default function Home() {
  const { status: authStatus, user, accessToken, signOut } = useAuth();
  const [apiState, setApiState] = useState<ApiState>("checking");
  const [detail, setDetail] = useState("Comprobando Backend, PostgreSQL y Redis…");
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
    setDetail("Comprobando Backend, PostgreSQL y Redis…");
    setAttempt((current) => current + 1);
  }, []);

  if (authStatus === "authenticated" && user && accessToken) {
    return <DashboardShell accessToken={accessToken} user={user} onSignOut={() => void signOut()} />;
  }

  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">NETPROTECT · PANEL DEL TUTOR</p>
        <h1>Panel del tutor</h1>
        <p className="lead">
          Inicia sesión con Google para vincular dispositivos y gestionar reglas, ubicación,
          alertas y todo lo demás desde un panel único.
        </p>

        <div className="authCard" aria-live="polite">
          {authStatus === "loading" && <span className="statusText">Comprobando sesión…</span>}
          {authStatus === "unauthenticated" && (
            <>
              <span className="statusText">Inicia sesión con tu cuenta de Google para continuar.</span>
              <GoogleSignInButton />
            </>
          )}
        </div>

        <div className="statusCard" aria-live="polite">
          <div>
            <span className={`dot ${apiState}`} aria-hidden="true" />
            <strong>Estado del incremento</strong>
          </div>
          <span className="statusText">{detail}</span>
          <button type="button" onClick={checkInfrastructure}>
            Volver a comprobar
          </button>
        </div>

        <div className="grid">
          <article>
            <span>01</span>
            <h2>Web</h2>
            <p>Next.js + TypeScript. Consume la API central y valida la infraestructura.</p>
          </article>
          <article>
            <span>02</span>
            <h2>Backend</h2>
            <p>FastAPI como frontera única para autenticación, reglas y datos.</p>
          </article>
          <article>
            <span>03</span>
            <h2>PostgreSQL</h2>
            <p>{ready ? `Estado: ${ready.database}` : "Fuente de verdad relacional."}</p>
          </article>
          <article>
            <span>04</span>
            <h2>Redis</h2>
            <p>{ready ? `Estado: ${ready.redis}` : "Cache y datos temporales; no es fuente de verdad."}</p>
          </article>
          <article>
            <span>05</span>
            <h2>Android</h2>
            <p>Una sola app Kotlin/Compose para Tutor y Supervisado. También valida la API.</p>
          </article>
        </div>
      </section>
    </main>
  );
}
