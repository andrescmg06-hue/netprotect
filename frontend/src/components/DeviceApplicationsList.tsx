"use client";

import { LayoutGrid, RefreshCw, Search, Smartphone } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { AppIcon, EmptyState, Input, Spinner, StatusBadge } from "@/components/ui";
import { ApiError, type DeviceApplication, listDeviceApplications } from "@/lib/apiClient";

import styles from "./DeviceApplicationsList.module.css";

type AppsState =
  | { kind: "loading" }
  | { kind: "loaded"; apps: DeviceApplication[] }
  | { kind: "error"; message: string };

function formatUsageDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) {
    return `${hours} h ${minutes} min`;
  }
  if (minutes > 0) {
    return `${minutes} min`;
  }
  return "< 1 min";
}

/** `usage_date` is an opaque YYYY-MM-DD label reported by the device (Sprint 7): format it as a
 * calendar date without letting the browser's time zone shift the day. */
function formatUsageDate(usageDate: string): string {
  const [year, month, day] = usageDate.split("-").map(Number);
  if (!year || !month || !day) return usageDate;
  return new Date(Date.UTC(year, month - 1, day)).toLocaleDateString("es-CO", {
    day: "numeric",
    month: "short",
    timeZone: "UTC",
  });
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("es-CO", { day: "numeric", month: "short", year: "numeric" });
}

/** Empty state drawing: a phone outline with an empty grid and the sync arrows beside it. */
function AppsIllustration() {
  return (
    <span className={styles.illustration} aria-hidden="true">
      <Smartphone className={styles.illusPhone} size={68} strokeWidth={1.1} />
      <LayoutGrid className={styles.illusGrid} size={22} strokeWidth={1.4} />
      <RefreshCw className={styles.illusSync} size={20} strokeWidth={1.5} />
    </span>
  );
}

function AppRow({ app, maxUsage }: { app: DeviceApplication; maxUsage: number }) {
  const seconds = app.latest_usage?.foreground_seconds ?? 0;
  const ratio = maxUsage > 0 ? seconds / maxUsage : 0;
  const uninstalled = Boolean(app.uninstalled_at);

  return (
    <li className={uninstalled ? `${styles.row} ${styles.rowMuted}` : styles.row}>
      <AppIcon label={app.app_label} seed={app.package_name} />
      <span className={styles.identity}>
        <span className={styles.appLabel}>{app.app_label}</span>
        <span className={styles.appPackage}>{app.package_name}</span>
      </span>
      <span className={styles.flags}>
        {app.is_system_app && <StatusBadge tone="neutral">Sistema</StatusBadge>}
        {uninstalled && app.uninstalled_at && (
          <span className={styles.uninstalledNote}>Desinstalada el {formatDate(app.uninstalled_at)}</span>
        )}
      </span>
      <span className={styles.usage}>
        {app.latest_usage ? (
          <>
            <span className={styles.usageValue}>{formatUsageDuration(seconds)}</span>
            <span className={styles.usageDate}>el {formatUsageDate(app.latest_usage.usage_date)}</span>
            <span className={styles.usageTrack} aria-hidden="true">
              <span className={styles.usageFill} style={{ transform: `scaleX(${ratio})` }} />
            </span>
          </>
        ) : (
          <span className={styles.usageNone}>Sin uso reportado</span>
        )}
      </span>
    </li>
  );
}

/** Sprint 34: adds metrics, local search and a relative usage bar over the same endpoint
 * (listDeviceApplications, Sprint 7) — sorting by usage and the "Desinstalada" badge are
 * unchanged from before, now built on DataTable instead of a plain <ul>.
 *
 * Sprint 56: back to a ruled list in the editorial direction. The three metric cards become one
 * sentence; uninstalled apps move to their own quieter section below the installed ones. Usage is
 * the latest day each app reported (its date is shown), never a sum across different days.
 */
