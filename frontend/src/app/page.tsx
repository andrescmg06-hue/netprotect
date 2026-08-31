"use client";

import { useCallback, useEffect, useState } from "react";

type ApiState = "checking" | "online" | "offline";

type ReadyPayload = {
  status: "ready";
  backend: "connected";
  database: "connected";
  redis: "connected";
};

export default function Home() {
  const [apiState, setApiState] = useState<ApiState>("checking");
  const [detail, setDetail] = useState("Comprobando Backend, PostgreSQL y Redis…");
  const [ready, setReady] = useState<ReadyPayload | null>(null);

  const checkInfrastructure = useCallback(async () => {
    setApiState("checking");
    setReady(null);
    setDetail("Comprobando Backend, PostgreSQL y Redis…");

    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";
    const controller = new AbortController();
    const timeout = window.setTimeout(() => controller.abort(), 5000);

    try {
      const response = await fetch(`${baseUrl}/api/v1/health/ready`, {
        signal: controller.signal,
        cache: "no-store",
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const payload = (await response.json()) as ReadyPayload;
      setReady(payload);
      setApiState("online");
      setDetail("Cadena Web → Backend → PostgreSQL validada. Redis también está disponible.");
    } catch {
      setApiState("offline");
      setDetail("No se pudo validar la infraestructura. Revisa backend, PostgreSQL, Redis y CORS.");
    } finally {
      window.clearTimeout(timeout);
    }
  }, []);

  useEffect(() => {
    void checkInfrastructure();
  }, [checkInfrastructure]);

  return (
    <main className="shell">
      <section className="hero">
        <p className="eyebrow">NETPROTECT · SPRINT 1</p>
        <h1>Arquitectura y entorno</h1>
        <p className="lead">
          Base ejecutable de NetProtect con una única API, PostgreSQL como fuente de verdad,
          Redis para datos temporales y clientes Web/Android sobre la misma arquitectura.
        </p>

        <div className="statusCard" aria-live="polite">
          <div>
            <span className={`dot ${apiState}`} aria-hidden="true" />
            <strong>Estado del incremento</strong>
          </div>
          <span className="statusText">{detail}</span>
          <button type="button" onClick={() => void checkInfrastructure()}>
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
