import { Database, Globe, Layers, RefreshCw, Server, Smartphone } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { Button, Logo, StatusBadge } from "@/components/ui";

import styles from "./LoginHero.module.css";

type ApiState = "checking" | "online" | "offline";

type ReadyPayload = {
  status: "ready";
  backend: "connected";
  database: "connected";
  redis: "connected";
};

const SERVICES: { key: keyof ReadyPayload | "web" | "android"; label: string; icon: LucideIcon; blurb: string }[] = [
  { key: "web", label: "Web", icon: Globe, blurb: "Next.js. Consume la API central." },
  { key: "backend", label: "Backend", icon: Server, blurb: "FastAPI, frontera única de la API." },
  { key: "database", label: "PostgreSQL", icon: Database, blurb: "Fuente de verdad relacional." },
  { key: "redis", label: "Redis", icon: Layers, blurb: "Cache y tiempo real; no es fuente de verdad." },
  { key: "android", label: "Android", icon: Smartphone, blurb: "Una app Kotlin/Compose, Tutor y Supervisado." },
];

/** Pre-login left column: what this evaluates before anyone signs in (Sprint 1's infrastructure
 * check), now the visual lead of the login screen instead of a page of its own. Logic (apiState,
 * ready, retry) stays in app/page.tsx — this only renders it.
 */
export function LoginHero({
  apiState,
  detail,
  ready,
  onRetry,
}: {
  apiState: ApiState;
  detail: string;
  ready: ReadyPayload | null;
  onRetry: () => void;
}) {
  const serviceState = (key: (typeof SERVICES)[number]["key"]): ApiState => {
    if (key === "web") return "online"; // this code is running, so the web tier is up by definition
    if (key === "android") return apiState; // no live signal; mirrors the same infra check
    return ready ? "online" : apiState === "checking" ? "checking" : "offline";
  };

  return (
    <div className={styles.hero}>
      <Logo height={40} />
      <h1 className={styles.title}>Protege lo que más importa</h1>
      <p className={styles.lead}>
        NetProtect te permite supervisar y gestionar el uso de dispositivos, aplicaciones y
        contenido de tus hijos. Mantén el control, fomenta hábitos digitales saludables y brinda
        un entorno más seguro.
      </p>

      <div className={styles.statusCard} aria-live="polite">
        <span className={apiState === "offline" ? `${styles.statusDot} ${styles.dotOffline}` : styles.statusDot} />
        <div className={styles.statusText}>
          <strong>
            {apiState === "checking" && "Comprobando Backend, PostgreSQL y Redis…"}
            {apiState === "online" && "Todo en funcionamiento"}
            {apiState === "offline" && "No se pudo validar la infraestructura"}
          </strong>
          <span>{detail}</span>
        </div>
        <Button size="sm" icon={RefreshCw} onClick={onRetry}>
          Volver a comprobar
        </Button>
      </div>

      <span className={styles.sectionLabel}>Estado de la infraestructura</span>
      <div className={styles.grid}>
        {SERVICES.map((service) => {
          const state = serviceState(service.key);
          return (
            <div className={styles.tile} key={service.key}>
              <span className={styles.tileIcon}>
                <service.icon size={20} strokeWidth={1.8} aria-hidden="true" />
              </span>
              <span className={styles.tileLabel}>{service.label}</span>
              <StatusBadge tone={state === "online" ? "success" : state === "offline" ? "danger" : "neutral"} dot>
                {state === "online" ? "Operativo" : state === "offline" ? "Sin conexión" : "Comprobando"}
              </StatusBadge>
              <p className={styles.tileBlurb}>{service.blurb}</p>
            </div>
          );
        })}
      </div>
    </div>
  );
}