export function DeviceApplicationsList({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [state, setState] = useState<AppsState>({ kind: "loading" });
  const [query, setQuery] = useState("");

  useEffect(() => {
    let cancelled = false;

    listDeviceApplications(accessToken, deviceId)
      .then(({ applications }) => {
        if (!cancelled) {
          setState({ kind: "loaded", apps: applications });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({
            kind: "error",
            message:
              error instanceof ApiError ? error.message : "No se pudo cargar la lista de apps",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId]);

  const apps = useMemo(() => (state.kind === "loaded" ? state.apps : []), [state]);
  const sorted = useMemo(
    () =>
      [...apps].sort(
        (a, b) => (b.latest_usage?.foreground_seconds ?? -1) - (a.latest_usage?.foreground_seconds ?? -1)
      ),
    [apps]
  );
  const maxUsage = sorted[0]?.latest_usage?.foreground_seconds ?? 0;
  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return sorted;
    return sorted.filter(
      (app) => app.app_label.toLowerCase().includes(needle) || app.package_name.toLowerCase().includes(needle)
    );
  }, [sorted, query]);

  const installed = filtered.filter((app) => !app.uninstalled_at);
  const uninstalled = filtered.filter((app) => app.uninstalled_at);
  const installedCount = apps.filter((app) => !app.uninstalled_at).length;
  const uninstalledCount = apps.length - installedCount;
  const latestUsageDate = apps.reduce<string | null>((latest, app) => {
    const date = app.latest_usage?.usage_date ?? null;
    return date && (!latest || date > latest) ? date : latest;
  }, null);

  if (state.kind === "loading") {
    return (
      <div className={styles.stateWrap}>
        <Spinner label="Cargando apps…" />
      </div>
    );
  }

  if (state.kind === "error") {
    return <p className={styles.error}>{state.message}</p>;
  }

  if (apps.length === 0) {
    return (
      <section className={styles.emptySection} aria-label="Inventario de apps">
        <EmptyState
          icon={LayoutGrid}
          illustration={<AppsIllustration />}
          title="Todavía no se sincronizó ninguna app"
          description="El inventario llega solo: cuando el teléfono supervisado sincronice con conexión a internet, aquí aparecerán sus apps instaladas y el tiempo de uso que reporte."
        />
      </section>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.head}>
        <div className={styles.headText}>
          <h2 className={styles.title}>Inventario</h2>
          <p className={styles.summary}>
            <strong>{installedCount}</strong> {installedCount === 1 ? "app instalada" : "apps instaladas"}
            {uninstalledCount > 0 && (
              <>
                {" "}
                · <strong>{uninstalledCount}</strong>{" "}
                {uninstalledCount === 1 ? "desinstalada" : "desinstaladas"}
              </>
            )}
            {latestUsageDate && <> · último uso reportado el {formatUsageDate(latestUsageDate)}</>}
          </p>
        </div>
        <div className={styles.search}>
          <Input
            icon={Search}
            type="search"
            placeholder="Buscar por nombre o paquete…"
            aria-label="Buscar aplicación"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
      </header>

      {filtered.length === 0 ? (
        <p className={styles.noResults}>Ninguna app coincide con «{query.trim()}».</p>
      ) : (
        <>
          {installed.length > 0 && (
            <section aria-label="Apps instaladas">
              <div className={styles.columnHeads} aria-hidden="true">
                <span>Aplicación</span>
                <span>Uso más reciente</span>
              </div>
              <ul className={styles.list}>
                {installed.map((app) => (
                  <AppRow key={app.package_name} app={app} maxUsage={maxUsage} />
                ))}
              </ul>
            </section>
          )}

          {uninstalled.length > 0 && (
            <section className={styles.uninstalledSection} aria-labelledby="apps-uninstalled-title">
              <h3 id="apps-uninstalled-title" className={styles.subtitle}>
                Desinstaladas
              </h3>
              <p className={styles.subtitleNote}>Ya no están en el teléfono; su registro se conserva.</p>
              <ul className={styles.list}>
                {uninstalled.map((app) => (
                  <AppRow key={app.package_name} app={app} maxUsage={maxUsage} />
                ))}
              </ul>
            </section>
          )}
        </>
      )}
    </div>
  );
}
