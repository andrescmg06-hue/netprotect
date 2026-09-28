"use client";

import { Ban, Clock, LayoutGrid, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import {
  AppIcon,
  Card,
  DataTable,
  EmptyState,
  Input,
  MetricCard,
  MetricGrid,
  Spinner,
  StatusBadge,
  type Column,
} from "@/components/ui";
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

/** Sprint 34: adds metrics, local search and a relative usage bar over the same endpoint
 * (listDeviceApplications, Sprint 7) — sorting by usage and the "Desinstalada" badge are
 * unchanged from before, now built on DataTable instead of a plain <ul>.
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

  const installedCount = apps.filter((app) => !app.uninstalled_at).length;
  const uninstalledCount = apps.length - installedCount;
  const totalSeconds = apps.reduce((sum, app) => sum + (app.latest_usage?.foreground_seconds ?? 0), 0);

  if (state.kind === "loading") {
    return (
      <Card>
        <Spinner label="Cargando apps…" />
      </Card>
    );
  }

  if (state.kind === "error") {
    return (
      <Card>
        <p className={styles.error}>{state.message}</p>
      </Card>
    );
  }

  const columns: Column<DeviceApplication>[] = [
    {
      key: "app",
      header: "Aplicación",
      primary: true,
      render: (app) => (
        <span className={styles.appCell}>
          <AppIcon label={app.app_label} seed={app.package_name} />
          <span className={styles.appText}>
            <span className={styles.appLabel}>{app.app_label}</span>
            <span className={styles.appPackage}>{app.package_name}</span>
          </span>
        </span>
      ),
    },
    {
      key: "usage",
      header: "Tiempo de uso",
      render: (app) => {
        const seconds = app.latest_usage?.foreground_seconds ?? 0;
        const ratio = maxUsage > 0 ? seconds / maxUsage : 0;
        return (
          <span className={styles.usageCell}>
            <span className={styles.usageBar}>
              <span className={styles.usageFill} style={{ width: `${Math.round(ratio * 100)}%` }} />
            </span>
            <span className={styles.usageText}>
              {app.latest_usage ? formatUsageDuration(seconds) : "Sin datos"}
            </span>
          </span>
        );
      },
    },
    {
      key: "status",
      header: "Estado",
      align: "right",
      render: (app) =>
        app.uninstalled_at ? (
          <StatusBadge tone="neutral">Desinstalada</StatusBadge>
        ) : (
          <StatusBadge tone="success">Instalada</StatusBadge>
        ),
    },
  ];

  return (
    <>
      <MetricGrid>
        <MetricCard icon={Clock} tone="purple" label="Tiempo total de uso" value={formatUsageDuration(totalSeconds)} />
        <MetricCard icon={LayoutGrid} tone="info" label="Apps instaladas" value={installedCount} />
        <MetricCard icon={Ban} tone="neutral" label="Apps desinstaladas" value={uninstalledCount} />
      </MetricGrid>

      <Card padding="none">
        <div className={styles.searchRow}>
          <Input
            icon={Search}
            placeholder="Buscar aplicación…"
            aria-label="Buscar aplicación"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>

        {apps.length === 0 ? (
          <div className={styles.emptyWrap}>
            <EmptyState
              icon={LayoutGrid}
              title="Todavía no se sincronizó ninguna app"
              description="Este dispositivo reportará sus apps instaladas la próxima vez que sincronice."
            />
          </div>
        ) : filtered.length === 0 ? (
          <p className={styles.noResults}>Sin resultados para “{query.trim()}”.</p>
        ) : (
          <DataTable columns={columns} rows={filtered} rowKey={(app) => app.package_name} />
        )}
      </Card>
    </>
  );
}
